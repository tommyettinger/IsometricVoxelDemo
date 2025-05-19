package gdx.liftoff;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.StringBuilder;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gdx.liftoff.game.*;

import java.util.Comparator;

import static gdx.liftoff.util.MathSupport.INVERSE_ROOT_2;

/**
 * This is the primary starting point in the core module, and the only platform-specific code should be in "Launcher"
 * classes in other modules. This is an isometric pixel art demo project where the player (a little person in blue) runs
 * around trying to save goldfish (little orange fish out of water) without bumping into enemies (green-skinned, brawny
 * orcs). Bumping into enemies will take away your health, and reaching 0 health is a game-over condition. Saving all 10
 * goldfish is the win condition.
 * <br>
 * This uses a special kind of coordinates because isometric coordinates just don't correspond nicely to x, y,
 * and z with any common convention. Here, when referring to isometric tiles, we use "f, g, h" positions.The f and g
 * axes are diagonal, and correspond to movement at a shallow angle on both world x and world y. The h axis is used for
 * elevation, and corresponds to movement on world y. The mnemonics here work as if on a world map, with the origin
 * somewhere in Belgium or the Netherlands:
 * <ul>
 *     <li>The f axis is roughly the diagonal from France to Finland.</li>
 *     <li>The g axis is roughly the diagonal from Germany to Greenland (or Greece to Germany, or Greece to Greenland).</li>
 *     <li>The h axis is the vertical line from your heel to your head (or Hell to Heaven).</li>
 * </ul>
 * The letters "t" and "r" also show up, with the geography mnemonics "Tallinn" (in Estonia, near Finland) and Reykjavík
 * (in Iceland, on the way to Greenland); these refer specifically to the positive f direction and positive g direction,
 * or the back faces of a voxel, while f and g refer to the faces on the front. "fgtr" are the recommended keys on a
 * QWERTY keyboard to move in those directions, or on a map of Europe, the locations of France, Germany, Tallinn (in
 * Estonia), and Reykjavík (in Iceland) relative to Amsterdam in the center. The "fgtr" keys also are close in shape to
 * matching the X-shape of directions you can travel on one axis at a time in isometric coordinates, at least on a
 * QWERTY keyboard.
 * <br>
 * When a Vector3 is used for an isometric position, x,y,z refer to f,g,h. This code also sometimes uses Vector4 when
 * camera distance needs to vary; in that case, the w coordinate refers to camera depth. While movement on the f and g
 * axes moves an object on both screen x and y, movement on the h axis only moves an object on screen y, and movement on
 * depth only changes its sort order (potentially rendering it before or after other objects, and sometimes covering it
 * up entirely). A major goal of this demo is to show how sort order works using only a SpriteBatch to draw things with
 * 3D (or really, 2.5D) positions, using only 2D sprites.
 */
public class Main extends ApplicationAdapter {
    /**
     * Used to draw things from back to front as 2D sprites. Even though our tiles use 3D positions. Well, 4D. Don't run
     * away, I explained all of this in the class comment!
     */
    private SpriteBatch batch;
    /**
     * This is the file name of the atlas of 2D assets used in the game. It uses
     * <a href="https://gvituri.itch.io/isometric-trpg">these free-to-use assets by Gustavo Vituri</a> and
     * <a href="https://ray3k.wordpress.com/clean-crispy-ui-skin-for-libgdx/">a mangled, pixelated skin originally by Raymond Buckley</a>.
     * <br>
     * CUSTOM TO YOUR GAME. This is closely related to {@link AssetData}, and if one changes, both should.
     */
    public static final String ATLAS_FILE_NAME = "isometric-trpg.atlas";
    /**
     * This is the actual TextureAtlas of 2D assets used in the game. It uses
     * <a href="https://gvituri.itch.io/isometric-trpg">these free-to-use assets by Gustavo Vituri</a> and
     * <a href="https://ray3k.wordpress.com/clean-crispy-ui-skin-for-libgdx/">a mangled, pixelated skin originally by Raymond Buckley</a>.
     * <br>
     * CUSTOM TO YOUR GAME. This is closely related to {@link AssetData}, and if one changes, both should.
     */
    private TextureAtlas atlas;
    /**
     * Animations taken from {@link #atlas} to be loaded in a more game-runtime-friendly format.
     * Index 0 is front-facing idle animations.
     * Index 1 is rear-facing idle animations.
     * Index 2 is front-facing attack animations.
     * Index 3 is rear-facing attack animations.
     * Inside each of those four Arrays, there is an Array of many Animations of AtlasSprites, with each Animation for a
     * different type of monster or character.
     * <br>
     * CUSTOM TO YOUR GAME.
     */
    private Array<Array<Animation<TextureAtlas.AtlasSprite>>> animations;
    /**
     * The depth modifier used by the player, so they can't remove terrain voxels by overlapping them.
     */
    public static final float PLAYER_W = (1f/8f);
    /**
     * The depth modifier used by goldfish, which is slightly different from the player or NPC depth modifiers so
     * goldfish can't get removed by an overlapping NPC, and so an overlapping player doesn't remove the goldfish before
     * its rescue can be processed.
     */
    public static final float FISH_W = PLAYER_W + (1f/1024f);
    /**
     * The depth modifier used by all moving NPCs; this is the same as {@link #PLAYER_W}.
     */
    public static final float NPC_W = PLAYER_W;
    /**
     * Can be changed to make the game harder with more enemies, or easier with fewer.
     */
    public static int ENEMY_COUNT = 10;
    /**
     * Used frequently here, this is the current location map that gameplay takes place in, which also stores the
     * inhabitants of that level.
     * <br>
     * CUSTOM TO YOUR GAME.
     */
    private LocalMap map;
    /**
     * The camera we use to show things with an isometric, or for sticklers, "dimetric" camera projection.
     * We can't use a PerspectiveCamera here, even with sort-of 3D positions, because it wouldn't be pixel-perfect.
     */
    private OrthographicCamera camera;
    /**
     * ScreenViewport is used here with a simple fraction for its {@link ScreenViewport#setUnitsPerPixel(float)}. Using
     * 1f, 1f/2f, 1f/3f, 1f/4f, etc. will ensure pixels stay all square consistently, and don't form ugly artifacts.
     */
    private ScreenViewport viewport;
    /**
     * Mover represents any moving creature or hazard, and can be a player character or non-player character (NPC).
     * This is the player, which has {@code npc = false;} and so won't move on their own.
     */
    private Mover player;
    /**
     * The enemies are stored in a simple Array. There aren't ever so many of them that the data structure could matter.
     * There's currently no logic for an enemy receiving damage; the player just tries to avoid the orc enemies.
     */
    private Array<Mover> enemies;
    /**
     * Not currently used, but present in the assets.
     * See <a href="https://github.com/raeleus/skin-composer/wiki/From-the-Ground-Up:-Scene2D.UI-Tutorials">some scene2d.ui docs</a>
     * for more information on how to use a Skin.
     */
    private Skin skin;
    /**
     * Shows current frames per second on the screen; you can remove this in production.
     */
    private Label fpsLabel;
    /**
     * Shows how many goldfish you need to rescue, if you have "won" by saving all goldfish, or if you have "died" by
     * taking 3 separate hits (you are hit by touching an enemy, which is always a green orc here).
     * <br>
     * CUSTOM TO YOUR GAME.
     */
    public Label goalLabel;
    /**
     * Only shows the current health of the player, using {@code "♥ "} for each point of health.
     * <br>
     * CUSTOM TO YOUR GAME.
     */
    public Label healthLabel;
    /**
     * This currently plays a public domain song by Komiku, "Road 4 Fight", the entire time.
     * I hope it isn't too annoying to be playing on loop...
     * <br>
     * CUSTOM TO YOUR GAME.
     */
    public Music backgroundMusic;
    /**
     * Currently, pressing 'c' will toggle the framerate cap, so you can see if any physics changes are still
     * framerate-independent.
     */
    private int cap = 60;
    /**
     * The horizontal distance in pixels between adjacent tiles. This is equivalent to the distance of one diagonal side
     * of the diamond-shaped top of any solid tile here, measured from left to right for a single side.
     */
    public static final int TILE_WIDTH = 8;
    /**
     * The vertical distance in pixels between adjacent tiles. This is equivalent to the distance of one diagonal side
     * of the diamond-shaped top of any solid tile here, measured from bottom to top for a single side.
     */
    public static final int TILE_HEIGHT = 4;
    /**
     * The vertical distance in pixels between stacked tiles. This is equivalent to the distance of a vertical side of
     * any full-sized solid tile (on the left or right side of the block), measured from bottom to top of a solid side.
     */
    public static final int TILE_DEPTH = 8;
    /**
     * The base width and height in tiles of a map; this may vary slightly when the map is created, for variety.
     * The variance only goes up by 0 to 3 width and height (by the same amount).
     */
    public static final int MAP_SIZE = 40;
    /**
     * The maximum number of voxels and creatures that can be stacked on top of each other in the map; typically also
     * includes some room to jump higher.
     */
    public static final int MAP_PEAK = 10;
    /**
     * The computed width in pixels of a full map at its largest possible {@link #MAP_SIZE}.
     */
    public static final int SCREEN_HORIZONTAL = (MAP_SIZE+3) * 2 * TILE_WIDTH;
    /**
     * The computed height in pixels of a full map at its largest possible {@link #MAP_SIZE} and {@link #MAP_PEAK}.
     */
    public static final int SCREEN_VERTICAL = (MAP_SIZE+3) * 2 * TILE_HEIGHT + MAP_PEAK * TILE_DEPTH;
    /**
     * The position in fractional tiles of the very center of the map, measured from bottom center.
     */
    public float mapCenter = (MAP_SIZE - 1f) * 0.5f;

    /**
     * Can be changed to any fraction that is {@code 1.0f} divided by any integer greater than 0, which makes the screen
     * zoom to double size if this is {@code 1.0f / 2}, or triple size if this is {@code 1.0f / 3}, and so on.
     */
    public float CAMERA_ZOOM = 1f;
    /**
     * In milliseconds, the time since the map was generated or regenerated.
     */
    public long startTime;
    /**
     * In milliseconds, the time at which any multi-frame animation started (usually a map rotation).
     */
    public long animationStart = -1000000L;

    /**
     * A temporary Vector3 used to store either pixel or world positions being projected or unprojected in 3D.
     */
    private static final Vector3 projectionTempVector = new Vector3();
    /**
     * A temporary Vector3 used to store tile positions (which are world positions on the diagonal grid) in 3D.
     */
    private static final Vector3 isoTempVector = new Vector3();
    /**
     * A temporary Vector2 used to store screen positions in pixels.
     */
    private static final Vector2 screenTempVector = new Vector2();
    /**
     * A temporary Vector4 used to store positions in {@link LocalMap#everything}, which stores every object and Mover
     * so they can be sorted correctly and then displayed in that order. This Vector4 is commonly set to some values
     * that should be checked if they exist in "everything", such as with {@link OrderedMap#containsKey(Object)}.
     * The x, y, and z coordinates correspond directly to a tile's isometric f, g, and h coordinates, while the
     * Vector4's w coordinate corresponds to the depth modifier for any sprite at that f,g,h position. A position with
     * the same x,y,z position (or f,g,h) but a different w (or depth modifier) is treated as different in "everything"
     * and this allows more than one sprite to share a position. This is how the outlines on terrain are handled, and
     * how goldfish can briefly occupy the same area as the player or an enemy.
     */
    private static final Vector4 tempVector4 = new Vector4();

    /**
     * Used to depth-sort isometric points, including if the map is mid-rotation. This gets the center of the LocalMap
     * directly from its size, and permits {@link LocalMap#rotationDegrees} to be any finite value in degrees.
     * It uses the map's pre-calculated {@link LocalMap#cosRotation} and sinRotation instead of needing to repeatedly
     * call cos() and sin(). The isometric points here are Vector4, but for the most part, only the x, y,
     * and z components are used. The fourth component, w, is only used to create another point at the same x, y, z
     * location but with a different depth. The depth change is currently used to draw outlines behind terrain tiles,
     * but have them be overdrawn by other terrain tiles if nearby. The outlines only appear if there is empty space
     * behind a terrain tile.
     */
    public final Comparator<? super Vector4> comparator =
        (a, b) -> Float.compare(
            IsoSprite.viewDistance(a.x, a.y, a.z, map.fCenter, map.gCenter, map.cosRotation, map.sinRotation) + a.w,
            IsoSprite.viewDistance(b.x, b.y, b.z, map.fCenter, map.gCenter, map.cosRotation, map.sinRotation) + b.w);

    // You can use this block of code instead; it may perform better if framerate is an issue in practice, but it isn't
    // quite as clear to read. Internally, this uses {@link NumberUtils#floatToIntBits(float)} instead of
    // {@link Float#compare(float, float)} because it still returns a completely valid comparison value (it only
    // distinguishes between an int that is positive, negative, or zero), and seems a tiny bit faster. To avoid
    // {@code -0.0f} being treated as a negative comparison value, this adds {@code 0.0f} to the difference of the two
    // compared depths. This is absolutely a magic trick, and the whole thing is probably unnecessary and gratuitous!
//    public final Comparator<? super Vector4> comparator =
//        (a, b) -> NumberUtils.floatToIntBits(
//            IsoSprite.viewDistance(a.x, a.y, a.z, map.fCenter, map.gCenter, map.cosRotation, map.sinRotation) + a.w -
//                IsoSprite.viewDistance(b.x, b.y, b.z, map.fCenter, map.gCenter, map.cosRotation, map.sinRotation) - b.w + 0.0f);

    @Override
    public void create() {
        // Change this to LOG_ERROR or LOG_NONE when releasing anything.
        Gdx.app.setLogLevel(Application.LOG_INFO);

        // Create and play looping background music.
        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("Komiku - Road 4 Fight.ogg"));
        backgroundMusic.setVolume(0.5f);
        backgroundMusic.setLooping(true);
        backgroundMusic.play();

        // We draw everything as 2D graphics with a carefully determined sort order.
        batch = new SpriteBatch();

        // Loads the atlas from an internal path, in "assets/".
        atlas = new TextureAtlas(ATLAS_FILE_NAME);
        // All regions in the atlas for creatures start with "entity" and have an index.
        Array<TextureAtlas.AtlasRegion> entities = atlas.findRegions("entity");
        // Extract animations from the atlas.
        // This step will be different for every game's assets.
        animations = Array.with(
            new Array<>(true, 16, Animation.class),
            new Array<>(true, 16, Animation.class),
            new Array<>(true, 16, Animation.class),
            new Array<>(true, 16, Animation.class));
        for (int i = 0, outer = 0; i < 16; i++, outer += 8) {
            /* Index 0 is front-facing idle animations.   */
            /* Index 1 is rear-facing idle animations.    */
            /* Index 2 is front-facing attack animations. */
            /* Index 3 is rear-facing attack animations.  */
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
        // The skin isn't used here much, but it is ready for more widgets to be used.
        skin = new Skin(Gdx.files.internal("isometric-trpg.json"), atlas);
        // The goal label changes when updateFish() or updateHealth() is called.
        goalLabel = new Label("", skin);
        goalLabel.setPosition(0, SCREEN_VERTICAL - 30, Align.center);
        updateFish();
        // The FPS label can be removed if you want in production.
        fpsLabel = new Label("0 FPS", skin);
        fpsLabel.setPosition(0, SCREEN_VERTICAL - 50, Align.center);
        // The health label shows red hearts (using BitmapFont markup to make them red) for your current health.
        healthLabel = new Label("[SCARLET]♥ ♥ ♥ ", skin);
        healthLabel.setPosition(-300, SCREEN_VERTICAL - 30, Align.left);
        updateHealth();
    }

    public void regenerate(long seed) {

        Mover.ID_COUNTER = 1;
        startTime = TimeUtils.millis();
        map = LocalMap.generateTestMap(
            seed,
            /* Used for both dimensions of the ground plane. */
            MAP_SIZE + ((int)seed & 3),
            /* Used for the depth of the map, in elevation. */
            MAP_PEAK,
            /* All terrain tiles in the tileset. */
            atlas);
        map.totalFish = 10;
        map.fishSaved = 0;
        map.placeFish(seed, map.totalFish, animations);
        mapCenter = (map.getFSize() - 1f) * 0.5f;
        // Random initial position for the player.
        int rf = MathUtils.random(1, MAP_SIZE - 2), rg = MathUtils.random(1, MAP_SIZE - 2);
        // Random character graphic for the player; id 0-3 will always be a human wearing blue.
        int id = MathUtils.random(3);
        player = new Mover(map, animations, id, rf, rg, MAP_PEAK - 1);
        map.addMover(player, PLAYER_W);
        enemies = new Array<>(ENEMY_COUNT);
        for (int i = 0; i < ENEMY_COUNT; i++) {
            // enemies can be anywhere except the very edges of the map.
            rf = MathUtils.random(1, MAP_SIZE - 2);
            rg = MathUtils.random(1, MAP_SIZE - 2);
            // id 4-7 will always be a green-skinned, brawny orc.
            id = MathUtils.random(4, 7);
            Mover enemy = new Mover(map, animations, id, rf, rg, MAP_PEAK - 1.6f);
            // We track enemies here as well as tracking them as general Movers in the map so that we can handle the
            // semi-random movement of enemies when we update them in Main, without semi-randomly moving the player.
            enemies.add(enemy);
            map.addMover(enemy, NPC_W);
        }
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        // handleInput() is where all keyboard input is handled. Mouse input isn't really handled right now, except to
        // add or remove blocks as a bit of debugging play.
        handleInput(delta);
        // Calling update() on a Mover makes them do all their logic for an unpaused game.
        player.update(delta);
        for(Mover e : enemies) e.update(delta);

        // This bit of code gets a little complex to handle rotating the map...
        // But rotating the map is so cool! You can do it by pressing '[' or ']' .
        float time = TimeUtils.timeSinceMillis(startTime) * 0.001f;
        // Rotations stop on a 90-degree angle increment, stored as an int from 0 to 3.
        int prevRotationIndex = (int)((map.rotationDegrees + 45f) * (1f / 90f)) & 3;

        // A rotation completes in half a second, which is quick enough to conceal some of the roughness during parts
        // of the animation.
        map.setRotationDegrees(MathUtils.lerpAngleDeg(map.previousRotation, map.targetRotation,
            Math.min(TimeUtils.timeSinceMillis(animationStart) * 0.002f, 1f)));

        // We sort the "everything" OrderedMap here using our custom comparator.
        final Array<Vector4> order = map.everything.orderedKeys();
        order.sort(comparator);

        // Our current rotation index, in 90-degree increments, so from 0 to 3.
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

        // When the rotation has finished, we set the previous rotation to what we just ended on.
        if(MathUtils.isEqual(map.rotationDegrees, map.targetRotation))
            map.previousRotation = map.targetRotation;
        // Very dark blue for the background color.
        ScreenUtils.clear(.14f, .15f, .2f, 1f);
        // Vital to get things to display. I don't actually know what the "combined" matrix is here.
        batch.setProjectionMatrix(camera.combined);
        // We need to apply() the viewport here in case it changed for any reason, such as from key inputs.
        viewport.apply();
        batch.begin();
        for (int i = 0, n = order.size; i < n; i++) {
            Vector4 pos = order.get(i);
            // Updates each voxel in "everything" and then draws it with the parameters needed for rotation.
            map.everything.get(pos).update(time).draw(batch, (map.getFSize() - 1) * 0.5f, (map.getGSize() - 1) * 0.5f, map.cosRotation, map.sinRotation);
        }

        Vector3 pos = player.getPosition();
        // Makes tempVector4 store the position we want to check: the players's location, rounded, at the fish depth.
        // If there is anything at that position, it is a fish the player is touching, and so has rescued.
        map.setToFishPosition(tempVector4, pos.x, pos.y, pos.z);
        IsoSprite fish = map.everything.get(tempVector4);
        if(fish instanceof AnimatedIsoSprite && fish != player.visual){
            ++map.fishSaved;
            map.everything.remove(tempVector4);
            ((Main)Gdx.app.getApplicationListener()).updateFish();
        }

        fpsLabel.getText().clear();
        fpsLabel.getText().append(Gdx.graphics.getFramesPerSecond()).append(" FPS");
        // Allows the FPS label to be drawn with the correct width.
        fpsLabel.invalidate();
        goalLabel.draw(batch, 1f);
        fpsLabel.draw(batch, 1f);
        healthLabel.draw(batch, 1f);
        batch.end();
    }

    /**
     * Only handles movement input for the player character. This is called from the main input handling in
     * {@link #handleInput(float)}.
     */
    private void handleInputPlayer() {
        // Our difference (delta) on the f and g isometric axes.
        float df = 0, dg = 0;
        // We allow f,g,t,r to move on one isometric axis, or the numpad to move in all 8 directions.
        // You can also hold a pair of f,g,t,r (adjacent on a QWERTY keyboard) to move north, south, east, or west.
             if (Gdx.input.isKeyPressed(Input.Keys.F) && Gdx.input.isKeyPressed(Input.Keys.G)) { df = -INVERSE_ROOT_2; dg = -INVERSE_ROOT_2; }
        else if (Gdx.input.isKeyPressed(Input.Keys.F) && Gdx.input.isKeyPressed(Input.Keys.R)) { df = -INVERSE_ROOT_2; dg = INVERSE_ROOT_2; }
        else if (Gdx.input.isKeyPressed(Input.Keys.G) && Gdx.input.isKeyPressed(Input.Keys.T)) { df = INVERSE_ROOT_2; dg = -INVERSE_ROOT_2; }
        else if (Gdx.input.isKeyPressed(Input.Keys.T) && Gdx.input.isKeyPressed(Input.Keys.R)) { df = INVERSE_ROOT_2; dg = INVERSE_ROOT_2; }
        else if (Gdx.input.isKeyPressed(Input.Keys.F) || Gdx.input.isKeyPressed(Input.Keys.NUMPAD_1)) df = -1;
        else if (Gdx.input.isKeyPressed(Input.Keys.G) || Gdx.input.isKeyPressed(Input.Keys.NUMPAD_3)) dg = -1;
        else if (Gdx.input.isKeyPressed(Input.Keys.T) || Gdx.input.isKeyPressed(Input.Keys.NUMPAD_9)) df = 1;
        else if (Gdx.input.isKeyPressed(Input.Keys.R) || Gdx.input.isKeyPressed(Input.Keys.NUMPAD_7)) dg = 1;
        else if (Gdx.input.isKeyPressed(Input.Keys.NUMPAD_2)) { df = -INVERSE_ROOT_2; dg = -INVERSE_ROOT_2; }
        else if (Gdx.input.isKeyPressed(Input.Keys.NUMPAD_4)) { df = -INVERSE_ROOT_2; dg = INVERSE_ROOT_2; }
        else if (Gdx.input.isKeyPressed(Input.Keys.NUMPAD_6)) { df = INVERSE_ROOT_2; dg = -INVERSE_ROOT_2; }
        else if (Gdx.input.isKeyPressed(Input.Keys.NUMPAD_8)) { df = INVERSE_ROOT_2; dg = INVERSE_ROOT_2; }

        // We account for the map's rotation so the visual rotation of the map (for the player) also affects the
        // direction in tiles for their chosen direction as they perceive it.
        float c = map.cosRotation;
        float s = map.sinRotation;
        float rf = c * df + s * dg;
        float rg = c * dg - s * df;

        player.move(rf, rg, Mover.MOVE_SPEED);

        // Whee! Space or Numpad 0 or 5 make the player character jump really high.
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
        updateFish();
        updateHealth();
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

    public void updateFish() {
        if(map.totalFish == map.fishSaved)
            goalLabel.setText("YOU SAVED THEM ALL! Great job!");
        else if(player.health > 0)
            goalLabel.setText("SAVE THE GOLDFISH!!! " + (map.totalFish - map.fishSaved) + " still " +
            ((map.totalFish - map.fishSaved) == 1 ? "needs" : "need") + " your help!");
        goalLabel.setAlignment(Align.center);
        goalLabel.setPosition(goalLabel.getX(), goalLabel.getY(), Align.center);
    }

    public void updateHealth() {
        if(player.health <= 0)
        {
            goalLabel.setText("YOU FAILED.. BY DYING...");
            goalLabel.setAlignment(Align.center);
            goalLabel.setPosition(goalLabel.getX(), goalLabel.getY(), Align.center);
            healthLabel.setText("[FIREBRICK]:(");
        }
        else {
            healthLabel.getText().clear();
            healthLabel.getText().append("[SCARLET]");
            for (int i = 0; i < player.health; i++) {
                healthLabel.getText().append(" ♥");
            }
            healthLabel.setText(healthLabel.getText().toString());
            healthLabel.invalidate();
            updateFish();
        }
    }
}

