package gdx.liftoff.game;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Vector4;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import gdx.liftoff.AnimatedIsoSprite;
import gdx.liftoff.LocalMap;

public class Player {
    public final Vector3 position = new Vector3();
    public final Vector3 velocity = new Vector3(0, 0, 0);
    public final Vector4 tempVectorA = new Vector4();
    private boolean isGrounded;

    private transient LocalMap map;

    private Array<Array<Animation<TextureAtlas.AtlasSprite>>> animations;
    public final int playerId;
    public transient float stateTime;
    private int currentDirection;

    private static final float GRAVITY = -0.5f; // multiplied by delta, which is expected to be about 1f/60f
    private static final float MAX_GRAVITY = -0.15f;
    private static final float JUMP_FORCE = 0.2f;
    private static final float MOVE_SPEED = 0.1f;
    private static final float PLAYER_SIZE = 1f;

    private transient final BoundingBox playerBox = new BoundingBox();
    private transient final BoundingBox tempBox = new BoundingBox();


    public AnimatedIsoSprite visual;

    public Player(LocalMap map, Array<Array<Animation<TextureAtlas.AtlasSprite>>> animations, int playerId,
                  float fPos, float gPos, float hPos) {
        this.map = map;
        this.position.set(fPos, gPos, hPos);
        this.stateTime = 0;
        this.currentDirection = 0; // Default: facing down
        this.playerId = playerId;

        this.animations = animations;

        visual = new AnimatedIsoSprite(animations.get(currentDirection).get(playerId), fPos, gPos, hPos);
    }

    public void update(float deltaTime) {
        stateTime += deltaTime;
        tempVectorA.set(position, 0);

        applyGravity(deltaTime);
        position.add(velocity);
        handleCollision();

        // while jumping, show attack animation; while standing, show idle animation.
        if (velocity.z != 0) {
            /* The "currentDirection + 2" gets an attack animation instead of an idle one for the appropriate facing. */
            visual.animation = animations.get(currentDirection + 2).get(playerId);
        } else {
            visual.animation = animations.get(currentDirection).get(playerId);
        }

        visual.setPosition(position);
        map.everything.remove(tempVectorA);
        map.everything.put(tempVectorA.set(position, 0f), visual);
    }

    private void applyGravity(float delta) {
        if (!isGrounded) {
            velocity.z = Math.max(velocity.z + GRAVITY * delta, MAX_GRAVITY); // Apply gravity to H axis (z in a Vector)
        }
    }

    public void jump() {
        if (isGrounded) {
            velocity.z = JUMP_FORCE; // Jump should affect H axis (heel to head, stored as z in a Vector)
            isGrounded = false;
        }
    }

    public void move(float df, float dg) {
        boolean movingDiagonally = (df != 0 && dg != 0);

        if (movingDiagonally) {
            // Normalize to maintain consistent movement speed
            float length = 1f / (float) Math.sqrt(df * df + dg * dg);
            df *= length;
            dg *= length;
        }

        velocity.x = df * MOVE_SPEED;
        velocity.y = dg * MOVE_SPEED;

        if (df == 0 && dg == 0) return;

        // Determine direction based on movement
        if (dg > 0 || df > 0) currentDirection = 1; // Up
        else currentDirection = 0; // Down
    }

    private void handleCollision() {
        isGrounded = false;

        // bottom of map
        final float groundLevel = 1f;
        if (position.z < groundLevel) {
            position.z = groundLevel;
            velocity.z = 0;
            isGrounded = true;
        }

        playerBox.min.set(position.x, position.y, position.z);
        playerBox.max.set(position.x + 1, position.y + 1, position.z + 1);
        playerBox.update();
        // make tempBox invalid (all coordinates about -1000000000) if we don't encounter a lower tile.
        tempBox.min.set(-2E9f, -2E9f, -2E9f);
        tempBox.max.set(-1E9f, -1E9f, -1E9f);
        tempBox.update();
        // Here, we look for any lower-elevation tile in the four possible tiles below the player.
        // If any are solid, tempBox is set to a 2x2x1 tile area below the player.
        LATERAL:
        for (int f = 0; f <= 1; f++) {
            for (int g = 0; g <= 1; g++) {
                if (map.getTile(position.x + f, position.y + g, position.z - 1) != -1) {
                    tempBox.min.set(MathUtils.round(position.x - 0.5f), MathUtils.round(position.y - 0.5f), MathUtils.round(position.z - 1));
                    tempBox.max.set(MathUtils.round(position.x + 1.5f), MathUtils.round(position.y + 1.5f), MathUtils.round(position.z));
                    tempBox.update();
                    // If tempBox was set to a 2x2x1 area below the player, then we check if the player intersects
                    // (or even just touched) that floor area. If they do touch or intersect, the player snaps to stand
                    // on the area, h-movement (velocity.z) is stopped, isGrounded is true (enabling jumping) and the
                    // player's position for future collisions is updated.
                    if (playerBox.intersects(tempBox)) {
                        position.z = tempBox.max.z; // Snap player to be standing on colliding tile
                        velocity.z = 0;
                        isGrounded = true;
                        playerBox.min.set(position.x, position.y, position.z);
                        playerBox.max.set(position.x + 1, position.y + 1, position.z + 1);
                        playerBox.update();
                    }
                    // We only want to do this step once, if any lower tile can be stood on.
                    break LATERAL;
                }
            }
        }

        // tile collision from the side
        if (map.getTile(position.x + 1, position.y, position.z) != -1) {
            int lo = MathUtils.round(position.x + 1);
            int hi = MathUtils.round(position.x + 2);

            if (playerBox.max.x > lo && playerBox.max.x < hi) {
                position.x = lo - 1;
                velocity.x *= -0.25f;
            }
        }
        if (map.getTile(position.x, position.y + 1, position.z) != -1) {
            int lo = MathUtils.round(position.y + 1);
            int hi = MathUtils.round(position.y + 2);

            if (playerBox.max.y > lo && playerBox.max.y < hi) {
                position.y = lo - 1;
                velocity.y *= -0.25f;
            }
        }

        if (map.getTile(position.x - 1, position.y, position.z) != -1) {
            int lo = MathUtils.round(position.x - 1);
            int hi = MathUtils.round(position.x    );

            if (playerBox.min.x > lo && playerBox.min.x < hi) {
                position.x = hi;
                velocity.x *= -0.25f;
            }
        }

        if (map.getTile(position.x, position.y - 1, position.z) != -1) {
            int lo = MathUtils.round(position.y - 1);
            int hi = MathUtils.round(position.y    );

            if (playerBox.min.y > lo && playerBox.min.y < hi) {
                position.y = hi;
                velocity.y *= -0.25f;
            }
        }
    }

    public LocalMap getMap() {
        return map;
    }

    public void setMap(LocalMap map) {
        this.map = map;
    }

    public int getCurrentDirection() {
        return currentDirection;
    }

    public void setCurrentDirection(int currentDirection) {
        this.currentDirection = currentDirection;
    }

    public Player place() {
        map.setEntity(position.x, position.y, position.z, visual);
        return this;
    }
}
