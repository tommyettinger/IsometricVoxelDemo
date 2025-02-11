package gdx.liftoff;

import com.badlogic.gdx.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Array;
import gdx.liftoff.game.Map;
import gdx.liftoff.game.TestMap;

import java.util.Random;

public class IsoEngine2D extends ApplicationAdapter {
    private SpriteBatch batch;
    private TextureAtlas tileset;
    private Map map;
    private OrthographicCamera camera;
    private Vector3 selectedTile;
    private Array<Sprite> tiles;

    public static final String TILESET_FILE_NAME = "isometric-trpg.atlas";
    public static final int TILE_WIDTH = 8;
    public static final int TILE_HEIGHT = 4;
    public static final int TILE_DEPTH = 8;
    private static final int MAP_SIZE = 20;
    private static final int TILESET_COLUMNS = 18;
    private static final float CAMERA_ZOOM = .25f;

    @Override
    public void create() {
        batch = new SpriteBatch();
        tileset = new TextureAtlas(TILESET_FILE_NAME);
        tiles = tileset.createSprites("tile");
        map = new TestMap(MAP_SIZE, MAP_SIZE, MAP_SIZE);

        camera = new OrthographicCamera(Gdx.graphics.getWidth() * CAMERA_ZOOM, Gdx.graphics.getHeight() * CAMERA_ZOOM);
        camera.position.set(0, 100, 0);
        camera.update();
    }

    @Override
    public void render() {
        handleInput();

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glClearColor(.14f, .15f, .2f, 1f);
//        Gdx.gl.glClearColor(.1f, .9f, 1f, 1f);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        for (int z = 0; z < MAP_SIZE; z++) { // Draw lowest layers first
            for (int sum = MAP_SIZE * 2 - 2; sum >= 0; sum--) { // Iterate in reverse
                for (int x = 0; x < MAP_SIZE; x++) {
                    int y = sum - x;
                    if (y >= 0 && y < MAP_SIZE) {
                        int blockId = map.getTile(x, y, z);
                        if (blockId != -1) {
                            Vector2 pos = isoToScreen(x, y, z);
                            Sprite spr = tiles.get(blockId % tiles.size);
                            spr.setPosition(pos.x, pos.y);
                            spr.draw(batch);
                        }
                    }
                }
            }
        }
        batch.end();
    }

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            reset();
            return;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.W)) camera.translate(0, 5);
        if (Gdx.input.isKeyPressed(Input.Keys.S)) camera.translate(0, -5);
        if (Gdx.input.isKeyPressed(Input.Keys.A)) camera.translate(-5, 0);
        if (Gdx.input.isKeyPressed(Input.Keys.D)) camera.translate(5, 0);
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) camera.zoom += .25f;
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) camera.zoom -= .25f;

        camera.update();

        if (Gdx.input.justTouched()) {
            Vector3 blockHit = raycastToBlock(Gdx.input.getX(), Gdx.input.getY());
            Vector3 targetBlock = getClickedFace(blockHit, Gdx.input.getX(), Gdx.input.getY());

            System.out.println("blockHit " + blockHit);
//            System.out.println("targetBlock " + targetBlock);

            if (map.isValid((int) targetBlock.x, (int) targetBlock.y, (int) targetBlock.z)) {
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                    map.setTile((int) targetBlock.x, (int) targetBlock.y, (int) targetBlock.z, new Random().nextInt(7));
                } else if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
                    map.setTile((int) blockHit.x, (int) blockHit.y, (int) blockHit.z, -1);
                }
            }
        }
    }

    private Vector3 raycastToBlock(float screenX, float screenY) {
        Vector3 worldPos = camera.unproject(new Vector3(screenX, screenY, 0));

        // Check from highest to lowest Z
        for (int z = MAP_SIZE - 1; z >= 0; z--) {
            for (int sum = MAP_SIZE * 2 - 2; sum >= 0; sum--) {
                for (int x = MAP_SIZE - 1; x >= 0; x--) { // Reverse X order
                    int y = sum - x;
                    if (y >= 0 && y < MAP_SIZE) {
                        if (map.getTile(x, y, z) != -1) { // Found a solid block
                            Vector2 tilePos = isoTransform(x, y, z);
                            float dx = worldPos.x - tilePos.x;
                            float dy = worldPos.y - tilePos.y;

                            // Check if click is inside tile bounds
                            if (Math.abs(dx) < TILE_WIDTH / 2f && Math.abs(dy) < TILE_HEIGHT / 2f) {
                                return new Vector3(x-1, y, z); // Return the first valid block found
                            }
                        }
                    }
                }
            }
        }

        // No block was hit, return ground level
        Vector3 groundCoords = screenToIso(worldPos.x, worldPos.y);
        int groundX = Math.round(groundCoords.x);
        int groundY = Math.round(groundCoords.y);

        return new Vector3(groundX, groundY, 0);
    }


    private Vector3 getClickedFace(Vector3 blockPos, float screenX, float screenY) {
        Vector2 tileCenter = isoTransform((int) blockPos.x, (int) blockPos.y, (int) blockPos.z);
        float localX = screenX - tileCenter.x;
        float localY = screenY - tileCenter.y;

        if (localY > TILE_HEIGHT * 0.3f) {
            return new Vector3(blockPos.x, blockPos.y, blockPos.z + 1); // Top face
        } else if (localX < -TILE_WIDTH * 0.25f) {
            return new Vector3(blockPos.x, blockPos.y + 1, blockPos.z); // Left face (Remove x shift)
        } else if (localX > TILE_WIDTH * 0.25f) {
            return new Vector3(blockPos.x + 1, blockPos.y, blockPos.z); // Right face (Only adjust X)
        }

        return blockPos; // Default to selecting the block itself
    }

    private Vector2 isoToScreen(int x, int y, int z) {
        float screenX = (x - y) * TILE_WIDTH;
        float screenY = (x + y) * TILE_HEIGHT + z * (TILE_DEPTH);
        return new Vector2(screenX, screenY);
    }

    private Vector2 isoTransform(int x, int y, int z) {
        return isoToScreen(x, y, z);
    }

    private Vector3 screenToIso(float screenX, float screenY) {
        float isoX = (screenX / (TILE_WIDTH) + screenY / (TILE_HEIGHT)) * 0.5f - 1;
        float isoY = (screenY / (TILE_HEIGHT) - screenX / (TILE_WIDTH)) * 0.5f;
        return new Vector3(isoX, isoY, 0);
    }

    private void reset() {
        dispose();
        create();
    }

    @Override
    public void dispose() {
        batch.dispose();
        tileset.dispose();
    }
}

