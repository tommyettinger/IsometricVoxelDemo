package gdx.liftoff.game;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Vector4;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import gdx.liftoff.AnimatedIsoSprite;
import gdx.liftoff.Main;
import gdx.liftoff.LocalMap;
import gdx.liftoff.util.HasPosition3D;
import gdx.liftoff.util.MiniNoise;

public class Mover implements HasPosition3D {
    private final Vector3 position = new Vector3();
    public final Vector3 velocity = new Vector3(0, 0, 0);
    private final Vector4 tempVectorA = new Vector4();
    public boolean isGrounded;
    public final int id;

    public final boolean npc;

    private transient LocalMap map;

    private final Array<Array<Animation<TextureAtlas.AtlasSprite>>> animations;
    public final int animationID;
    private transient float accumulator;
    private transient float totalMoveTime = 0f;
    private transient float invincibilityEndTime = -100f;
    private int currentDirection;

    public int health = 3;

    private static final float GRAVITY = -0.04f;
    private static final float MAX_GRAVITY = -0.3f;
    private static final float JUMP_FORCE = 0.6f;
    public static final float MOVE_SPEED = 0.15f;
    public static final float NPC_MOVE_SPEED = 0.07f;

    private static int ID_COUNTER = 1;

    public AnimatedIsoSprite visual;

    public Mover(LocalMap map, Array<Array<Animation<TextureAtlas.AtlasSprite>>> animations, int playerId,
                 float fPos, float gPos, float hPos) {
        this.map = map;
        this.position.set(fPos, gPos, hPos);
        this.accumulator = 0;
        this.currentDirection = 0; // Default: facing down
        this.animationID = playerId;

        this.animations = animations;

        visual = new AnimatedIsoSprite(animations.get(currentDirection).get(playerId), fPos, gPos, hPos);
        id = ID_COUNTER++;
        npc = id > 1;
    }

    public void update(float deltaTime) {
        totalMoveTime += deltaTime;
        if(npc){
            float df = MiniNoise.PerlueNoise.instance.getNoiseWithSeed(totalMoveTime * 1.7548f, id);
            float dg = MiniNoise.PerlueNoise.instance.getNoiseWithSeed(totalMoveTime * 1.5698f, ~id);
            float c = map.cosRotation;
            float s = map.sinRotation;
            float rf = c * df + s * dg;
            float rg = c * dg - s * df;

            if(isGrounded && df * df + dg * dg > 0.5f) jump();
            move(rf, rg, NPC_MOVE_SPEED);
        }
        accumulator += deltaTime;
        while (accumulator > (1f/60f)) {
            accumulator -= (1f / 60f);
            tempVectorA.set(position, Main.PLAYER_W);

            applyGravity();
            handleCollision();
            position.add(velocity);

            // while jumping, show attack animation; while standing, show idle animation. NPCs are always attacking.
            if (npc || velocity.z != 0) {
                /* The "currentDirection + 2" gets an attack animation instead of an idle one for the appropriate facing. */
                visual.animation = animations.get(currentDirection + 2).get(animationID);
            } else {
                visual.animation = animations.get(currentDirection).get(animationID);
            }

            visual.setPosition(position);
            map.everything.remove(tempVectorA);
            map.everything.put(tempVectorA.set(position, Main.PLAYER_W), visual);
            if(totalMoveTime < invincibilityEndTime)
                visual.sprite.setAlpha(Math.min(Math.max(MathUtils.sin(totalMoveTime * 100f) * 0.75f + 0.5f, 0f), 1f));
            else
                visual.sprite.setAlpha(1f);
        }
    }

    private void applyGravity() {
        if (!isGrounded) {
            velocity.z = Math.max(velocity.z + GRAVITY, MAX_GRAVITY); // Apply gravity to H axis (z in a Vector)
        }
    }

    public void jump() {
        if (isGrounded) {
            velocity.z = JUMP_FORCE; // Jump should affect H axis (heel to head, stored as z in a Vector)
            isGrounded = false;
        }
    }

    public void takeDamage() {
        if(totalMoveTime < invincibilityEndTime) return;
        health--;
        if(health <= 0) {
            if(npc) map.movers.entities.removeValue(this, true);
        } else {
            invincibilityEndTime = totalMoveTime + 2f;
        }
    }

    public void move(float df, float dg, float speed) {
        boolean movingDiagonally = (df != 0 && dg != 0);

        if (movingDiagonally) {
            // Normalize to maintain consistent movement speed
            float length = 1f / (float) Math.sqrt(df * df + dg * dg);
            df *= length;
            dg *= length;
        }

        velocity.x = df * speed;
        velocity.y = dg * speed;

        if (df == 0 && dg == 0) return;

        // Determine direction based on movement
        if (MathUtils.cosDeg(-45f - map.rotationDegrees) * dg - MathUtils.sinDeg(-45f - map.rotationDegrees) * df > 0.1f) currentDirection = 1; // Up
        else currentDirection = 0; // Down
    }

    private void handleCollision() {
//        isGrounded = false;

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
                map.getTile(position.x + 1, position.y, position.z) != -1 ||
                map.checkCollision(this).notEmpty())) {
            int lo = MathUtils.round(position.x);
            int hi = MathUtils.round(position.x + 1);

            if (position.x >= lo && position.x <= hi) {
                position.x = lo;
                velocity.x = 0;
                lateralCollision = true;
            }
            if(!npc && map.movers.colliding.notEmpty()) takeDamage();
        }
        if (velocity.y >= 0 &&
            (!map.isValid(position.x, position.y + 1, position.z) ||
                map.getTile(position.x, position.y + 1, position.z) != -1 ||
                map.checkCollision(this).notEmpty())) {
            int lo = MathUtils.round(position.y    );
            int hi = MathUtils.round(position.y + 1);

            if (position.y >= lo && position.y <= hi) {
                position.y = lo;
                velocity.y = 0;
                lateralCollision = true;
            }
            if(!npc && map.movers.colliding.notEmpty()) takeDamage();
        }
        if (velocity.x <= 0 &&
            (!map.isValid(position.x - 1, position.y, position.z) ||
            map.getTile(position.x - 1, position.y, position.z) != -1 ||
                map.checkCollision(this).notEmpty())) {
            int lo = MathUtils.round(position.x - 1);
            int hi = MathUtils.round(position.x    );

            if (position.x >= lo && position.x <= hi) {
                position.x = hi;
                velocity.x = 0;
                lateralCollision = true;
            }
            if(!npc && map.movers.colliding.notEmpty()) takeDamage();
        }
        if (velocity.y <= 0 &&
            (!map.isValid(position.x, position.y - 1, position.z) ||
                map.getTile(position.x, position.y - 1, position.z) != -1 ||
                map.checkCollision(this).notEmpty())) {
            int lo = MathUtils.round(position.y - 1);
            int hi = MathUtils.round(position.y    );

            if (position.y >= lo && position.y <= hi) {
                position.y = hi;
                velocity.y = 0;
                lateralCollision = true;
            }
            if(!npc && map.movers.colliding.notEmpty()) takeDamage();
        }

        // tile collision from the side, two axes
        if (velocity.x > 0 && velocity.y > 0 &&
            (!map.isValid(position.x + 1, position.y + 1, position.z) ||
                map.getTile(position.x + 1, position.y + 1, position.z) != -1 ||
                map.checkCollision(this).notEmpty())) {
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
            if(!npc && map.movers.colliding.notEmpty()) takeDamage();
        }
        if (velocity.x > 0 && velocity.y < 0 &&
            (!map.isValid(position.x + 1, position.y - 1, position.z) ||
                map.getTile(position.x + 1, position.y - 1, position.z) != -1 ||
                map.checkCollision(this).notEmpty())) {
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
            if(!npc && map.movers.colliding.notEmpty()) takeDamage();
        }
        if (velocity.x < 0 && velocity.y > 0 &&
            (!map.isValid(position.x - 1, position.y + 1, position.z) ||
                map.getTile(position.x - 1, position.y + 1, position.z) != -1 ||
                map.checkCollision(this).notEmpty())) {
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
            if(!npc && map.movers.colliding.notEmpty()) takeDamage();
        }
        if (velocity.x < 0 && velocity.y < 0 &&
            (!map.isValid(position.x - 1, position.y - 1, position.z) ||
                map.getTile(position.x - 1, position.y - 1, position.z) != -1 ||
                map.checkCollision(this).notEmpty())) {
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
            if(!npc && map.movers.colliding.notEmpty()) takeDamage();
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
                        jump();
                    }
                }
            }
        } else {
            isGrounded = false;
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

    public Mover place(float depth) {
        map.setEntity(position.x, position.y, position.z, depth, visual);
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

    public Vector3 getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return "Mover { visual: " +  visual + ", type: " + AssetData.ENTITIES.findKey(animationID) + " }";
    }
}
