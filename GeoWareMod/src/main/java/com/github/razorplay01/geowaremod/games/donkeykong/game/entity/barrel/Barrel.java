package com.github.razorplay01.geowaremod.games.donkeykong.game.entity.barrel;

import com.github.razorplay01.geowaremod.games.donkeykong.game.entity.DonkeyKongEntity;
import com.github.razorplay01.geowaremod.games.donkeykong.game.mapobject.Ladder;
import com.github.razorplay01.geowaremod.games.donkeykong.game.mapobject.Platform;
import com.github.razorplay01.razorplayapi.util.ScreenSide;
import com.github.razorplay01.razorplayapi.util.hitbox.Hitbox;
import com.github.razorplay01.razorplayapi.util.hitbox.RectangleHitbox;
import com.github.razorplay01.razorplayapi.util.screen.GameScreen;
import com.github.razorplay01.razorplayapi.util.texture.Animation;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;

import java.util.*;

import static com.github.razorplay01.geowaremod.games.donkeykong.game.util.TextureProvider.*;
import static com.github.razorplay01.geowaremod.games.donkeykong.DonkeyKongGame.*;

@Getter
public class Barrel extends DonkeyKongEntity {
    private final Animation horizontalRightAnimation = new Animation(BARREL_R_TEXTURES, 1.5f, true);
    private final Animation horizontalLeftAnimation = new Animation(BARREL_L_TEXTURES, 1.5f, true);
    private final Animation verticalAnimation = new Animation(BARREL_V_TEXTURES, 1.5f, true);

    private static final float LADDER_DESCENT_SPEED = 1.0f;
    private static final float PROBABILITY_TO_USE_LADDER = 0.10f;
    private static final float ALIGNMENT_TOLERANCE = 4f;
    private static final float PLATFORM_DETECTION_THRESHOLD = 2f;
    private static final float PLATFORM_SEGMENT_WIDTH = 16f;
    private static final float SLOPE_Y = 1f;
    private static final int FALL_GRACE_TICKS = 2;
    private static final int MIN_LADDER_TICKS = 2; // Mínimo de ticks para considerar que usó la escalera

    private final Random random = new Random(System.currentTimeMillis());
    private final Set<Ladder> checkedLadders = new HashSet<>();

    @Setter
    private boolean remove = false;
    private boolean shouldChangeDirectionAfterLadder;
    private boolean hasJumpedOverBarrel = false;
    private int fallGraceCounter = 0;
    private int ladderTicks = 0; // Contador de ticks en la escalera

    public Barrel(float x, float y, GameScreen gameScreen) {
        super(x, y, 12, 12, gameScreen, 0xffffaaff);
        this.velocityX = 2.5f; // Dirección inicial hacia la derecha

        this.hitboxes.add(new RectangleHitbox(HITBOX_LADDER, xPos - 2, yPos - 2, 16, 16, -2, -2, LADDER_HITBOX_COLOR));
        this.hitboxes.add(new RectangleHitbox(HITBOX_PLAYER, xPos + (this.width - 8) / 2, yPos + (this.height - 8) / 2, 8, 8, (this.width - 8) / 2, (this.height - 8) / 2, PLAYER_HITBOX_COLOR));
    }

    @Override
    public void update() {
        if (isOnLadder) {
            handleLadderMovement();
        } else {
            handleNormalMovement();
        }
        updateHitboxes();
        verifyScreenBoundsCollision();
        if (fallGraceCounter > 0) {
            fallGraceCounter--;
        }
    }

    private void handleLadderMovement() {
        if (currentLadder == null) return;

        ladderTicks++; // Incrementar el contador de ticks en la escalera
        yPos += LADDER_DESCENT_SPEED;
        xPos = currentLadder.getXPos() + (currentLadder.getWidth() - width) / 2;

        if (yPos + height >= currentLadder.getYPos() + currentLadder.getHeight() - PLATFORM_DETECTION_THRESHOLD) {
            Platform nextPlatform = findNextPlatform(game.getPlatforms());
            if (nextPlatform != null && Math.abs(nextPlatform.getYPos() - (yPos + height)) <= PLATFORM_DETECTION_THRESHOLD) {
                yPos = nextPlatform.getYPos() - height;
                exitLadder();
            } else {
                exitLadder();
            }
        }
    }

    private void exitLadder() {
        isOnLadder = false;
        currentLadder = null;
        velocityY = 2.0f;
        fallGraceCounter = FALL_GRACE_TICKS;
        if (shouldChangeDirectionAfterLadder && ladderTicks >= MIN_LADDER_TICKS) {
            velocityX = -velocityX; // Solo cambiar dirección si estuvo en la escalera al menos MIN_LADDER_TICKS
            shouldChangeDirectionAfterLadder = false;
        }
        ladderTicks = 0; // Reiniciar el contador
    }

    private void handleNormalMovement() {
        velocityY += gravity;
        velocityY = Math.min(velocityY, maxFallSpeed);

        float nextX = xPos + velocityX;
        float nextY = yPos + velocityY;

        Platform platformBeneath = null;
        if (fallGraceCounter == 0) {
            for (Platform platform : game.getPlatforms()) {
                if (willCollideWithPlatform(platform, nextX, nextY)) {
                    platformBeneath = platform;
                    break;
                }
            }
        }

        if (platformBeneath != null) {
            float platformStartX = platformBeneath.getXPos();
            float platformStartY = platformBeneath.getYPos();
            float relativeX = nextX - platformStartX;
            int segment = (int) (relativeX / PLATFORM_SEGMENT_WIDTH);
            float effectiveSlopeY = platformStartX > 100f ? -SLOPE_Y : SLOPE_Y;
            float slopeAdjustment = segment * effectiveSlopeY;

            yPos = platformStartY - height + slopeAdjustment;
            xPos = nextX;
            velocityY = 0;
            currentPlatform = platformBeneath;
            checkLadderCollision(game.getLadders());
        } else {
            xPos = nextX;
            yPos = nextY;
            currentPlatform = null;
        }
    }

    private boolean willCollideWithPlatform(Platform platform, float nextX, float nextY) {
        RectangleHitbox barrelHitbox = (RectangleHitbox) this.getDefaultHitbox();
        if (barrelHitbox == null) return false;

        barrelHitbox.updatePosition(nextX, nextY);
        Hitbox platformHitbox = platform.getDefaultHitbox();
        if (platformHitbox == null) return false;

        if (barrelHitbox.intersects(platformHitbox)) {
            float barrelBottom = nextY + height;
            float platformTop = platformHitbox.getYPos();
            return velocityY >= 0 && barrelBottom >= platformTop && yPos < platformTop;
        }
        return false;
    }

    protected void verifyScreenBoundsCollision() {
        int screenX = gameScreen.getGame().getScreen().getGameScreenXPos();
        int screenY = gameScreen.getGame().getScreen().getGameScreenYPos();
        int screenWidth = gameScreen.getGame().getScreenWidth();
        int screenHeight = gameScreen.getGame().getScreenHeight();

        if (xPos < screenX) {
            xPos = screenX;
            if (velocityY <= 0) {
                onScreenBoundaryCollision(ScreenSide.LEFT);
            }
        } else if (xPos + width > screenX + screenWidth) {
            xPos = screenX + screenWidth - width;
            if (velocityY <= 0) {
                onScreenBoundaryCollision(ScreenSide.RIGHT);
            }
        } else if (yPos < screenY) {
            yPos = screenY;
            onScreenBoundaryCollision(ScreenSide.TOP);
        } else if (yPos + height > screenY + screenHeight) {
            yPos = screenY + screenHeight - height;
            onScreenBoundaryCollision(ScreenSide.BOTTOM);
        }
    }

    protected void onScreenBoundaryCollision(ScreenSide side) {
        if (side == ScreenSide.LEFT || side == ScreenSide.RIGHT) {
            if (this.yPos > game.getPlayer().getYPos() + 32) {
                remove = true;
            } else {
                this.velocityX *= -1;
                this.xPos = Math.clamp(xPos, game.getScreen().getGameScreenXPos(), game.getScreen().getGameScreenXPos() + game.getScreenWidth() - width);
            }
        } else if (side == ScreenSide.TOP || side == ScreenSide.BOTTOM) {
            remove = true;
            velocityY = 0;
        }
    }

    private Platform findNextPlatform(List<Platform> platforms) {
        Platform closest = null;
        float minDistance = Float.MAX_VALUE;
        float barrelBottom = yPos + height;

        for (Platform platform : platforms) {
            if (platform.getYPos() > barrelBottom) {
                float distance = platform.getYPos() - barrelBottom;
                if (distance < minDistance &&
                        xPos + width > platform.getXPos() &&
                        xPos < platform.getXPos() + platform.getWidth()) {
                    minDistance = distance;
                    closest = platform;
                }
            }
        }
        return closest;
    }

    private void checkLadderCollision(List<Ladder> ladders) {
        if (isOnLadder) return; // Evitar chequeos si ya está en una escalera

        List<Ladder> validLadders = new ArrayList<>();
        Hitbox barrelLadderHitbox = getHitboxByName(HITBOX_LADDER);
        if (barrelLadderHitbox == null) return;

        for (Ladder ladder : ladders) {
            if (!checkedLadders.contains(ladder) && ladder.isCanPassThroughPlatform() && canUseLadder(this, ladder, ALIGNMENT_TOLERANCE)) {
                validLadders.add(ladder);
            }
        }
        if (!validLadders.isEmpty() && random.nextFloat() < PROBABILITY_TO_USE_LADDER) {
            Ladder selectedLadder = validLadders.get(random.nextInt(validLadders.size()));
            checkedLadders.add(selectedLadder);
            isOnLadder = true;
            currentLadder = selectedLadder;
            velocityY = 0;
            shouldChangeDirectionAfterLadder = true;
            xPos = selectedLadder.getXPos() + (selectedLadder.getWidth() - width) / 2;
        }
    }

    public boolean canUseLadder(Barrel barrel, Ladder ladder, float alignmentTolerance) {
        RectangleHitbox barrelLadderHitbox = (RectangleHitbox) barrel.getHitboxByName(HITBOX_LADDER);
        RectangleHitbox ladderHitbox = (RectangleHitbox) ladder.getDefaultHitbox();
        if (barrelLadderHitbox == null || ladderHitbox == null) return false;
        if (!barrelLadderHitbox.intersects(ladderHitbox)) return false;

        float barrelCenterX = barrelLadderHitbox.getXPos() + barrelLadderHitbox.getWidth() / 2;
        float ladderCenterX = ladderHitbox.getXPos() + ladderHitbox.getWidth() / 2;
        float horizontalAlignment = Math.abs(barrelCenterX - ladderCenterX);
        return horizontalAlignment <= alignmentTolerance;
    }

    public boolean hasBeenJumpedOver() {
        return hasJumpedOverBarrel;
    }

    public void resetJumpState() {
        hasJumpedOverBarrel = false;
    }

    @Override
    public void render(DrawContext context, float delta) {
        if (currentLadder != null) {
            verticalAnimation.update(delta);
        }
        if (velocityX > 0) {
            horizontalRightAnimation.update(delta);
        } else {
            horizontalLeftAnimation.update(delta);
        }

        Animation currentAnimation;
        int xOffset = 0;
        int yOffset = -1;

        if (isOnLadder) {
            currentAnimation = verticalAnimation;
        } else if (velocityX > 0) {
            currentAnimation = horizontalRightAnimation;
        } else {
            currentAnimation = horizontalLeftAnimation;
        }

        renderTexture(context, this, currentAnimation, xOffset, yOffset);
    }
}