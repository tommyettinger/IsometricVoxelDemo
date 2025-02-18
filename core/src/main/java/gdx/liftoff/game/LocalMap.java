package gdx.liftoff.game;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.GridPoint3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.OrderedMap;

public class LocalMap {
    public int[][][] tiles;
    public OrderedMap<GridPoint3, IsoSprite> everything;
    public Array<TextureAtlas.AtlasRegion> tileset;

    private static final GridPoint3 tempPointA = new GridPoint3();
    private static final GridPoint3 tempPointB = new GridPoint3();
    private static final GridPoint3 tempPointC = new GridPoint3();

    public LocalMap(int width, int height, int depth, Array<TextureAtlas.AtlasRegion> tileset) {
        this.tileset = tileset;
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

    public boolean isValid(GridPoint3 point) {
        return isValid(point.x, point.y, point.z);
    }

    public int getTile(int f, int g, int h) {
        return isValid(f, g, h) ? tiles[f][g][h] : -1;
    }

    public IsoSprite getIsoSprite(int f, int g, int h) {
        return everything.get(tempPointA.set(f, g, h));
    }

    public IsoSprite getIsoSprite(GridPoint3 point) {
        return everything.get(point);
    }

    public void setTile(int f, int g, int h, int tileId) {
        if (isValid(f, g, h)) {
            tiles[f][g][h] = tileId;
            if (tileId == -1) {
                everything.remove(tempPointB.set(f, g, h));
            } else {
                IsoSprite iso;
                if ((iso = everything.get(tempPointB.set(f, g, h))) != null) {
                    iso.setSprite(new TextureAtlas.AtlasSprite(tileset.get(tileId)));
                } else {
                    everything.put(new GridPoint3(f, g, h), new IsoSprite(new TextureAtlas.AtlasSprite(tileset.get(tileId)), f, g, h));
                }
            }
        }
    }

    public void setTile(GridPoint3 point, int tileId) {
        if (isValid(point)) {
            tiles[point.x][point.y][point.z] = tileId;
            if (tileId == -1) {
                everything.remove(point);
            } else {
                IsoSprite iso;
                if ((iso = everything.get(point)) != null) {
                    iso.setPosition(point);
                } else {
                    everything.put(point, new IsoSprite(new TextureAtlas.AtlasSprite(tileset.get(tileId)), point));
                }
            }
        }
    }

    public void setEntity(int f, int g, int h, IsoSprite sprite) {
        if (isValid(f, g, h)) {
            tiles[f][g][h] = -1;
            sprite.setPosition(f, g, h);
            everything.put(new GridPoint3(f, g, h), sprite);
        }
    }

    public int getXSize() {
        return tiles.length;
    }

    public int getYSize() {
        return tiles[0].length;
    }

    public int getZSize() {
        return tiles[0][0].length;
    }
}
