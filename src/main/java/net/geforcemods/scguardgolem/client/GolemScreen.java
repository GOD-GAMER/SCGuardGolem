package net.geforcemods.scguardgolem.client;

import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.geforcemods.scguardgolem.inventory.GolemMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GolemScreen extends AbstractContainerScreen<GolemMenu> {

    // ?? Colors ??
    private static final int C_BG        = 0xFFC6C6C6; // vanilla gray background
    private static final int C_BORDER_LT = 0xFFFFFFFF; // white highlight
    private static final int C_BORDER_DK = 0xFF555555; // dark shadow
    private static final int C_SLOT_BG   = 0xFF8B8B8B; // slot interior
    private static final int C_SLOT_LT   = 0xFFFFFFFF; // slot bottom/right
    private static final int C_SLOT_DK   = 0xFF373737; // slot top/left
    private static final int C_TAB_ON    = 0xFFC6C6C6; // active tab = same as bg
    private static final int C_TAB_OFF   = 0xFF8B8B8B; // inactive tab = darker
    private static final int C_TITLE     = 0xFF404040; // dark text
    private static final int C_LABEL     = 0xFF404040; // label text
    private static final int C_DIM       = 0xFF666666; // dim text
    private static final int C_ACCENT    = 0xFF3366CC; // blue accent
    private static final int C_SEP       = 0xFFAAAAAA; // separator

    // ?? Dimensions ??
    private static final int W = 176;
    private static final int H = 166;
    private static final int TAB_BAR_H = 14;

    // Toggle buttons for config tab
    private Button patrolBtn, threatBtn, cameraBtn;

    // Tab buttons
    private Button configTabBtn, lootTabBtn;

    public GolemScreen(GolemMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title, W, H);
        this.inventoryLabelY = 999;
        this.titleLabelY = 999;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos;
        int y = topPos;

        // ?? Tab bar (above main panel) ??
        int tabW = W / 2;
        configTabBtn = addRenderableWidget(
            Button.builder(Component.literal("Configure"), b -> switchTab(GolemMenu.TAB_CONFIG))
                .bounds(x, y - TAB_BAR_H, tabW, TAB_BAR_H).build());
        lootTabBtn = addRenderableWidget(
            Button.builder(Component.literal("Loot Chest"), b -> switchTab(GolemMenu.TAB_LOOT))
                .bounds(x + tabW, y - TAB_BAR_H, tabW, TAB_BAR_H).build());

        // ?? Config tab: 3 toggle buttons on the RIGHT side ??
        int btnX = x + 90;
        int btnY = y + 28;
        int btnW = 78;
        int btnH = 20;
        int btnGap = 24;

        patrolBtn = addRenderableWidget(
            Button.builder(getPatrolText(), b -> { clickButton(0); b.setMessage(getPatrolText()); })
                .bounds(btnX, btnY, btnW, btnH).build());
        threatBtn = addRenderableWidget(
            Button.builder(getThreatText(), b -> { clickButton(1); b.setMessage(getThreatText()); })
                .bounds(btnX, btnY + btnGap, btnW, btnH).build());
        cameraBtn = addRenderableWidget(
            Button.builder(getCameraText(), b -> { clickButton(2); b.setMessage(getCameraText()); })
                .bounds(btnX, btnY + btnGap * 2, btnW, btnH).build());

        switchTab(GolemMenu.TAB_CONFIG);
    }

    private void switchTab(int tab) {
        menu.setTab(tab);
        boolean config = (tab == GolemMenu.TAB_CONFIG);
        patrolBtn.visible = config;
        threatBtn.visible = config;
        cameraBtn.visible = config;
    }

    private void clickButton(int id) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        super.extractRenderState(g, mx, my, pt);
        extractTooltip(g, mx, my);
        patrolBtn.setMessage(getPatrolText());
        threatBtn.setMessage(getThreatText());
        cameraBtn.setMessage(getCameraText());
    }

    @Override
    public void extractContents(GuiGraphicsExtractor g, int mx, int my, float pt) {
        int x = leftPos;
        int y = topPos;
        boolean config = menu.getCurrentTab() == GolemMenu.TAB_CONFIG;

        // ?? Main panel (vanilla chest style) ??
        drawPanel(g, x, y, W, H);

        // ?? Tab indicators above panel ??
        // Active tab gets same color as panel, inactive gets darker
        int tabW = W / 2;
        g.fill(x, y - TAB_BAR_H, x + tabW, y, config ? C_TAB_ON : C_TAB_OFF);
        g.fill(x + tabW, y - TAB_BAR_H, x + W, y, config ? C_TAB_OFF : C_TAB_ON);
        // Bottom border of inactive tab
        if (config) {
            g.fill(x + tabW, y - 1, x + W, y, C_BORDER_DK);
        } else {
            g.fill(x, y - 1, x + tabW, y, C_BORDER_DK);
        }

        // ?? Title ??
        String title = config ? "Security Golem" : "Collected Loot";
        g.text(font, title, x + 8, y + 6, C_TITLE, false);

        if (config) {
            drawConfigTab(g, x, y);
        } else {
            drawLootTab(g, x, y);
        }
    }

    // ?????????? CONFIG TAB ??????????
    private void drawConfigTab(GuiGraphicsExtractor g, int x, int y) {
        // ?? Left side: Module upgrade slots ??
        g.text(font, "Modules", x + 8, y + 18, C_LABEL, false);

        String[] labels = {"Harm", "Speed", "Smart", "Allow", "Deny", "Store"};
        for (int i = 0; i < 6; i++) {
            int col = i % 3;
            int row = i / 3;
            int sx = x + GolemMenu.MOD_X - 1 + col * GolemMenu.MOD_COL;
            int sy = y + GolemMenu.MOD_Y - 1 + row * GolemMenu.MOD_ROW;

            drawSlot(g, sx, sy);
            // Label below each slot
            g.text(font, labels[i], sx + 1, sy + 19, C_DIM, false);
        }

        // ?? Right side heading ??
        g.text(font, "Controls", x + 90, y + 18, C_LABEL, false);

        // ?? Separator line between left and right ??
        g.fill(x + 84, y + 18, x + 85, y + 100, C_SEP);

        // ?? Bottom: Status info ??
        g.fill(x + 4, y + 105, x + W - 4, y + 106, C_SEP);

        String owner = menu.getGolem().getOwnerName();
        g.text(font, "Owner: " + (owner.isEmpty() ? "None" : owner), x + 8, y + 110, C_DIM, false);

        String hp = String.format("HP: %.0f/%.0f", menu.getGolem().getHealth(), menu.getGolem().getMaxHealth());
        g.text(font, hp, x + 8, y + 122, C_DIM, false);

        int lootCap = menu.getLootRows() * 9;
        g.text(font, "Loot: " + lootCap + " slots", x + 90, y + 110, C_DIM, false);

        String mode = SecurityGolemEntity.ThreatMode.fromOrdinal(menu.getData().get(1)).name();
        g.text(font, "Mode: " + mode, x + 90, y + 122, C_DIM, false);

        // ?? Hint ??
        g.text(font, "Place SC modules to upgrade", x + 8, y + H - 12, C_DIM, false);
    }

    // ?????????? LOOT TAB ??????????
    private void drawLootTab(GuiGraphicsExtractor g, int x, int y) {
        int lootRows = menu.getLootRows();

        // Loot slot backgrounds
        for (int row = 0; row < lootRows; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(g, x + GolemMenu.LOOT_X - 1 + col * 18, y + GolemMenu.LOOT_Y - 1 + row * 18);
            }
        }

        // Separator + label before player inventory
        int sepY = y + GolemMenu.LOOT_Y + lootRows * 18 + 3;
        g.fill(x + 4, sepY, x + W - 4, sepY + 1, C_SEP);
        g.text(font, "Inventory", x + 8, sepY + 2, C_DIM, false);

        // Player inventory slot backgrounds
        int playerInvY = GolemMenu.LOOT_Y + lootRows * 18 + 14;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(g, x + GolemMenu.PLAYER_INV_X - 1 + col * 18, y + playerInvY - 1 + row * 18);
            }
        }

        // Hotbar
        int hotbarY = playerInvY + 58;
        for (int col = 0; col < 9; col++) {
            drawSlot(g, x + GolemMenu.PLAYER_INV_X - 1 + col * 18, y + hotbarY - 1);
        }
    }

    // ?????????? DRAWING HELPERS ??????????

    /** Draws a vanilla-style panel background */
    private void drawPanel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        // Fill
        g.fill(x, y, x + w, y + h, C_BG);
        // Top highlight
        g.fill(x, y, x + w, y + 1, C_BORDER_LT);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, C_BORDER_LT);
        // Left highlight
        g.fill(x, y, x + 1, y + h, C_BORDER_LT);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, C_BORDER_LT);
        // Bottom shadow
        g.fill(x, y + h - 1, x + w, y + h, C_BORDER_DK);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, C_BORDER_DK);
        // Right shadow
        g.fill(x + w - 1, y, x + w, y + h, C_BORDER_DK);
        g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, C_BORDER_DK);
    }

    /** Draws a single 18×18 vanilla-style item slot */
    private void drawSlot(GuiGraphicsExtractor g, int sx, int sy) {
        // Top/left shadow
        g.fill(sx, sy, sx + 18, sy + 1, C_SLOT_DK);
        g.fill(sx, sy + 1, sx + 1, sy + 17, C_SLOT_DK);
        // Bottom/right highlight
        g.fill(sx + 1, sy + 17, sx + 18, sy + 18, C_SLOT_LT);
        g.fill(sx + 17, sy + 1, sx + 18, sy + 17, C_SLOT_LT);
        // Inner fill
        g.fill(sx + 1, sy + 1, sx + 17, sy + 17, C_SLOT_BG);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {
        // handled in extractContents per-tab
    }

    private Component getPatrolText() {
        boolean on = menu.getData().get(0) != 0;
        return Component.literal(on ? "\u00a72Patrol ON" : "\u00a74Patrol OFF");
    }

    private Component getThreatText() {
        int mode = menu.getData().get(1);
        String name = SecurityGolemEntity.ThreatMode.fromOrdinal(mode).name();
        return Component.literal("Mode: " + name);
    }

    private Component getCameraText() {
        boolean on = menu.getData().get(2) != 0;
        return Component.literal(on ? "\u00a72Camera ON" : "\u00a77Camera OFF");
    }
}
