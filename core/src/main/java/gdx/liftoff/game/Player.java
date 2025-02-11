package gdx.liftoff.game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import gdx.liftoff.IsoEngine3D;

public class Player {
    public Vector3 position;
    private Vector3 velocity;
    private boolean isGrounded;
    private Map map;

    private Decal playerDecal;
    private Texture spriteSheet;
    private TextureRegion[][] frames;
    private Animation<TextureRegion>[] walkAnimations;
    private TextureRegion[] idleFrames;
    private float stateTime;
    private int currentDirection;

    private static final float GRAVITY = -0.008f;
    private static final float MAX_GRAVITY = -0.15f;
    private static final float JUMP_FORCE = 0.25f;
    private static final float MOVE_SPEED = 0.03f;
    private static final float PLAYER_SIZE = 1f;
    private static final int FRAME_COLS = 8;
    private static final int FRAME_ROWS = 12;

    public Player(Map map) {
        this.map = map;
        this.position = new Vector3(0f, 5f, 2f);
        this.velocity = new Vector3(0, 0, 0);
        this.stateTime = 0;
        this.currentDirection = 4; // Default: facing down

        // Load sprite sheet
        spriteSheet = new Texture("Characters_by_AxulArt.png");
        frames = TextureRegion.split(spriteSheet, spriteSheet.getWidth() / FRAME_COLS, spriteSheet.getHeight() / FRAME_ROWS);

        // Extract animations
        walkAnimations = new Animation[8];
        idleFrames = new TextureRegion[8];

        for (int i = 0; i < 8; i++) {
            // Walking animations (3 frames from row 9 & 11)
            walkAnimations[i] = new Animation<>(0.1f, frames[9][i], frames[10][i], frames[11][i]);

            // Idle frames (middle row of bottom 3)
            idleFrames[i] = frames[10][i];
        }

        // Initialize the decal
        playerDecal = Decal.newDecal(1f, 1f, idleFrames[currentDirection], true);
    }

    public void update(float deltaTime) {
        stateTime += deltaTime;

        applyGravity();
        position.add(velocity);
        handleCollision();

        // Update animation frame
        TextureRegion currentFrame;
        if (velocity.x != 0 || velocity.z != 0) {
            currentFrame = walkAnimations[currentDirection].getKeyFrame(stateTime, true);
        } else {
            currentFrame = idleFrames[currentDirection];
        }

        // Update decal
        playerDecal.setTextureRegion(currentFrame);
        playerDecal.setPosition(position);
        playerDecal.setRotation(IsoEngine3D.getInstance().camera.direction, IsoEngine3D.getInstance().camera.up); // Billboard effect
    }

    public void render(DecalBatch batch) {
        batch.add(playerDecal);
    }

    private void applyGravity() {
        if (!isGrounded) {
            velocity.y = Math.max(velocity.y + GRAVITY, MAX_GRAVITY); // Apply gravity to Y (not Z)
        }
    }

    public void jump() {
        if (isGrounded) {
            velocity.y = JUMP_FORCE; // Jump should affect Y (height)
//            position.y += .1f;
            isGrounded = false;
        }
    }

    public void move(float dx, float dz) {
        boolean movingDiagonally = (dx != 0 && dz != 0);

        if (movingDiagonally) {
            // Scale movement based on tile proportions
            dx *= IsoEngine3D.TILE_RATIO;  // Scale x-movement to match tile proportions
            dz *= 1f;          // Keep z-movement unchanged (height already accounts for it)

            // Normalize to maintain consistent movement speed
            float length = (float) Math.sqrt(dx * dx + dz * dz);
            dx /= length;
            dz /= length;
        }

        velocity.x = dx * MOVE_SPEED;
        velocity.z = dz * MOVE_SPEED;

        if (dx == 0 && dz == 0) return;

        // Determine direction based on movement
        if (dx > 0 && dz > 0) currentDirection = 1; // Up-right
        else if (dx > 0 && dz < 0) currentDirection = 3; // Down-right
        else if (dx < 0 && dz > 0) currentDirection = 7; // Up-left
        else if (dx < 0 && dz < 0) currentDirection = 5; // Down-left
        else if (dx > 0) currentDirection = 2; // Right
        else if (dx < 0) currentDirection = 6; // Left
        else if (dz > 0) currentDirection = 0; // Up
        else if (dz < 0) currentDirection = 4; // Down
    }

    private void handleCollision() {
        isGrounded = false;

        // bottom of map
        float groundLevel = 1f;
        if (position.y < groundLevel) {
            position.y = groundLevel;
            velocity.y = 0;
            isGrounded = true;
        }

        BoundingBox playerBox = new BoundingBox(
            new Vector3(position.x - PLAYER_SIZE / 2, position.y, position.z - PLAYER_SIZE / 2),
            new Vector3(position.x + PLAYER_SIZE / 2, position.y + PLAYER_SIZE, position.z + PLAYER_SIZE / 2)
        );

        for (int y = 0; y < map.getYSize(); y++) {
            for (int x = 0; x < map.getXSize(); x++) {
                for (int z = 0; z < map.getZSize(); z++) {
                    if (map.getTile(x, y, z) != -1) {
                        BoundingBox blockBox = new BoundingBox(
                            new Vector3(x, y, z),
                            new Vector3(x + 1, y + 1, z + 1)
                        );

                        if (playerBox.intersects(blockBox)) {
                            if (position.y > y) { // Check if falling onto a tile
                                position.y = y + 1; // Snap player to tile height
                                velocity.y = 0;
                                isGrounded = true;
                            } /*else { // tile collision from the side
                                velocity.x = 0;
                                velocity.z = 0;
                            }*/
                        }
                    }
                }
            }
        }
    }
}
