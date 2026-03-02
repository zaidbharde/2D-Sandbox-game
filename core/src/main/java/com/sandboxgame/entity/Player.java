package com.sandboxgame.entity;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.sandboxgame.inventory.Inventory;
import com.sandboxgame.inventory.ItemStack;
import com.sandboxgame.utils.Constants;
import com.sandboxgame.world.BlockType;
import com.sandboxgame.world.World;

public class Player extends Entity {
    private static final float COLLISION_STEP = 4f;

    private final Inventory inventory;
    private final Rectangle boundsTmp = new Rectangle();

    private boolean onGround;
    private int selectedSlot;

    private int miningBlockX = Integer.MIN_VALUE;
    private int miningBlockY = Integer.MIN_VALUE;
    private float miningProgress;

    public Player(float spawnX, float spawnY) {
        super(spawnX, spawnY, Constants.PLAYER_WIDTH, Constants.PLAYER_HEIGHT, 100);
        this.inventory = new Inventory(Constants.HOTBAR_SIZE);

        inventory.setSlot(0, BlockType.DIRT, 32);
        inventory.setSlot(1, BlockType.GRASS, 16);
        inventory.setSlot(2, BlockType.STONE, 16);
        inventory.setSlot(3, BlockType.WOOD, 16);
        inventory.setSlot(4, BlockType.LEAVES, 16);
    }

    public Inventory getInventory() {
        return inventory;
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public void setSelectedSlot(int selectedSlot) {
        this.selectedSlot = Math.floorMod(selectedSlot, inventory.getSize());
    }

    public void cycleSelectedSlot(int direction) {
        if (direction == 0) {
            return;
        }
        selectedSlot = Math.floorMod(selectedSlot + direction, inventory.getSize());
    }

    public boolean isOnGround() {
        return onGround;
    }

    public float getMiningProgressNormalized() {
        if (miningBlockX == Integer.MIN_VALUE) {
            return 0f;
        }
        return miningProgress;
    }

    public void update(float delta, World world, boolean moveLeft, boolean moveRight, boolean jumpPressed) {
        if (moveLeft == moveRight) {
            velocity.x *= 0.80f;
            if (Math.abs(velocity.x) < 4f) {
                velocity.x = 0f;
            }
        } else if (moveLeft) {
            velocity.x = -Constants.PLAYER_MOVE_SPEED;
        } else {
            velocity.x = Constants.PLAYER_MOVE_SPEED;
        }

        if (jumpPressed && onGround) {
            velocity.y = Constants.PLAYER_JUMP_VELOCITY;
            onGround = false;
        }

        velocity.y += Constants.PLAYER_GRAVITY * delta;
        velocity.y = Math.max(velocity.y, Constants.PLAYER_MAX_FALL_SPEED);

        moveAxis(world, velocity.x * delta, true);
        onGround = false;
        moveAxis(world, velocity.y * delta, false);

        if (position.y < -64f) {
            position.y = 128f;
            velocity.y = 0f;
            health = Math.max(1, health - 10);
        }
    }

    public void resetMining() {
        miningBlockX = Integer.MIN_VALUE;
        miningBlockY = Integer.MIN_VALUE;
        miningProgress = 0f;
    }

    public float mine(float delta, World world, int blockX, int blockY) {
        BlockType target = world.getBlock(blockX, blockY);
        if (target == BlockType.AIR) {
            resetMining();
            return 0f;
        }

        if (blockX != miningBlockX || blockY != miningBlockY) {
            miningBlockX = blockX;
            miningBlockY = blockY;
            miningProgress = 0f;
        }

        float breakTime = Math.max(0.05f, target.getBreakTime());
        miningProgress += delta;

        if (miningProgress >= breakTime) {
            world.setBlock(blockX, blockY, BlockType.AIR);
            inventory.addBlock(target);
            resetMining();
            return 1f;
        }

        return MathUtils.clamp(miningProgress / breakTime, 0f, 1f);
    }

    public boolean placeSelectedBlock(World world, int blockX, int blockY) {
        ItemStack stack = inventory.getSlot(selectedSlot);
        if (stack == null) {
            return false;
        }

        BlockType type = stack.getType();
        if (type == BlockType.AIR || world.getBlock(blockX, blockY) != BlockType.AIR) {
            return false;
        }

        if (intersectsBlock(blockX, blockY)) {
            return false;
        }

        world.setBlock(blockX, blockY, type);
        inventory.removeOne(selectedSlot);
        return true;
    }

    public boolean intersectsBlock(int blockX, int blockY) {
        float bx = blockX * Constants.BLOCK_SIZE;
        float by = blockY * Constants.BLOCK_SIZE;

        return getBounds(boundsTmp).overlaps(new Rectangle(bx, by, Constants.BLOCK_SIZE, Constants.BLOCK_SIZE));
    }

    private void moveAxis(World world, float amount, boolean horizontal) {
        float remaining = amount;

        while (Math.abs(remaining) > 0f) {
            float step = MathUtils.clamp(remaining, -COLLISION_STEP, COLLISION_STEP);
            if (horizontal) {
                position.x += step;
            } else {
                position.y += step;
            }

            if (collides(world)) {
                if (horizontal) {
                    resolveHorizontal(step);
                    velocity.x = 0f;
                } else {
                    resolveVertical(step);
                    velocity.y = 0f;
                }
                break;
            }

            remaining -= step;
        }
    }

    private void resolveHorizontal(float step) {
        if (step > 0f) {
            int tileX = (int) Math.floor((position.x + width - 0.001f) / Constants.BLOCK_SIZE);
            position.x = tileX * Constants.BLOCK_SIZE - width - 0.001f;
        } else if (step < 0f) {
            int tileX = (int) Math.floor(position.x / Constants.BLOCK_SIZE);
            position.x = (tileX + 1) * Constants.BLOCK_SIZE + 0.001f;
        }
    }

    private void resolveVertical(float step) {
        if (step > 0f) {
            int tileY = (int) Math.floor((position.y + height - 0.001f) / Constants.BLOCK_SIZE);
            position.y = tileY * Constants.BLOCK_SIZE - height - 0.001f;
        } else if (step < 0f) {
            int tileY = (int) Math.floor(position.y / Constants.BLOCK_SIZE);
            position.y = (tileY + 1) * Constants.BLOCK_SIZE + 0.001f;
            onGround = true;
        }
    }

    private boolean collides(World world) {
        int minX = (int) Math.floor(position.x / Constants.BLOCK_SIZE);
        int maxX = (int) Math.floor((position.x + width - 0.001f) / Constants.BLOCK_SIZE);
        int minY = (int) Math.floor(position.y / Constants.BLOCK_SIZE);
        int maxY = (int) Math.floor((position.y + height - 0.001f) / Constants.BLOCK_SIZE);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (world.getBlock(x, y).isSolid()) {
                    return true;
                }
            }
        }

        return false;
    }
}
