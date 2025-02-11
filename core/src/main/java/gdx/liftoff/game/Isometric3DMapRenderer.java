package gdx.liftoff.game;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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

public class Isometric3DMapRenderer implements Disposable {
    private final Map map;
    private final Texture tileset;
    private final int tileSize;
    private final int tilesetColumns;

    private DecalBatch decalBatch;
    private Array<Decal> decals;

    public Isometric3DMapRenderer(Camera camera, Map map, Texture tileset, int tileSize, int tilesetColumns) {
        decalBatch = new DecalBatch(new CameraGroupStrategy(camera));
        decals = new Array<>();
        this.map = map;
        this.tileset = tileset;
        this.tileSize = tileSize;
        this.tilesetColumns = tilesetColumns;
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
        decals.sort((a, b) -> Float.compare(b.getY(), a.getY()));
    }

    private Decal createTileDecal(int x, int y, int z, int tileId) {
        float worldX = (x - y) * .5f;
        float worldY = z * 2;
        float worldZ = (x + y) * 0.22f;

        int srcX = (tileId % tilesetColumns) * tileSize;
        int srcY = (tileId / tilesetColumns) * tileSize;

        Decal decal = Decal.newDecal(1f, 1f,
                new TextureRegion(tileset, srcX, srcY, tileSize, tileSize), true);

        decal.setPosition(worldX, worldY, worldZ);

        // Fix rotation: Decals should always face upwards, no rotation
        decal.setRotation(new Quaternion().setEulerAngles(0, 90, 0));

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
                        Vector3 tilePos = new Vector3((x - y) * tileSize, z * tileSize, (x + y) * (tileSize / 2f));

                        BoundingBox bbox = new BoundingBox(
                                new Vector3(tilePos.x - tileSize / 2f, tilePos.y, tilePos.z - tileSize / 2f),
                                new Vector3(tilePos.x + tileSize / 2f, tilePos.y + tileSize, tilePos.z + tileSize / 2f)
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
