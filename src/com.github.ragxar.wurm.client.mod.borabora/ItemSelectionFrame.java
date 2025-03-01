package com.github.ragxar.wurm.client.mod.borabora;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.*;
import com.wurmonline.client.renderer.gui.text.TextFont;
import com.wurmonline.client.resources.textures.IconLoader;
import com.wurmonline.client.resources.textures.Texture;

import java.text.DecimalFormat;

public class ItemSelectionFrame {
    private final DecimalFormat decimalFormat = new DecimalFormat("##0.00");
    private final int maxCharacterToRender = 20;
    private final TextureButton frame;
    private Texture itemTexture;
    private TextureButton removeButton;
    private final int frameWidth = 44;
    private final int frameheight = 44;
    private int x;
    private int y;
    public int itemsCount = 0;

    public ItemSelectionFrame(int xPosition, int yPosition, String hoverText) {
        frame = new TextureButton("img.gui.paperdoll.frame", 44, 44, xPosition, yPosition, hoverText, 3, 21);
        removeButton = new TextureButton("img.gui.crafting.remove", 20, 20, xPosition + 45, yPosition - 3, "Remove", 3, 21);
    }

    public void clearTexture() {
        itemTexture = null;
    }

    public void render(Queue queue, int windowX, int windowY) {
        x = windowX;
        y = windowY;
        frame.loadTexture();
        frame.gameTick(windowX, windowY);
        frame.render(queue, false);
        removeButton.loadTexture();
        removeButton.gameTick(windowX, windowY);
        removeButton.render(queue, removeButton.getIsToggled());

        if (itemTexture != null) {
            int x1 = x + frame.getLocalPositionX() + 3 + 5;
            int y1 = y + frame.getLocalPositionY() + 21 + 5;
            Renderer.texturedQuadAlphaBlend(queue, itemTexture, 1.0F, 1.0F, 1.0F, 1.0F, (float)x1, (float)y1, 32.0F, 32.0F, 0.0F, 0.0F, 1.0F, 1.0F);
        }
    }

    public void renderText(Queue queue, TextFont font) {
        int tx = x + frame.getLocalPositionX() + 3 + 5 + 1;
        int ty = y + frame.getLocalPositionY() + 21 + 15 + 1;
        font.moveTo(tx, ty);
        if (itemTexture == null) return;
        font.paint(queue, "" + itemsCount);
    }

    public boolean checkIfSlotHovered(int xMouse, int yMouse) {
        return frame.checkIfHovered(xMouse, yMouse);
    }

    public void leftPressed(int xMouse, int yMouse) {
        if (removeButton.checkIfHovered(xMouse, yMouse)) {
            removeButton.setIsToggled(true);
        }
    }

    public void leftReleased(int xMouse, int yMouse) {
        if (removeButton.checkIfHovered(xMouse, yMouse)) {
            if (removeButton.getIsToggled()) {
                clearTexture();
            }
        }
        removeButton.setIsToggled(false);
    }

    public void setTexture(short iconId) {
        itemTexture = IconLoader.getIcon(iconId);
    }

    public void setTexture(InventoryMetaItem item) {
        if (item == null) return;

        setTexture(item.getType());
    }

    public Texture getTexture() {
        return itemTexture;
    }
}
