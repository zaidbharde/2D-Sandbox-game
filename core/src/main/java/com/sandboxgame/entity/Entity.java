package com.sandboxgame.entity;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public abstract class Entity {
    protected final Vector2 position;
    protected final Vector2 velocity;
    protected final float width;
    protected final float height;

    protected int health;
    protected int maxHealth;

    protected Entity(float x, float y, float width, float height, int maxHealth) {
        this.position = new Vector2(x, y);
        this.velocity = new Vector2();
        this.width = width;
        this.height = height;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
    }

    public float getX() {
        return position.x;
    }

    public float getY() {
        return position.y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public float getCenterX() {
        return position.x + width * 0.5f;
    }

    public float getCenterY() {
        return position.y + height * 0.5f;
    }

    public Vector2 getVelocity() {
        return velocity;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public void setHealth(int health) {
        this.health = Math.max(0, Math.min(maxHealth, health));
    }

    public void setPosition(float x, float y) {
        position.set(x, y);
    }

    public Rectangle getBounds(Rectangle out) {
        return out.set(position.x, position.y, width, height);
    }
}
