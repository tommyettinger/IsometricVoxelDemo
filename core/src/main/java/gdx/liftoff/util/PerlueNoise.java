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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import static com.badlogic.gdx.math.MathUtils.floor;
import static com.badlogic.gdx.math.MathUtils.lerp;
import static gdx.liftoff.util.GradientVectors.*;

/**
 * A mix of "Classic" Perlin noise, written by Ken Perlin before he created Simplex Noise, with value noise calculated
 * at the same time. This uses cubic interpolation throughout (instead of the quintic interpolation used in Simplex
 * Noise, because cubic looks a little smoother and doesn't alternate as badly between sharp change and low change), and
 * has a single {@code int} seed. Perlue Noise can have significant grid-aligned and 45-degree-diagonal artifacts when
 * too few octaves are used, but sometimes this is irrelevant, such as when sampling 3D noise on the surface of a
 * sphere. These artifacts sometimes manifest as "waves" of quickly-changing and then slowly-changing noise, when 3D
 * noise uses time as the z axis.
 * <br>
 * This tends to look fairly different from vanilla PerlinNoise or ValueNoise; it is capable of more chaotic
 * arrangements of high and low values than either of those, but it still tends to have clusters of values of a specific
 * size more often than clusters with very different sizes.
 */
public class PerlueNoise {
    public static final PerlueNoise instance = new PerlueNoise();

    public static final float SCALE2 = 1.41421330f; //towardsZero(1f/ (float) Math.sqrt(2f / 4f));
    public static final float SCALE3 = 1.15470030f; //towardsZero(1f/ (float) Math.sqrt(3f / 4f));
    public static final float SCALE4 = 0.99999990f; //towardsZero(1f)                            ;
    public static final float SCALE5 = 0.89442706f; //towardsZero(1f/ (float) Math.sqrt(5f / 4f));
    public static final float SCALE6 = 0.81649643f; //towardsZero(1f/ (float) Math.sqrt(6f / 4f));

    public static final float EQ_ADD_2 = 1.0f / 0.85f;
    public static final float EQ_ADD_3 = 0.8f / 0.85f;
    public static final float EQ_ADD_4 = 0.6f / 0.85f;
    public static final float EQ_ADD_5 = 0.4f / 0.85f;
    public static final float EQ_ADD_6 = 0.2f / 0.85f;

    public static final float EQ_MUL_2 = 1.2535664f;
    public static final float EQ_MUL_3 = 1.2071217f;
    public static final float EQ_MUL_4 = 1.1588172f;
    public static final float EQ_MUL_5 = 1.1084094f;
    public static final float EQ_MUL_6 = 1.0555973f;

    public int seed;

    public PerlueNoise() {
        this(0x1337BEEF);
    }

    public PerlueNoise(final int seed) {
        this.seed = seed;
    }

    public PerlueNoise(PerlueNoise other) {
        this.seed = other.seed;
    }

    /**
     * Gets the minimum dimension supported by this generator, which is 1.
     *
     * @return the minimum supported dimension, 1
     */
    public int getMinDimension() {
        return 1;
    }

    /**
     * Gets the maximum dimension supported by this generator, which is 6.
     *
     * @return the maximum supported dimension, 6
     */
    public int getMaxDimension() {
        return 6;
    }

    /**
     * Returns true because this generator can be seeded with {@link #setSeed(int)} and retrieved with
     * {@link #getSeed()}.
     *
     * @return true
     */
    public boolean hasEfficientSetSeed() {
        return true;
    }

    /**
     * Sets the seed to the given int.
     * @param seed an int seed, with no restrictions
     */
    public void setSeed(int seed) {
        this.seed = seed;
    }

    /**
     * Gets the current seed of the generator, as an int.
     *
     * @return the current seed, as an int
     */
    public int getSeed() {
        return seed;
    }

    /**
     * Returns the constant String {@code "PerlueNoise"} that identifies this in serialized Strings.
     *
     * @return a short String constant that identifies this RawNoise type, {@code "PerlueNoise"}
     */
    public String getTag() {
        return "PerlueNoise";
    }

    /**
     * Creates a copy of this PerlueNoise, which should be a deep copy for any mutable state but can be shallow for immutable
     * types such as functions. This almost always just calls a copy constructor.
     *
     * @return a copy of this PerlueNoise
     */
    public PerlueNoise copy() {
        return new PerlueNoise(this.seed);
    }

    protected static float gradCoord2D(int seed, int x, int y,
                                       float xd, float yd) {
        final int h = hashAll(x, y, seed);
        final int hash = h & (255 << 1);
        return (h * 0x1p-32f) + xd * GRADIENTS_2D[hash] + yd * GRADIENTS_2D[hash + 1];
    }
    protected static float gradCoord3D(int seed, int x, int y, int z, float xd, float yd, float zd) {
        final int h = hashAll(x, y, z, seed);
        final int hash = h & (255 << 2);
        return (h * 0x1p-32f) + xd * GRADIENTS_3D[hash] + yd * GRADIENTS_3D[hash + 1] + zd * GRADIENTS_3D[hash + 2];
    }
    protected static float gradCoord4D(int seed, int x, int y, int z, int w,
                                       float xd, float yd, float zd, float wd) {
        final int h = hashAll(x, y, z, w, seed);
        final int hash = h & (255 << 2);
        return (h * 0x1p-32f) + xd * GRADIENTS_4D[hash] + yd * GRADIENTS_4D[hash + 1] + zd * GRADIENTS_4D[hash + 2] + wd * GRADIENTS_4D[hash + 3];
    }
    protected static float gradCoord5D(int seed, int x, int y, int z, int w, int u,
                                       float xd, float yd, float zd, float wd, float ud) {
        final int h = hashAll(x, y, z, w, u, seed);
        final int hash = h & (255 << 3);
        return (h * 0x1p-32f) + xd * GRADIENTS_5D[hash] + yd * GRADIENTS_5D[hash + 1] + zd * GRADIENTS_5D[hash + 2]
                + wd * GRADIENTS_5D[hash + 3] + ud * GRADIENTS_5D[hash + 4];
    }
    protected static float gradCoord6D(int seed, int x, int y, int z, int w, int u, int v,
                                       float xd, float yd, float zd, float wd, float ud, float vd) {
        final int h = hashAll(x, y, z, w, u, v, seed);
        final int hash = h & (255 << 3);
        return (h * 0x1p-32f) + xd * GRADIENTS_6D[hash] + yd * GRADIENTS_6D[hash + 1] + zd * GRADIENTS_6D[hash + 2]
                + wd * GRADIENTS_6D[hash + 3] + ud * GRADIENTS_6D[hash + 4] + vd * GRADIENTS_6D[hash + 5];
    }

    /**
     * Given inputs as {@code x} in the range -1.0 to 1.0 that are too biased towards 0.0, this "squashes" the range
     * softly to widen it and spread it away from 0.0 without increasing bias anywhere else.
     * <br>
     * This starts with a common sigmoid function, {@code x / sqrt(x * x + add)}, but instead of approaching -1 and 1
     * but never reaching them, this multiplies the result so the line crosses -1 when x is -1, and crosses 1 when x is
     * 1. It has a smooth derivative, if that matters to you.
     *
     * @param x a float between -1 and 1
     * @param add if greater than 1, this will have nearly no effect; the lower this goes below 1, the more this will
     *           separate results near the center of the range. This must be greater than or equal to 0.0
     * @param mul typically the result of calling {@code (float) Math.sqrt(add + 1f)}
     * @return a float with a slightly different distribution from {@code x}, but still between -1 and 1
     */
    public static float equalize(float x, float add, float mul) {
        return x * mul / (float) Math.sqrt(x * x + add);
    }

    public float getNoise(final float x) {
        return getNoiseWithSeed(x, seed);
    }

    /**
     * Sway smoothly using bicubic interpolation between 4 points (the two integers before t and the two after).
     * This pretty much never produces steep changes between peaks and valleys; this may make it more useful for things
     * like generating terrain that can be walked across in a side-scrolling game.
     *
     * @param t    a distance traveled; should change by less than 1 between calls, and should be less than about 10000
     * @param seed any long
     * @return a smoothly-interpolated swaying value between -1 and 1, both exclusive
     */
    public float getNoiseWithSeed(float t, int seed)
    {
        final long floor = (long) Math.floor(t);
        // what we add here ensures that at the very least, the upper half will have some non-zero bits.
        long s = ((seed & 0xFFFFFFFFL) ^ (seed >>> 16)) + 0x9E3779B97F4A7C15L;
        // fancy XOR-rotate-rotate is a way to mix bits both up and down without multiplication.
        s = (s ^ (s << 21 | s >>> 43) ^ (s << 50 | s >>> 14)) + floor;
        // we use a different technique here, relative to other wobble methods.
        // to avoid frequent multiplication and replace it with addition by constants, we track 3 variables, each of
        // which updates with a different large, negative long increment. when we want to get a result, we just XOR
        // m, n, and o, and use only the upper bits (by multiplying by a tiny fraction).
        final long m = s * 0xD1B54A32D192ED03L;
        final long n = s * 0xABC98388FB8FAC03L;
        final long o = s * 0x8CB92BA72F3D8DD7L;

        final float a = (m ^ n ^ o);
        final float b = (m + 0xD1B54A32D192ED03L ^ n + 0xABC98388FB8FAC03L ^ o + 0x8CB92BA72F3D8DD7L);
        final float c = (m + 0xA36A9465A325DA06L ^ n + 0x57930711F71F5806L ^ o + 0x1972574E5E7B1BAEL);
        final float d = (m + 0x751FDE9874B8C709L ^ n + 0x035C8A9AF2AF0409L ^ o + 0xA62B82F58DB8A985L);

        // get the fractional part of t.
        t -= floor;
        // this is bicubic interpolation, inlined
        final float p = (d - c) - (a - b);
        // 7.7.228014483236334E-20 , or 0x1.5555555555428p-64 , is just inside {@code -2f/3f/Long.MIN_VALUE} .
        // it gets us about as close as we can go to 1.0 .
        return (t * (t * t * p + t * (a - b - p) + c - a) + b) * 7.228014E-20f;
    }


    public float getNoise(final float x, final float y) {
        return getNoiseWithSeed(x, y, seed);
    }

    public float getNoiseWithSeed(float x, float y, final int seed) {
        final int
                xi = floor(x), x0 = xi * X2,
                yi = floor(y), y0 = yi * Y2;
        final float xf = x - xi, yf = y - yi;

        final float xa = xf * xf * (1 - xf - xf + 2);//* xf * (xf * (xf * 6.0f - 15.0f) + 9.999998f);
        final float ya = yf * yf * (1 - yf - yf + 2);//* yf * (yf * (yf * 6.0f - 15.0f) + 9.999998f);
        return
                equalize(lerp(lerp(gradCoord2D(seed, x0, y0, xf, yf), gradCoord2D(seed, x0+ X2, y0, xf - 1, yf), xa),
                                lerp(gradCoord2D(seed, x0, y0+ Y2, xf, yf-1), gradCoord2D(seed, x0+ X2, y0+ Y2, xf - 1, yf - 1), xa),
                                ya) * SCALE2, EQ_ADD_2, EQ_MUL_2);//* 0.875;// * 1.4142;
    }

    public float getNoise(final float x, final float y, final float z) {
        return getNoiseWithSeed(x, y, z, seed);
    }

    public float getNoiseWithSeed(float x, float y, float z, final int seed) {
        final int
                xi = floor(x), x0 = xi * X3,
                yi = floor(y), y0 = yi * Y3,
                zi = floor(z), z0 = zi * Z3;
        final float xf = x - xi, yf = y - yi, zf = z - zi;

        final float xa = xf * xf * (1 - xf - xf + 2);//* xf * (xf * (xf * 6.0f - 15.0f) + 9.999998f);
        final float ya = yf * yf * (1 - yf - yf + 2);//* yf * (yf * (yf * 6.0f - 15.0f) + 9.999998f);
        final float za = zf * zf * (1 - zf - zf + 2);//* zf * (zf * (zf * 6.0f - 15.0f) + 9.999998f);
         return
                 equalize(
                         lerp(
                                 lerp(
                                         lerp(
                                                 gradCoord3D(seed, x0, y0, z0, xf, yf, zf),
                                                 gradCoord3D(seed, x0+ X3, y0, z0, xf - 1, yf, zf),
                                                 xa),
                                         lerp(
                                                 gradCoord3D(seed, x0, y0+ Y3, z0, xf, yf-1, zf),
                                                 gradCoord3D(seed, x0+ X3, y0+ Y3, z0, xf - 1, yf - 1, zf),
                                                 xa),
                                         ya),
                                 lerp(
                                         lerp(
                                                 gradCoord3D(seed, x0, y0, z0+ Z3, xf, yf, zf-1),
                                                 gradCoord3D(seed, x0+ X3, y0, z0+ Z3, xf - 1, yf, zf-1),
                                                 xa),
                                         lerp(
                                                 gradCoord3D(seed, x0, y0+ Y3, z0+ Z3, xf, yf-1, zf-1),
                                                 gradCoord3D(seed, x0+ X3, y0+ Y3, z0+ Z3, xf - 1, yf - 1, zf-1),
                                                 xa),
                                         ya),
                                 za) * SCALE3, EQ_ADD_3, EQ_MUL_3); // 1.0625f
    }

    public float getNoise(final float x, final float y, final float z, final float w) {
        return getNoiseWithSeed(x, y, z, w, seed);
    }

    public float getNoiseWithSeed(float x, float y, float z, float w, final int seed) {
        final int
                xi = floor(x), x0 = xi * X4,
                yi = floor(y), y0 = yi * Y4,
                zi = floor(z), z0 = zi * Z4,
                wi = floor(w), w0 = wi * W4;
        final float xf = x - xi, yf = y - yi, zf = z - zi, wf = w - wi;

        final float xa = xf * xf * (1 - xf - xf + 2);//* xf * (xf * (xf * 6.0f - 15.0f) + 9.999998f);
        final float ya = yf * yf * (1 - yf - yf + 2);//* yf * (yf * (yf * 6.0f - 15.0f) + 9.999998f);
        final float za = zf * zf * (1 - zf - zf + 2);//* zf * (zf * (zf * 6.0f - 15.0f) + 9.999998f);
        final float wa = wf * wf * (1 - wf - wf + 2);//* wf * (wf * (wf * 6.0f - 15.0f) + 9.999998f);
        return
                equalize(
                        lerp(
                                lerp(
                                        lerp(
                                                lerp(
                                                        gradCoord4D(seed, x0, y0, z0, w0, xf, yf, zf, wf),
                                                        gradCoord4D(seed, x0+ X4, y0, z0, w0, xf - 1, yf, zf, wf),
                                                        xa),
                                                lerp(
                                                        gradCoord4D(seed, x0, y0+ Y4, z0, w0, xf, yf-1, zf, wf),
                                                        gradCoord4D(seed, x0+ X4, y0+ Y4, z0, w0, xf - 1, yf - 1, zf, wf),
                                                        xa),
                                                ya),
                                        lerp(
                                                lerp(
                                                        gradCoord4D(seed, x0, y0, z0+ Z4, w0, xf, yf, zf-1, wf),
                                                        gradCoord4D(seed, x0+ X4, y0, z0+ Z4, w0, xf - 1, yf, zf-1, wf),
                                                        xa),
                                                lerp(
                                                        gradCoord4D(seed, x0, y0+ Y4, z0+ Z4, w0, xf, yf-1, zf-1, wf),
                                                        gradCoord4D(seed, x0+ X4, y0+ Y4, z0+ Z4, w0, xf - 1, yf - 1, zf-1, wf),
                                                        xa),
                                                ya),
                                        za),
                                lerp(
                                        lerp(
                                                lerp(
                                                        gradCoord4D(seed, x0, y0, z0, w0+ W4, xf, yf, zf, wf - 1),
                                                        gradCoord4D(seed, x0+ X4, y0, z0, w0+ W4, xf - 1, yf, zf, wf - 1),
                                                        xa),
                                                lerp(
                                                        gradCoord4D(seed, x0, y0+ Y4, z0, w0+ W4, xf, yf-1, zf, wf - 1),
                                                        gradCoord4D(seed, x0+ X4, y0+ Y4, z0, w0+ W4, xf - 1, yf - 1, zf, wf - 1),
                                                        xa),
                                                ya),
                                        lerp(
                                                lerp(
                                                        gradCoord4D(seed, x0, y0, z0+ Z4, w0+ W4, xf, yf, zf-1, wf - 1),
                                                        gradCoord4D(seed, x0+ X4, y0, z0+ Z4, w0+ W4, xf - 1, yf, zf-1, wf - 1),
                                                        xa),
                                                lerp(
                                                        gradCoord4D(seed, x0, y0+ Y4, z0+ Z4, w0+ W4, xf, yf-1, zf-1, wf - 1),
                                                        gradCoord4D(seed, x0+ X4, y0+ Y4, z0+ Z4, w0+ W4, xf - 1, yf - 1, zf-1, wf - 1),
                                                        xa),
                                                ya),
                                        za),
                                wa) * SCALE4, EQ_ADD_4, EQ_MUL_4);//0.555f);
    }


    public float getNoise(final float x, final float y, final float z, final float w, final float u) {
        return getNoiseWithSeed(x, y, z, w, u, seed);
    }

    public float getNoiseWithSeed(float x, float y, float z, float w, float u, final int seed) {
        final int
                xi = floor(x), x0 = xi * X5,
                yi = floor(y), y0 = yi * Y5,
                zi = floor(z), z0 = zi * Z5,
                wi = floor(w), w0 = wi * W5,
                ui = floor(u), u0 = ui * U5;
        final float xf = x - xi, yf = y - yi, zf = z - zi, wf = w - wi, uf = u - ui;

        final float xa = xf * xf * (1 - xf - xf + 2);// * xf * (xf * (xf * 6.0f - 15.0f) + 9.999998f);
        final float ya = yf * yf * (1 - yf - yf + 2);// * yf * (yf * (yf * 6.0f - 15.0f) + 9.999998f);
        final float za = zf * zf * (1 - zf - zf + 2);// * zf * (zf * (zf * 6.0f - 15.0f) + 9.999998f);
        final float wa = wf * wf * (1 - wf - wf + 2);// * wf * (wf * (wf * 6.0f - 15.0f) + 9.999998f);
        final float ua = uf * uf * (1 - uf - uf + 2);// * uf * (uf * (uf * 6.0f - 15.0f) + 9.999998f);
        return
                equalize(
                lerp(lerp(
                        lerp(
                                lerp(
                                        lerp(gradCoord5D(seed, x0, y0, z0, w0, u0, xf, yf, zf, wf, uf),
                                                gradCoord5D(seed, x0+ X5, y0, z0, w0, u0, xf-1, yf, zf, wf, uf), xa),
                                        lerp(gradCoord5D(seed, x0, y0+ Y5, z0, w0, u0, xf, yf-1, zf, wf, uf),
                                                gradCoord5D(seed, x0+ X5, y0+ Y5, z0, w0, u0, xf-1, yf-1, zf, wf, uf), xa),
                                        ya),
                                lerp(
                                        lerp(gradCoord5D(seed, x0, y0, z0+ Z5, w0, u0, xf, yf, zf-1, wf, uf),
                                                gradCoord5D(seed, x0+ X5, y0, z0+ Z5, w0, u0, xf-1, yf, zf-1, wf, uf), xa),
                                        lerp(gradCoord5D(seed, x0, y0+ Y5, z0+ Z5, w0, u0, xf, yf-1, zf-1, wf, uf),
                                                gradCoord5D(seed, x0+ X5, y0+ Y5, z0+ Z5, w0, u0, xf-1, yf-1, zf-1, wf, uf), xa),
                                        ya),
                                za),
                        lerp(
                                lerp(
                                        lerp(gradCoord5D(seed, x0, y0, z0, w0+ W5, u0, xf, yf, zf, wf-1, uf),
                                                gradCoord5D(seed, x0+ X5, y0, z0, w0+ W5, u0, xf-1, yf, zf, wf-1, uf), xa),
                                        lerp(gradCoord5D(seed, x0, y0+ Y5, z0, w0+ W5, u0, xf, yf-1, zf, wf-1, uf),
                                                gradCoord5D(seed, x0+ X5, y0+ Y5, z0, w0+ W5, u0, xf-1, yf-1, zf, wf-1, uf), xa),
                                        ya),
                                lerp(
                                        lerp(gradCoord5D(seed, x0, y0, z0+ Z5, w0+ W5, u0, xf, yf, zf-1, wf-1, uf),
                                                gradCoord5D(seed, x0+ X5, y0, z0+ Z5, w0+ W5, u0, xf-1, yf, zf-1, wf-1, uf), xa),
                                        lerp(gradCoord5D(seed, x0, y0+ Y5, z0+ Z5, w0+ W5, u0, xf, yf-1, zf-1, wf-1, uf),
                                                gradCoord5D(seed, x0+ X5, y0+ Y5, z0+ Z5, w0+ W5, u0, xf-1, yf-1, zf-1, wf-1, uf), xa),
                                        ya),
                                za),
                        wa),
                        lerp(
                                lerp(
                                        lerp(
                                                lerp(gradCoord5D(seed, x0, y0, z0, w0, u0+ U5, xf, yf, zf, wf, uf-1),
                                                        gradCoord5D(seed, x0+ X5, y0, z0, w0, u0+ U5, xf-1, yf, zf, wf, uf-1), xa),
                                                lerp(gradCoord5D(seed, x0, y0+ Y5, z0, w0, u0+ U5, xf, yf-1, zf, wf, uf-1),
                                                        gradCoord5D(seed, x0+ X5, y0+ Y5, z0, w0, u0+ U5, xf-1, yf-1, zf, wf, uf-1), xa),
                                                ya),
                                        lerp(
                                                lerp(gradCoord5D(seed, x0, y0, z0+ Z5, w0, u0+ U5, xf, yf, zf-1, wf, uf-1),
                                                        gradCoord5D(seed, x0+ X5, y0, z0+ Z5, w0, u0+ U5, xf-1, yf, zf-1, wf, uf-1), xa),
                                                lerp(gradCoord5D(seed, x0, y0+ Y5, z0+ Z5, w0, u0+ U5, xf, yf-1, zf-1, wf, uf-1),
                                                        gradCoord5D(seed, x0+ X5, y0+ Y5, z0+ Z5, w0, u0+ U5, xf-1, yf-1, zf-1, wf, uf-1), xa),
                                                ya),
                                        za),
                                lerp(
                                        lerp(
                                                lerp(gradCoord5D(seed, x0, y0, z0, w0+ W5, u0+ U5, xf, yf, zf, wf-1, uf-1),
                                                        gradCoord5D(seed, x0+ X5, y0, z0, w0+ W5, u0+ U5, xf-1, yf, zf, wf-1, uf-1), xa),
                                                lerp(gradCoord5D(seed, x0, y0+ Y5, z0, w0+ W5, u0+ U5, xf, yf-1, zf, wf-1, uf-1),
                                                        gradCoord5D(seed, x0+ X5, y0+ Y5, z0, w0+ W5, u0+ U5, xf-1, yf-1, zf, wf-1, uf-1), xa),
                                                ya),
                                        lerp(
                                                lerp(gradCoord5D(seed, x0, y0, z0+ Z5, w0+ W5, u0+ U5, xf, yf, zf-1, wf-1, uf-1),
                                                        gradCoord5D(seed, x0+ X5, y0, z0+ Z5, w0+ W5, u0+ U5, xf-1, yf, zf-1, wf-1, uf-1), xa),
                                                lerp(gradCoord5D(seed, x0, y0+ Y5, z0+ Z5, w0+ W5, u0+ U5, xf, yf-1, zf-1, wf-1, uf-1),
                                                        gradCoord5D(seed, x0+ X5, y0+ Y5, z0+ Z5, w0+ W5, u0+ U5, xf-1, yf-1, zf-1, wf-1, uf-1), xa),
                                                ya),
                                        za),
                                wa),
                        ua) * SCALE5, EQ_ADD_5, EQ_MUL_5);//0.7777777f);
    }

    public float getNoise(final float x, final float y, final float z, final float w, final float u, final float v) {
        return getNoiseWithSeed(x, y, z, w, u, v, seed);
    }

    public float getNoiseWithSeed(float x, float y, float z, float w, float u, float v, final int seed) {
        final int
                xi = floor(x), x0 = xi * X6,
                yi = floor(y), y0 = yi * Y6,
                zi = floor(z), z0 = zi * Z6,
                wi = floor(w), w0 = wi * W6,
                ui = floor(u), u0 = ui * U6,
                vi = floor(v), v0 = vi * V6;
        final float xf = x - xi, yf = y - yi, zf = z - zi, wf = w - wi, uf = u - ui, vf = v - vi;
        final float xa = xf * xf * (1 - xf - xf + 2);// * xf * (xf * (xf * 6.0f - 15.0f) + 9.999998f);
        final float ya = yf * yf * (1 - yf - yf + 2);// * yf * (yf * (yf * 6.0f - 15.0f) + 9.999998f);
        final float za = zf * zf * (1 - zf - zf + 2);// * zf * (zf * (zf * 6.0f - 15.0f) + 9.999998f);
        final float wa = wf * wf * (1 - wf - wf + 2);// * wf * (wf * (wf * 6.0f - 15.0f) + 9.999998f);
        final float ua = uf * uf * (1 - uf - uf + 2);// * uf * (uf * (uf * 6.0f - 15.0f) + 9.999998f);
        final float va = vf * vf * (1 - vf - vf + 2);// * vf * (vf * (vf * 6.0f - 15.0f) + 9.999998f);
        return equalize(
                lerp(
                        lerp(
                                lerp(
                                        lerp(
                                                lerp(
                                                        lerp(gradCoord6D(seed, x0, y0, z0, w0, u0, v0, xf, yf, zf, wf, uf, vf),
                                                                gradCoord6D(seed, x0+ X6, y0, z0, w0, u0, v0, xf - 1, yf, zf, wf, uf, vf), xa),
                                                        lerp(gradCoord6D(seed, x0, y0+ Y6, z0, w0, u0, v0, xf, yf - 1, zf, wf, uf, vf),
                                                                gradCoord6D(seed, x0+ X6, y0+ Y6, z0, w0, u0, v0, xf - 1, yf - 1, zf, wf, uf, vf), xa),
                                                        ya),
                                                lerp(
                                                        lerp(gradCoord6D(seed, x0, y0, z0+ Z6, w0, u0, v0, xf, yf, zf - 1, wf, uf, vf),
                                                                gradCoord6D(seed, x0+ X6, y0, z0+ Z6, w0, u0, v0, xf - 1, yf, zf - 1, wf, uf, vf), xa),
                                                        lerp(gradCoord6D(seed, x0, y0+ Y6, z0+ Z6, w0, u0, v0, xf, yf - 1, zf - 1, wf, uf, vf),
                                                                gradCoord6D(seed, x0+ X6, y0+ Y6, z0+ Z6, w0, u0, v0, xf - 1, yf - 1, zf - 1, wf, uf, vf), xa),
                                                        ya),
                                                za),
                                        lerp(
                                                lerp(
                                                        lerp(gradCoord6D(seed, x0, y0, z0, w0+ W6, u0, v0, xf, yf, zf, wf - 1, uf, vf),
                                                                gradCoord6D(seed, x0+ X6, y0, z0, w0+ W6, u0, v0, xf - 1, yf, zf, wf - 1, uf, vf), xa),
                                                        lerp(gradCoord6D(seed, x0, y0+ Y6, z0, w0+ W6, u0, v0, xf, yf - 1, zf, wf - 1, uf, vf),
                                                                gradCoord6D(seed, x0+ X6, y0+ Y6, z0, w0+ W6, u0, v0, xf - 1, yf - 1, zf, wf - 1, uf, vf), xa),
                                                        ya),
                                                lerp(
                                                        lerp(gradCoord6D(seed, x0, y0, z0+ Z6, w0+ W6, u0, v0, xf, yf, zf - 1, wf - 1, uf, vf),
                                                                gradCoord6D(seed, x0+ X6, y0, z0+ Z6, w0+ W6, u0, v0, xf - 1, yf, zf - 1, wf - 1, uf, vf), xa),
                                                        lerp(gradCoord6D(seed, x0, y0+ Y6, z0+ Z6, w0+ W6, u0, v0, xf, yf - 1, zf - 1, wf - 1, uf, vf),
                                                                gradCoord6D(seed, x0+ X6, y0+ Y6, z0+ Z6, w0+ W6, u0, v0, xf - 1, yf - 1, zf - 1, wf - 1, uf, vf), xa),
                                                        ya),
                                                za),
                                        wa),
                                lerp(
                                        lerp(
                                                lerp(
                                                        lerp(gradCoord6D(seed, x0, y0, z0, w0, u0+ U6, v0, xf, yf, zf, wf, uf - 1, vf),
                                                                gradCoord6D(seed, x0+ X6, y0, z0, w0, u0+ U6, v0, xf - 1, yf, zf, wf, uf - 1, vf), xa),
                                                        lerp(gradCoord6D(seed, x0, y0+ Y6, z0, w0, u0+ U6, v0, xf, yf - 1, zf, wf, uf - 1, vf),
                                                                gradCoord6D(seed, x0+ X6, y0+ Y6, z0, w0, u0+ U6, v0, xf - 1, yf - 1, zf, wf, uf - 1, vf), xa),
                                                        ya),
                                                lerp(
                                                        lerp(gradCoord6D(seed, x0, y0, z0+ Z6, w0, u0+ U6, v0, xf, yf, zf - 1, wf, uf - 1, vf),
                                                                gradCoord6D(seed, x0+ X6, y0, z0+ Z6, w0, u0+ U6, v0, xf - 1, yf, zf - 1, wf, uf - 1, vf), xa),
                                                        lerp(gradCoord6D(seed, x0, y0+ Y6, z0+ Z6, w0, u0+ U6, v0, xf, yf - 1, zf - 1, wf, uf - 1, vf),
                                                                gradCoord6D(seed, x0+ X6, y0+ Y6, z0+ Z6, w0, u0+ U6, v0, xf - 1, yf - 1, zf - 1, wf, uf - 1, vf), xa),
                                                        ya),
                                                za),
                                        lerp(
                                                lerp(
                                                        lerp(gradCoord6D(seed, x0, y0, z0, w0+ W6, u0+ U6, v0, xf, yf, zf, wf - 1, uf - 1, vf),
                                                                gradCoord6D(seed, x0+ X6, y0, z0, w0+ W6, u0+ U6, v0, xf - 1, yf, zf, wf - 1, uf - 1, vf), xa),
                                                        lerp(gradCoord6D(seed, x0, y0+ Y6, z0, w0+ W6, u0+ U6, v0, xf, yf - 1, zf, wf - 1, uf - 1, vf),
                                                                gradCoord6D(seed, x0+ X6, y0+ Y6, z0, w0+ W6, u0+ U6, v0, xf - 1, yf - 1, zf, wf - 1, uf - 1, vf), xa),
                                                        ya),
                                                lerp(
                                                        lerp(gradCoord6D(seed, x0, y0, z0+ Z6, w0+ W6, u0+ U6, v0, xf, yf, zf - 1, wf - 1, uf - 1, vf),
                                                                gradCoord6D(seed, x0+ X6, y0, z0+ Z6, w0+ W6, u0+ U6, v0, xf - 1, yf, zf - 1, wf - 1, uf - 1, vf), xa),
                                                        lerp(gradCoord6D(seed, x0, y0+ Y6, z0+ Z6, w0+ W6, u0+ U6, v0, xf, yf - 1, zf - 1, wf - 1, uf - 1, vf),
                                                                gradCoord6D(seed, x0+ X6, y0+ Y6, z0+ Z6, w0+ W6, u0+ U6, v0, xf - 1, yf - 1, zf - 1, wf - 1, uf - 1, vf), xa),
                                                        ya),
                                                za),
                                        wa),
                                ua),
                        lerp(
                                lerp(
                                        lerp(
                                                lerp(
                                                        lerp(gradCoord6D(seed, x0, y0, z0, w0, u0, v0+ V6, xf, yf, zf, wf, uf, vf - 1),
                                                                gradCoord6D(seed, x0+ X6, y0, z0, w0, u0, v0+ V6, xf - 1, yf, zf, wf, uf, vf - 1), xa),
                                                        lerp(gradCoord6D(seed, x0, y0+ Y6, z0, w0, u0, v0+ V6, xf, yf - 1, zf, wf, uf, vf - 1),
                                                                gradCoord6D(seed, x0+ X6, y0+ Y6, z0, w0, u0, v0+ V6, xf - 1, yf - 1, zf, wf, uf, vf - 1), xa),
                                                        ya),
                                                lerp(
                                                        lerp(gradCoord6D(seed, x0, y0, z0+ Z6, w0, u0, v0+ V6, xf, yf, zf - 1, wf, uf, vf - 1),
                                                                gradCoord6D(seed, x0+ X6, y0, z0+ Z6, w0, u0, v0+ V6, xf - 1, yf, zf - 1, wf, uf, vf - 1), xa),
                                                        lerp(gradCoord6D(seed, x0, y0+ Y6, z0+ Z6, w0, u0, v0+ V6, xf, yf - 1, zf - 1, wf, uf, vf - 1),
                                                                gradCoord6D(seed, x0+ X6, y0+ Y6, z0+ Z6, w0, u0, v0+ V6, xf - 1, yf - 1, zf - 1, wf, uf, vf - 1), xa),
                                                        ya),
                                                za),
                                        lerp(
                                                lerp(
                                                        lerp(gradCoord6D(seed, x0, y0, z0, w0+ W6, u0, v0+ V6, xf, yf, zf, wf - 1, uf, vf - 1),
                                                                gradCoord6D(seed, x0+ X6, y0, z0, w0+ W6, u0, v0+ V6, xf - 1, yf, zf, wf - 1, uf, vf - 1), xa),
                                                        lerp(gradCoord6D(seed, x0, y0+ Y6, z0, w0+ W6, u0, v0+ V6, xf, yf - 1, zf, wf - 1, uf, vf - 1),
                                                                gradCoord6D(seed, x0+ X6, y0+ Y6, z0, w0+ W6, u0, v0+ V6, xf - 1, yf - 1, zf, wf - 1, uf, vf - 1), xa),
                                                        ya),
                                                lerp(
                                                        lerp(gradCoord6D(seed, x0, y0, z0+ Z6, w0+ W6, u0, v0+ V6, xf, yf, zf - 1, wf - 1, uf, vf - 1),
                                                                gradCoord6D(seed, x0+ X6, y0, z0+ Z6, w0+ W6, u0, v0+ V6, xf - 1, yf, zf - 1, wf - 1, uf, vf - 1), xa),
                                                        lerp(gradCoord6D(seed, x0, y0+ Y6, z0+ Z6, w0+ W6, u0, v0+ V6, xf, yf - 1, zf - 1, wf - 1, uf, vf - 1),
                                                                gradCoord6D(seed, x0+ X6, y0+ Y6, z0+ Z6, w0+ W6, u0, v0+ V6, xf - 1, yf - 1, zf - 1, wf - 1, uf, vf - 1), xa),
                                                        ya),
                                                za),
                                        wa),
                                lerp(
                                        lerp(
                                                lerp(
                                                        lerp(gradCoord6D(seed, x0, y0, z0, w0, u0+ U6, v0+ V6, xf, yf, zf, wf, uf - 1, vf - 1),
                                                                gradCoord6D(seed, x0+ X6, y0, z0, w0, u0+ U6, v0+ V6, xf - 1, yf, zf, wf, uf - 1, vf - 1), xa),
                                                        lerp(gradCoord6D(seed, x0, y0+ Y6, z0, w0, u0+ U6, v0+ V6, xf, yf - 1, zf, wf, uf - 1, vf - 1),
                                                                gradCoord6D(seed, x0+ X6, y0+ Y6, z0, w0, u0+ U6, v0+ V6, xf - 1, yf - 1, zf, wf, uf - 1, vf - 1), xa),
                                                        ya),
                                                lerp(
                                                        lerp(gradCoord6D(seed, x0, y0, z0+ Z6, w0, u0+ U6, v0+ V6, xf, yf, zf - 1, wf, uf - 1, vf - 1),
                                                                gradCoord6D(seed, x0+ X6, y0, z0+ Z6, w0, u0+ U6, v0+ V6, xf - 1, yf, zf - 1, wf, uf - 1, vf - 1), xa),
                                                        lerp(gradCoord6D(seed, x0, y0+ Y6, z0+ Z6, w0, u0+ U6, v0+ V6, xf, yf - 1, zf - 1, wf, uf - 1, vf - 1),
                                                                gradCoord6D(seed, x0+ X6, y0+ Y6, z0+ Z6, w0, u0+ U6, v0+ V6, xf - 1, yf - 1, zf - 1, wf, uf - 1, vf - 1), xa),
                                                        ya),
                                                za),
                                        lerp(
                                                lerp(
                                                        lerp(gradCoord6D(seed, x0, y0, z0, w0+ W6, u0+ U6, v0+ V6, xf, yf, zf, wf - 1, uf - 1, vf - 1),
                                                                gradCoord6D(seed, x0+ X6, y0, z0, w0+ W6, u0+ U6, v0+ V6, xf - 1, yf, zf, wf - 1, uf - 1, vf - 1), xa),
                                                        lerp(gradCoord6D(seed, x0, y0+ Y6, z0, w0+ W6, u0+ U6, v0+ V6, xf, yf - 1, zf, wf - 1, uf - 1, vf - 1),
                                                                gradCoord6D(seed, x0+ X6, y0+ Y6, z0, w0+ W6, u0+ U6, v0+ V6, xf - 1, yf - 1, zf, wf - 1, uf - 1, vf - 1), xa),
                                                        ya),
                                                lerp(
                                                        lerp(gradCoord6D(seed, x0, y0, z0+ Z6, w0+ W6, u0+ U6, v0+ V6, xf, yf, zf - 1, wf - 1, uf - 1, vf - 1),
                                                                gradCoord6D(seed, x0+ X6, y0, z0+ Z6, w0+ W6, u0+ U6, v0+ V6, xf - 1, yf, zf - 1, wf - 1, uf - 1, vf - 1), xa),
                                                        lerp(gradCoord6D(seed, x0, y0+ Y6, z0+ Z6, w0+ W6, u0+ U6, v0+ V6, xf, yf - 1, zf - 1, wf - 1, uf - 1, vf - 1),
                                                                gradCoord6D(seed, x0+ X6, y0+ Y6, z0+ Z6, w0+ W6, u0+ U6, v0+ V6, xf - 1, yf - 1, zf - 1, wf - 1, uf - 1, vf - 1), xa),
                                                        ya),
                                                za),
                                        wa),
                                ua),
                        va) * SCALE6, EQ_ADD_6, EQ_MUL_6);//1.61f);
    }

    /**
     * Produces a String that describes everything needed to recreate this RawNoise in full. This String can be read back
     * in by {@link #stringDeserialize(String)} to reassign the described state to another RawNoise.
     * @return a String that describes this PerlueNoise for serialization
     */
    public String stringSerialize() {
        return "`" + seed + "`";
    }

    /**
     * Given a serialized String produced by {@link #stringSerialize()}, reassigns this PerlueNoise to have the
     * described state from the given String. The serialized String must have been produced by a PerlueNoise.
     *
     * @param data a serialized String, typically produced by {@link #stringSerialize()}
     * @return this PerlueNoise, after being modified (if possible)
     */
    public PerlueNoise stringDeserialize(String data) {
        seed = MathSupport.intFromDec(data, 1, data.indexOf('`', 1));
        return this;
    }

    public static PerlueNoise recreateFromString(String data) {
        return new PerlueNoise(MathSupport.intFromDec(data, 1, data.indexOf('`', 1)));
    }
    /**
     * Gets a simple human-readable String that describes this noise generator. This should use names instead of coded
     * numbers, and should be enough to differentiate any two generators.
     * @return a String that describes this noise generator for human consumption
     */
    public String toHumanReadableString(){
        return getTag() + " with seed " + getSeed();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PerlueNoise that = (PerlueNoise) o;

        return (seed == that.seed);
    }

    public int hashCode() {
        return seed;
    }

    public String toString() {
        return "PerlueNoise{seed=" + seed + '}';
    }

    /**
     * A 32-bit point hash that needs 2 dimensions pre-multiplied by constants {@link #X2} and {@link #Y2}, as
     * well as an int seed.
     * @param x x position, as an int pre-multiplied by {@link #X2}
     * @param y y position, as an int pre-multiplied by {@link #Y2}
     * @param s any int, a seed to be able to produce many hashes for a given point
     * @return 8-bit hash of the x,y point with the given state s, shifted for {@link GradientVectors#GRADIENTS_2D}
     */
    public static int hashAll(int x, int y, int s) {
        final int h = (s ^ x ^ y) * 0x125493;
        return (h ^ (h << 11 | h >>> 21) ^ (h << 23 | h >>> 9));
    }
    /**
     * A 32-bit point hash that needs 3 dimensions pre-multiplied by constants {@link #X3} through {@link #Z3}, as
     * well as an int seed.
     * @param x x position, as an int pre-multiplied by {@link #X3}
     * @param y y position, as an int pre-multiplied by {@link #Y3}
     * @param z z position, as an int pre-multiplied by {@link #Z3}
     * @param s any int, a seed to be able to produce many hashes for a given point
     * @return 8-bit hash of the x,y,z point with the given state s, shifted for {@link GradientVectors#GRADIENTS_3D}
     */
    public static int hashAll(int x, int y, int z, int s) {
        final int h = (s ^ x ^ y ^ z) * 0x125493;
        return (h ^ (h << 11 | h >>> 21) ^ (h << 23 | h >>> 9));
    }

    /**
     * A 32-bit point hash that needs 4 dimensions pre-multiplied by constants {@link #X4} through {@link #W4}, as
     * well as an int seed.
     * @param x x position, as an int pre-multiplied by {@link #X4}
     * @param y y position, as an int pre-multiplied by {@link #Y4}
     * @param z z position, as an int pre-multiplied by {@link #Z4}
     * @param w w position, as an int pre-multiplied by {@link #W4}
     * @param s any int, a seed to be able to produce many hashes for a given point
     * @return 8-bit hash of the x,y,z,w point with the given state s, shifted for {@link GradientVectors#GRADIENTS_4D}
     */
    public static int hashAll(int x, int y, int z, int w, int s) {
        final int h = (s ^ x ^ y ^ z ^ w) * 0x125493;
        return (h ^ (h << 11 | h >>> 21) ^ (h << 23 | h >>> 9));
    }
    /**
     * A 32-bit point hash that needs 5 dimensions pre-multiplied by constants {@link #X5} through {@link #U5}, as
     * well as an int seed.
     * @param x x position, as an int pre-multiplied by {@link #X5}
     * @param y y position, as an int pre-multiplied by {@link #Y5}
     * @param z z position, as an int pre-multiplied by {@link #Z5}
     * @param w w position, as an int pre-multiplied by {@link #W5}
     * @param u u position, as an int pre-multiplied by {@link #U5}
     * @param s any int, a seed to be able to produce many hashes for a given point
     * @return 8-bit hash of the x,y,z,w,u point with the given state s, shifted for {@link GradientVectors#GRADIENTS_5D}
     */
    public static int hashAll(int x, int y, int z, int w, int u, int s) {
        final int h = (s ^ x ^ y ^ z ^ w ^ u) * 0x125493;
        return (h ^ (h << 11 | h >>> 21) ^ (h << 23 | h >>> 9));
    }

    /**
     * A 32-bit point hash that needs 6 dimensions pre-multiplied by constants {@link #X6} through {@link #V6}, as
     * well as an int seed.
     * @param x x position, as an int pre-multiplied by {@link #X6}
     * @param y y position, as an int pre-multiplied by {@link #Y6}
     * @param z z position, as an int pre-multiplied by {@link #Z6}
     * @param w w position, as an int pre-multiplied by {@link #W6}
     * @param u u position, as an int pre-multiplied by {@link #U6}
     * @param v v position, as an int pre-multiplied by {@link #V6}
     * @param s any int, a seed to be able to produce many hashes for a given point
     * @return 8-bit hash of the x,y,z,w,u,v point with the given state s
     */
    public static int hashAll(int x, int y, int z, int w, int u, int v, int s) {
        final int h = (s ^ x ^ y ^ z ^ w ^ u ^ v) * 0x125493;
        return (h ^ (h << 11 | h >>> 21) ^ (h << 23 | h >>> 9));
    }

    public static final int
        X2 = 0x1827F5, Y2 = 0x123C3B,
        X3 = 0x1A36BF, Y3 = 0x157931, Z3 = 0x119749,
        X4 = 0x1B69E5, Y4 = 0x177C1F, Z4 = 0x141E75, W4 = 0x113C33,
        X5 = 0x1C3367, Y5 = 0x18DA39, Z5 = 0x15E6E3, W5 = 0x134D49, U5 = 0x110281,
        X6 = 0x1CC205, Y6 = 0x19D7B5, Z6 = 0x173935, W6 = 0x14DEC5, U6 = 0x12C139, V6 = 0x10DAAD;
}
