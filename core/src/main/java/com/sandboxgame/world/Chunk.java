package com.sandboxgame.world;

import com.sandboxgame.utils.Constants;

public class Chunk {
    private final int chunkX;
    private final BlockType[][] blocks;
    private final float[][] light;
    private boolean dirty;

    public Chunk(int chunkX) {
        this.chunkX = chunkX;
        this.blocks = new BlockType[Constants.CHUNK_WIDTH][Constants.WORLD_HEIGHT];
        this.light = new float[Constants.CHUNK_WIDTH][Constants.WORLD_HEIGHT];

        for (int x = 0; x < Constants.CHUNK_WIDTH; x++) {
            for (int y = 0; y < Constants.WORLD_HEIGHT; y++) {
                blocks[x][y] = BlockType.AIR;
                light[x][y] = 1f;
            }
        }
    }

    public int getChunkX() {
        return chunkX;
    }

    public BlockType getBlock(int localX, int y) {
        if (!isValid(localX, y)) {
            return BlockType.AIR;
        }
        return blocks[localX][y];
    }

    public void setBlock(int localX, int y, BlockType type) {
        setBlock(localX, y, type, true);
    }

    public void setBlock(int localX, int y, BlockType type, boolean markDirty) {
        if (!isValid(localX, y)) {
            return;
        }

        blocks[localX][y] = type;
        recalculateLightColumn(localX);
        if (markDirty) {
            dirty = true;
        }
    }

    public float getLight(int localX, int y) {
        if (!isValid(localX, y)) {
            return 1f;
        }
        return light[localX][y];
    }

    public void recalculateLighting() {
        for (int x = 0; x < Constants.CHUNK_WIDTH; x++) {
            recalculateLightColumn(x);
        }
    }

    public void recalculateLightColumn(int localX) {
        if (localX < 0 || localX >= Constants.CHUNK_WIDTH) {
            return;
        }

        float sunlight = 1f;
        for (int y = Constants.WORLD_HEIGHT - 1; y >= 0; y--) {
            BlockType type = blocks[localX][y];

            if (type == BlockType.AIR) {
                sunlight = Math.min(1f, sunlight + 0.01f);
            } else if (type == BlockType.LEAVES) {
                sunlight = Math.max(Constants.UNDERGROUND_LIGHT_MIN, sunlight - 0.02f);
            } else {
                sunlight = Math.max(Constants.UNDERGROUND_LIGHT_MIN, sunlight - 0.09f - 0.03f * type.getHardness());
            }

            float finalLight = sunlight;
            if (y < 64) {
                finalLight = Math.max(Constants.UNDERGROUND_LIGHT_MIN, sunlight * 0.85f);
            }
            light[localX][y] = finalLight;
        }
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    private boolean isValid(int localX, int y) {
        return localX >= 0 && localX < Constants.CHUNK_WIDTH && y >= 0 && y < Constants.WORLD_HEIGHT;
    }
}
