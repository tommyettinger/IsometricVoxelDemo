package gdx.liftoff.game;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import gdx.liftoff.IsoEngine3D;

public class Player {
    public final Vector3 position = new Vector3();
    public final Vector3 velocity = new Vector3(0, 0, 0);
    private boolean isGrounded;

    private transient LocalMap map;

    private Array<Array<Animation<Sprite>>> animations;
    public final int playerId;
    public transient float stateTime;
    private int currentDirection;

    private static final float GRAVITY = -0.5f; // multiplied by delta, which is expected to be about 1f/60f
    private static final float MAX_GRAVITY = -0.15f;
    private static final float JUMP_FORCE = 0.25f;
    private static final float MOVE_SPEED = 0.03f;
    private static final float PLAYER_SIZE = 1f;

    private transient final BoundingBox playerBox = new BoundingBox();
    private transient final BoundingBox tempBox = new BoundingBox();


    private Decal playerDecal;

    public Player(LocalMap map, Array<Array<Animation<Sprite>>> animations, int playerId) {
        this.map = map;
        this.position.set(0, 0, map.getHSize() - 1);
        this.stateTime = 0;
        this.currentDirection = 0; // Default: facing down
        this.playerId = playerId;

        this.animations = animations;

        // Initialize the decal
        playerDecal = Decal.newDecal(1f, 1f, animations.get(currentDirection).get(playerId).getKeyFrame(stateTime), true);
    }

    public void update(float deltaTime) {
        stateTime += deltaTime;

        applyGravity(deltaTime);
        position.add(velocity);
        handleCollision();

        // Update animation frame
        TextureRegion currentFrame;
        // while jumping, show attack animation; while standing, show idle animation.
        if (velocity.z != 0) {
            /* The "currentDirection + 2" gets an attack animation instead of an idle one for the appropriate facing. */
            currentFrame = animations.get(currentDirection + 2).get(playerId).getKeyFrame(stateTime, true);
        } else {
            currentFrame = animations.get(currentDirection).get(playerId).getKeyFrame(stateTime, true);
        }

        // Update decal
        playerDecal.setTextureRegion(currentFrame);
        playerDecal.setPosition(position);
        playerDecal.setRotation(IsoEngine3D.getInstance().camera.direction, IsoEngine3D.getInstance().camera.up); // Billboard effect
    }

    public void render(DecalBatch batch) {
        batch.add(playerDecal);
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

        playerBox.min.set(position.x - PLAYER_SIZE * 0.5f, position.y - PLAYER_SIZE * 0.5f, position.z);
        playerBox.max.set(position.x + PLAYER_SIZE * 0.5f, position.y + PLAYER_SIZE * 0.5f, position.z + PLAYER_SIZE);

        for (int f = -1; f <= 1; f++) {
            for (int g = -1; g <= 1; g++) {
                for (int h = -1; h <= 0; h++) {
                    if (map.getTile(position.x + f, position.y + g, position.z + h) != -1) {
                        tempBox.min.set(position.x + f - 0.5f, position.y + g - 0.5f, position.z + h);
                        tempBox.max.set(position.x + f + 0.5f, position.y + g + 0.5f, position.z + h + 1f);
                        if (playerBox.intersects(tempBox)) {
                            if (h == -1) { // Check if falling onto a tile
                                position.z = tempBox.max.z; // Snap player to be standing on colliding tile
                                velocity.z = 0;
                                isGrounded = true;
                            } else { // tile collision from the side
                                int tileF = MathUtils.round(position.x + f);
                                int tileG = MathUtils.round(position.y + g);
                                if(position.x + 1 >= tileF) {
                                    position.x = tileF - 1;
                                    velocity.x = 0;
                                } else if(position.x - 1 <= tileF) {
                                    position.x = tileF + 1;
                                    velocity.x = 0;
                                }

                                if(position.y + 1 >= tileG) {
                                    position.y = tileG - 1;
                                    velocity.y = 0;
                                } else if(position.y - 1 <= tileG) {
                                    position.y = tileG + 1;
                                    velocity.y = 0;
                                }
                            }
                        }
                    }
                }
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
}
