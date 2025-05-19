package gdx.liftoff;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.OrderedMap;
import gdx.liftoff.game.AssetData;
import gdx.liftoff.game.Mover;
import gdx.liftoff.util.MathSupport;
import gdx.liftoff.util.MiniNoise;
import gdx.liftoff.util.VoxelCollider;

import static com.badlogic.gdx.math.MathUtils.round;

/**
 * Stores a current "level" of the game and its contents, including moving beings and immobile terrain.
 * One of the more important and widely-used classes here.
 */
public class LocalMap {

    /**
     * Grid-aligned terrain storage, this uses an int ID per voxel for convenience. In this case, you could also use
     * byte IDs, and some games might require short IDs, but int is used just because Java can directly create ints.
     */
    public int[][][] tiles;
    /**
     * The critical, sortable mapping of Vector4 positions to visible IsoSprites. This includes two IsoSprites for each
     * voxel of terrain: one is the terrain cube itself, and one is its outline, which has a substantial depth modifier
     * so it will only render if no other terrain is covering it. Movers' IsoSprite (and AnimatedIsoSprite) instances
     * are also stored in here.
     */
    public OrderedMap<Vector4, IsoSprite> everything;
    /**
     * The Array of voxel types that can be shown here, typically drawn from a TextureAtlas.
     */
    public Array<TextureAtlas.AtlasRegion> tileset;
    /**
     * The reused Sprite for all voxel edges, each of which is a translucent outline above the upper edges of the voxel.
     */
    public Sprite edge;
    /**
     * The center position, in fractional tiles, of the entire map's f-size.
     */
    public float fCenter;
    /**
     * The center position, in fractional tiles, of the entire map's g-size.
     */
    public float gCenter;
    /**
     * How many fish we started out needing to rescue.
     */
    public int totalFish = 10;
    /**
     * How many fish have been saved so far.
     */
    public int fishSaved = 0;

    /**
     * A collision tracker for Movers so we can tell when the player should be damaged by touching an enemy.
     */
    public VoxelCollider<Mover> movers;

    /**
     * @return the rotation of the map in degrees
     */
    public float getRotationDegrees() {
        return rotationDegrees;
    }

    /**
     * Sets {@link #rotationDegrees}, but also {@link #cosRotation} and {@link #sinRotation}.
     * @param rotationDegrees the desired rotation of the map, in degrees
     */
    public void setRotationDegrees(float rotationDegrees) {
        this.rotationDegrees = rotationDegrees;
        cosRotation = MathUtils.cosDeg(rotationDegrees);
        sinRotation = MathUtils.sinDeg(rotationDegrees);
    }

    /**
     * The map's current rotation, in degrees.
     */
    public float rotationDegrees = 0f;
    /**
     * The cosine of the map's current rotation, used to avoid repeated calls to {@link MathUtils#cosDeg(float)}.
     */
    public float cosRotation = 0f;
    /**
     * The sine of the map's current rotation, used to avoid repeated calls to {@link MathUtils#sinDeg(float)}.
     */
    public float sinRotation = 0f;
    /**
     * During a rotation animation, this is the starting rotation, in degrees.
     */
    public float previousRotation = 0f;
    /**
     * During a rotation animation, this is the target rotation, in degrees.
     */
    public float targetRotation = 0f;

    /**
     * Mutated often in-place and used to check for positions in {@link #everything}.
     */
    private static final Vector4 tempVec4 = new Vector4();

    /**
     * Present for serialization only, this creates a LocalMap but needs many fields initialized.
     * {@link #tileset}, {@link #tiles}, {@link #edge}, {@link #fCenter}, {@link #gCenter}, {@link #movers}, and of
     * course {@link #everything} need to be initialized if you use this.
     */
    public LocalMap() {

    }
    /**
     * Creates a new LocalMap and initializes all fields.
     * @param width the f-size of the map
     * @param height the g-size of the map
     * @param layers the h-size of the map
     * @param atlas a TextureAtlas this will pull all "tile" regions from and the Sprite for "edge"
     */
    public LocalMap(int width, int height, int layers, TextureAtlas atlas) {
        this.tileset = atlas.findRegions("tile");
        this.edge = atlas.createSprite("edge");
        tiles = new int[width][height][layers];
        fCenter = (width - 1) * 0.5f;
        gCenter = (height - 1) * 0.5f;
        for (int f = 0; f < width; f++) {
            for (int g = 0; g < height; g++) {
                for (int h = 0; h < layers; h++) {
                    tiles[f][g][h] = -1;
                }
            }
        }
        everything = new OrderedMap<>(width * height * layers * 3 >>> 2, 0.625f);

        movers = new VoxelCollider<>();
    }

    /**
     * Checks if the given f,g,h isometric tile position is a valid position that can be looked up in {@link #tiles}.
     * @param f f-coordinate; if negative or too high, this returns false
     * @param g g-coordinate; if negative or too high, this returns false
     * @param h h-coordinate; if negative or too high, this returns false
     * @return true if the given coordinates are valid for array indices into {@link #tiles}, or false otherwise
     */
    public boolean isValid(int f, int g, int h) {
        return f >= 0 && g >= 0 && h >= 0 && f < tiles.length && g < tiles[0].length && h < tiles[0][0].length;
    }

    /**
     * Rounds f, g, and h and passes them to {@link #isValid(int, int, int)}. Note that this permits small negative
     * inputs due to rounding bringing them up to 0.0f if they are greater than -0.5f .
     * @param f f-coordinate; if too low or too high, this returns false
     * @param g g-coordinate; if too low or too high, this returns false
     * @param h h-coordinate; if too low or too high, this returns false
     * @return true if the given coordinates, after rounding, are valid for array indices into {@link #tiles}, or false otherwise
     */
    public boolean isValid(float f, float g, float h) {
        return isValid(round(f), round(g), round(h));
    }
    /**
     * Delegates to {@link #isValid(float, float, float)} using only the x, y, and z coordinates of {@code point}.
     * @param point a Vector4 of which only x, y, and z will be checked
     * @return true if the given point, after rounding x, y, and z, is valid for array indices into {@link #tiles}, or false otherwise
     */
    public boolean isValid(Vector4 point) {
        return isValid(point.x, point.y, point.z);
    }

    /**
     * If f, g, and h are valid, this returns the tile at that location; if no tile is present or if the coordinates are
     * invalid, this returns -1.
     * @param f f-coordinate; if negative or too high, this returns false
     * @param g g-coordinate; if negative or too high, this returns false
     * @param h h-coordinate; if negative or too high, this returns false
     * @return the tile ID at the given location, or -1 if no tile is present or the location is invalid
     */
    public int getTile(int f, int g, int h) {
        return isValid(f, g, h) ? tiles[f][g][h] : -1;
    }

    /**
     * Rounds the given float coordinates and passes them to {@link #getTile(int, int, int)}. Note that this permits
     * small negative inputs due to rounding bringing them up to 0.0f if they are greater than -0.5f .
     * @param f f-coordinate; if too low or too high, this returns false
     * @param g g-coordinate; if too low or too high, this returns false
     * @param h h-coordinate; if too low or too high, this returns false
     * @return the tile ID at the given location, or -1 if no tile is present or the location is invalid
     */
    public int getTile(float f, float g, float h) {
        return getTile(round(f), round(g), round(h));
    }

    /**
     * Rounds the x, y, and z of {@code point} and passes them to {@link #getTile(int, int, int)}. Note that this
     * permits small negative inputs due to rounding bringing them up to 0.0f if they are greater than -0.5f .
     * @param point a Vector4 of which only x, y, and z will be checked
     * @return the tile ID at the given location, or -1 if no tile is present or the location is invalid
     */
    public int getTile(Vector4 point) {
        return getTile(round(point.x), round(point.y), round(point.z));
    }

    /**
     * Gets the IsoSprite with the appropriate depth for a terrain voxel at the given isometric position (not rounded).
     * @param f f position of the terrain, almost always an integer stored in a float
     * @param g g position of the terrain, almost always an integer stored in a float
     * @param h h position of the terrain, almost always an integer stored in a float
     * @return the IsoSprite for terrain at the given position or {@code null} if none is present
     */
    public IsoSprite getIsoSpriteTerrain(float f, float g, float h) {
        return everything.get(tempVec4.set(f, g, h, 0));
    }

    /**
     * Modifies the given Vector4 so it holds the given [f,g,h] position with the depth a fish can have.
     * This rounds f, g, and h because a fish is always at an all-integer position. If {@link #everything} does not
     * have anything present at the Vector4 this produces, there is no fish present at that position, but if it does
     * have any IsoSprite present, it will be a fish.
     *
     * @param changing a Vector4 that will be modified in-place
     * @param f the "France to Finland" isometric coordinate; will be rounded and assigned to changing
     * @param g the "Germany to Greenland" isometric coordinate; will be rounded and assigned to changing
     * @param h the "heel to head" isometric coordinate; will be rounded and assigned to changing
     */
    public void setToFishPosition(Vector4 changing, float f, float g, float h) {
        changing.set(round(f), round(g), round(h), Main.FISH_W);
    }

    /**
     * Adds a {@link Mover} to {@link #everything} and {@link #movers}
     * @param mover
     * @param depth
     * @return
     */
    public Vector4 addMover(Mover mover, float depth) {
        mover.getPosition().z = getLayers() - 1;
        Vector4 pos = new Vector4(mover.getPosition(), depth);
        while (getTile(pos) != -1 || movers.collisionsWith(mover).notEmpty()) {
            pos.x = MathUtils.random(getWidth() - 1);
            pos.y = MathUtils.random(getHeight() - 1);
            mover.getPosition().set(pos.x, pos.y, pos.z);
        }
        mover.place(depth);
        movers.entities.add(mover);
        return pos;
    }

    /**
     * When point.w is 0, this selects terrain; when it is ENTITY_W, it selects an entity.
     * @param point
     * @return
     */
    public IsoSprite getIsoSprite(Vector4 point) {
        return everything.get(point);
    }

    /**
     * Sets the voxel terrain tile at the given isometric tile position to the tile with the given ID.
     * IDs can be seen in {@link AssetData}.
     * This also places an {@link #edge} at the same isometric position but a lower depth, so it only shows if there is
     * empty space behind the voxel in the depth sort.
     * @param f f-position as an int
     * @param g g-position as an int
     * @param h h-position as an int
     * @param tileId an ID for a tile, typically from {@link AssetData}
     */
    public void setTile(int f, int g, int h, int tileId) {
        if (isValid(f, g, h)) {
            tiles[f][g][h] = tileId;
            if (tileId == -1) {
                everything.remove(tempVec4.set(f, g, h, 0));
                everything.remove(tempVec4.set(f, g, h, -1.5f));
            } else {
                IsoSprite iso;
                if ((iso = everything.get(tempVec4.set(f, g, h, 0))) != null) {
                    iso.setSprite(new TextureAtlas.AtlasSprite(tileset.get(tileId)));
                } else {
                    everything.put(new Vector4(f, g, h, 0), new IsoSprite(new TextureAtlas.AtlasSprite(tileset.get(tileId)), f, g, h));
                    // Environment tiles have an outline that may render if there is empty space behind them.
                    // The position has -1.5 for w, and w is added to the depth for the purpose of sorting.
                    // Adjacent environment tiles should have a depth that is +1 or -1 from this tile.
                    // Because the outline is -1.5 behind this tile, adjacent environment tiles will render over it,
                    // but if there is empty space behind a tile, the outline will be in front of the further tiles.
                    everything.put(new Vector4(f, g, h, -1.5f), new IsoSprite(edge, f, g, h));
                }
            }
        }
    }

    /**
     * Sets the voxel terrain tile at the given isometric tile position (as a Vector4, which will be rounded when used
     * in {@link #tiles}) to the tile with the given ID. IDs can be seen in {@link AssetData}.
     * This also places an {@link #edge} at the same isometric position but a lower depth, so it only shows if there is
     * empty space behind the voxel in the depth sort.
     * @param point a Vector4 of which x, y, and z will be used for a position and w will genrerally be treated as 0
     * @param tileId an ID for a tile, typically from {@link AssetData}
     */
    public void setTile(Vector4 point, int tileId) {
        int f = round(point.x);
        int g = round(point.y);
        int h = round(point.z);
        if (isValid(f, g, h)) {
            tiles[f][g][h] = tileId;
            if (tileId == -1) {
                everything.remove(point);
                // remove the outline, too
                everything.remove(point.add(0,0,0,-1.5f));
            } else {
                IsoSprite iso;
                if ((iso = everything.get(point)) != null) {
                    iso.setPosition(point.x, point.y, point.z);
                } else {
                    everything.put(point, new IsoSprite(new TextureAtlas.AtlasSprite(tileset.get(tileId)), point.x, point.y, point.z));
                    // Environment tiles have an outline that may render if there is empty space behind them.
                    // The position has -1.5 for w, and w is added to the depth for the purpose of sorting.
                    // Adjacent environment tiles should have a depth that is +1 or -1 from this tile.
                    // Because the outline is -1.5 behind this tile, adjacent environment tiles will render over it,
                    // but if there is empty space behind a tile, the outline will be in front of the further tiles.
                    everything.put(new Vector4(point.x, point.y, point.z, -1.5f), new IsoSprite(edge, point.x, point.y, point.z));

                }
            }
        }
    }

    public void setEntity(float f, float g, float h, float depth, IsoSprite sprite) {
        int rf = round(f), rg = round(g), rh = round(h);
        if (isValid(rf, rg, rh)) {
            tiles[rf][rg][rh] = -1;
            sprite.setPosition(f, g, h);
            everything.put(new Vector4(f, g, h, depth), sprite);
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

    public int getLayers() {
        return tiles[0][0].length;
    }

    private static final GridPoint2[] DIRECTIONS = {new GridPoint2(1, 0), new GridPoint2(0, 1), new GridPoint2(-1, 0), new GridPoint2(0, -1)};
    /**
     * Generates a simple test map that assumes a specific tileset (using {@code isometric-trpg.atlas} as
     * {@code tileset}, {@code tileset.findRegions("tile")}). Allows setting a specific seed to get the same map every
     * time. This requires a minimum {@code mapSize} of 11 and a minimum {@code mapPeak} of 4.
     * @param seed if this {@code long} is the same, the same map will be produced on each call
     * @param mapSize the width and height of the map, or the dimensions of the ground plane in tiles
     * @param mapPeak the layer count or max elevation of the map
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
                int height = (int)(mapPeak * Math.pow(7.0, baseNoise.getNoise(f, g) * 0.56 + ridgeNoise.getNoise(f, g) * 0.43 - 1.0));
                // Some tiles are dirt, but most are grass; the 1.1f + 0.6f * baseNoise... is usually 1, but sometimes 0.
                int tile = (int)(1.1f + 0.6f * baseNoise.getNoiseWithSeed(f * 2.3f, g * 2.3f, ~baseNoise.getSeed()));
                map.setTile(f, g, height, tile);
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

    public LocalMap placeBushes(long seed, int bushCount) {
        GridPoint2 point = new GridPoint2();
        int fs = getFSize(), gs = getGSize(), hs = getHSize();
        seed = (seed ^ 0x9E3779B97F4A7C15L) * 0xD1B54A32D192ED03L;
        PER_BUSH:
        for (int i = 0; i < bushCount; i++) {
            MathSupport.fillR2(point, seed + i, fs, gs);
            for (int h = hs - 2; h >= 0; h--) {
                int below = getTile(point.x, point.y, h);
                if (below == AssetData.DECO_HEDGE) {
                    bushCount++;
                    continue PER_BUSH; // labeled break; we want to try to place a bush in another location.
                }
                if (below != -1) {
                    tiles[point.x][point.y][h + 1] = AssetData.DECO_HEDGE;
                    everything.put(new Vector4(point.x, point.y, h + 1, Main.FISH_W),
                        new IsoSprite(new TextureAtlas.AtlasSprite(tileset.get(AssetData.DECO_HEDGE)), point.x, point.y, h + 1));
                    setTile(point.x, point.y, h, AssetData.DIRT);
                    setTile(point.x + 1, point.y, h, AssetData.DIRT);
                    setTile(point.x - 1, point.y, h, AssetData.DIRT);
                    setTile(point.x, point.y + 1, h, AssetData.DIRT);
                    setTile(point.x, point.y - 1, h, AssetData.DIRT);
                    break;
                }
            }
        }
        return this;
    }

    public LocalMap placeFish(long seed, int fishCount, Array<Array<Animation<TextureAtlas.AtlasSprite>>> animations) {
        GridPoint2 point = new GridPoint2();
        int fs = getFSize(), gs = getGSize(), hs = getHSize();
        seed = (seed ^ 0x9E3779B97F4A7C15L) * 0xD1B54A32D192ED03L;
        for (int i = 0; i < fishCount; i++) {
            MathSupport.fillR2(point, seed + i, fs, gs);
            for (int h = hs - 2; h >= 0; h--) {
                int below = getTile(point.x, point.y, h);
                if (below != -1) {
                    setEntity(point.x, point.y, h + 1, Main.FISH_W, new AnimatedIsoSprite(animations.get(0).get(AssetData.FISH), point.x, point.y, h + 1));
                    break;
                }
            }
        }
        return this;
    }

    public Array<Mover> checkCollision(Mover mover) {
        return movers.collisionsWith(mover);
    }
}
