package gdx.liftoff;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.GridPoint3;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gdx.liftoff.game.AnimatedIsoSprite;
import gdx.liftoff.game.AssetData;
import gdx.liftoff.game.IsoSprite;
import gdx.liftoff.game.LocalMap;

import java.util.Comparator;

public class IsoEngine2D extends ApplicationAdapter {
    private SpriteBatch batch;
    private TextureAtlas tileset;
    private Array<Array<Animation<TextureAtlas.AtlasSprite>>> animations;
    private LocalMap map;
    private OrthographicCamera camera;
    private ScreenViewport viewport;

    public static final String TILESET_FILE_NAME = "isometric-trpg.atlas";
    public static final int TILE_WIDTH = 8;
    public static final int TILE_HEIGHT = 4;
    public static final int TILE_DEPTH = 8;
    public static final int MAP_SIZE = 20;
    public static final int MAP_PEAK = 4;
    public static final int SCREEN_HORIZONTAL = MAP_SIZE * 2 * TILE_WIDTH;
    public static final int SCREEN_VERTICAL = MAP_SIZE * 2 * TILE_HEIGHT + MAP_PEAK * TILE_DEPTH;
    public float mapCenter = (MAP_SIZE - 1f) * 0.5f;

    public float CAMERA_ZOOM = 1f;
    public float rotationDegrees = 0f, previousRotation = 0f, targetRotation = 0f;
    public long startTime, animationStart = -1000000L;

    private static final Vector3 projectionTempVector = new Vector3();
    private static final Vector3 isoTempVector = new Vector3();
    private static final Vector2 screenTempVector = new Vector2();
    private static final GridPoint3 tempPointA = new GridPoint3();
    private static final GridPoint3 tempPointB = new GridPoint3();
    private static final GridPoint3 tempPointC = new GridPoint3();

    /**
     * Used to depth-sort isometric points, including if the map is mid-rotation. This requires {@link #mapCenter}
     * to be set to the center point on the floor of a map that is assumed square, and permits {@link #rotationDegrees}
     * to be any finite value in degrees.
     * <br>
     * Internally, this uses {@link NumberUtils#floatToIntBits(float)} instead of {@link Float#compare(float, float)}
     * because it still returns a completely valid comparison value (it only distinguishes between an int that is
     * positive, negative, or zero), and seems a tiny bit faster. To avoid {@code -0.0f} being treated as a negative
     * comparison value, this adds {@code 0.0f} to the difference of the two compared depths. This is absolutely a magic
     * trick, and it is probably unnecessary and gratuitous!
     */
    public final Comparator<? super GridPoint3> comparator =
        (a, b) -> NumberUtils.floatToIntBits(
            IsoSprite.viewDistance(a.x, a.y, a.z, mapCenter, mapCenter, rotationDegrees) -
                IsoSprite.viewDistance(b.x, b.y, b.z, mapCenter, mapCenter, rotationDegrees) + 0.0f);

    // The above is equivalent to:
//    public final Comparator<? super GridPoint3> comparator =
//        (a, b) -> Float.compare(
//            IsoSprite.viewDistance(a.x, a.y, a.z, MAP_CENTER, MAP_CENTER, rotationDegrees),
//            IsoSprite.viewDistance(b.x, b.y, b.z, MAP_CENTER, MAP_CENTER, rotationDegrees));

    @Override
    public void create() {
        // Change this to LOG_ERROR or LOG_NONE when releasing anything.
        Gdx.app.setLogLevel(Application.LOG_INFO);

        batch = new SpriteBatch();
        tileset = new TextureAtlas(TILESET_FILE_NAME);

        Array<TextureAtlas.AtlasRegion> entities = tileset.findRegions("entity");
        // Extract animations
        animations = Array.with(
            new Array<>(true, 16, Animation.class),
            new Array<>(true, 16, Animation.class),
            new Array<>(true, 16, Animation.class),
            new Array<>(true, 16, Animation.class));
        for (int i = 0, outer = 0; i < 16; i++, outer += 8) {
            /* Index 0 is front-facing idle animations. */
            /* Index 1 is rear-facing idle animations. */
            /* Index 2 is front-facing attack animations. */
            /* Index 3 is rear-facing attack animations. */
            animations.get(0).add(new Animation<>(0.4f, Array.with(new TextureAtlas.AtlasSprite(entities.get(outer+0)), new TextureAtlas.AtlasSprite(entities.get(outer+1))), Animation.PlayMode.LOOP));
            animations.get(1).add(new Animation<>(0.4f, Array.with(new TextureAtlas.AtlasSprite(entities.get(outer+4)), new TextureAtlas.AtlasSprite(entities.get(outer+5))), Animation.PlayMode.LOOP));
            animations.get(2).add(new Animation<>(0.2f, Array.with(new TextureAtlas.AtlasSprite(entities.get(outer+2)), new TextureAtlas.AtlasSprite(entities.get(outer+3))), Animation.PlayMode.LOOP));
            animations.get(3).add(new Animation<>(0.2f, Array.with(new TextureAtlas.AtlasSprite(entities.get(outer+6)), new TextureAtlas.AtlasSprite(entities.get(outer+7))), Animation.PlayMode.LOOP));
        }

        camera = new OrthographicCamera(Gdx.graphics.getWidth() * CAMERA_ZOOM, Gdx.graphics.getHeight() * CAMERA_ZOOM);
        camera.position.set(0, 100, 0);
        camera.update();
        viewport = new ScreenViewport(camera);

        regenerate(
            /* The seed will change after just over one hour, and will stay the same for over an hour. */
            TimeUtils.millis() >>> 22);
    }

    public void regenerate(long seed) {

        startTime = TimeUtils.millis();
        map = LocalMap.generateTestMap(
            seed,
            /* Used for both dimensions of the ground plane. */
            MAP_SIZE + ((int)seed & 3),
            /* Used for the depth of the map, in elevation. */
            MAP_PEAK,
            /* All terrain tiles in the tileset. */
            tileset.findRegions("tile"));
        mapCenter = (map.getFSize() - 1f) * 0.5f;
        map.setEntity(3, 3, 1, new AnimatedIsoSprite(animations.get(0).get(MathUtils.random(15)), 3, 3, 1));

    }

    @Override
    public void render() {
        handleInput();
        float time = TimeUtils.timeSinceMillis(startTime) * 0.001f;
        int prevRotationIndex = (int)((rotationDegrees + 45f) * (1f / 90f)) & 3;

        rotationDegrees = MathUtils.lerpAngleDeg(previousRotation, targetRotation, Math.min(TimeUtils.timeSinceMillis(animationStart) * 0.002f, 1f));
        final Array<GridPoint3> order = map.everything.orderedKeys();
        order.sort(comparator);

        int rotationIndex = (int)((rotationDegrees + 45f) * (1f / 90f)) & 3;
        if(prevRotationIndex != rotationIndex) {
            for (int i = 0, n = order.size; i < n; i++) {
                GridPoint3 gp = order.get(i);
                int[] rots = AssetData.ROTATIONS.get(map.getTile(gp));
                if(rots != null)
                    map.everything.get(gp).sprite.setRegion(map.tileset.get(rots[rotationIndex]));
            }
        }

        if(MathUtils.isEqual(rotationDegrees, targetRotation))
            previousRotation = targetRotation;
        ScreenUtils.clear(.14f, .15f, .2f, 1f);
        batch.setProjectionMatrix(camera.combined);
        viewport.apply();
        batch.begin();
        for (int i = 0, n = order.size; i < n; i++) {
            map.everything.get(order.get(i)).update(time).draw(batch, mapCenter, mapCenter, rotationDegrees);
        }

//        for (int line = 0, maxLines = MAP_SIZE * 2 - 1; line <= maxLines; line++) {
//            int span = Math.min(line + 1, maxLines - line);
//            int offset = line + 1 - span >> 1;
//            int f = Math.max(MAP_SIZE - 1 - line, 0);
//            int g = MAP_SIZE - 1 - offset;
//            for (int across = 0; across < span; across++) {
//                for (int h = 0; h < MAP_PEAK; h++) {
//                    int blockId = map.getTile(f, g, h);
//                    if (blockId != -1) {
//                        Vector2 pos = isoToScreen(f, g, h);
//                        Sprite spr = tiles.get(blockId % tiles.size);
//                        spr.setPosition(pos.x, pos.y);
//                        spr.draw(batch);
//                    }
//                }
//                g--;
//                f++;
//            }
//        }
        batch.end();
    }

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
            return;
        }
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

        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET)) {
            previousRotation = rotationDegrees;
            targetRotation = (MathUtils.round(rotationDegrees * (1f/90f)) + 1 & 3) * 90;
            animationStart = TimeUtils.millis();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET)) {
            previousRotation = rotationDegrees;
            targetRotation = (MathUtils.round(rotationDegrees * (1f/90f)) - 1 & 3) * 90;
            animationStart = TimeUtils.millis();
        }

        camera.update();

        if (Gdx.input.justTouched()) {
            Vector3 targetBlock = raycastToBlock(Gdx.input.getX(), Gdx.input.getY());

            // raycastToBlock() returns a Vector3 full of integers; they just need to be cast to int.
            int f = (int)targetBlock.x, g = (int)targetBlock.y, h = (int)targetBlock.z;
            Gdx.app.log("CLICK", "targetBlock " + targetBlock);

            if (map.isValid(f, g, h)) {
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                    if(map.getTile(f, g, h) == -1)
                        map.setTile(f, g, h, MathUtils.random(3));
                    else
                        map.setTile(f, g, h + 1, MathUtils.random(3));
                } else if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
                    map.setTile(f, g, h, -1);
                }
            }
        }
    }

    public Vector3 raycastToBlock(float screenX, float screenY) {
        Vector3 worldPos = camera.unproject(projectionTempVector.set(screenX, screenY, 0));
        // Why is the projection slightly off on both x and y? It is a mystery!
        worldPos.x -= TILE_WIDTH;
        worldPos.y -= TILE_HEIGHT * 4;
        // Check from highest to lowest Z
        for (int h = MAP_PEAK - 1; h >= 0; h--) {
            int f = MathUtils.ceil(worldPos.y * (0.5f / TILE_HEIGHT) + worldPos.x * (0.5f / TILE_WIDTH) - h);
            int g = MathUtils.ceil(worldPos.y * (0.5f / TILE_HEIGHT) - worldPos.x * (0.5f / TILE_WIDTH) - h);
            if (f >= 0 && f < MAP_SIZE && g >= 0 && g < MAP_SIZE) {
                if (map.getTile(f, g, h) != -1) { // Found a solid block
                    return isoTempVector.set(f, g, h); // Return the first valid block found
                }
            }
        }

        // No block was hit, return ground level
        Vector3 groundCoords = screenToIso(worldPos.x, worldPos.y);
        return groundCoords.set(MathUtils.ceil(groundCoords.x), MathUtils.ceil(groundCoords.y), 0);
    }

    public static Vector2 isoToScreen(float f, float g, float h) {
        float screenX = (f - g) * TILE_WIDTH;
        float screenY = (f + g) * TILE_HEIGHT + h * TILE_DEPTH;
        return screenTempVector.set(screenX, screenY);
    }

    public static Vector3 screenToIso(float screenX, float screenY) {
        float f = screenY * (0.5f / TILE_HEIGHT) + screenX * (0.5f / TILE_WIDTH) + 1;
        float g = screenY * (0.5f / TILE_HEIGHT) - screenX * (0.5f / TILE_WIDTH) + 1;
        return isoTempVector.set(f, g, 0);
    }

    private void reset() {
        regenerate(MathUtils.random.nextLong());
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
        viewport.setUnitsPerPixel(1f / Math.max(1, (int) Math.min(
            width  / ((MAP_SIZE+1f) * (TILE_WIDTH * 2f)),
            height / ((MAP_SIZE+1f) * (TILE_HEIGHT * 2f) + TILE_DEPTH * MAP_PEAK))));
        viewport.update(width, height);
    }
}

