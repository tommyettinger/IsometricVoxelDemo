package gdx.liftoff.game;

import com.badlogic.gdx.Gdx;
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

/**
 * A creature, hero, or hazard that can move around of its own accord.
 * A Mover can be an {@link #npc} or not; NPCs move on their own in somewhat-random paths, while the player character
 * should be the only Mover with {@code npc = false}, and should be moved by the player's input.
 */
public class Mover implements HasPosition3D {
    /**
     * Can be retrieved with {@link #getPosition()}, which satisfies our interface requirement.
     */
    private final Vector3 position = new Vector3();
    /**
     * The direction this Mover is... moving in. Carries over between frames.
     */
    public final Vector3 velocity = new Vector3(0, 0, 0);
    /**
     * Used to make queries to {@link LocalMap#everything}, when we don't know if a position has anything at it.
     */
    private final Vector4 tempVectorA = new Vector4();
    /**
     * While "grounded" this Mover is stepping on the ground, preventing it from falling and allowing it to jump.
     */
    public boolean isGrounded;
    /**
     * The unique identifier for this Mover; this is usually a sequential int starting at 1, where 1 is the player only.
     */
    public final int id;

    /**
     * If true, this Mover will move on its own; if false, it depends on player input to move.
     */
    public final boolean npc;

    /**
     * Each Mover knows what map it is on, and uses this to check for collisions with terrain and other Movers.
     */
    private transient LocalMap map;

    /**
     * Typically calculated at game start in the main game class, this is an Array of four groups of sprite Animations,
     * with each group referring to a different facing ({@link #currentDirection}) and whether it is attacking.
     */
    private final Array<Array<Animation<TextureAtlas.AtlasSprite>>> animations;
    /**
     * The index into the inner Arrays of {@link #animations}, determining which creature this appears as.
     */
    public final int animationIndex;
    /**
     * Goes up until it reaches a threshold, then physics steps run a number of times based on how much the accumulator
     * went past that threshold. A common concept in game physics.
     */
    private transient float accumulator;
    /**
     * The amount of time, in seconds, this Mover has been on-screen and able to move. Used for both the invincibility
     * flash for the player when damaged, and to determine the meandering path of NPCs.
     */
    private transient float totalMoveTime = 0f;
    /**
     * If the player is invincible due to just taking damage, this timestamp (in seconds) will be greater than
     * {@link #totalMoveTime}. The player is also invincible when they first spawn.
     */
    private transient float invincibilityEndTime = -100f;
    /**
     * An int denoting the current facing direction of the sprite and whether it is attacking. 0 and 1 are facing down
     * and up, respectively, without attacking, and 2 and 3 are down and up, respectively, while attacking.
     */
    private int currentDirection;

    /**
     * This only matters for the player currently, but it determines how many times they can take damage.
     */
    public int health = 3;

    // These constants are hard to adjust, but can be changed to fit your game, very carefully.
    private static final float GRAVITY = -0.04f;
    private static final float MAX_GRAVITY = -0.3f;
    private static final float JUMP_FORCE = 0.6f;
    public static final float MOVE_SPEED = 0.15f;
    public static final float NPC_MOVE_SPEED = 0.07f;

    /**
     * Goes up every time an {@link #id} needs to be assigned, and is reset to 0 if the map resets.
     */
    public static int ID_COUNTER = 1;

    /**
     * The animated sprite used as the visual representation of this Mover; also has an isometric tile position.
     */
    public AnimatedIsoSprite visual;

    /**
     * Creates a Mover in the given LocalMap, drawing an index from animations, and using the given f,g,h position.
     * @param map a LocalMap that this Mover exists in
     * @param animations typically created in the main game class from a TextureAtlas
     * @param index the index into the inner Array of animations
     * @param fPos isometric tile f-coordinate
     * @param gPos isometric tile g-coordinate
     * @param hPos isometric tile h-coordinate
     */
    public Mover(LocalMap map, Array<Array<Animation<TextureAtlas.AtlasSprite>>> animations, int index,
                 float fPos, float gPos, float hPos) {
        this.map = map;
        this.position.set(fPos, gPos, hPos);
        this.accumulator = 0;
        this.currentDirection = 0; // Default: facing down
        this.animationIndex = index;

        this.animations = animations;

        visual = new AnimatedIsoSprite(animations.get(currentDirection).get(index), fPos, gPos, hPos);
        id = ID_COUNTER++;
        npc = id > 1;
        if(!npc) invincibilityEndTime = totalMoveTime + 2f;
    }

    /**
     * Updates this Mover's movement, physics, appearance, and for NPCs, "AI" as much as it can be called that.
     * @param deltaTime the amount of time in seconds since the last update
     */
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
                visual.animation = animations.get(currentDirection + 2).get(animationIndex);
            } else {
                visual.animation = animations.get(currentDirection).get(animationIndex);
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

    /**
     * If this Mover is midair, makes their velocity go more negative unless it has already reached terminal velocity.
     */
    private void applyGravity() {
        if (!isGrounded) {
            velocity.z = Math.max(velocity.z + GRAVITY, MAX_GRAVITY); // Apply gravity to H axis (z in a Vector)
        }
    }

    /**
     * If this Mover is on the ground, makes their velocity suddenly spike upward, and makes them no longer grounded.
     */
    public void jump() {
        if (isGrounded) {
            velocity.z = JUMP_FORCE; // Jump should affect H axis (heel to head, stored as z in a Vector)
            isGrounded = false;
        }
    }

    /**
     * Mostly meant for the player at this point, this checks if the player is currently invincible, and if they aren't,
     * their health goes down by one, and they become invincible for two seconds. This also updates the health label
     * with {@link Main#updateHealth()}.
     */
    public void takeDamage() {
        if(totalMoveTime < invincibilityEndTime) return;
        health--;
        if(health <= 0) {
            if(npc) map.movers.entities.removeValue(this, true);
        } else {
            invincibilityEndTime = totalMoveTime + 2f;
        }
        if(!npc) ((Main) Gdx.app.getApplicationListener()).updateHealth();
    }

    /**
     * Changes the velocity of this Mover in the given direction (as df and dg) at the given speed (which is a constant
     * rate usually). The velocity is considered by {@link #update(float)}, which allows part of it to move the player
     * based on delta time for that physics update.
     * @param df the direction on the isometric tile f-axis
     * @param dg the direction on the isometric tile g-axis
     * @param speed how fast the movement should be
     */
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

    /**
     * Every {@link #update(float)}, this checks for collisions before finally moving a Mover to its next location.
     * If a collision occurs, the movement is refused. If a collision occurs between movers and this is the player, then
     * the player takes damage.
     */
    private void handleCollision() {
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
            if (velocity.z < 0) {

                position.z = MathUtils.round(position.z);
                isGrounded = true;
                velocity.z = 0;
                // If we start to overlap with another tile, force a jump to avoid an unwanted collision.
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

    /**
     * Puts this Mover into {@link LocalMap#everything} at the given depth modifier, such as {@link Main#PLAYER_W}.
     * @param depth a depth modifier like {@link Main#NPC_W} or {@link Main#FISH_W}
     * @return this Mover, for chaining
     */
    public Mover place(float depth) {
        map.setEntity(position.x, position.y, position.z, depth, visual);
        return this;
    }

    /**
     * Not currently used, but this can test two BoundingBoxes to see if they are not touching at all on x and y (they
     * must not be overlapping or equal), but if z is overlapping or equal this considers it to be intersecting.
     * @param a a BoundingBox
     * @param b a BoundingBox
     * @return true if a and b overlap on x or y, and either touch or overlap on z
     */
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
        return "Mover { visual: " +  visual + ", type: " + AssetData.ENTITIES.findKey(animationIndex) + " }";
    }
}
