package gdx.liftoff.game;

public class Map {
    int[][][] tiles;

    public Map(int width, int height, int depth) {
        tiles = new int[width][height][depth];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    tiles[x][y][z] = -1;
                }
            }
        }
    }

    public int getTile(int x, int y, int z) {
        return isValid(x, y, z) ? tiles[x][y][z] : -1;
    }

    public void setTile(int x, int y, int z, int tileId) {
        if (isValid(x, y, z)) {
            tiles[x][y][z] = tileId;
        }
    }

    public boolean isValid(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < tiles.length && y < tiles[0].length && z < tiles[0][0].length;
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
