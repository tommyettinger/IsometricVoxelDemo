package gdx.liftoff;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gdx.liftoff.game.*;

import java.util.Comparator;

import static gdx.liftoff.util.MathSupport.INVERSE_ROOT_2;

public class Main extends ApplicationAdapter {
    public static final float ENTITY_W = 0.125f;
    private SpriteBatch batch;
    private TextureAtlas atlas;
    private Array<Array<Animation<TextureAtlas.AtlasSprite>>> animations;
    private LocalMap map;
    private OrthographicCamera camera;
    private ScreenViewport viewport;
    private Mover player;
    private Skin skin;
    private Label fpsLabel;
    private Label goalLabel;
    private int cap = 60;
    public static final String ATLAS_FILE_NAME = "isometric-trpg.atlas";
    public static final int TILE_WIDTH = 8;
    public static final int TILE_HEIGHT = 4;
    public static final int TILE_DEPTH = 8;
    public static final int MAP_SIZE = 40;
    public static final int MAP_PEAK = 10;
    public static final int SCREEN_HORIZONTAL = (MAP_SIZE+3) * 2 * TILE_WIDTH;
    public static final int SCREEN_VERTICAL = (MAP_SIZE+3) * 2 * TILE_HEIGHT + MAP_PEAK * TILE_DEPTH;
    public float mapCenter = (MAP_SIZE - 1f) * 0.5f;

    public float CAMERA_ZOOM = 1f;
    public long startTime, animationStart = -1000000L;

    private static final Vector3 projectionTempVector = new Vector3();
    private static final Vector3 isoTempVector = new Vector3();
    private static final Vector2 screenTempVector = new Vector2();
    private static final GridPoint3 tempPointA = new GridPoint3();
    private static final GridPoint3 tempPointB = new GridPoint3();
    private static final GridPoint3 tempPointC = new GridPoint3();

    /**
     * Used to depth-sort isometric points, including if the map is mid-rotation. This gets the center of the LocalMap
     * directly from its size, and permits {@link LocalMap#rotationDegrees}
     * to be any finite value in degrees. The isometric points here are Vector4, but for the most part, only the x, y,
     * and z components are used. The fourth component, w, is only used to create another point at the same x, y, z
     * location but with a different depth. The depth change is currently used to draw outlines behind terrain tiles,
     * but have them be overdrawn by other terrain tiles if nearby. The outlines only appear if there is empty space
     * behind a terrain tile.
     * <br>
     * Internally, this uses {@link NumberUtils#floatToIntBits(float)} instead of {@link Float#compare(float, float)}
     * because it still returns a completely valid comparison value (it only distinguishes between an int that is
     * positive, negative, or zero), and seems a tiny bit faster. To avoid {@code -0.0f} being treated as a negative
     * comparison value, this adds {@code 0.0f} to the difference of the two compared depths. This is absolutely a magic
     * trick, and it is probably unnecessary and gratuitous!
     */
    public final Comparator<? super Vector4> comparator =
        (a, b) -> NumberUtils.floatToIntBits(
            IsoSprite.viewDistance(a.x, a.y, a.z, map.fCenter, map.gCenter, map.cosRotation, map.sinRotation) + a.w -
                IsoSprite.viewDistance(b.x, b.y, b.z, map.fCenter, map.gCenter, map.cosRotation, map.sinRotation) - b.w + 0.0f);
//
//    // The above is equivalent to:
//    public final Comparator<? super Vector4> comparator =
//        (a, b) -> Float.compare(
//            IsoSprite.viewDistance(a.x, a.y, a.z, map.fCenter, map.gCenter, map.cosRotation, map.sinRotation) + a.w,
//            IsoSprite.viewDistance(b.x, b.y, b.z, map.fCenter, map.gCenter, map.cosRotation, map.sinRotation) + b.w);

    @Override
    public void create() {
        // Change this to LOG_ERROR or LOG_NONE when releasing anything.
        Gdx.app.setLogLevel(Application.LOG_INFO);

        batch = new SpriteBatch();
        atlas = new TextureAtlas(ATLAS_FILE_NAME);
        skin = new Skin(Gdx.files.internal("isometric-trpg.json"), atlas);
        goalLabel = new Label("SAVE THE GOLDFISH!!!", skin);
        goalLabel.setPosition(0, SCREEN_VERTICAL - 30, Align.center);
        fpsLabel = new Label("0 FPS", skin);
        fpsLabel.setPosition(0, SCREEN_VERTICAL - 50, Align.center);

        Array<TextureAtlas.AtlasRegion> entities = atlas.findRegions("entity");
        // Extract animations from the atlas.
        // This step will be different for every game's assets.
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

        // Initialize a Camera with the width and height of the area to be shown.
        camera = new OrthographicCamera(Gdx.graphics.getWidth() * CAMERA_ZOOM, Gdx.graphics.getHeight() * CAMERA_ZOOM);
        // Center the camera in the middle of the map.
        camera.position.set(TILE_WIDTH, SCREEN_VERTICAL * 0.5f, 0);
        // Updating the camera allows the changes we made to actually take effect.
        camera.update();
        // ScreenViewport is not always a great choice, but here we want only pixel-perfect zooms, and it can do that.
        viewport = new ScreenViewport(camera);

        // Calling regenerate() does the procedural map generation, and chooses a random player character.
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
            atlas);
        map.placeFish(seed, 10, animations);
        mapCenter = (map.getFSize() - 1f) * 0.5f;
        int rf = MathUtils.random(1, MAP_SIZE - 2), rg = MathUtils.random(1, MAP_SIZE - 2);
//        for (int h = MAP_PEAK - 2; h >= 0; h--) {
//            if(map.getTile(rf, rg, h) != -1) {
                int id = MathUtils.random(15);
                player = new Mover(map, animations, id, rg, rg, MAP_PEAK - 1);
                player.place();
//                break;
//            }
//        }

    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        handleInput(delta);
        player.update(delta);
        float time = TimeUtils.timeSinceMillis(startTime) * 0.001f;
        int prevRotationIndex = (int)((map.rotationDegrees + 45f) * (1f / 90f)) & 3;

        map.setRotationDegrees(MathUtils.lerpAngleDeg(map.previousRotation, map.targetRotation,
            Math.min(TimeUtils.timeSinceMillis(animationStart) * 0.002f, 1f)));
        final Array<Vector4> order = map.everything.orderedKeys();
        order.sort(comparator);

        int rotationIndex = (int)((map.rotationDegrees + 45f) * (1f / 90f)) & 3;
        if(prevRotationIndex != rotationIndex) {
            for (int i = 0, n = order.size; i < n; i++) {
                Vector4 pt = order.get(i);
                if(pt.w != 0f) continue; // 0f is used for terrain, higher values for creatures, lower for outlines.
                int[] rots = AssetData.ROTATIONS.get(map.getTile(pt)); // some tiles change appearance when rotated
                if(rots != null)
                    map.everything.get(pt).sprite.setRegion(map.tileset.get(rots[rotationIndex]));
            }
        }

        if(MathUtils.isEqual(map.rotationDegrees, map.targetRotation))
            map.previousRotation = map.targetRotation;
        ScreenUtils.clear(.14f, .15f, .2f, 1f);
        batch.setProjectionMatrix(camera.combined);
        viewport.apply();
        batch.begin();
        for (int i = 0, n = order.size; i < n; i++) {
            Vector4 pos = order.get(i);
            map.everything.get(pos).update(time).draw(batch, (map.getFSize() - 1) * 0.5f, (map.getGSize() - 1) * 0.5f, map.cosRotation, map.sinRotation);
        }

        fpsLabel.getText().clear();
        fpsLabel.getText().append(Gdx.graphics.getFramesPerSecond()).append(" FPS");
        fpsLabel.invalidate();
        goalLabel.draw(batch, 1f);
        fpsLabel.draw(batch, 1f);
        batch.end();
    }

    private void handleInputPlayer() {
        float df = 0, dg = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.F) || Gdx.input.isKeyPressed(Input.Keys.NUMPAD_1)) df = -1;
        else if (Gdx.input.isKeyPressed(Input.Keys.G) || Gdx.input.isKeyPressed(Input.Keys.NUMPAD_3)) dg = -1;
        else if (Gdx.input.isKeyPressed(Input.Keys.T) || Gdx.input.isKeyPressed(Input.Keys.NUMPAD_9)) df = 1;
        else if (Gdx.input.isKeyPressed(Input.Keys.R) || Gdx.input.isKeyPressed(Input.Keys.NUMPAD_7)) dg = 1;
        else if (Gdx.input.isKeyPressed(Input.Keys.NUMPAD_2)) { df = -INVERSE_ROOT_2; dg = -INVERSE_ROOT_2;}
        else if (Gdx.input.isKeyPressed(Input.Keys.NUMPAD_4)) { df = -INVERSE_ROOT_2; dg = INVERSE_ROOT_2;}
        else if (Gdx.input.isKeyPressed(Input.Keys.NUMPAD_6)) { df = INVERSE_ROOT_2; dg = -INVERSE_ROOT_2;}
        else if (Gdx.input.isKeyPressed(Input.Keys.NUMPAD_8)) { df = INVERSE_ROOT_2; dg = INVERSE_ROOT_2;}

        float c = map.cosRotation;
        float s = map.sinRotation;
        float rf = c * df + s * dg;
        float rg = c * dg - s * df;

        player.move(rf, rg);

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
         || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_0)
         || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_5)) {
            player.jump();
        }
    }

    private void handleInput(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
            return;
        }
        // zero state
        if (Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
            reset();
            return;
        }
        // cap for frame rate
        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            cap ^= 60;
            Gdx.graphics.setForegroundFPS(cap);
            return;
        }
        handleInputPlayer();

        if (Gdx.input.isKeyPressed(Input.Keys.UP)) camera.translate(0, 200 * delta);
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) camera.translate(0, -200 * delta);
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) camera.translate(-200 * delta, 0);
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) camera.translate(200 * delta, 0);
        if (Gdx.input.isKeyJustPressed(Input.Keys.I)) camera.zoom *= .5f; // In
        if (Gdx.input.isKeyJustPressed(Input.Keys.O)) camera.zoom *= 2f; // Out

        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET)) {
            map.previousRotation = map.rotationDegrees;
            map.targetRotation = (MathUtils.round(map.rotationDegrees * (1f/90f)) + 1 & 3) * 90;
            animationStart = TimeUtils.millis();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET)) {
            map.previousRotation = map.rotationDegrees;
            map.targetRotation = (MathUtils.round(map.rotationDegrees * (1f/90f)) - 1 & 3) * 90;
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
        atlas.dispose();
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

