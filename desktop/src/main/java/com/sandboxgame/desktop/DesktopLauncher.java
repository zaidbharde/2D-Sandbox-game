package com.sandboxgame.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.sandboxgame.MainGame;

public class DesktopLauncher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("2D Sandbox");
        config.setWindowedMode(1280, 720);
        config.useVsync(true);
        config.setForegroundFPS(144);

        new Lwjgl3Application(new MainGame(), config);
    }
}
