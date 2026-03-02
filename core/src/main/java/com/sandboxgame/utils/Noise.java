package com.sandboxgame.utils;

import java.util.Random;

public class Noise {
    private final int[] perm = new int[512];

    public Noise(long seed) {
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) {
            p[i] = i;
        }

        Random random = new Random(seed);
        for (int i = 255; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int t = p[i];
            p[i] = p[j];
            p[j] = t;
        }

        for (int i = 0; i < 512; i++) {
            perm[i] = p[i & 255];
        }
    }

    public float noise(float x, float y) {
        int xi = fastFloor(x) & 255;
        int yi = fastFloor(y) & 255;

        float xf = x - fastFloor(x);
        float yf = y - fastFloor(y);

        float u = fade(xf);
        float v = fade(yf);

        int aa = perm[perm[xi] + yi];
        int ab = perm[perm[xi] + yi + 1];
        int ba = perm[perm[xi + 1] + yi];
        int bb = perm[perm[xi + 1] + yi + 1];

        float x1 = lerp(grad(aa, xf, yf), grad(ba, xf - 1f, yf), u);
        float x2 = lerp(grad(ab, xf, yf - 1f), grad(bb, xf - 1f, yf - 1f), u);

        return lerp(x1, x2, v);
    }

    public float fbm(float x, float y, int octaves, float lacunarity, float gain) {
        float amplitude = 1f;
        float frequency = 1f;
        float value = 0f;
        float normalization = 0f;

        for (int i = 0; i < octaves; i++) {
            value += noise(x * frequency, y * frequency) * amplitude;
            normalization += amplitude;
            amplitude *= gain;
            frequency *= lacunarity;
        }

        return normalization == 0f ? 0f : value / normalization;
    }

    private static int fastFloor(float v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }

    private static float fade(float t) {
        return t * t * t * (t * (t * 6f - 15f) + 10f);
    }

    private static float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    private static float grad(int hash, float x, float y) {
        int h = hash & 7;
        float u = h < 4 ? x : y;
        float v = h < 4 ? y : x;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}
