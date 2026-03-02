package com.sandboxgame.inventory;

import com.sandboxgame.utils.Constants;
import com.sandboxgame.world.BlockType;

public class Inventory {
    private final ItemStack[] slots;

    public Inventory(int size) {
        this.slots = new ItemStack[size];
    }

    public int getSize() {
        return slots.length;
    }

    public ItemStack getSlot(int slot) {
        if (slot < 0 || slot >= slots.length) {
            return null;
        }
        return slots[slot];
    }

    public boolean addBlock(BlockType type) {
        return add(type, 1) == 0;
    }

    public int add(BlockType type, int amount) {
        if (type == null || type == BlockType.AIR || amount <= 0) {
            return amount;
        }

        int remaining = amount;

        for (int i = 0; i < slots.length && remaining > 0; i++) {
            ItemStack stack = slots[i];
            if (stack != null && stack.getType() == type && stack.getCount() < Constants.MAX_STACK_SIZE) {
                remaining = stack.add(remaining, Constants.MAX_STACK_SIZE);
            }
        }

        for (int i = 0; i < slots.length && remaining > 0; i++) {
            if (slots[i] == null || slots[i].isEmpty()) {
                int placed = Math.min(Constants.MAX_STACK_SIZE, remaining);
                slots[i] = new ItemStack(type, placed);
                remaining -= placed;
            }
        }

        return remaining;
    }

    public boolean removeOne(int slot) {
        ItemStack stack = getSlot(slot);
        if (stack == null) {
            return false;
        }

        stack.remove(1);
        if (stack.isEmpty()) {
            slots[slot] = null;
        }
        return true;
    }

    public void setSlot(int slot, BlockType type, int count) {
        if (slot < 0 || slot >= slots.length) {
            return;
        }

        if (type == null || type == BlockType.AIR || count <= 0) {
            slots[slot] = null;
        } else {
            slots[slot] = new ItemStack(type, Math.min(count, Constants.MAX_STACK_SIZE));
        }
    }
}
