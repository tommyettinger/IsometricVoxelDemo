package gdx.liftoff;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gdx.liftoff.game.LocalMap;
import gdx.liftoff.game.TestMap;

public class IsoEngine2D extends ApplicationAdapter {
    private SpriteBatch batch;
    private TextureAtlas tileset;
    private LocalMap map;
    private OrthographicCamera camera;
    private ScreenViewport viewport;
    private Array<Sprite> tiles;

    public static final String TILESET_FILE_NAME = "isometric-trpg.atlas";
    public static final int TILE_WIDTH = 8;
    public static final int TILE_HEIGHT = 4;
    public static final int TILE_DEPTH = 8;
    private static final int MAP_SIZE = 20;
    private static final int MAP_PEAK = 4;
    private static final int SCREEN_HORIZONTAL = MAP_SIZE * 2 * TILE_WIDTH;
    private static final int SCREEN_VERTICAL = (MAP_SIZE * 2 - 1) * TILE_HEIGHT + MAP_PEAK * TILE_DEPTH;
    private static final float CAMERA_ZOOM = 1f;

    private final Vector3 projectionTempVector = new Vector3();
    private final Vector3 isoTempVector = new Vector3();
    private final Vector3 faceTempVector = new Vector3();
    private final Vector2 screenTempVector = new Vector2();

    @Override
    public void create() {
        batch = new SpriteBatch();
        tileset = new TextureAtlas(TILESET_FILE_NAME);
        tiles = tileset.createSprites("tile");
        map = new TestMap(MAP_SIZE, MAP_SIZE, MAP_SIZE);

        camera = new OrthographicCamera(Gdx.graphics.getWidth() * CAMERA_ZOOM, Gdx.graphics.getHeight() * CAMERA_ZOOM);
        camera.position.set(0, 100, 0);
        camera.update();
        viewport = new ScreenViewport(camera);
    }

    @Override
    public void render() {
        handleInput();

        ScreenUtils.clear(.14f, .15f, .2f, 1f);
        batch.setProjectionMatrix(camera.combined);
        viewport.apply();
        batch.begin();

        for (int line = 0, maxLines = MAP_SIZE * 2 - 1; line <= maxLines; line++) {
            int span = Math.min(line + 1, maxLines - line);
            int offset = line + 1 - span >> 1;
            int f = Math.max(MAP_SIZE - 1 - line, 0);
            int g = MAP_SIZE - 1 - offset;
            for (int across = 0; across < span; across++) {
                g--;
                f++;
                for (int z = 0; z < MAP_PEAK; z++) {
                    int blockId = map.getTile(f, g, z);
                    if (blockId != -1) {
                        Vector2 pos = isoToScreen(f, g, z);
//                        int x = line * -TILE_WIDTH + (across + offset) * (TILE_WIDTH * 2);
//                        int y = (maxLines - line) * TILE_HEIGHT;
                        Sprite spr = tiles.get(blockId % tiles.size);
                        spr.setPosition(pos.x, pos.y);
                        spr.draw(batch);
                    }
                }
            }
        }


//        for (int z = 0; z < MAP_SIZE; z++) { // Draw lowest layers first
//            for (int sum = MAP_SIZE * 2 - 2; sum >= 0; sum--) { // Iterate in reverse
//                for (int x = 0; x < MAP_SIZE; x++) {
//                    int y = sum - x;
//                    if (y >= 0 && y < MAP_SIZE) {
//                        int blockId = map.getTile(x, y, z);
//                        if (blockId != -1) {
//                            Vector2 pos = isoToScreen(x, y, z);
//                            Sprite spr = tiles.get(blockId % tiles.size);
//                            spr.setPosition(pos.x, pos.y);
//                            spr.draw(batch);
//                        }
//                    }
//                }
//            }
//        }
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

            if (map.isValid(Math.round(targetBlock.x), Math.round(targetBlock.y), Math.round(targetBlock.z))) {
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                    map.setTile(Math.round(targetBlock.x), Math.round(targetBlock.y), Math.round(targetBlock.z), MathUtils.random(3));
                } else if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
                    map.setTile(Math.round(blockHit.x), Math.round(blockHit.y), Math.round(blockHit.z), -1);
                }
            }
        }
    }

    private Vector3 raycastToBlock(float screenX, float screenY) {
        Vector3 worldPos = camera.unproject(projectionTempVector.set(screenX, screenY, 0));

        // Check from highest to lowest Z
        for (int z = MAP_SIZE - 1; z >= 0; z--) {
            float h = z;
            float f = worldPos.y / TILE_HEIGHT + worldPos.x / TILE_WIDTH - h;
            float g = worldPos.y / TILE_HEIGHT - worldPos.x / TILE_WIDTH - h;
            int x = MathUtils.round(f);
            int y = MathUtils.round(g);
            if (x >= 0 && x < MAP_SIZE && y >= 0 && y < MAP_SIZE) {
                if (map.getTile(x, y, z) != -1) { // Found a solid block
                    Vector2 tilePos = isoToScreen(x, y, z);
                    float dx = tilePos.x - worldPos.x;
                    float dy = tilePos.y - worldPos.y;

                    // Check if click is inside tile bounds
                    if (Math.abs(dx) < TILE_WIDTH / 2f && Math.abs(dy) < TILE_HEIGHT / 2f) {
                        System.out.println("Valid block found at " + x + ", " + y + ", " + z);
                        return isoTempVector.set(x, y, z); // Return the first valid block found
                    }
                }
            }
        }

        // No block was hit, return ground level
        Vector3 groundCoords = screenToIso(worldPos.x, worldPos.y);
        return groundCoords.set(Math.round(groundCoords.x), Math.round(groundCoords.y), 0);
    }


    private Vector3 getClickedFace(Vector3 blockPos, float screenX, float screenY) {
        Vector2 tileCenter = isoToScreen(Math.round(blockPos.x), Math.round(blockPos.y), Math.round(blockPos.z));
        float localX = screenX - tileCenter.x;
        float localY = screenY - tileCenter.y;

        if (localY > TILE_DEPTH) {
            return faceTempVector.set(blockPos.x, blockPos.y, blockPos.z + 1); // Top face
        } else if (localX < TILE_WIDTH * -0.5f) {
            return faceTempVector.set(blockPos.x, blockPos.y + 1, blockPos.z); // Left face (Remove x shift)
        } else if (localX > TILE_WIDTH * 0.5f) {
            return faceTempVector.set(blockPos.x + 1, blockPos.y, blockPos.z); // Right face (Only adjust X)
        }

        return faceTempVector.set(blockPos); // Default to selecting the block itself
    }

    private Vector2 isoToScreen(int x, int y, int z) {
        float screenX = (x - y + 0.5f) * TILE_WIDTH;
        float screenY = (x + y + 0.5f) * TILE_HEIGHT + (z + 0.5f) * (TILE_DEPTH);
        return screenTempVector.set(screenX, screenY);
    }

    private Vector3 screenToIso(float screenX, float screenY) {
        float isoX = (screenX / (TILE_WIDTH) + screenY / (TILE_HEIGHT)) * 0.5f;
        float isoY = (screenY / (TILE_HEIGHT) - screenX / (TILE_WIDTH)) * 0.5f;
        return isoTempVector.set(isoX, isoY, 0);
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

    @Override
    public void resize(int width, int height) {
        // If unitsPerPixel are a fraction like 1f/2 or 1f/3, then that makes each pixel 2x or 3x the size, resp.
        // This will only divide 1f by an integer amount 1 or greater, which makes pixels always the exact right size.
        // This meant to fit an isometric map that is about MAP_SIZE by MAP_PEAK by MAP_SIZE, where MAP_PEAK is how many
        // layers of voxels can be stacked on top of each other.
        viewport.setUnitsPerPixel(1f / Math.max(1, (int)(Math.min(
            width / ((MAP_SIZE+1f) * TILE_WIDTH * 2f),
            height / ((MAP_SIZE+1f) * (TILE_HEIGHT * 2f) + TILE_DEPTH * MAP_PEAK)))));
        viewport.update(width, height);
    }
}

