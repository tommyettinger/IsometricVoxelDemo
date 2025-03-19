/*
 * Copyright (c) 2022-2023 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gdx.liftoff.util;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.NumberUtils;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class MiniNoise {

    /**
     * "Standard" layered octaves of noise, where each octave has a different frequency and weight.
     * Tends to look cloudy with more octaves, and generally like a natural process. This only has
     * an effect with 2 octaves or more.
     * <br>
     * Meant to be used with {@link #setMode(int)}.
     */
    public static final int FBM = 0;
    /**
     * A less common way to layer octaves of noise, where most results are biased toward higher values,
     * but "valleys" show up filled with much lower values.
     * This probably has some good uses in 3D or higher noise, but it isn't used too frequently.
     * <br>
     * Meant to be used with {@link #setMode(int)}.
     */
    public static final int BILLOW = 1;
    /**
     * A way to layer octaves of noise so most values are biased toward low values but "ridges" of high
     * values run across the noise. This can be a good way of highlighting the least-natural aspects of
     * some kinds of noise; Perlin Noise has mostly ridges along 45-degree angles,
     * Simplex Noise has many ridges along a triangular grid, and so on. Foam Noise
     * and Honey Noise do well with this mode, though, and look something like lightning or
     * bubbling fluids, respectively. Using Foam or Honey will have this look natural, but Perlin in
     * particular will look unnatural if the grid is visible.
     * <br>
     * Meant to be used with {@link #setMode(int)}.
     */
    public static final int RIDGED = 2;
    /**
     * Layered octaves of noise, where each octave has a different frequency and weight, and the results of
     * earlier octaves affect the inputs to later octave calculations. Tends to look cloudy but with swirling
     * distortions, and generally like a natural process. This only has an effect with 2 octaves or more.
     * <br>
     * Meant to be used with {@link #setMode(int)}.
     */
    public static final int WARP = 3;

    /**
     * The names that correspond to the numerical mode constants, with the constant value matching the index here.
     */
    public static final String[] MODES = {"FBM", "Billow", "Ridged", "Warp"};

    public PerlueNoise wrapped;
    public float frequency;
    public int mode;
    protected int octaves;

    public MiniNoise() {
        this(new PerlueNoise(123), 123, 0.03125f, FBM, 1);

    }

    public MiniNoise(PerlueNoise toWrap){
        this(toWrap, toWrap.getSeed(), 0.03125f, FBM, 1);
    }

    public MiniNoise(PerlueNoise toWrap, float frequency, int mode, int octaves){
        this(toWrap, toWrap.getSeed(), frequency, mode, octaves);
    }
    public MiniNoise(PerlueNoise toWrap, int seed, float frequency, int mode, int octaves){
        wrapped = toWrap;
        setSeed(seed);
        this.frequency = frequency;
        this.mode = mode;
        this.octaves = octaves;
    }

    public MiniNoise(MiniNoise other) {
        setWrapped(other.getWrapped().copy());
        setSeed(other.getSeed());
        setFrequency(other.getFrequency());
        setFractalType(other.getFractalType());
        setFractalOctaves(other.getFractalOctaves());
    }

    public PerlueNoise getWrapped() {
        return wrapped;
    }

    public void setWrapped(PerlueNoise wrapped) {
        this.wrapped = wrapped;
    }

    public float getFrequency() {
        return frequency;
    }

    public void setFrequency(float frequency) {
        this.frequency = frequency;
    }

    /**
     * Wraps {@link #getFractalType()}.
     * @return an int between 0 and 4, corresponding to {@link #FBM}, {@link #BILLOW}, {@link #RIDGED}, or {@link #WARP}
     */
    public int getMode() {
        return getFractalType();
    }

    /**
     * Wraps {@link #setFractalType(int)}
     * @param mode an int between 0 and 4, corresponding to {@link #FBM}, {@link #BILLOW}, {@link #RIDGED}, or {@link #WARP}
     */
    public void setMode(int mode) {
        setFractalType(mode);
    }

    public int getFractalType() {
        return mode;
    }

    /**
     * @param mode an int between 0 and 4, corresponding to {@link #FBM}, {@link #BILLOW}, {@link #RIDGED}, or {@link #WARP}
     */
    public void setFractalType(int mode) {
        this.mode = mode;
    }

    /**
     * Wraps {@link #getFractalOctaves()}.
     * @return how many octaves this uses to increase detail
     */
    public int getOctaves() {
        return getFractalOctaves();
    }

    /**
     * Wraps {@link #setFractalOctaves(int)}.
     * @param octaves how many octaves to use to increase detail; must be at least 1.
     */
    public void setOctaves(int octaves) {
        setFractalOctaves(octaves);
    }

    public int getFractalOctaves() {
        return octaves;
    }

    /**
     * @param octaves how many octaves to use to increase detail; must be at least 1.
     */
    public void setFractalOctaves(int octaves) {
        this.octaves = Math.max(1, octaves);
    }

    public int getMinDimension() {
        return wrapped.getMinDimension();
    }

    public int getMaxDimension() {
        return wrapped.getMaxDimension();
    }

    public boolean hasEfficientSetSeed() {
        return wrapped.hasEfficientSetSeed();
    }

    public String getTag() {
        return "MiniNoise";
    }

    public String stringSerialize() {
        return "`" + wrapped.seed + '~' +
                frequency + '~' +
                mode + '~' +
                octaves + '`';
    }

    public MiniNoise stringDeserialize(String data) {
        int pos = data.indexOf('`', data.indexOf('`', 2) + 1)+1;
        setWrapped(new PerlueNoise(MathSupport.intFromDec(data, 1, pos)));
        setFrequency(MathSupport.floatFromDec(data, pos+1, pos = data.indexOf('~', pos+2)));
        setMode(MathSupport.intFromDec(data, pos+1, pos = data.indexOf('~', pos+2)));
        setOctaves(MathSupport.intFromDec(data, pos+1, pos = data.indexOf('`', pos+2)));
        return this;
    }

    public MiniNoise copy() {
        return new MiniNoise(this);
    }

    public String toString() {
        return "MiniNoise{" +
                "wrapped=" + wrapped +
                ", frequency=" + frequency +
                ", mode=" + mode +
                ", octaves=" + octaves +
                '}';
    }

    public String toHumanReadableString() {
        return getTag() + " wrapping (" + wrapped.toHumanReadableString() + "), with frequency " + frequency +
                ", " + octaves + " octaves, and mode " + MODES[mode];
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MiniNoise that = (MiniNoise) o;

        if (Float.compare(that.frequency, frequency) != 0) return false;
        if (mode != that.mode) return false;
        if (octaves != that.octaves) return false;
        return wrapped.equals(that.wrapped);
    }

    public int hashCode() {
        int result = wrapped.hashCode();
        result = 31 * result + NumberUtils.floatToIntBits(frequency + 0f);
        result = 31 * result + mode;
        result = 31 * result + octaves;
        return result;
    }

    // The big part.

    public float getNoise(float x) {
        final int seed = wrapped.getSeed();
        switch (mode) {
            default:
            case 0: return fbm(x * frequency, seed);
            case 1: return billow(x * frequency, seed);
            case 2: return ridged(x * frequency, seed);
            case 3: return warp(x * frequency, seed);
        }
    }

    public float getNoise(float x, float y) {
        final int seed = wrapped.getSeed();
        switch (mode) {
            default:
            case 0: return fbm(x * frequency, y * frequency, seed);
            case 1: return billow(x * frequency, y * frequency, seed);
            case 2: return ridged(x * frequency, y * frequency, seed);
            case 3: return warp(x * frequency, y * frequency, seed);
        }
    }

    public float getNoise(float x, float y, float z) {
        final int seed = wrapped.getSeed();
        switch (mode) {
            default:
            case 0: return fbm(x * frequency, y * frequency, z * frequency, seed);
            case 1: return billow(x * frequency, y * frequency, z * frequency, seed);
            case 2: return ridged(x * frequency, y * frequency, z * frequency, seed);
            case 3: return warp(x * frequency, y * frequency, z * frequency, seed);
        }
    }

    public float getNoise(float x, float y, float z, float w) {
        final int seed = wrapped.getSeed();
        switch (mode) {
            default:
            case 0: return fbm(x * frequency, y * frequency, z * frequency, w * frequency, seed);
            case 1: return billow(x * frequency, y * frequency, z * frequency, w * frequency, seed);
            case 2: return ridged(x * frequency, y * frequency, z * frequency, w * frequency, seed);
            case 3: return warp(x * frequency, y * frequency, z * frequency, w * frequency, seed);
        }
    }

    public float getNoise(float x, float y, float z, float w, float u) {
        final int seed = wrapped.getSeed();
        switch (mode) {
            default:
            case 0: return fbm(x * frequency, y * frequency, z * frequency, w * frequency, u * frequency, seed);
            case 1: return billow(x * frequency, y * frequency, z * frequency, w * frequency, u * frequency, seed);
            case 2: return ridged(x * frequency, y * frequency, z * frequency, w * frequency, u * frequency, seed);
            case 3: return warp(x * frequency, y * frequency, z * frequency, w * frequency, u * frequency, seed);
        }
    }

    public float getNoise(float x, float y, float z, float w, float u, float v) {
        final int seed = wrapped.getSeed();
        switch (mode) {
            default:
            case 0: return fbm(x * frequency, y * frequency, z * frequency, w * frequency, u * frequency, v * frequency, seed);
            case 1: return billow(x * frequency, y * frequency, z * frequency, w * frequency, u * frequency, v * frequency, seed);
            case 2: return ridged(x * frequency, y * frequency, z * frequency, w * frequency, u * frequency, v * frequency, seed);
            case 3: return warp(x * frequency, y * frequency, z * frequency, w * frequency, u * frequency, v * frequency, seed);
        }
    }

    public void setSeed(int seed) {
        wrapped.setSeed(seed);
    }

    public int getSeed() {
        return wrapped.getSeed();
    }

    public float getNoiseWithSeed(float x, int seed) {
        switch (mode) {
            default:
            case 0: return fbm(x * frequency, seed);
            case 1: return billow(x * frequency, seed);
            case 2: return ridged(x * frequency, seed);
            case 3: return warp(x * frequency, seed);
        }
    }

    public float getNoiseWithSeed(float x, float y, int seed) {
        switch (mode) {
            default:
            case 0: return fbm(x * frequency, y * frequency, seed);
            case 1: return billow(x * frequency, y * frequency, seed);
            case 2: return ridged(x * frequency, y * frequency, seed);
            case 3: return warp(x * frequency, y * frequency, seed);
        }
    }

    public float getNoiseWithSeed(float x, float y, float z, int seed) {
        switch (mode) {
            default:
            case 0: return fbm(x * frequency, y * frequency, z * frequency, seed);
            case 1: return billow(x * frequency, y * frequency, z * frequency, seed);
            case 2: return ridged(x * frequency, y * frequency, z * frequency, seed);
            case 3: return warp(x * frequency, y * frequency, z * frequency, seed);
        }
    }

    public float getNoiseWithSeed(float x, float y, float z, float w, int seed) {
        switch (mode) {
            default:
            case 0: return fbm(x * frequency, y * frequency, z * frequency, w * frequency, seed);
            case 1: return billow(x * frequency, y * frequency, z * frequency, w * frequency, seed);
            case 2: return ridged(x * frequency, y * frequency, z * frequency, w * frequency, seed);
            case 3: return warp(x * frequency, y * frequency, z * frequency, w * frequency, seed);
        }
    }

    public float getNoiseWithSeed(float x, float y, float z, float w, float u, int seed) {
        switch (mode) {
            default:
            case 0: return fbm(x * frequency, y * frequency, z * frequency, w * frequency, u * frequency, seed);
            case 1: return billow(x * frequency, y * frequency, z * frequency, w * frequency, u * frequency, seed);
            case 2: return ridged(x * frequency, y * frequency, z * frequency, w * frequency, u * frequency, seed);
            case 3: return warp(x * frequency, y * frequency, z * frequency, w * frequency, u * frequency, seed);
        }
    }

    public float getNoiseWithSeed(float x, float y, float z, float w, float u, float v, int seed) {
        switch (mode) {
            default:
            case 0: return fbm(x * frequency, y * frequency, z * frequency, w * frequency, u * frequency, v * frequency, seed);
            case 1: return billow(x * frequency, y * frequency, z * frequency, w * frequency, u * frequency, v * frequency, seed);
            case 2: return ridged(x * frequency, y * frequency, z * frequency, w * frequency, u * frequency, v * frequency, seed);
            case 3: return warp(x * frequency, y * frequency, z * frequency, w * frequency, u * frequency, v * frequency, seed);
        }
    }

    // 1D

    protected float fbm(float x, int seed) {
        float sum = wrapped.getNoiseWithSeed(x, seed);
        if(octaves <= 1) return sum;

        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;

            amp *= 0.5f;
            sum += wrapped.getNoiseWithSeed(x, seed + i) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }
    protected float billow(float x, int seed) {
        float sum = Math.abs(wrapped.getNoiseWithSeed(x, seed)) * 2 - 1;
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;

            amp *= 0.5f;
            sum += (Math.abs(wrapped.getNoiseWithSeed(x, seed + i)) * 2 - 1) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }

    protected float ridged(float x, int seed) {
        float sum = 0f, exp = 1f, correction = 0f, spike;
        for (int i = 0; i < octaves; i++) {
            spike = 1f - Math.abs(wrapped.getNoiseWithSeed(x, seed + i));
            sum += spike * exp;
            correction += (exp *= 0.5f);
            x *= 2f;
        }
        return sum / correction - 1f;
    }

    protected float warp(float x, int seed) {
        float latest = wrapped.getNoiseWithSeed(x, seed);
        if(octaves <= 1) return latest;

        float sum = latest;
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x = x * 2f;
            final float idx = latest * 180;
            float a = MathUtils.sinDeg(idx);
            amp *= 0.5f;
            sum += (latest = wrapped.getNoiseWithSeed(x + a, seed + i)) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }

    // 2D

    protected float fbm(float x, float y, int seed) {
        float sum = wrapped.getNoiseWithSeed(x, y, seed);
        if(octaves <= 1) return sum;

        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;
            y *= 2f;

            amp *= 0.5f;
            sum += wrapped.getNoiseWithSeed(x, y, seed + i) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }
    protected float billow(float x, float y, int seed) {
        float sum = Math.abs(wrapped.getNoiseWithSeed(x, y, seed)) * 2 - 1;
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;
            y *= 2f;

            amp *= 0.5f;
            sum += (Math.abs(wrapped.getNoiseWithSeed(x, y, seed + i)) * 2 - 1) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }

    protected float ridged(float x, float y, int seed) {
        float sum = 0f, exp = 1f, correction = 0f, spike;
        for (int i = 0; i < octaves; i++) {
            spike = 1f - Math.abs(wrapped.getNoiseWithSeed(x, y, seed + i));
            sum += spike * exp;
            correction += (exp *= 0.5f);
            x *= 2f;
            y *= 2f;
        }
        return sum / correction - 1f;
    }

    protected float warp(float x, float y, int seed) {
        float latest = wrapped.getNoiseWithSeed(x, y, seed);
        if(octaves <= 1) return latest;

        float sum = latest;
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x = x * 2f;
            y = y * 2f;
            final float idx = latest * 180;
            float a = MathUtils.sinDeg(idx);
            float b = MathUtils.sinDeg(idx + (180/2f));
            amp *= 0.5f;
            sum += (latest = wrapped.getNoiseWithSeed(x + a, y + b, seed + i)) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }

    // 3D

    protected float fbm(float x, float y, float z, int seed) {
        float sum = wrapped.getNoiseWithSeed(x, y, z, seed);
        if(octaves <= 1) return sum;

        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;
            y *= 2f;
            z *= 2f;

            amp *= 0.5f;
            sum += wrapped.getNoiseWithSeed(x, y, z, seed + i) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }
    protected float billow(float x, float y, float z, int seed) {
        float sum = Math.abs(wrapped.getNoiseWithSeed(x, y, z, seed)) * 2 - 1;
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;
            y *= 2f;
            z *= 2f;

            amp *= 0.5f;
            sum += (Math.abs(wrapped.getNoiseWithSeed(x, y, z, seed + i)) * 2 - 1) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }

    protected float ridged(float x, float y, float z, int seed) {
        float sum = 0f, exp = 1f, correction = 0f, spike;
        for (int i = 0; i < octaves; i++) {
            spike = 1f - Math.abs(wrapped.getNoiseWithSeed(x, y, z, seed + i));
            sum += spike * exp;
            correction += (exp *= 0.5f);
            x *= 2f;
            y *= 2f;
            z *= 2f;
        }
        return sum / correction - 1f;
    }

    protected float warp(float x, float y, float z, int seed) {
        float latest = wrapped.getNoiseWithSeed(x, y, z, seed);
        if(octaves <= 1) return latest;

        float sum = latest;
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;
            y *= 2f;
            z *= 2f;

            final float idx = latest * 180;
            float a = MathUtils.sinDeg(idx);
            float b = MathUtils.sinDeg(idx + (180/3f));
            float c = MathUtils.sinDeg(idx + (180*2/3f));

            amp *= 0.5f;
            sum += (latest = wrapped.getNoiseWithSeed(x + a, y + b, z + c, seed + i)) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }

    // 4D

    protected float fbm(float x, float y, float z, float w, int seed) {
        float sum = wrapped.getNoiseWithSeed(x, y, z, w, seed);
        if(octaves <= 1) return sum;

        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;
            y *= 2f;
            z *= 2f;
            w *= 2f;

            amp *= 0.5f;
            sum += wrapped.getNoiseWithSeed(x, y, z, w, seed + i) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }
    protected float billow(float x, float y, float z, float w, int seed) {
        float sum = Math.abs(wrapped.getNoiseWithSeed(x, y, z, w, seed)) * 2 - 1;
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;
            y *= 2f;
            z *= 2f;
            w *= 2f;

            amp *= 0.5f;
            sum += (Math.abs(wrapped.getNoiseWithSeed(x, y, z, w, seed + i)) * 2 - 1) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }

    protected float ridged(float x, float y, float z, float w, int seed) {
        float sum = 0f, exp = 1f, correction = 0f, spike;
        for (int i = 0; i < octaves; i++) {
            spike = 1f - Math.abs(wrapped.getNoiseWithSeed(x, y, z, w, seed + i));
            sum += spike * exp;
            correction += (exp *= 0.5f);
            x *= 2f;
            y *= 2f;
            z *= 2f;
            w *= 2f;
        }
        return sum / correction - 1f;
    }

    protected float warp(float x, float y, float z, float w, int seed) {
        float latest = wrapped.getNoiseWithSeed(x, y, z, w, seed);
        if(octaves <= 1) return latest;

        float sum = latest;
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;
            y *= 2f;
            z *= 2f;
            w *= 2f;

            final float idx = latest * 180;
            float a = MathUtils.sinDeg(idx);
            float b = MathUtils.sinDeg(idx + (180/4f));
            float c = MathUtils.sinDeg(idx + (180*2/4f));
            float d = MathUtils.sinDeg(idx + (180*3/4f));

            amp *= 0.5f;
            sum += (latest = wrapped.getNoiseWithSeed(x + a, y + b, z + c, w + d, seed + i)) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }

    // 5D

    protected float fbm(float x, float y, float z, float w, float u, int seed) {
        float sum = wrapped.getNoiseWithSeed(x, y, z, w, u, seed);
        if(octaves <= 1) return sum;

        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;
            y *= 2f;
            z *= 2f;
            w *= 2f;
            u *= 2f;

            amp *= 0.5f;
            sum += wrapped.getNoiseWithSeed(x, y, z, w, u, seed + i) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }
    protected float billow(float x, float y, float z, float w, float u, int seed) {
        float sum = Math.abs(wrapped.getNoiseWithSeed(x, y, z, w, u, seed)) * 2 - 1;
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;
            y *= 2f;
            z *= 2f;
            w *= 2f;
            u *= 2f;

            amp *= 0.5f;
            sum += (Math.abs(wrapped.getNoiseWithSeed(x, y, z, w, u, seed + i)) * 2 - 1) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }

    protected float ridged(float x, float y, float z, float w, float u, int seed) {
        float sum = 0f, exp = 1f, correction = 0f, spike;
        for (int i = 0; i < octaves; i++) {
            spike = 1f - Math.abs(wrapped.getNoiseWithSeed(x, y, z, w, u, seed + i));
            sum += spike * exp;
            correction += (exp *= 0.5f);
            x *= 2f;
            y *= 2f;
            z *= 2f;
            w *= 2f;
            u *= 2f;
        }
        return sum / correction - 1f;
    }

    protected float warp(float x, float y, float z, float w, float u, int seed) {
        float latest = wrapped.getNoiseWithSeed(x, y, z, w, u, seed);
        if(octaves <= 1) return latest;

        float sum = latest;
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;
            y *= 2f;
            z *= 2f;
            w *= 2f;
            u *= 2f;

            final float idx = latest * 180;
            float a = MathUtils.sinDeg(idx);
            float b = MathUtils.sinDeg(idx + (180/5f));
            float c = MathUtils.sinDeg(idx + (180*2/5f));
            float d = MathUtils.sinDeg(idx + (180*3/5f));
            float e = MathUtils.sinDeg(idx + (180*4/5f));

            amp *= 0.5f;
            sum += (latest = wrapped.getNoiseWithSeed(x + a, y + b, z + c, w + d, u + e, seed + i)) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }

    // 6D

    protected float fbm(float x, float y, float z, float w, float u, float v, int seed) {
        float sum = wrapped.getNoiseWithSeed(x, y, z, w, u, v, seed);
        if(octaves <= 1) return sum;

        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;
            y *= 2f;
            z *= 2f;
            w *= 2f;
            u *= 2f;
            v *= 2f;

            amp *= 0.5f;
            sum += wrapped.getNoiseWithSeed(x, y, z, w, u, v, seed + i) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }
    protected float billow(float x, float y, float z, float w, float u, float v, int seed) {
        float sum = Math.abs(wrapped.getNoiseWithSeed(x, y, z, w, u, v, seed)) * 2 - 1;
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;
            y *= 2f;
            z *= 2f;
            w *= 2f;
            u *= 2f;
            v *= 2f;

            amp *= 0.5f;
            sum += (Math.abs(wrapped.getNoiseWithSeed(x, y, z, w, u, v, seed + i)) * 2 - 1) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }

    protected float ridged(float x, float y, float z, float w, float u, float v, int seed) {
        float sum = 0f, exp = 1f, correction = 0f, spike;
        for (int i = 0; i < octaves; i++) {
            spike = 1f - Math.abs(wrapped.getNoiseWithSeed(x, y, z, w, u, v, seed + i));
            sum += spike * exp;
            correction += (exp *= 0.5f);
            x *= 2f;
            y *= 2f;
            z *= 2f;
            w *= 2f;
            u *= 2f;
            v *= 2f;
        }
        return sum / correction - 1f;
    }

    protected float warp(float x, float y, float z, float w, float u, float v, int seed) {
        float latest = wrapped.getNoiseWithSeed(x, y, z, w, u, v, seed);
        if(octaves <= 1) return latest;

        float sum = latest;
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;
            y *= 2f;
            z *= 2f;
            w *= 2f;
            u *= 2f;
            v *= 2f;

            final float idx = latest * 180;
            float a = MathUtils.sinDeg(idx);
            float b = MathUtils.sinDeg(idx + (180/6f));
            float c = MathUtils.sinDeg(idx + (180*2/6f));
            float d = MathUtils.sinDeg(idx + (180*3/6f));
            float e = MathUtils.sinDeg(idx + (180*4/6f));
            float f = MathUtils.sinDeg(idx + (180*5/6f));

            amp *= 0.5f;
            sum += (latest = wrapped.getNoiseWithSeed(x + a, y + b, z + c, w + d, u + e, v + f, seed + i)) * amp;
        }

        return sum / (amp * ((1 << octaves) - 1));
    }
}
