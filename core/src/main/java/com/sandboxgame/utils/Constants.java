package com.sandboxgame.utils;

public final class Constants {
    private Constants() {
    }

    public static final int BLOCK_SIZE = 16;
    public static final int CHUNK_WIDTH = 16;
    public static final int WORLD_HEIGHT = 256;

    public static final int HOTBAR_SIZE = 9;
    public static final int MAX_STACK_SIZE = 64;

    public static final float PLAYER_WIDTH = 12f;
    public static final float PLAYER_HEIGHT = 28f;
    public static final float PLAYER_MOVE_SPEED = 165f;
    public static final float PLAYER_JUMP_VELOCITY = 430f;
    public static final float PLAYER_GRAVITY = -1300f;
    public static final float PLAYER_MAX_FALL_SPEED = -950f;
    public static final float PLAYER_REACH_BLOCKS = 6f;

    public static final int RENDER_DISTANCE_CHUNKS = 8;
    public static final int UNLOAD_DISTANCE_CHUNKS = 12;

    public static final float CAMERA_FOLLOW_SPEED = 8f;
    public static final float CAMERA_MIN_ZOOM = 0.5f;
    public static final float CAMERA_MAX_ZOOM = 2.25f;

    public static final float UNDERGROUND_LIGHT_MIN = 0.22f;

    public static final String SAVE_ROOT = "saves";
}
