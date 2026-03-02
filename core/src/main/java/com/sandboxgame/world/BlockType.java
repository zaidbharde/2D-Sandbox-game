package com.sandboxgame.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.sandboxgame.utils.Constants;

public enum BlockType {
    AIR(false, 0f, 0f, new Color(0f, 0f, 0f, 0f)),
    DIRT(true, 1f, 0.35f, new Color(0.50f, 0.32f, 0.16f, 1f)),
    GRASS(true, 1f, 0.40f, new Color(0.24f, 0.70f, 0.22f, 1f)),
    STONE(true, 1.6f, 0.90f, new Color(0.46f, 0.46f, 0.50f, 1f)),
    WOOD(true, 1.2f, 0.70f, new Color(0.57f, 0.39f, 0.18f, 1f)),
    LEAVES(true, 0.6f, 0.25f, new Color(0.16f, 0.62f, 0.18f, 0.95f));

    private final boolean solid;
    private final float hardness;
    private final float breakTime;
    private final Color baseColor;

    private Texture texture;
    private TextureRegion textureRegion;

    private static boolean initialized;

    BlockType(boolean solid, float hardness, float breakTime, Color baseColor) {
        this.solid = solid;
        this.hardness = hardness;
        this.breakTime = breakTime;
        this.baseColor = baseColor;
    }

    public boolean isSolid() {
        return solid;
    }

    public float getHardness() {
        return hardness;
    }

    public float getBreakTime() {
        return breakTime;
    }

    public TextureRegion getTextureRegion() {
        return textureRegion;
    }

    public static void initializeTextures() {
        if (initialized) {
            return;
        }

        for (BlockType type : values()) {
            if (type == AIR) {
                continue;
            }
            type.buildTexture();
        }
        initialized = true;
    }

    public static void disposeTextures() {
        for (BlockType type : values()) {
            if (type.texture != null) {
                type.texture.dispose();
                type.texture = null;
                type.textureRegion = null;
            }
        }
        initialized = false;
    }

    private void buildTexture() {
        Pixmap pixmap = new Pixmap(Constants.BLOCK_SIZE, Constants.BLOCK_SIZE, Pixmap.Format.RGBA8888);
        pixmap.setColor(baseColor);
        pixmap.fill();

        if (this == GRASS) {
            pixmap.setColor(0.18f, 0.76f, 0.18f, 1f);
            pixmap.fillRectangle(0, Constants.BLOCK_SIZE - 4, Constants.BLOCK_SIZE, 4);
        } else if (this == STONE) {
            pixmap.setColor(0.58f, 0.58f, 0.62f, 1f);
            for (int i = 1; i < Constants.BLOCK_SIZE; i += 4) {
                pixmap.drawPixel(i, (i * 7) % Constants.BLOCK_SIZE);
            }
        } else if (this == LEAVES) {
            pixmap.setColor(0.12f, 0.50f, 0.16f, 0.85f);
            for (int y = 0; y < Constants.BLOCK_SIZE; y += 2) {
                for (int x = (y % 4 == 0 ? 0 : 1); x < Constants.BLOCK_SIZE; x += 3) {
                    pixmap.drawPixel(x, y);
                }
            }
        } else if (this == WOOD) {
            pixmap.setColor(0.42f, 0.27f, 0.11f, 1f);
            for (int x = 2; x < Constants.BLOCK_SIZE; x += 5) {
                pixmap.drawLine(x, 0, x, Constants.BLOCK_SIZE - 1);
            }
        }

        texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        textureRegion = new TextureRegion(texture);
        pixmap.dispose();
    }
}
