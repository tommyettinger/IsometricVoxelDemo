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

    private static final float GRAVITY = -0.04f * 60;
    private static final float MAX_GRAVITY = -0.3f * 60;
    private static final float JUMP_FORCE = 0.6f * 60;
    private static final float MOVE_SPEED = 0.15f * 60;
    private static final float PLAYER_SIZE = 1f;

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
        tempVectorA.set(position, LocalMap.ENTITY_W);

        applyGravity(deltaTime);
        handleCollision(deltaTime);
//        position.add(velocity);
        position.mulAdd(velocity, deltaTime);

        // while jumping, show attack animation; while standing, show idle animation.
        if (velocity.z != 0) {
            /* The "currentDirection + 2" gets an attack animation instead of an idle one for the appropriate facing. */
            visual.animation = animations.get(currentDirection + 2).get(playerId);
        } else {
            visual.animation = animations.get(currentDirection).get(playerId);
        }

        visual.setPosition(position);
        map.everything.remove(tempVectorA);
        map.everything.put(tempVectorA.set(position, LocalMap.ENTITY_W), visual);
    }

    private void applyGravity(float delta) {
        if (!isGrounded) {
            velocity.z = Math.max(velocity.z + GRAVITY, MAX_GRAVITY); // Apply gravity to H axis (z in a Vector)
        }
    }

    public void jump(float delta) {
        if (isGrounded) {
            velocity.z = JUMP_FORCE; // Jump should affect H axis (heel to head, stored as z in a Vector)
            isGrounded = false;
        }
    }

    public void move(float df, float dg, float delta) {
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
        if (MathUtils.cosDeg(-45f - map.rotationDegrees) * dg - MathUtils.sinDeg(-45f - map.rotationDegrees) * df > 0.1f) currentDirection = 1; // Up
        else currentDirection = 0; // Down
    }

    private void handleCollision(float delta) {
        isGrounded = false;

        // bottom of map
        final float groundLevel = 1f;
        if (position.z < groundLevel) {
            position.z = groundLevel;
            velocity.z = 0;
            isGrounded = true;
        }

        boolean lateralCollision = false;
        // tile collision from the side, one axis
        if (velocity.x >= 0 &&
            (!map.isValid(position.x + 1, position.y, position.z) ||
                map.getTile(position.x + 1, position.y, position.z) != -1)) {
            int lo = MathUtils.round(position.x    );
            int hi = MathUtils.round(position.x + 1);

            if (position.x >= lo && position.x <= hi) {
                position.x = lo;
                velocity.x = 0;
                lateralCollision = true;
            }
        }
        if (velocity.y >= 0 &&
            (!map.isValid(position.x, position.y + 1, position.z) ||
                map.getTile(position.x, position.y + 1, position.z) != -1)) {
            int lo = MathUtils.round(position.y    );
            int hi = MathUtils.round(position.y + 1);

            if (position.y >= lo && position.y <= hi) {
                position.y = lo;
                velocity.y = 0;
                lateralCollision = true;
            }
        }
        if (velocity.x <= 0 &&
            (!map.isValid(position.x - 1, position.y, position.z) ||
            map.getTile(position.x - 1, position.y, position.z) != -1)) {
            int lo = MathUtils.round(position.x - 1);
            int hi = MathUtils.round(position.x    );

            if (position.x >= lo && position.x <= hi) {
                position.x = hi;
                velocity.x = 0;
                lateralCollision = true;
            }
        }
        if (velocity.y <= 0 &&
            (!map.isValid(position.x, position.y - 1, position.z) ||
                map.getTile(position.x, position.y - 1, position.z) != -1)) {
            int lo = MathUtils.round(position.y - 1);
            int hi = MathUtils.round(position.y    );

            if (position.y >= lo && position.y <= hi) {
                position.y = hi;
                velocity.y = 0;
                lateralCollision = true;
            }
        }

        // tile collision from the side, two axes
        if (velocity.x > 0 && velocity.y > 0 &&
            (!map.isValid(position.x + 1, position.y + 1, position.z) ||
                map.getTile(position.x + 1, position.y + 1, position.z) != -1)) {
            int loX = MathUtils.round(position.x    );
            int hiX = MathUtils.round(position.x + 1);
            int loY = MathUtils.round(position.y    );
            int hiY = MathUtils.round(position.y + 1);

            if (position.x >= loX && position.x <= hiX && position.y >= loY && position.y <= hiY) {
                position.x = loX;
                position.y = loY;
                velocity.x = 0;
                velocity.y = 0;
                lateralCollision = true;
            }
        }
        if (velocity.x > 0 && velocity.y < 0 &&
            (!map.isValid(position.x + 1, position.y - 1, position.z) ||
                map.getTile(position.x + 1, position.y - 1, position.z) != -1)) {
            int loX = MathUtils.round(position.x    );
            int hiX = MathUtils.round(position.x + 1);
            int loY = MathUtils.round(position.y - 1);
            int hiY = MathUtils.round(position.y    );

            if (position.x >= loX && position.x <= hiX && position.y >= loY && position.y <= hiY) {
                position.x = loX;
                position.y = hiY;
                velocity.x = 0;
                velocity.y = 0;
                lateralCollision = true;
            }
        }
        if (velocity.x < 0 && velocity.y > 0 &&
            (!map.isValid(position.x - 1, position.y + 1, position.z) ||
                map.getTile(position.x - 1, position.y + 1, position.z) != -1)) {
            int loX = MathUtils.round(position.x - 1);
            int hiX = MathUtils.round(position.x    );
            int loY = MathUtils.round(position.y    );
            int hiY = MathUtils.round(position.y + 1);

            if (position.x >= loX && position.x <= hiX && position.y >= loY && position.y <= hiY) {
                position.x = hiX;
                position.y = loY;
                velocity.x = 0;
                velocity.y = 0;
                lateralCollision = true;
            }
        }
        if (velocity.x < 0 && velocity.y < 0 &&
            (!map.isValid(position.x - 1, position.y - 1, position.z) ||
                map.getTile(position.x - 1, position.y - 1, position.z) != -1)) {
            int loX = MathUtils.round(position.x - 1);
            int hiX = MathUtils.round(position.x    );
            int loY = MathUtils.round(position.y - 1);
            int hiY = MathUtils.round(position.y    );

            if (position.x >= loX && position.x <= hiX && position.y >= loY && position.y <= hiY) {
                position.x = hiX;
                position.y = hiY;
                velocity.x = 0;
                velocity.y = 0;
                lateralCollision = true;
            }
        }


        // Here, we look for any lower-elevation tile in the four possible tiles below the player.
        // If any are solid, tempBox is set to a 1x1x1 or 2x2x1 tile area below the player.
        if (   map.getTile(position.x - 0.5f, position.y - 0.5f, position.z - 1) != -1
            || map.getTile(position.x - 0.5f, position.y + 0.5f, position.z - 1) != -1
            || map.getTile(position.x + 0.5f, position.y - 0.5f, position.z - 1) != -1
            || map.getTile(position.x + 0.5f, position.y + 0.5f, position.z - 1) != -1
        ) {
//                    tempBox.min.set(MathUtils.floor(position.x)  , MathUtils.floor(position.y)  , MathUtils.round(position.z - 1));
//                    tempBox.max.set(MathUtils.ceil (position.x)+1, MathUtils.ceil (position.y)+1, MathUtils.round(position.z));
            if (velocity.z < 0) {

//                    tempBox.min.set(MathUtils.round(position.x - 0.5f), MathUtils.round(position.y - 0.5f), MathUtils.round(position.z - 1));
//                    tempBox.max.set(MathUtils.round(position.x + 1.5f), MathUtils.round(position.y + 1.5f), MathUtils.round(position.z    ));
//                    tempBox.update();
                // If tempBox was set to a 2x2x1 area below the player, then we check if the player intersects
                // (or even just touched) that floor area. If they do touch or intersect, the player snaps to stand
                // on the area, h-movement (velocity.z) is stopped, isGrounded is true (enabling jumping) and the
                // player's position for future collisions is updated.
//                    if (playerBox.intersects(tempBox)) {
//                        System.out.println("tempBox " + tempBox + " collided with playerBox " + playerBox);
//                    if (position.z != (int) position.z) {
                position.z = MathUtils.round(position.z);
//                    }
                isGrounded = true;
                velocity.z = 0;
                if(!lateralCollision) {
                    if     (map.getTile(position.x - 0.5f, position.y - 0.5f, position.z) != -1 ||
                            map.getTile(position.x - 0.5f, position.y + 0.5f, position.z) != -1 ||
                            map.getTile(position.x + 0.5f, position.y - 0.5f, position.z) != -1 ||
                            map.getTile(position.x + 0.5f, position.y + 0.5f, position.z) != -1) {
                        jump(delta);
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

    public Player place() {
        map.setEntity(position.x, position.y, position.z, visual);
        return this;
    }

    public static boolean intersectsExclusiveLateral (BoundingBox a, BoundingBox b) {
        if (!a.isValid() || !b.isValid()) return false;

        // test using SAT (separating axis theorem)

        float lx = Math.abs(a.getCenterX() - b.getCenterX());
        float sumX = (a.getWidth() / 2.0f) + (b.getWidth() / 2.0f);

        float ly = Math.abs(a.getCenterY() - b.getCenterY());
        float sumY = (a.getHeight() / 2.0f) + (b.getHeight() / 2.0f);

        float lz = Math.abs(a.getCenterZ() - b.getCenterZ());
        float sumZ = (a.getDepth() / 2.0f) + (b.getDepth() / 2.0f);

        return (lx < sumX && ly < sumY && lz <= sumZ);
    }

}
