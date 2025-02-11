package gdx.liftoff.game;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.NumberUtils;

public class Isometric3DMapRenderer implements Disposable {
    private final Map map;
    private final Array<TextureAtlas.AtlasRegion> tiles;
    private final int tileWidth;
    private final int tileHeight;
    private final int tileDepth;

    private final DecalBatch decalBatch;
    private final Array<Decal> decals;

    private static final Quaternion faceCamera = new Quaternion().setEulerAngles(0, 90, 0);

    public Isometric3DMapRenderer(Camera camera, Map map, Array<TextureAtlas.AtlasRegion> tiles, int tileWidth, int tileHeight, int tileDepth) {
        decalBatch = new DecalBatch(new CameraGroupStrategy(camera));
        decals = new Array<>();
        this.map = map;
        this.tiles = tiles;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.tileDepth = tileDepth;
    }

    public void generateDecals() {
        decals.clear();
        for (int z = 0; z < map.getZSize(); z++) {
            for (int x = 0; x < map.getXSize(); x++) {
                for (int y = 0; y < map.getYSize(); y++) {
                    int tileId = map.getTile(x, y, z);
                    if (tileId != -1) {
                        Decal decal = createTileDecal(x, y, z, tileId);
                        decals.add(decal);
                    }
                }
            }
        }

        // Sort decals by Y value (depth sorting to prevent clipping issues)
        // This approach does not permit NaN as a y-value, but that would be bad anyway.
        decals.sort((a, b) -> NumberUtils.floatToIntBits(b.getY() - a.getY()));
    }

    private Decal createTileDecal(int x, int y, int z, int tileId) {
        float worldX = (x - y) * 0.5f;
        float worldY = z * 1.666f;
        float worldZ = (x + y) * 0.25f;

        Decal decal = Decal.newDecal(1f, 1f, tiles.get(tileId % tiles.size), true);

        decal.setPosition(worldX, worldY, worldZ);

        // Fix rotation: Decals should always face the camera, no rotation
        decal.setRotation(faceCamera);

        return decal;
    }

    public void draw() {
        for (Decal decal : decals) {
            decalBatch.add(decal);
        }
        decalBatch.flush();
    }

    public Vector3 raycastToTile(Camera camera, float screenX, float screenY) {
        Ray ray = camera.getPickRay(screenX, screenY);

        Vector3 closestTile = null;
        float closestDistance = Float.MAX_VALUE;

        for (int z = 0; z < map.getZSize(); z++) {
            for (int x = 0; x < map.getXSize(); x++) {
                for (int y = 0; y < map.getYSize(); y++) {
                    if (map.getTile(x, y, z) != -1) { // Only check existing tiles
                        Vector3 tilePos = new Vector3((x - y) * tileWidth, z * tileWidth, (x + y) * (tileWidth / 2f));

                        BoundingBox bbox = new BoundingBox(
                                new Vector3(tilePos.x - tileWidth / 2f, tilePos.y, tilePos.z - tileWidth / 2f),
                                new Vector3(tilePos.x + tileWidth / 2f, tilePos.y + tileWidth, tilePos.z + tileWidth / 2f)
                        );

                        if (Intersector.intersectRayBoundsFast(ray, bbox)) {
                            float distance = camera.position.dst(tilePos);
                            if (distance < closestDistance) {
                                closestDistance = distance;
                                closestTile = new Vector3(x, y, z);
                            }
                        }
                    }
                }
            }
        }

        return closestTile;
    }

    public DecalBatch getDecalBatch() {
        return decalBatch;
    }

    @Override
    public void dispose() {
        decalBatch.dispose();
    }
}
