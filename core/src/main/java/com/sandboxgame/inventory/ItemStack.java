package com.sandboxgame.inventory;

import com.sandboxgame.world.BlockType;

public class ItemStack {
    private final BlockType type;
    private int count;

    public ItemStack(BlockType type, int count) {
        this.type = type;
        this.count = count;
    }

    public BlockType getType() {
        return type;
    }

    public int getCount() {
        return count;
    }

    public int add(int amount, int maxStack) {
        int space = maxStack - count;
        int added = Math.min(space, amount);
        count += added;
        return amount - added;
    }

    public int remove(int amount) {
        int removed = Math.min(amount, count);
        count -= removed;
        return removed;
    }

    public boolean isEmpty() {
        return count <= 0;
    }
}
