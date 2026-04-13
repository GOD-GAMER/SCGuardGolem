package net.geforcemods.scguardgolem.client;

import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.geforcemods.scguardgolem.inventory.GolemMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class GolemScreen extends AbstractContainerScreen<GolemMenu> {

    private static final Identifier GUI_TEXTURE = Identifier.parse("scguardgolem:textures/gui/golem_gui.png");

    // Background colors (SecurityCraft dark theme)
    private static final int BG_PANEL = 0xFF1A1A2E;
    private static final int BORDER_LIGHT = 0xFF3D3D5C;
    private static final int BORDER_DARK = 0xFF0A0A15;
    private static final int SLOT_BG = 0xFF111122;
    private static final int SLOT_LIGHT = 0xFF2A2A44;
    private static final int SLOT_DARK = 0xFF060610;
    private static final int TITLE_BG = 0xFF16213E;
    private static final int SEPARATOR = 0xFF3D3D5C;

    private static final int LABEL_MODULE = 0xFF55FFFF;
    private static final int LABEL_SECTION = 0xFFAAAAAA;
    private static final int LABEL_TITLE = 0xFF55FF55;

    // Layout
    private static final int TITLE_H = 18;
    private static final int MODULE_AREA_H = 50;
    private static final int LOOT_HEADER_H = 14;
    private static final int INV_LABEL_H = 12;
    private static final int INV_GAP = 4;
    private static final int HOTBAR_GAP = 4;

    private Button patrolButton;
    private Button threatButton;
    private Button cameraButton;

    public GolemScreen(GolemMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title, 176, calculateHeight(menu.getLootRows()));
        this.inventoryLabelY = this.imageHeight - 94;
    }

    private static int calculateHeight(int lootRows) {
        return TITLE_H + MODULE_AREA_H + LOOT_HEADER_H + lootRows * 18 + INV_LABEL_H + INV_GAP + 54 + HOTBAR_GAP + 18 + 4;
    }

    @Override
    protected void init() {
        super.init();
        int bx = leftPos + 97;
        int by = topPos + TITLE_H + 2;

        patrolButton = addRenderableWidget(
                Button.builder(getPatrolText(), btn -> {
                    clickButton(0);
                    btn.setMessage(getPatrolText());
                }).bounds(bx, by, 72, 14).build());

        threatButton = addRenderableWidget(
                Button.builder(getThreatText(), btn -> {
                    clickButton(1);
                    btn.setMessage(getThreatText());
                }).bounds(bx, by + 17, 72, 14).build());

        cameraButton = addRenderableWidget(
                Button.builder(getCameraText(), btn -> {
                    clickButton(2);
                    btn.setMessage(getCameraText());
                }).bounds(bx, by + 34, 72, 14).build());
    }

    private void clickButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        extractTooltip(graphics, mouseX, mouseY);

        patrolButton.setMessage(getPatrolText());
        threatButton.setMessage(getThreatText());
        cameraButton.setMessage(getCameraText());
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        int w = imageWidth;
        int h = imageHeight;

        // === Main panel background ===
        graphics.fill(x, y, x + w, y + h, BG_PANEL);

        // === Outer border (3D effect) ===
        graphics.fill(x, y, x + w, y + 1, BORDER_LIGHT);
        graphics.fill(x + 1, y + 1, x + w - 1, y + 2, BORDER_DARK);
        graphics.fill(x, y + h - 1, x + w, y + h, BORDER_DARK);
        graphics.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, BORDER_LIGHT);
        graphics.fill(x, y, x + 1, y + h, BORDER_LIGHT);
        graphics.fill(x + 1, y + 1, x + 2, y + h - 1, BORDER_DARK);
        graphics.fill(x + w - 1, y, x + w, y + h, BORDER_DARK);
        graphics.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, BORDER_LIGHT);

        // === Title bar ===
        graphics.fill(x + 2, y + 2, x + w - 2, y + TITLE_H, TITLE_BG);
        graphics.fill(x + 2, y + TITLE_H, x + w - 2, y + TITLE_H + 1, SEPARATOR);

        // === Module section ===
        int moduleY = y + TITLE_H + 1;
        graphics.fill(x + 2, moduleY, x + 90, moduleY + MODULE_AREA_H, 0xFF151528);

        int[][] moduleSlotPos = {
            {7, 17}, {35, 17}, {63, 17},
            {7, 39}, {35, 39}, {63, 39}
        };
        String[] topLabels = {"Harm", "Speed", "Smart"};
        String[] botLabels = {"Allow", "Deny", "Store"};

        for (int i = 0; i < 6; i++) {
            drawSlot(graphics, x + moduleSlotPos[i][0], y + moduleSlotPos[i][1]);
        }

        for (int i = 0; i < 3; i++) {
            int lx = x + moduleSlotPos[i][0] + 1;
            graphics.text(font, topLabels[i], lx, y + TITLE_H + 3, LABEL_MODULE, false);
            graphics.text(font, botLabels[i], lx, y + TITLE_H + 25, LABEL_MODULE, false);
        }

        // Vertical separator between modules and buttons
        graphics.fill(x + 92, moduleY + 2, x + 93, moduleY + MODULE_AREA_H - 2, SEPARATOR);

        // === Loot section ===
        int lootHeaderY = moduleY + MODULE_AREA_H;
        graphics.fill(x + 2, lootHeaderY, x + w - 2, lootHeaderY + 1, SEPARATOR);
        graphics.text(font, "Loot Chest", x + 8, lootHeaderY + 3, LABEL_SECTION, false);

        int lootRows = menu.getLootRows();
        int lootSlotY = lootHeaderY + LOOT_HEADER_H;
        for (int row = 0; row < lootRows; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(graphics, x + 7 + col * 18, lootSlotY + row * 18);
            }
        }

        // === Player inventory section ===
        int invLabelY = lootSlotY + lootRows * 18;
        graphics.fill(x + 2, invLabelY, x + w - 2, invLabelY + 1, SEPARATOR);
        graphics.text(font, "Inventory", x + 8, invLabelY + 2, LABEL_SECTION, false);

        int invSlotY = invLabelY + INV_LABEL_H + INV_GAP;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(graphics, x + 7 + col * 18, invSlotY + row * 18);
            }
        }

        // Hotbar
        int hotbarY = invSlotY + 54 + HOTBAR_GAP;
        graphics.fill(x + 2, hotbarY - 2, x + w - 2, hotbarY - 1, SEPARATOR);
        for (int col = 0; col < 9; col++) {
            drawSlot(graphics, x + 7 + col * 18, hotbarY);
        }
    }

    private void drawSlot(GuiGraphicsExtractor graphics, int sx, int sy) {
        graphics.fill(sx, sy, sx + 18, sy + 1, SLOT_DARK);
        graphics.fill(sx, sy, sx + 1, sy + 18, SLOT_DARK);
        graphics.fill(sx + 1, sy + 17, sx + 18, sy + 18, SLOT_LIGHT);
        graphics.fill(sx + 17, sy + 1, sx + 18, sy + 18, SLOT_LIGHT);
        graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, SLOT_BG);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX + 2, 5, LABEL_TITLE, false);
    }

    private Component getPatrolText() {
        boolean on = menu.getData().get(0) != 0;
        return Component.literal("Patrol: " + (on ? "\u00a7aON" : "\u00a7cOFF"));
    }

    private Component getThreatText() {
        int mode = menu.getData().get(1);
        String name = SecurityGolemEntity.ThreatMode.fromOrdinal(mode).name();
        return Component.literal("Mode: \u00a7e" + name);
    }

    private Component getCameraText() {
        boolean on = menu.getData().get(2) != 0;
        return Component.literal("Camera: " + (on ? "\u00a7aON" : "\u00a77OFF"));
    }
}
