package gdx.liftoff.game;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import gdx.liftoff.IsoEngine2D;

/**
 * Wraps a {@link Sprite} so its position can be set using isometric coordinates.
 * There are many possible isometric coordinate systems, so the one used here intentionally avoids calling the isometric
 * axes x, y, or z - here, the axes are f, g, and h, with mnemonics to help remember them. This uses an origin point at
 * the bottom center of the rotated rectangular map, typically the lowest corner of the map. The f and g axes are
 * diagonal, and correspond to movement at a shallow angle on both screen x and screen y. The h axis is used for
 * elevation, and corresponds to movement on screen y. The mnemonics here work as if on a world map, with the origin
 * somewhere in Belgium or the Netherlands:
 * <ul>
 *     <li>The f axis is roughly the diagonal from France to Finland.</li>
 *     <li>The g axis is roughly the diagonal from Germany to Greenland (or Greece to Germany, or Greece to Greenland).</li>
 *     <li>The h axis is the vertical line from your heel to your head (or Hell to Heaven).</li>
 * </ul>
 */
public class IsoSprite {
    public static float UNIT = IsoEngine2D.TILE_HEIGHT;
    public Sprite sprite;
    public float f, g, h;

    public IsoSprite() {
        this(new Sprite());
    }

    public IsoSprite(Sprite sprite) {
        this.sprite = sprite;
    }
    public IsoSprite(Sprite sprite, float f, float g, float h) {
        this.sprite = sprite;
        setPosition(f, g, h);
    }

    public void setPosition(float f, float g, float h) {
        this.f = f;
        this.g = g;
        this.h = h;
        float screenX = (f - g) * (2 * UNIT);
        float screenY = (f + g) * UNIT + h * (2 * UNIT);
        sprite.setPosition(screenX, screenY);
    }

    public void setOriginBasedPosition(float f, float g, float h) {
        this.f = f;
        this.g = g;
        this.h = h;
        float screenX = (f - g) * (2 * UNIT);
        float screenY = (f + g) * UNIT + h * (2 * UNIT);
        sprite.setOriginBasedPosition(screenX, screenY);
    }
    public void setOrigin(float originX, float originY) {
        sprite.setOrigin(originX, originY);
    }

    public void setOriginCenter() {
        sprite.setOriginCenter();
    }

    /**
     * Gets a float that roughly determines how far this IsoSprite is from the viewer/camera, for sorting purposes.
     * This won't work for IsoSprites that can have f or g go outside the -1023 to 1023 range.
     * The distance this returns is only useful relative to other results of this method, not in-general position.
     * @return an estimate of the distance this IsoSprite is from the viewer/camera, in no particular scale.
     */
    public float getViewDistance() {
        return (h + h - f - g) + (f - g) * (1f/2048);
    }

    public void draw(Batch batch) {
        sprite.draw(batch);
    }

    public void draw(Batch batch, float alphaModulation) {
        sprite.draw(batch, alphaModulation);
    }
}
