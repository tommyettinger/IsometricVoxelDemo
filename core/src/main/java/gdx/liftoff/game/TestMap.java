package gdx.liftoff.game;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

public class TestMap extends LocalMap {
    public TestMap(int width, int height, int depth, Array<TextureAtlas.AtlasRegion> tileset) {
        super(width, height, depth, tileset);

        // ground
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    if (z == 0) {
                        setTile(x, y, z, MathUtils.random(3) + MathUtils.random(1) * 44);
                    }
                }
            }
        }

        // place random tiles in center of map
        int margin = 5;
        for (int x = margin; x < width - margin; x++) {
            for (int y = margin; y < height - margin; y++) {
                if (MathUtils.randomBoolean(.4f)) {
                    setTile(x,y,1, MathUtils.random(3) + MathUtils.random(1) * 44 + MathUtils.random(1) * 11);
                }
            }
        }

        // outline
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if ((x == 0 || x == width - 1) || (y == 0 || y == height - 1)) {
                    setTile(x,y,0, 2 + Math.max(MathUtils.random(1), MathUtils.random(1)) * 22);
                }
            }
        }
        setTile(0,0,0, 2);
        setTile(0,0,1, 2);
        setTile(width-1,0,0, 2);
        setTile(width-1,0,1, 2);
        setTile(0,height-1,0, 2);
        setTile(0,height-1,1, 2);
        setTile(width-1,height-1,0, 2);
        setTile(width-1,height-1,1, 2);
    }
}
