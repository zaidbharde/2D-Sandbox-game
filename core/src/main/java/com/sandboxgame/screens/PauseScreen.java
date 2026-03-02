package com.sandboxgame.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.sandboxgame.MainGame;

public class PauseScreen implements Screen {
    private final MainGame game;
    private final GameScreen gameScreen;
    private final OrthographicCamera uiCamera;

    public PauseScreen(MainGame game, GameScreen gameScreen) {
        this.game = game;
        this.gameScreen = gameScreen;
        this.uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            game.setScreen(gameScreen);
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            gameScreen.dispose();
            game.setScreen(new MenuScreen(game));
            return;
        }

        gameScreen.renderPaused(delta);

        uiCamera.update();
        game.shapeRenderer.setProjectionMatrix(uiCamera.combined);
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        game.shapeRenderer.setColor(0f, 0f, 0f, 0.45f);
        game.shapeRenderer.rect(0f, 0f, uiCamera.viewportWidth, uiCamera.viewportHeight);
        game.shapeRenderer.end();

        game.batch.setProjectionMatrix(uiCamera.combined);
        game.batch.begin();
        game.font.draw(game.batch, "Paused", uiCamera.viewportWidth * 0.5f, uiCamera.viewportHeight * 0.60f,
                0f, Align.center, false);
        game.font.draw(game.batch, "ESC / P - Resume", uiCamera.viewportWidth * 0.5f, uiCamera.viewportHeight * 0.52f,
                0f, Align.center, false);
        game.font.draw(game.batch, "M - Main Menu", uiCamera.viewportWidth * 0.5f, uiCamera.viewportHeight * 0.47f,
                0f, Align.center, false);
        game.batch.end();
    }

    @Override
    public void resize(int width, int height) {
        uiCamera.setToOrtho(false, width, height);
        gameScreen.resize(width, height);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
    }
}
