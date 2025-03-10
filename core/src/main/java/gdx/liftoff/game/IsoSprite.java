package gdx.liftoff.game;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.GridPoint3;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.NumberUtils;
import gdx.liftoff.IsoEngine2D;

/**
 * Wraps a {@link Sprite} so its position can be set using isometric coordinates.
 * There are many possible isometric coordinate systems, so the one used here intentionally avoids calling the isometric
 * axes x, y, or z - here, the axes are f, g, and h, with mnemonics to help remember them. This uses an origin point at
 * the bottom center of the rotated rectangular map, typically the lowest corner of the map. The f and g axes are
 * diagonal, and correspond to movement at a shallow angle on both world x and world y. The h axis is used for
 * elevation, and corresponds to movement on world y. The mnemonics here work as if on a world map, with the origin
 * somewhere in Belgium or the Netherlands:
 * <ul>
 *     <li>The f axis is roughly the diagonal from France to Finland.</li>
 *     <li>The g axis is roughly the diagonal from Germany to Greenland (or Greece to Germany, or Greece to Greenland).</li>
 *     <li>The h axis is the vertical line from your heel to your head (or Hell to Heaven).</li>
 * </ul>
 */
public class IsoSprite implements Comparable<IsoSprite> {
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

    public IsoSprite(Sprite sprite, Vector3 position) {
        this.sprite = sprite;
        setPosition(position.x, position.y, position.z);
    }

    public IsoSprite(Sprite sprite, GridPoint3 position) {
        this.sprite = sprite;
        setPosition(position.x, position.y, position.z);
    }

    public void setPosition(float f, float g, float h) {
        this.f = f;
        this.g = g;
        this.h = h;
        float worldX = (f - g) * (2 * UNIT);
        float worldY = (f + g) * UNIT + h * (2 * UNIT);
        sprite.setPosition(worldX, worldY);
    }

    public void setPosition(GridPoint3 point) {
        setPosition(point.x, point.y, point.z);
    }

    public void setPosition(Vector3 point) {
        setPosition(point.x, point.y, point.z);
    }

    public void setOriginBasedPosition(float f, float g, float h) {
        this.f = f;
        this.g = g;
        this.h = h;
        float worldX = (f - g) * (2 * UNIT);
        float worldY = (f + g) * UNIT + h * (2 * UNIT);
        sprite.setOriginBasedPosition(worldX, worldY);
    }
    public void setOrigin(float originX, float originY) {
        sprite.setOrigin(originX, originY);
    }

    public void setOriginCenter() {
        sprite.setOriginCenter();
    }

    public float getOriginX() {
        return sprite.getOriginX();
    }

    public float getOriginY() {
        return sprite.getOriginY();
    }

    public Sprite getSprite() {
        return sprite;
    }

    public void setSprite(Sprite sprite) {
        float worldX = (f - g) * (2 * UNIT);
        float worldY = (f + g) * UNIT + h * (2 * UNIT);
        sprite.setPosition(worldX, worldY);
        sprite.setPackedColor(this.sprite.getPackedColor());
        sprite.setOrigin(this.sprite.getOriginX(), this.sprite.getOriginY());
        this.sprite = sprite;
    }

    public float getF() {
        return f;
    }

    public void setF(float f) {
        this.f = f;
    }

    public float getG() {
        return g;
    }

    public void setG(float g) {
        this.g = g;
    }

    public float getH() {
        return h;
    }

    public void setH(float h) {
        this.h = h;
    }

    /**
     * Gets a float that roughly determines how close this IsoSprite is to the viewer/camera, for sorting purposes.
     * This won't work for IsoSprites that can have f or g go outside the -1023 to 1023 range.
     * The distance this returns is only useful relative to other results of this method, not in-general position.
     * Higher returned values mean the IsoSprite is closer to the camera, and so should be rendered later.
     * @return an estimate of how close this IsoSprite is to the viewer/camera, in no particular scale
     */
    public float getViewDistance() {
        return (h + h - f - g) + (f - g) * (1f/2048);
    }
    public static float viewDistance(float f, float g, float h) {
        return (h + h - f - g) + (f - g) * (1f/2048);
    }
    public static float viewDistance(float f, float g, float h, float originF, float originG, float rotationDegrees) {
        float c = MathUtils.cosDeg(rotationDegrees), s = MathUtils.sinDeg(rotationDegrees);
        f -= originF;
        g -= originG;
        float rf = c * f - s * g + originF, rg = c * g + s * f + originG;
        return (h + h - rf - rg) + (rf - rg) * (1f/2048);
    }

    /**
     * Just like {@link #getViewDistance()}, but this returns an int for cases where sorting ints is easier.
     * Higher returned values mean the IsoSprite is closer to the camera, and so should be rendered later.
     * @return an int code that will be greater for IsoSprites that are closer to the camera
     */
    public int getSortCode() {
        int bits = NumberUtils.floatToIntBits((h + h - f - g) + (f - g) * (1f/2048) + 0f);
        return bits ^ (bits >> 31 & 0x7FFFFFFF);
    }

    public void draw(Batch batch) {
        sprite.draw(batch);
    }

    public void draw(Batch batch, float alphaModulation) {
        sprite.draw(batch, alphaModulation);
    }

    public void draw(Batch batch, float originF, float originG, float rotationDegrees) {
        float c = MathUtils.cosDeg(rotationDegrees);
        float s = MathUtils.sinDeg(rotationDegrees);
        float af = f - originF;
        float ag = g - originG;
        float rf = c * af - s * ag + originF;
        float rg = c * ag + s * af + originG;
        float worldX = (rf - rg) * (2 * UNIT);
        float worldY = (rf + rg) * UNIT + h * (2 * UNIT);
        sprite.setPosition(worldX, worldY);
        sprite.draw(batch);
    }

    public void draw(Batch batch, float alphaModulation, float originF, float originG, float rotationDegrees) {
        float c = MathUtils.cosDeg(rotationDegrees);
        float s = MathUtils.sinDeg(rotationDegrees);
        float af = f - originF;
        float ag = g - originG;
        float rf = c * af - s * ag + originF;
        float rg = c * ag + s * af + originG;
        float worldX = (rf - rg) * (2 * UNIT);
        float worldY = (rf + rg) * UNIT + h * (2 * UNIT);
        sprite.setPosition(worldX, worldY);
        sprite.draw(batch, alphaModulation);
    }

    /**
     * Does nothing here, but can be overridden in subclasses to do something with a current time.
     * @param stateTime time, typically in seconds, and typically since some event started (like creating this object)
     * @return this object, for chaining
     */
    public IsoSprite update(float stateTime) {
        return this;
    }

    @Override
    public int compareTo(IsoSprite other) {
        return NumberUtils.floatToIntBits(getViewDistance() - other.getViewDistance() + 0f);
    }
}
