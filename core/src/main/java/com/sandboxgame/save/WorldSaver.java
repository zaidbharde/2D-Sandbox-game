package com.sandboxgame.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.sandboxgame.entity.Player;
import com.sandboxgame.inventory.Inventory;
import com.sandboxgame.inventory.ItemStack;
import com.sandboxgame.utils.Constants;
import com.sandboxgame.world.BlockType;
import com.sandboxgame.world.Chunk;

public class WorldSaver {
    private final Json json;
    private final FileHandle worldRoot;
    private final FileHandle chunkDir;
    private final FileHandle playerFile;
    private final FileHandle metaFile;

    public WorldSaver(String worldName) {
        this.json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);

        this.worldRoot = Gdx.files.local(Constants.SAVE_ROOT + "/" + worldName);
        this.chunkDir = worldRoot.child("chunks");
        this.playerFile = worldRoot.child("player.json");
        this.metaFile = worldRoot.child("world.json");

        if (!worldRoot.exists()) {
            worldRoot.mkdirs();
        }
        if (!chunkDir.exists()) {
            chunkDir.mkdirs();
        }
    }

    public long loadOrCreateSeed() {
        try {
            if (metaFile.exists()) {
                WorldMeta meta = json.fromJson(WorldMeta.class, metaFile);
                if (meta != null && meta.seed != 0L) {
                    return meta.seed;
                }
            }
        } catch (Exception ignored) {
        }

        long seed = System.currentTimeMillis();
        WorldMeta meta = new WorldMeta();
        meta.seed = seed;
        metaFile.writeString(json.prettyPrint(meta), false, "UTF-8");
        return seed;
    }

    public void saveChunk(Chunk chunk) {
        ChunkData data = new ChunkData();
        data.chunkX = chunk.getChunkX();
        data.blocks = new short[Constants.CHUNK_WIDTH * Constants.WORLD_HEIGHT];

        for (int x = 0; x < Constants.CHUNK_WIDTH; x++) {
            for (int y = 0; y < Constants.WORLD_HEIGHT; y++) {
                int index = x * Constants.WORLD_HEIGHT + y;
                data.blocks[index] = (short) chunk.getBlock(x, y).ordinal();
            }
        }

        FileHandle out = chunkDir.child(chunkFileName(chunk.getChunkX()));
        out.writeString(json.prettyPrint(data), false, "UTF-8");
        chunk.setDirty(false);
    }

    public Chunk loadChunk(int chunkX) {
        FileHandle in = chunkDir.child(chunkFileName(chunkX));
        if (!in.exists()) {
            return null;
        }

        try {
            ChunkData data = json.fromJson(ChunkData.class, in);
            if (data == null || data.blocks == null || data.blocks.length != Constants.CHUNK_WIDTH * Constants.WORLD_HEIGHT) {
                return null;
            }

            Chunk chunk = new Chunk(chunkX);
            BlockType[] values = BlockType.values();
            for (int x = 0; x < Constants.CHUNK_WIDTH; x++) {
                for (int y = 0; y < Constants.WORLD_HEIGHT; y++) {
                    int index = x * Constants.WORLD_HEIGHT + y;
                    int ordinal = data.blocks[index];
                    BlockType type = (ordinal >= 0 && ordinal < values.length) ? values[ordinal] : BlockType.AIR;
                    chunk.setBlock(x, y, type, false);
                }
            }

            chunk.recalculateLighting();
            chunk.setDirty(false);
            return chunk;
        } catch (Exception ex) {
            return null;
        }
    }

    public void savePlayer(Player player) {
        PlayerData data = new PlayerData();
        data.x = player.getX();
        data.y = player.getY();
        data.health = player.getHealth();
        data.selectedSlot = player.getSelectedSlot();

        Inventory inventory = player.getInventory();
        data.types = new String[inventory.getSize()];
        data.counts = new int[inventory.getSize()];
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getSlot(i);
            if (stack != null) {
                data.types[i] = stack.getType().name();
                data.counts[i] = stack.getCount();
            }
        }

        playerFile.writeString(json.prettyPrint(data), false, "UTF-8");
    }

    public PlayerData loadPlayer() {
        if (!playerFile.exists()) {
            return null;
        }

        try {
            return json.fromJson(PlayerData.class, playerFile);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String chunkFileName(int chunkX) {
        return "chunk_" + chunkX + ".json";
    }

    private static class WorldMeta {
        public long seed;
    }

    private static class ChunkData {
        public int chunkX;
        public short[] blocks;
    }

    public static class PlayerData {
        public float x;
        public float y;
        public int health;
        public int selectedSlot;
        public String[] types;
        public int[] counts;
    }
}
