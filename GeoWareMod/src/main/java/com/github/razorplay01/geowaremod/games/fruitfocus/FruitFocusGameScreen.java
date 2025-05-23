package com.github.razorplay01.geowaremod.games.fruitfocus;

import com.github.razorplay01.razorplayapi.util.screen.GameScreen;
import net.minecraft.text.Text;

public class FruitFocusGameScreen extends GameScreen {
    public FruitFocusGameScreen(int timeLimitSeconds, int prevScore, int hideDurationSeconds, int fruitsToHide) {
        super(Text.empty());
        this.game = new FruitFocusGame(this, timeLimitSeconds, prevScore, hideDurationSeconds, fruitsToHide);
    }
}