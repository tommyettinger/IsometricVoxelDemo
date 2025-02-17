package gdx.liftoff.game;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;

public class AnimatedIsoSprite extends IsoSprite {
    public Animation<Sprite> animation;

    private AnimatedIsoSprite() {
        super();
    }

    public AnimatedIsoSprite(Animation<Sprite> animation) {
        super(animation.getKeyFrame(0, true));
        this.animation = animation;
    }

    public AnimatedIsoSprite(Animation<Sprite> animation, float f, float g, float h) {
        super(animation.getKeyFrame(0, true), f, g, h);
        this.animation = animation;
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

    public void updateAnimation(float stateTime) {
        updateAnimation(stateTime, true);
    }

    public void updateAnimation(float stateTime, boolean looping) {
        super.setSprite(animation.getKeyFrame(stateTime, looping));
    }
}
