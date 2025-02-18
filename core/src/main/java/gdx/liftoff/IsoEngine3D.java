package gdx.liftoff;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g3d.utils.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.scenes.scene2d.utils.UIUtils;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import gdx.liftoff.game.Isometric3DMapRenderer;
import gdx.liftoff.game.LocalMap;
import gdx.liftoff.game.Player;
import gdx.liftoff.game.TestMap;

public class IsoEngine3D extends ApplicationAdapter {
    private static IsoEngine3D instance;

    public static final String TILESET_FILE_NAME = "isometric-trpg.atlas";
    public static final int TILE_WIDTH = 8;
    public static final int TILE_HEIGHT = 4;
    public static final int TILE_DEPTH = 8;
    public static final float TILE_RATIO = 2f;
    public static final int MAP_SIZE = 20;
    public static final float CAMERA_SCALE = 60.0f;

    public Camera camera;
    CameraInputController cameraInputController;
    private TextureAtlas tileset;
    private Array<Array<Animation<Sprite>>> animations;
    private LocalMap map;
    Isometric3DMapRenderer isometric3DMapRenderer;
    Player player;

    BitmapFont bitmapFont;
    SpriteBatch spriteBatch;

    boolean usePerspectiveCamera;
    private boolean cameraFollowsPlayer;
    private boolean spectatorCamera;

    public static IsoEngine3D getInstance() {
        return instance;
    }

    @Override
    public void create() {
        instance = this;

        tileset = new TextureAtlas(TILESET_FILE_NAME);
        Array<Sprite> entities = tileset.createSprites("entity");
        // Extract animations
        animations = Array.with(
            new Array<>(true, 16, Animation.class),
            new Array<>(true, 16, Animation.class),
            new Array<>(true, 16, Animation.class),
            new Array<>(true, 16, Animation.class));
        for (int i = 0, outer = 0; i < 16; i++, outer += 8) {
            /* Index 0 is front-facing idle animations. */
            animations.get(0).add(new Animation<>(0.4f, Array.with(entities.get(outer+0), entities.get(outer+1)), Animation.PlayMode.LOOP));
            /* Index 1 is rear-facing idle animations. */
            animations.get(1).add(new Animation<>(0.4f, Array.with(entities.get(outer+4), entities.get(outer+5)), Animation.PlayMode.LOOP));
            /* Index 2 is front-facing attack animations. */
            animations.get(2).add(new Animation<>(0.2f, Array.with(entities.get(outer+2), entities.get(outer+3)), Animation.PlayMode.LOOP));
            /* Index 3 is rear-facing attack animations. */
            animations.get(3).add(new Animation<>(0.2f, Array.with(entities.get(outer+6), entities.get(outer+7)), Animation.PlayMode.LOOP));
        }

        map = new TestMap(MAP_SIZE, MAP_SIZE, MAP_SIZE, tileset.findRegions("tile"));
        player = new Player(map, animations, MathUtils.random(0, 15));

        createCamera();

        isometric3DMapRenderer = new Isometric3DMapRenderer(camera, map, tileset.findRegions("tile"), TILE_WIDTH, TILE_HEIGHT, TILE_DEPTH);
        isometric3DMapRenderer.generateDecals();

        bitmapFont = new BitmapFont();
        bitmapFont.setColor(Color.valueOf("ffffaa"));
        bitmapFont.getData().markupEnabled = true;
        spriteBatch = new SpriteBatch();
    }

    private void createCamera() {
        if (usePerspectiveCamera) {
            camera = new PerspectiveCamera(10.3f, Gdx.graphics.getWidth() / CAMERA_SCALE, Gdx.graphics.getHeight() / CAMERA_SCALE);
            cameraInputController = new CameraInputController(camera);
            cameraInputController.rotateLeftKey = -1;
            cameraInputController.rotateRightKey = -1;
            Gdx.input.setInputProcessor(cameraInputController);
        } else {
            camera = new OrthographicCamera(Gdx.graphics.getWidth() / CAMERA_SCALE, Gdx.graphics.getHeight() / CAMERA_SCALE);
        }

        camera.position.set(0, 90, -20); // Move camera to an overhead position
//        camera.position.set(0, CHUNK_SIZE, 0); // Move camera to an overhead position
        camera.direction.set(0, -1, 0.27f).nor(); // Look downward at the world
        camera.up.set(0, 0, 1); // Keep Z as "up"
//        camera.lookAt(CHUNK_SIZE / 2f, CHUNK_SIZE / 2f, 0f);
//        camera.near = .1f;
//        camera.far = 10000f;
        camera.update();
    }

    @Override
    public void render() {
        handleInput();
        player.update(Gdx.graphics.getDeltaTime());
        if (cameraFollowsPlayer) {
            camera.position.set(player.position.x, player.position.y + 70, player.position.z - 20);
        }
        camera.update();

        ScreenUtils.clear(.14f, .15f, .2f, 1f, true);

        // Draw the player
        player.render(isometric3DMapRenderer.getDecalBatch());

        isometric3DMapRenderer.draw();

        drawUI();
    }

    private void handleInputPlayer() {
        float dx = 0, dy = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) dy = 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) dy = -1;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) dx = 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dx = -1;

        player.move(dx, dy);

//        if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            player.jump();
        }
    }

    private void handleInputSpectator() {
//        cameraInputController.target.set(camera.position);
//        cameraInputController.update();

        float moveSpeed = .1f;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) camera.position.add(0f, moveSpeed, 0); // Up-right
        if (Gdx.input.isKeyPressed(Input.Keys.S)) camera.position.add(0f, -moveSpeed, 0); // Down-left
        if (Gdx.input.isKeyPressed(Input.Keys.A)) camera.position.add(moveSpeed, 0f, 0); // Up-left
        if (Gdx.input.isKeyPressed(Input.Keys.D)) camera.position.add(-moveSpeed, 0f, 0); // Down-right
//        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) camera.zoom *= 1.1f;
//        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) camera.zoom *= 0.9f;
    }

    private void handleInput() {
        // controls
        if (spectatorCamera) {
            handleInputSpectator();
        } else {
            handleInputPlayer();
        }

        // tile placement / removal
        if (Gdx.input.justTouched()) {
            boolean leftClick = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
            boolean rightClick = Gdx.input.isButtonPressed(Input.Buttons.RIGHT);
            Vector3 hitTile = isometric3DMapRenderer.raycastToTile(camera, Gdx.input.getX(), Gdx.input.getY());

            if (hitTile != null) {
                int x = (int) hitTile.x;
                int y = (int) hitTile.y;
                int z = (int) hitTile.z;

                if (leftClick) {
                    // Place a tile on top of the clicked tile
                    map.setTile(x, y, z + 1, 1);
                } else if (rightClick) {
                    // Remove the clicked tile
                    map.setTile(x, y, z, -1);
                }

                // Regenerate decals to update rendering
                isometric3DMapRenderer.generateDecals();
            }
        }

        // settings
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            if (UIUtils.shift()) {
                reset();
            } else {
                isometric3DMapRenderer.generateDecals();
            }
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            cameraFollowsPlayer = !cameraFollowsPlayer;
            reset();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            usePerspectiveCamera = !usePerspectiveCamera;
            reset();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            spectatorCamera = !spectatorCamera;
            reset();
        }
    }

    public void reset() {
        dispose();
        create();
    }

    @Override
    public void dispose() {
        tileset.dispose();
        isometric3DMapRenderer.dispose();
    }

    private void drawUI() {
        if (!spriteBatch.isDrawing()) {
            spriteBatch.begin();
        }

        // info
        int y = 0;
        bitmapFont.draw(spriteBatch, "Player: " + (int)player.position.x + ", " + (int)player.position.y + ", " + (int)player.position.z, 10, y += 25);
        bitmapFont.draw(spriteBatch, "Camera: " + (int)camera.position.x + ", " + (int)camera.position.y + ", " + (int)camera.position.z, 10, y += 25);
        bitmapFont.draw(spriteBatch, "Camera Direction: " + camera.direction, 10, y += 25);
        bitmapFont.draw(spriteBatch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 10, y += 25);

        // controls
        y = 0;
        bitmapFont.draw(spriteBatch, "Press [#ffa]R[] to restart ([#ffa]SHIFT+R[] for hard reset)", Gdx.graphics.getWidth(), y += 25, -10, Align.right, false);
        if (usePerspectiveCamera) {
            bitmapFont.draw(spriteBatch, "[#ffa]WASD + mouse[] to move camera", Gdx.graphics.getWidth(), y += 25, -10, Align.right, false);
        } else if (spectatorCamera) {
            bitmapFont.draw(spriteBatch, "[#ffa]WASD[] to move camera", Gdx.graphics.getWidth(), y += 25, -10, Align.right, false);
        }
        bitmapFont.draw(spriteBatch, "Press [#ffa]P[] to toggle camera mode ([#aff]" + camera.getClass().getSimpleName() + "[])", Gdx.graphics.getWidth(), y += 25, -10, Align.right, false);
        bitmapFont.draw(spriteBatch, "Press [#ffa]F[] to toggle camera following player ([#aff]" + (!cameraFollowsPlayer ? "not " : "") + "following[])", Gdx.graphics.getWidth(), y += 25, -10, Align.right, false);
        bitmapFont.draw(spriteBatch, "Press [#ffa]M[] to toggle spectator camera ([#aff]" + (!spectatorCamera ? "not " : "") + "spectating[])", Gdx.graphics.getWidth(), y += 25, -10, Align.right, false);
        bitmapFont.draw(spriteBatch, "[#ffa]Left/right click[] to place/remove tiles", Gdx.graphics.getWidth(), y += 25, -10, Align.right, false);

        spriteBatch.end();
    }
}
