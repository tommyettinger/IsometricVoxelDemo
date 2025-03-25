package gdx.liftoff.game;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector4;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.OrderedMap;
import gdx.liftoff.util.MiniNoise;

public class LocalMap {
    public int[][][] tiles;
    public OrderedMap<Vector4, IsoSprite> everything;
    public Array<TextureAtlas.AtlasRegion> tileset;
    public Sprite edge;

    private static final Vector4 tempPointA = new Vector4();
    private static final Vector4 tempPointB = new Vector4();

    public LocalMap(int width, int height, int depth, TextureAtlas atlas) {
        this.tileset = atlas.findRegions("tile");
        this.edge = atlas.createSprite("edge");
        tiles = new int[width][height][depth];

        for (int f = 0; f < width; f++) {
            for (int g = 0; g < height; g++) {
                for (int h = 0; h < depth; h++) {
                    tiles[f][g][h] = -1;
                }
            }
        }
        everything = new OrderedMap<>(width * height * depth >>> 1, 0.625f);
    }

    public boolean isValid(int f, int g, int h) {
        return f >= 0 && g >= 0 && h >= 0 && f < tiles.length && g < tiles[0].length && h < tiles[0][0].length;
    }

    public boolean isValid(Vector4 point) {
        return isValid((int)point.x, (int)point.y, (int)point.z);
    }

    public int getTile(int f, int g, int h) {
        return isValid(f, g, h) ? tiles[f][g][h] : -1;
    }

    public int getTile(Vector4 point) {
        return isValid(point) ? tiles[(int)point.x][(int)point.y][(int)point.z] : -1;
    }

    public IsoSprite getIsoSprite(int f, int g, int h) {
        return everything.get(tempPointA.set(f, g, h, 0));
    }

    public IsoSprite getIsoSprite(Vector4 point) {
        return everything.get(point);
    }

    public void setTile(int f, int g, int h, int tileId) {
        if (isValid(f, g, h)) {
            tiles[f][g][h] = tileId;
            if (tileId == -1) {
                everything.remove(tempPointB.set(f, g, h, 0));
                everything.remove(tempPointB.set(f, g, h, -1.5f));
            } else {
                IsoSprite iso;
                if ((iso = everything.get(tempPointB.set(f, g, h, 0))) != null) {
                    iso.setSprite(new TextureAtlas.AtlasSprite(tileset.get(tileId)));
                } else {
                    everything.put(new Vector4(f, g, h, 0), new IsoSprite(new TextureAtlas.AtlasSprite(tileset.get(tileId)), f, g, h));
                    everything.put(new Vector4(f, g, h, -1.5f), new IsoSprite(edge, f, g, h));
                }
            }
        }
    }

    public void setTile(Vector4 point, int tileId) {
        if (isValid(point)) {
            tiles[(int)point.x][(int)point.y][(int)point.z] = tileId;
            if (tileId == -1) {
                everything.remove(point);
                everything.remove(point.add(0,0,0,-1.5f));
            } else {
                IsoSprite iso;
                if ((iso = everything.get(point)) != null) {
                    iso.setPosition(point.x, point.y, point.z);
                } else {
                    everything.put(point, new IsoSprite(new TextureAtlas.AtlasSprite(tileset.get(tileId)), point.x, point.y, point.z));
                    everything.put(new Vector4(point.x, point.y, point.z, -1.5f), new IsoSprite(edge, point.x, point.y, point.z));

                }
            }
        }
    }

    public void setEntity(int f, int g, int h, IsoSprite sprite) {
        if (isValid(f, g, h)) {
            tiles[f][g][h] = -1;
            sprite.setPosition(f, g, h);
            everything.put(new Vector4(f, g, h, 0), sprite);
        }
    }

    public int getFSize() {
        return tiles.length;
    }

    public int getGSize() {
        return tiles[0].length;
    }

    public int getHSize() {
        return tiles[0][0].length;
    }

    public int getWidth() {
        return tiles.length;
    }

    public int getHeight() {
        return tiles[0].length;
    }

    public int getDepth() {
        return tiles[0][0].length;
    }

    private static final GridPoint2[] DIRECTIONS = {new GridPoint2(1, 0), new GridPoint2(0, 1), new GridPoint2(-1, 0), new GridPoint2(0, -1)};
    /**
     * Generates a simple test map that assumes a specific tileset (using {@code isometric-trpg.atlas} as
     * {@code tileset}, {@code tileset.findRegions("tile")}). Allows setting a specific seed to get the same map every
     * time. This requires a minimum {@code mapSize} of 11 and a minimum {@code mapPeak} of 4.
     * @param seed if this {@code long} is the same, the same map will be produced on each call
     * @param mapSize the width and height of the map, or the dimensions of the ground plane in tiles
     * @param mapPeak the depth or max elevation of the map
     * @param atlas should probably be the TextureAtlas loaded from {@code isometric-trpg.atlas}
     * @return a new LocalMap
     */
    public static LocalMap generateTestMap(long seed, int mapSize, int mapPeak, TextureAtlas atlas) {

        // noise that gradually moves a little
        MiniNoise baseNoise = new MiniNoise((int) (seed), 0.06f, MiniNoise.FBM, 3);
        // noise that is usually a low value, but has ridges of high values
        MiniNoise ridgeNoise = new MiniNoise((int) (seed >> 32), 0.1f, MiniNoise.RIDGED, 1);
        MathUtils.random.setSeed(seed);

        mapSize = Math.max(11, mapSize);
        mapPeak = Math.max(mapPeak, 4);

        LocalMap map = new LocalMap(mapSize, mapSize, mapPeak, atlas);
        // Random voxels as a base, with height determined by noise. Either dirt 25% of the time, or grass the rest.
        for (int f = 0; f < mapSize; f++) {
            for (int g = 0; g < mapSize; g++) {
                // I fiddled with this for a while to get results I liked.
                // This combines baseNoise's slowly changing shallow hills with a little of ridgeNoise's sharp crests.
                // The result is scaled and moved into the -1.99 to -0.01 range, then fed into
                // Math.pow with a base of 7, which is pretty much a complete guess that was refined over a few tries.
                // Then that pow call (which can produce values from close to 0 to almost 1) is scaled by mapPeak.
                int height = (int)(mapPeak * Math.pow(7.0, baseNoise.getNoise(f, g) * 0.76 + ridgeNoise.getNoise(f, g) * 0.23 - 1.0));
                // Some tiles are dirt, but most are grass; the 1.1f + 0.6f * baseNoise... is usually 1, but sometimes 0.
                map.setTile(f, g, height, (int)(1.1f + 0.6f * baseNoise.getNoiseWithSeed(f * 2.3f, g * 2.3f, ~baseNoise.getSeed())));
                // Anything below one of these tiles must be dirt.
                for (int h = height - 1; h >= 0; h--) {
                    map.setTile(f, g, h, AssetData.DIRT);
                }
            }
        }

//        // Place random full-height stone tiles over center of map.
//        int margin = 5;
//        for (int f = margin; f < mapSize - margin; f++) {
//            for (int g = margin; g < mapSize - margin; g++) {
//                // More likely to place tiles in the middle of the map than the edges.
//                if (MathUtils.randomBoolean(1.4f / (1f + Math.abs(mapSize * 0.5f - f) + Math.abs(mapSize * 0.5f - g)))) {
//                    map.setTile(f, g, 1, AssetData.BASALT);
//                }
//            }
//        }

//        // outline
//        for (int f = 0; f < mapSize; f++) {
//            for (int g = 0; g < mapSize; g++) {
//                if ((f == 0 || f == mapSize - 1) || (g == 0 || g == mapSize - 1)) {
//                    // Produces either lava or basalt, with lava much more likely.
//                    map.setTile(f, g, 0, 2 + Math.max(MathUtils.random(1), MathUtils.random(1)) * 22);
//                }
//            }
//        }

//        // Sets the corners to 2-voxel-tall basalt pillars.
//        map.setTile(0, 0, 0, 2);
//        map.setTile(0, 0, 1, 2);
//        map.setTile(mapSize -1, 0, 0, 2);
//        map.setTile(mapSize -1, 0, 1, 2);
//        map.setTile(0, mapSize -1, 0, 2);
//        map.setTile(0, mapSize -1, 1, 2);
//        map.setTile(mapSize -1, mapSize -1, 0, 2);
//        map.setTile(mapSize -1, mapSize -1 ,1, 2);

        int pathF = mapSize / 2 + MathUtils.random(mapSize / -4, mapSize / 4);
        int pathG = mapSize / 4 + MathUtils.random(mapSize / -6, mapSize / 6);
        float angle = 1f;
        if(MathUtils.randomBoolean()){
            int temp = pathG;
            pathG = pathF;
            pathF = temp;
            angle = 0f;
        }
        if(MathUtils.randomBoolean()){
            pathF = mapSize - 1 - pathF;
            pathG = mapSize - 1 - pathG;
            angle += 2f;
        }

        baseNoise.setOctaves(1);
        for (int i = 0, n = mapSize + mapSize; i < n; i++) {
            if(map.isValid(pathF, pathG, 0)) {
                for (int h = mapPeak - 1; h >= 0; h--) {
                    if(map.getTile(pathF, pathG, h) != -1) {
                        if(MathUtils.randomBoolean(0.8f))
                            map.setTile(pathF, pathG, h, AssetData.PATH_GRASS_FGTR);
                        break;
                    }
                }
            } else {
                pathF = mapSize / 2 + MathUtils.random(mapSize / -4, mapSize / 4);
                pathG = mapSize / 4 + MathUtils.random(mapSize / -6, mapSize / 6);
                angle = 1f;
                if(MathUtils.randomBoolean()){
                    int temp = pathG;
                    pathG = pathF;
                    pathF = temp;
                    angle = 0f;
                }
                if(MathUtils.randomBoolean()){
                    pathF = mapSize - 1 - pathF;
                    pathG = mapSize - 1 - pathG;
                    angle += 2f;
                }

            }
            angle = (angle + baseNoise.getNoise(i * 25f) * 0.7f + 4f) % 4f;
            GridPoint2 dir = DIRECTIONS[(int) angle];
            pathF += dir.x;
            pathG += dir.y;
        }

        AssetData.realignPaths(map);
        return map;
    }
}
