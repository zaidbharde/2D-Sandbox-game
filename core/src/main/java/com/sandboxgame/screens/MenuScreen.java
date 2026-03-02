package com.sandboxgame.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.ScreenUtils;
import com.sandboxgame.MainGame;

public class MenuScreen implements Screen {
    private final MainGame game;
    private final OrthographicCamera uiCamera;
    private final GlyphLayout layout = new GlyphLayout();

    public MenuScreen(MainGame game) {
        this.game = game;
        this.uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            game.setScreen(new GameScreen(game, "default_world"));
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
            return;
        }

        ScreenUtils.clear(0.08f, 0.11f, 0.16f, 1f);
        uiCamera.update();

        game.batch.setProjectionMatrix(uiCamera.combined);
        game.batch.begin();

        String title = "2D Sandbox";
        layout.setText(game.font, title);
        game.font.draw(game.batch, title,
                (uiCamera.viewportWidth - layout.width) * 0.5f,
                uiCamera.viewportHeight * 0.62f);

        String line1 = "ENTER - Start / Continue";
        layout.setText(game.font, line1);
        game.font.draw(game.batch, line1,
                (uiCamera.viewportWidth - layout.width) * 0.5f,
                uiCamera.viewportHeight * 0.48f);

        String line2 = "ESC - Exit";
        layout.setText(game.font, line2);
        game.font.draw(game.batch, line2,
                (uiCamera.viewportWidth - layout.width) * 0.5f,
                uiCamera.viewportHeight * 0.43f);

        game.batch.end();
    }

    @Override
    public void resize(int width, int height) {
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
    }

    @Override
    public void dispose() {
    }
}
