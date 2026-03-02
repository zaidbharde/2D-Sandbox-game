package com.sandboxgame.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntMap;
import com.sandboxgame.save.WorldSaver;
import com.sandboxgame.utils.Constants;

public class World {
    private final IntMap<Chunk> loadedChunks = new IntMap<>();
    private final WorldSaver saver;
    private final WorldGenerator generator;

    public World(WorldSaver saver) {
        this.saver = saver;
        long seed = saver.loadOrCreateSeed();
        this.generator = new WorldGenerator(seed);
    }

    public void updateStreaming(float playerXPixel) {
        int playerChunk = pixelToChunk(playerXPixel);

        for (int chunkX = playerChunk - Constants.RENDER_DISTANCE_CHUNKS;
             chunkX <= playerChunk + Constants.RENDER_DISTANCE_CHUNKS; chunkX++) {
            getOrLoadChunk(chunkX);
        }

        IntArray toUnload = new IntArray();
        for (IntMap.Entry<Chunk> entry : loadedChunks.entries()) {
            if (Math.abs(entry.key - playerChunk) > Constants.UNLOAD_DISTANCE_CHUNKS) {
                toUnload.add(entry.key);
            }
        }

        for (int i = 0; i < toUnload.size; i++) {
            int chunkX = toUnload.get(i);
            Chunk chunk = loadedChunks.get(chunkX);
            if (chunk != null && chunk.isDirty()) {
                saver.saveChunk(chunk);
            }
            loadedChunks.remove(chunkX);
        }
    }

    public void render(SpriteBatch batch, OrthographicCamera camera) {
        float halfWidth = camera.viewportWidth * camera.zoom * 0.5f;
        float halfHeight = camera.viewportHeight * camera.zoom * 0.5f;

        int minBlockX = (int) Math.floor((camera.position.x - halfWidth) / Constants.BLOCK_SIZE) - 1;
        int maxBlockX = (int) Math.floor((camera.position.x + halfWidth) / Constants.BLOCK_SIZE) + 1;
        int minBlockY = Math.max(0, (int) Math.floor((camera.position.y - halfHeight) / Constants.BLOCK_SIZE) - 1);
        int maxBlockY = Math.min(Constants.WORLD_HEIGHT - 1,
                (int) Math.floor((camera.position.y + halfHeight) / Constants.BLOCK_SIZE) + 1);

        int minChunkX = blockToChunk(minBlockX);
        int maxChunkX = blockToChunk(maxBlockX);

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            Chunk chunk = loadedChunks.get(chunkX);
            if (chunk == null) {
                continue;
            }

            for (int localX = 0; localX < Constants.CHUNK_WIDTH; localX++) {
                int globalBlockX = chunkX * Constants.CHUNK_WIDTH + localX;
                if (globalBlockX < minBlockX || globalBlockX > maxBlockX) {
                    continue;
                }

                float drawX = globalBlockX * Constants.BLOCK_SIZE;
                for (int y = minBlockY; y <= maxBlockY; y++) {
                    BlockType type = chunk.getBlock(localX, y);
                    if (type == BlockType.AIR) {
                        continue;
                    }

                    TextureRegion texture = type.getTextureRegion();
                    if (texture == null) {
                        continue;
                    }

                    float light = chunk.getLight(localX, y);
                    batch.setColor(light, light, light, 1f);
                    batch.draw(texture, drawX, y * Constants.BLOCK_SIZE, Constants.BLOCK_SIZE, Constants.BLOCK_SIZE);
                }
            }
        }

        batch.setColor(Color.WHITE);
    }

    public BlockType getBlock(int blockX, int blockY) {
        if (blockY < 0) {
            return BlockType.STONE;
        }
        if (blockY >= Constants.WORLD_HEIGHT) {
            return BlockType.AIR;
        }

        Chunk chunk = getOrLoadChunk(blockToChunk(blockX));
        return chunk.getBlock(blockToLocal(blockX), blockY);
    }

    public float getLight(int blockX, int blockY) {
        if (blockY < 0 || blockY >= Constants.WORLD_HEIGHT) {
            return 1f;
        }

        Chunk chunk = getOrLoadChunk(blockToChunk(blockX));
        return chunk.getLight(blockToLocal(blockX), blockY);
    }

    public void setBlock(int blockX, int blockY, BlockType type) {
        if (blockY < 0 || blockY >= Constants.WORLD_HEIGHT) {
            return;
        }

        int chunkX = blockToChunk(blockX);
        int localX = blockToLocal(blockX);
        Chunk chunk = getOrLoadChunk(chunkX);
        chunk.setBlock(localX, blockY, type, true);

        if (localX == 0) {
            Chunk left = loadedChunks.get(chunkX - 1);
            if (left != null) {
                left.recalculateLightColumn(Constants.CHUNK_WIDTH - 1);
            }
        } else if (localX == Constants.CHUNK_WIDTH - 1) {
            Chunk right = loadedChunks.get(chunkX + 1);
            if (right != null) {
                right.recalculateLightColumn(0);
            }
        }
    }

    public int findSpawnY(int blockX) {
        return generator.getSurfaceHeight(blockX) + 4;
    }

    public int getLoadedChunkCount() {
        return loadedChunks.size;
    }

    public void saveAllChunks() {
        for (IntMap.Entry<Chunk> entry : loadedChunks.entries()) {
            if (entry.value.isDirty()) {
                saver.saveChunk(entry.value);
            }
        }
    }

    public void dispose() {
        saveAllChunks();
    }

    private Chunk getOrLoadChunk(int chunkX) {
        Chunk chunk = loadedChunks.get(chunkX);
        if (chunk != null) {
            return chunk;
        }

        Chunk loaded = saver.loadChunk(chunkX);
        if (loaded == null) {
            loaded = generator.generateChunk(chunkX);
        }
        loadedChunks.put(chunkX, loaded);
        return loaded;
    }

    private static int pixelToChunk(float worldPixelX) {
        int blockX = (int) Math.floor(worldPixelX / Constants.BLOCK_SIZE);
        return blockToChunk(blockX);
    }

    private static int blockToChunk(int blockX) {
        return Math.floorDiv(blockX, Constants.CHUNK_WIDTH);
    }

    private static int blockToLocal(int blockX) {
        return Math.floorMod(blockX, Constants.CHUNK_WIDTH);
    }
}
