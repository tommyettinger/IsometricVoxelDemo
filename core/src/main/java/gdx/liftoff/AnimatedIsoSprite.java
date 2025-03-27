package gdx.liftoff;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;

public class AnimatedIsoSprite extends IsoSprite {
    public Animation<? extends Sprite> animation;

    private AnimatedIsoSprite() {
        super();
    }

    public AnimatedIsoSprite(Animation<? extends Sprite> animation) {
        super(animation.getKeyFrame(0, true));
        this.animation = animation;
    }

    public AnimatedIsoSprite(Animation<? extends Sprite> animation, float f, float g, float h) {
        super(animation.getKeyFrame(0, true));
        this.animation = animation;
        setPosition(f, g, h);
    }

    @Override
    public void setPosition(float f, float g, float h) {
        this.f = f;
        this.g = g;
        this.h = h;
        float worldX = (f - g) * (2 * UNIT);
        float worldY = (f + g) * UNIT + h * (2 * UNIT);
        for (Sprite s : animation.getKeyFrames()) {
            s.setPosition(worldX, worldY);
        }
    }

    @Override
    public void setOriginBasedPosition(float f, float g, float h) {
        this.f = f;
        this.g = g;
        this.h = h;
        float worldX = (f - g) * (2 * UNIT);
        float worldY = (f + g) * UNIT + h * (2 * UNIT);
        for (Sprite s : animation.getKeyFrames()) {
            s.setOriginBasedPosition(worldX, worldY);
        }
    }

    @Override
    public void setOrigin(float originX, float originY) {
        for (Sprite s : animation.getKeyFrames()) {
            s.setOrigin(originX, originY);
        }
    }

    @Override
    public void setOriginCenter() {
        for (Sprite s : animation.getKeyFrames()) {
            s.setOriginCenter();
        }
    }

    /**
     * Changes the currently drawn Sprite based on the current key frame in {@link #animation} given {@code stateTime}.
     * This delegates to {@link #update(float, boolean)} with {@code looping} set to true.
     *
     * @param stateTime time, in seconds; typically since this was constructed or since the last time the game un-paused
     */
    @Override
    public AnimatedIsoSprite update(float stateTime) {
        return update(stateTime, true);
    }

    /**
     * Changes the currently drawn Sprite based on the current key frame in {@link #animation} given {@code stateTime}.
     * If {@code looping} is true, then sequential {@link Animation.PlayMode} settings will always loop, or if looping
     * is false, then if stateTime is greater than {@link Animation#getAnimationDuration()}, the last Sprite will be
     * used instead of wrapping around.
     *
     * @param stateTime time, in seconds; typically since this was constructed or since the last time the game un-paused
     * @param looping if true, the animation will always loop to the beginning; if false, it will end on the last Sprite
     *               in the animation if stateTime is too high
     */
    public AnimatedIsoSprite update(float stateTime, boolean looping) {
        super.setSprite(animation.getKeyFrame(stateTime, looping));
        return this;
    }
}
