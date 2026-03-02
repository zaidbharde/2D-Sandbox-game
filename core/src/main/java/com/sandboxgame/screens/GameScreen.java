package com.sandboxgame.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.sandboxgame.MainGame;
import com.sandboxgame.entity.Player;
import com.sandboxgame.inventory.ItemStack;
import com.sandboxgame.save.WorldSaver;
import com.sandboxgame.utils.Constants;
import com.sandboxgame.world.BlockType;
import com.sandboxgame.world.World;

public class GameScreen implements Screen, InputProcessor {
    private final MainGame game;
    private final OrthographicCamera worldCamera;
    private final OrthographicCamera uiCamera;

    private final WorldSaver saver;
    private final World world;
    private final Player player;

    private final Vector3 mouseWorld = new Vector3();
    private final RaycastResult raycast = new RaycastResult();

    private boolean showDebug = true;
    private boolean rightMouseWasDown;
    private float zoom = 1f;
    private float miningProgress;
    private float autosaveTimer;

    public GameScreen(MainGame game, String worldName) {
        this.game = game;
        this.worldCamera = new OrthographicCamera();
        this.uiCamera = new OrthographicCamera();

        this.saver = new WorldSaver(worldName);
        this.world = new World(saver);
        this.player = loadOrCreatePlayer();

        world.updateStreaming(player.getX());

        worldCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        worldCamera.zoom = zoom;
        worldCamera.position.set(player.getCenterX(), clampCameraY(player.getCenterY(), worldCamera), 0f);
        worldCamera.update();

        uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new PauseScreen(game, this));
            return;
        }

        boolean moveLeft = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean moveRight = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);
        boolean jumpJustPressed = Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                || Gdx.input.isKeyJustPressed(Input.Keys.W)
                || Gdx.input.isKeyJustPressed(Input.Keys.UP);

        player.update(delta, world, moveLeft, moveRight, jumpJustPressed);
        world.updateStreaming(player.getX());

        updateRaycast();
        handleMiningAndPlacing(delta);

        updateCamera(delta);

        autosaveTimer += delta;
        if (autosaveTimer >= 5f) {
            saveGameState();
            autosaveTimer = 0f;
        }

        renderScene();
    }

    public void renderPaused(float delta) {
        updateRaycast();
        updateCamera(delta);
        renderScene();
    }

    private void renderScene() {
        ScreenUtils.clear(0.53f, 0.76f, 0.95f, 1f);

        game.batch.setProjectionMatrix(worldCamera.combined);
        game.batch.begin();
        world.render(game.batch, worldCamera);
        game.batch.end();

        game.shapeRenderer.setProjectionMatrix(worldCamera.combined);
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        game.shapeRenderer.setColor(0.95f, 0.79f, 0.58f, 1f);
        game.shapeRenderer.rect(player.getX(), player.getY(), player.getWidth(), player.getHeight());

        if (raycast.hit && miningProgress > 0f) {
            float bx = raycast.hitX * Constants.BLOCK_SIZE;
            float by = (raycast.hitY + 1) * Constants.BLOCK_SIZE + 2f;
            game.shapeRenderer.setColor(0f, 0f, 0f, 0.65f);
            game.shapeRenderer.rect(bx, by, Constants.BLOCK_SIZE, 3f);
            game.shapeRenderer.setColor(1f, 0.83f, 0.10f, 1f);
            game.shapeRenderer.rect(bx, by, Constants.BLOCK_SIZE * miningProgress, 3f);
        }
        game.shapeRenderer.end();

        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        if (raycast.hit) {
            game.shapeRenderer.setColor(Color.YELLOW);
            game.shapeRenderer.rect(raycast.hitX * Constants.BLOCK_SIZE, raycast.hitY * Constants.BLOCK_SIZE,
                    Constants.BLOCK_SIZE, Constants.BLOCK_SIZE);
        }
        if (raycast.place) {
            game.shapeRenderer.setColor(0.28f, 0.86f, 0.96f, 1f);
            game.shapeRenderer.rect(raycast.placeX * Constants.BLOCK_SIZE, raycast.placeY * Constants.BLOCK_SIZE,
                    Constants.BLOCK_SIZE, Constants.BLOCK_SIZE);
        }
        game.shapeRenderer.end();

        renderHud();
    }

    private void renderHud() {
        uiCamera.update();

        float slotSize = 40f;
        float gap = 6f;
        float hotbarWidth = Constants.HOTBAR_SIZE * slotSize + (Constants.HOTBAR_SIZE - 1) * gap;
        float hotbarX = (uiCamera.viewportWidth - hotbarWidth) * 0.5f;
        float hotbarY = 16f;

        game.shapeRenderer.setProjectionMatrix(uiCamera.combined);
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        game.shapeRenderer.setColor(0f, 0f, 0f, 0.55f);
        game.shapeRenderer.rect(hotbarX - 8f, hotbarY - 8f, hotbarWidth + 16f, slotSize + 16f);

        for (int i = 0; i < Constants.HOTBAR_SIZE; i++) {
            float sx = hotbarX + i * (slotSize + gap);
            game.shapeRenderer.setColor(i == player.getSelectedSlot() ? 0.82f : 0.25f,
                    i == player.getSelectedSlot() ? 0.74f : 0.25f,
                    i == player.getSelectedSlot() ? 0.22f : 0.28f,
                    0.95f);
            game.shapeRenderer.rect(sx, hotbarY, slotSize, slotSize);
        }

        float hpRatio = MathUtils.clamp(player.getHealth() / (float) player.getMaxHealth(), 0f, 1f);
        float hpX = 18f;
        float hpY = uiCamera.viewportHeight - 28f;
        float hpW = 180f;
        float hpH = 14f;

        game.shapeRenderer.setColor(0f, 0f, 0f, 0.45f);
        game.shapeRenderer.rect(hpX - 2f, hpY - 2f, hpW + 4f, hpH + 4f);
        game.shapeRenderer.setColor(0.70f, 0.15f, 0.14f, 1f);
        game.shapeRenderer.rect(hpX, hpY, hpW * hpRatio, hpH);
        game.shapeRenderer.end();

        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        game.shapeRenderer.setColor(0f, 0f, 0f, 0.9f);
        for (int i = 0; i < Constants.HOTBAR_SIZE; i++) {
            float sx = hotbarX + i * (slotSize + gap);
            game.shapeRenderer.rect(sx, hotbarY, slotSize, slotSize);
        }
        game.shapeRenderer.end();

        game.batch.setProjectionMatrix(uiCamera.combined);
        game.batch.begin();

        for (int i = 0; i < Constants.HOTBAR_SIZE; i++) {
            float sx = hotbarX + i * (slotSize + gap);
            ItemStack stack = player.getInventory().getSlot(i);
            if (stack == null) {
                continue;
            }

            TextureRegion region = stack.getType().getTextureRegion();
            if (region != null) {
                game.batch.draw(region, sx + 8f, hotbarY + 8f, 24f, 24f);
            }
            game.font.draw(game.batch, Integer.toString(stack.getCount()), sx + 3f, hotbarY + 12f);
        }

        game.font.draw(game.batch, "HP: " + player.getHealth() + "/" + player.getMaxHealth(), 20f, uiCamera.viewportHeight - 8f);

        if (showDebug) {
            float dy = uiCamera.viewportHeight - 44f;
            game.font.draw(game.batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 20f, dy);
            game.font.draw(game.batch, "Chunks: " + world.getLoadedChunkCount(), 20f, dy - 16f);
            game.font.draw(game.batch,
                    "Player: " + (int) (player.getX() / Constants.BLOCK_SIZE) + "," + (int) (player.getY() / Constants.BLOCK_SIZE),
                    20f, dy - 32f);
            game.font.draw(game.batch, "Zoom: " + String.format("%.2f", zoom), 20f, dy - 48f);

            if (raycast.hit) {
                game.font.draw(game.batch, "Target: " + raycast.hitX + "," + raycast.hitY, 20f, dy - 64f);
            }
        }

        game.batch.end();
    }

    private void updateRaycast() {
        mouseWorld.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        worldCamera.unproject(mouseWorld);

        float startX = player.getCenterX();
        float startY = player.getCenterY();
        float dirX = mouseWorld.x - startX;
        float dirY = mouseWorld.y - startY;

        float distance = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        float maxDistance = Constants.PLAYER_REACH_BLOCKS * Constants.BLOCK_SIZE;

        raycast.clear();

        if (distance < 0.0001f) {
            return;
        }

        float limitedDistance = Math.min(distance, maxDistance);
        dirX /= distance;
        dirY /= distance;

        float step = Constants.BLOCK_SIZE / 4f;
        int lastAirX = Integer.MIN_VALUE;
        int lastAirY = Integer.MIN_VALUE;
        int prevBlockX = Integer.MIN_VALUE;
        int prevBlockY = Integer.MIN_VALUE;

        for (float t = 0f; t <= limitedDistance; t += step) {
            float px = startX + dirX * t;
            float py = startY + dirY * t;
            int bx = (int) Math.floor(px / Constants.BLOCK_SIZE);
            int by = (int) Math.floor(py / Constants.BLOCK_SIZE);

            if (bx == prevBlockX && by == prevBlockY) {
                continue;
            }
            prevBlockX = bx;
            prevBlockY = by;

            if (by < 0 || by >= Constants.WORLD_HEIGHT) {
                continue;
            }

            BlockType type = world.getBlock(bx, by);
            if (type.isSolid()) {
                raycast.hit = true;
                raycast.hitX = bx;
                raycast.hitY = by;

                if (lastAirX != Integer.MIN_VALUE) {
                    raycast.place = true;
                    raycast.placeX = lastAirX;
                    raycast.placeY = lastAirY;
                }
                return;
            }

            lastAirX = bx;
            lastAirY = by;
        }

        if (lastAirX != Integer.MIN_VALUE) {
            raycast.place = true;
            raycast.placeX = lastAirX;
            raycast.placeY = lastAirY;
        }
    }

    private void handleMiningAndPlacing(float delta) {
        boolean leftDown = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        boolean rightDown = Gdx.input.isButtonPressed(Input.Buttons.RIGHT);

        if (leftDown && raycast.hit) {
            miningProgress = player.mine(delta, world, raycast.hitX, raycast.hitY);
        } else {
            miningProgress = 0f;
            player.resetMining();
        }

        if (rightDown && !rightMouseWasDown && raycast.place) {
            if (player.placeSelectedBlock(world, raycast.placeX, raycast.placeY)) {
                miningProgress = 0f;
            }
        }

        rightMouseWasDown = rightDown;
    }

    private void updateCamera(float delta) {
        float alpha = 1f - (float) Math.exp(-Constants.CAMERA_FOLLOW_SPEED * delta);
        float targetX = player.getCenterX();
        float targetY = player.getCenterY();

        worldCamera.position.x += (targetX - worldCamera.position.x) * alpha;
        worldCamera.position.y += (targetY - worldCamera.position.y) * alpha;
        worldCamera.position.y = clampCameraY(worldCamera.position.y, worldCamera);
        worldCamera.zoom = zoom;
        worldCamera.update();
    }

    private static float clampCameraY(float y, OrthographicCamera camera) {
        float halfH = camera.viewportHeight * camera.zoom * 0.5f;
        float minY = halfH;
        float maxY = Constants.WORLD_HEIGHT * Constants.BLOCK_SIZE - halfH;
        if (maxY < minY) {
            maxY = minY;
        }
        return MathUtils.clamp(y, minY, maxY);
    }

    private void setZoom(float zoom) {
        this.zoom = MathUtils.clamp(zoom, Constants.CAMERA_MIN_ZOOM, Constants.CAMERA_MAX_ZOOM);
    }

    private Player loadOrCreatePlayer() {
        WorldSaver.PlayerData data = saver.loadPlayer();
        if (data == null) {
            int spawnBlockX = 0;
            int spawnBlockY = world.findSpawnY(spawnBlockX);
            return new Player(spawnBlockX * Constants.BLOCK_SIZE + 2f, spawnBlockY * Constants.BLOCK_SIZE);
        }

        Player loaded = new Player(data.x, data.y);
        if (data.health > 0) {
            loaded.setHealth(data.health);
        }
        loaded.setSelectedSlot(data.selectedSlot);

        for (int i = 0; i < loaded.getInventory().getSize(); i++) {
            loaded.getInventory().setSlot(i, null, 0);
        }

        if (data.types != null && data.counts != null) {
            int n = Math.min(loaded.getInventory().getSize(), Math.min(data.types.length, data.counts.length));
            for (int i = 0; i < n; i++) {
                if (data.types[i] == null || data.counts[i] <= 0) {
                    continue;
                }
                try {
                    BlockType type = BlockType.valueOf(data.types[i]);
                    loaded.getInventory().setSlot(i, type, data.counts[i]);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        return loaded;
    }

    private void saveGameState() {
        saver.savePlayer(player);
        world.saveAllChunks();
    }

    @Override
    public void resize(int width, int height) {
        worldCamera.setToOrtho(false, width, height);
        worldCamera.zoom = zoom;
        worldCamera.position.set(player.getCenterX(), clampCameraY(player.getCenterY(), worldCamera), 0f);
        worldCamera.update();

        uiCamera.setToOrtho(false, width, height);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        saveGameState();
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        saveGameState();
        world.dispose();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.F3) {
            showDebug = !showDebug;
            return true;
        }
        if (keycode == Input.Keys.MINUS) {
            setZoom(zoom * 1.1f);
            return true;
        }
        if (keycode == Input.Keys.EQUALS || keycode == Input.Keys.PLUS) {
            setZoom(zoom * 0.9f);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        if (Math.abs(amountY) < 0.001f) {
            return false;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) {
            if (amountY > 0f) {
                setZoom(zoom * 1.08f);
            } else {
                setZoom(zoom * 0.92f);
            }
        } else {
            player.cycleSelectedSlot(amountY > 0f ? 1 : -1);
        }
        return true;
    }

    private static class RaycastResult {
        boolean hit;
        int hitX;
        int hitY;

        boolean place;
        int placeX;
        int placeY;

        void clear() {
            hit = false;
            place = false;
            hitX = 0;
            hitY = 0;
            placeX = 0;
            placeY = 0;
        }
    }
}
