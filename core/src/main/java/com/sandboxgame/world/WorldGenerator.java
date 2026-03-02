package com.sandboxgame.world;

import com.sandboxgame.utils.Constants;
import com.sandboxgame.utils.Noise;

public class WorldGenerator {
    private final long seed;
    private final Noise terrainNoise;
    private final Noise caveNoise;
    private final Noise treeNoise;

    public WorldGenerator(long seed) {
        this.seed = seed;
        this.terrainNoise = new Noise(seed ^ 0xC0FFEE);
        this.caveNoise = new Noise(seed ^ 0xFACADE);
        this.treeNoise = new Noise(seed ^ 0xA11CE);
    }

    public Chunk generateChunk(int chunkX) {
        Chunk chunk = new Chunk(chunkX);

        for (int localX = 0; localX < Constants.CHUNK_WIDTH; localX++) {
            int globalX = chunkX * Constants.CHUNK_WIDTH + localX;
            int surfaceY = getSurfaceHeight(globalX);

            for (int y = 0; y <= surfaceY && y < Constants.WORLD_HEIGHT; y++) {
                BlockType type;
                if (y == surfaceY) {
                    type = BlockType.GRASS;
                } else if (y >= surfaceY - 4) {
                    type = BlockType.DIRT;
                } else {
                    type = BlockType.STONE;
                }
                chunk.setBlock(localX, y, type, false);
            }

            carveCaves(chunk, localX, globalX, surfaceY);

            if (localX >= 2 && localX <= Constants.CHUNK_WIDTH - 3 && shouldGenerateTree(globalX, surfaceY)) {
                generateTree(chunk, localX, surfaceY + 1, globalX);
            }
        }

        chunk.recalculateLighting();
        chunk.setDirty(false);
        return chunk;
    }

    public int getSurfaceHeight(int globalX) {
        float lowFreq = terrainNoise.fbm(globalX * 0.0065f, 7.11f, 4, 2f, 0.5f);
        float highFreq = terrainNoise.fbm(globalX * 0.022f, 19.9f, 3, 2f, 0.55f);
        int surface = (int) (104 + lowFreq * 28 + highFreq * 10);
        if (surface < 32) {
            surface = 32;
        }
        if (surface > Constants.WORLD_HEIGHT - 24) {
            surface = Constants.WORLD_HEIGHT - 24;
        }
        return surface;
    }

    private void carveCaves(Chunk chunk, int localX, int globalX, int surfaceY) {
        int maxY = Math.max(8, surfaceY - 3);
        for (int y = 8; y < maxY; y++) {
            float caveValue = caveNoise.fbm(globalX * 0.055f, y * 0.055f, 3, 2f, 0.5f);
            float caveMask = caveNoise.fbm(globalX * 0.018f + 300f, y * 0.018f + 120f, 2, 2f, 0.5f);
            if (caveValue > 0.34f && caveMask > -0.2f) {
                chunk.setBlock(localX, y, BlockType.AIR, false);
            }
        }
    }

    private boolean shouldGenerateTree(int globalX, int surfaceY) {
        if (surfaceY < 48 || surfaceY >= Constants.WORLD_HEIGHT - 8) {
            return false;
        }
        float treeSignal = treeNoise.fbm(globalX * 0.19f, seed * 0.0001f, 2, 2f, 0.5f);
        return treeSignal > 0.56f;
    }

    private void generateTree(Chunk chunk, int localX, int startY, int globalX) {
        int trunkHeight = 4 + Math.abs(hash(globalX)) % 3;
        for (int i = 0; i < trunkHeight && startY + i < Constants.WORLD_HEIGHT; i++) {
            chunk.setBlock(localX, startY + i, BlockType.WOOD, false);
        }

        int leafBaseY = startY + trunkHeight - 2;
        for (int dx = -2; dx <= 2; dx++) {
            int lx = localX + dx;
            if (lx < 0 || lx >= Constants.CHUNK_WIDTH) {
                continue;
            }

            for (int dy = 0; dy <= 3; dy++) {
                int y = leafBaseY + dy;
                if (y <= 0 || y >= Constants.WORLD_HEIGHT) {
                    continue;
                }

                if (dx * dx + (dy - 1) * (dy - 1) <= 6) {
                    if (chunk.getBlock(lx, y) == BlockType.AIR) {
                        chunk.setBlock(lx, y, BlockType.LEAVES, false);
                    }
                }
            }
        }
    }

    private static int hash(int value) {
        int x = value;
        x ^= x << 13;
        x ^= x >>> 17;
        x ^= x << 5;
        return x;
    }
}
