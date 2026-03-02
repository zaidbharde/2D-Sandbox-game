package com.sandboxgame.world;

public class Block {
    private BlockType type;
    private float light;

    public Block(BlockType type, float light) {
        this.type = type;
        this.light = light;
    }

    public BlockType getType() {
        return type;
    }

    public void setType(BlockType type) {
        this.type = type;
    }

    public float getLight() {
        return light;
    }

    public void setLight(float light) {
        this.light = light;
    }
}
