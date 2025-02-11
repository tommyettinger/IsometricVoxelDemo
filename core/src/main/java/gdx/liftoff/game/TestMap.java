package gdx.liftoff.game;

import com.badlogic.gdx.math.MathUtils;

public class TestMap extends Map {
    public TestMap(int width, int height, int depth) {
        super(width, height, depth);

        // ground
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    if (z == 0) {
                        tiles[x][y][z] = MathUtils.random(10, 11);
                    }
                }
            }
        }

        // place random tiles in center of map
        int margin = 5;
        for (int x = margin; x < width - margin; x++) {
            for (int y = margin; y < height - margin; y++) {
                if (MathUtils.randomBoolean(.5f)) {
                    tiles[x][y][1] = MathUtils.random(40);
                }
            }
        }

        // outline
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if ((x == 0 || x == width - 1) || (y == 0 || y == height - 1)) {
                    tiles[x][y][0] = MathUtils.random(4, 5);
                }
            }
        }
    }

}
