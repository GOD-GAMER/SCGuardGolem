package net.geforcemods.scguardgolem.client;

import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.geforcemods.scguardgolem.inventory.GolemMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GolemScreen extends AbstractContainerScreen<GolemMenu> {

    // ?? Palette ??
    private static final int BG = 0xFF0F0F1E;
    private static final int PANEL = 0xFF1A1A30;
    private static final int PANEL_LIGHT = 0xFF222240;
    private static final int BORDER = 0xFF3A3A5E;
    private static final int BORDER_DARK = 0xFF0A0A18;
    private static final int TAB_ACTIVE = 0xFF2A2A4A;
    private static final int TAB_INACTIVE = 0xFF14142A;
    private static final int ACCENT = 0xFF4488FF;
    private static final int SLOT_BG = 0xFF111125;
    private static final int SLOT_LIGHT = 0xFF2A2A48;
    private static final int SLOT_DARK = 0xFF060612;
    private static final int TEXT_HEADING = 0xFF55FFFF;
    private static final int TEXT_BODY = 0xFFCCCCCC;
    private static final int TEXT_DIM = 0xFF888899;
    private static final int SEPARATOR = 0xFF2A2A48;

    // ?? Layout ??
    private static final int GUI_W = 220;
    private static final int GUI_H = 200;
    private static final int TAB_H = 22;
    private static final int CONTENT_TOP = TAB_H + 2;

    private static final String[] TAB_LABELS = {"\u2699 Settings", "\u25A0 Modules", "\u25B6 Loot"};

    private Button patrolButton;
    private Button threatButton;
    private Button cameraButton;
    private Button[] tabButtons = new Button[3];

    public GolemScreen(GolemMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title, GUI_W, GUI_H);
        this.inventoryLabelY = 999;
        this.titleLabelY = 999;
    }

    @Override
    protected void init() {
        super.init();

        // ?? Tab buttons ??
        int tabW = 68;
        int tabGap = 2;
        int totalTabW = tabW * 3 + tabGap * 2;
        int tabStartX = leftPos + (GUI_W - totalTabW) / 2;

        for (int i = 0; i < 3; i++) {
            final int idx = i;
            tabButtons[i] = addRenderableWidget(
                Button.builder(Component.literal(TAB_LABELS[i]), btn -> switchTab(idx))
                    .bounds(tabStartX + i * (tabW + tabGap), topPos + 2, tabW, TAB_H - 4)
                    .build());
        }

        // ?? Settings buttons (big, centered, spacious) ??
        int cx = leftPos + GUI_W / 2;
        int by = topPos + CONTENT_TOP + 38;
        int btnW = 140;
        int btnH = 20;
        int btnGap = 30;

        patrolButton = addRenderableWidget(
            Button.builder(getPatrolText(), btn -> {
                clickButton(0);
                btn.setMessage(getPatrolText());
            }).bounds(cx - btnW / 2, by, btnW, btnH).build());

        threatButton = addRenderableWidget(
            Button.builder(getThreatText(), btn -> {
                clickButton(1);
                btn.setMessage(getThreatText());
            }).bounds(cx - btnW / 2, by + btnGap, btnW, btnH).build());

        cameraButton = addRenderableWidget(
            Button.builder(getCameraText(), btn -> {
                clickButton(2);
                btn.setMessage(getCameraText());
            }).bounds(cx - btnW / 2, by + btnGap * 2, btnW, btnH).build());

        switchTab(GolemMenu.TAB_SETTINGS);
    }

    private void switchTab(int tab) {
        menu.setTab(tab);
        boolean settings = (tab == GolemMenu.TAB_SETTINGS);
        patrolButton.visible = settings;
        threatButton.visible = settings;
        cameraButton.visible = settings;
    }

    private void clickButton(int id) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mx, int my, float pt) {
        super.extractRenderState(gfx, mx, my, pt);
        extractTooltip(gfx, mx, my);
        patrolButton.setMessage(getPatrolText());
        threatButton.setMessage(getThreatText());
        cameraButton.setMessage(getCameraText());
    }

    @Override
    public void extractContents(GuiGraphicsExtractor gfx, int mx, int my, float pt) {
        int x = leftPos;
        int y = topPos;

        // ?? Background ??
        gfx.fill(x, y, x + GUI_W, y + GUI_H, BG);
        drawBorder(gfx, x, y, GUI_W, GUI_H, BORDER, BORDER_DARK);

        // ?? Tab bar ??
        gfx.fill(x + 1, y + 1, x + GUI_W - 1, y + TAB_H, TAB_INACTIVE);
        int tabW = 68;
        int tabGap = 2;
        int totalTabW = tabW * 3 + tabGap * 2;
        int tabStartX = x + (GUI_W - totalTabW) / 2;
        int active = menu.getCurrentTab();
        int ax = tabStartX + active * (tabW + tabGap);
        gfx.fill(ax - 1, y + 1, ax + tabW + 1, y + TAB_H, TAB_ACTIVE);
        gfx.fill(ax, y + TAB_H - 1, ax + tabW, y + TAB_H, ACCENT);

        // ?? Separator below tabs ??
        gfx.fill(x + 1, y + TAB_H, x + GUI_W - 1, y + TAB_H + 1, BORDER);

        // ?? Content area ??
        int cy = y + CONTENT_TOP;
        gfx.fill(x + 1, cy, x + GUI_W - 1, y + GUI_H - 1, PANEL);

        switch (active) {
            case GolemMenu.TAB_SETTINGS -> drawSettings(gfx, x, cy);
            case GolemMenu.TAB_MODULES -> drawModules(gfx, x, cy);
            case GolemMenu.TAB_LOOT -> drawLoot(gfx, x, cy);
        }
    }

    // ??????????? SETTINGS ???????????
    private void drawSettings(GuiGraphicsExtractor gfx, int x, int cy) {
        centeredText(gfx, "Security Golem Configuration", x + GUI_W / 2, cy + 8, TEXT_HEADING);
        gfx.fill(x + 20, cy + 20, x + GUI_W - 20, cy + 21, SEPARATOR);

        // Status panel at bottom
        int px = x + 12;
        int py = cy + 120;
        int pw = GUI_W - 24;
        int ph = 48;

        gfx.fill(px, py, px + pw, py + ph, PANEL_LIGHT);
        drawBorder(gfx, px, py, pw, ph, BORDER, BORDER_DARK);

        gfx.text(font, "\u00a7bStatus", px + 6, py + 4, TEXT_HEADING, false);
        gfx.fill(px + 2, py + 14, px + pw - 2, py + 15, SEPARATOR);

        String owner = menu.getGolem().getOwnerName();
        gfx.text(font, "Owner: \u00a7f" + (owner.isEmpty() ? "None" : owner), px + 6, py + 18, TEXT_DIM, false);

        String hp = String.format("%.0f / %.0f", menu.getGolem().getHealth(), menu.getGolem().getMaxHealth());
        gfx.text(font, "Health: \u00a7a" + hp, px + 6, py + 30, TEXT_DIM, false);

        int lootCap = menu.getLootRows() * 9;
        gfx.text(font, "Loot: \u00a7e" + lootCap + " slots", px + pw / 2, py + 30, TEXT_DIM, false);
    }

    // ??????????? MODULES ???????????
    private void drawModules(GuiGraphicsExtractor gfx, int x, int cy) {
        centeredText(gfx, "Module Upgrades", x + GUI_W / 2, cy + 6, TEXT_HEADING);
        gfx.fill(x + 20, cy + 17, x + GUI_W - 20, cy + 18, SEPARATOR);

        String[] names = {"Harming", "Speed", "Smart", "Allowlist", "Denylist", "Storage"};
        String[] descs = {"+3 dmg/lvl", "+0.03 spd/lvl", "Better AI", "Friend list", "Enemy list", "+9 slots/lvl"};

        int slotX0 = 35;
        int slotY0 = 50;
        int colSp = 50;
        int rowSp = 36;

        for (int i = 0; i < 6; i++) {
            int col = i % 3;
            int row = i / 3;
            int bgX = x + slotX0 - 1 + col * colSp;
            int bgY = topPos + slotY0 - 1 + row * rowSp;

            // Label above
            gfx.text(font, names[i], bgX + 1, bgY - 10, TEXT_BODY, false);
            // Slot visual
            drawSlot(gfx, bgX, bgY);
            // Description below
            gfx.text(font, descs[i], bgX + 1, bgY + 20, TEXT_DIM, false);
        }

        centeredText(gfx, "Drop SC modules in to upgrade", x + GUI_W / 2, cy + GUI_H - CONTENT_TOP - 16, TEXT_DIM);
    }

    // ??????????? LOOT ???????????
    private void drawLoot(GuiGraphicsExtractor gfx, int x, int cy) {
        int lootRows = menu.getLootRows();

        gfx.text(font, "\u00a7bCollected Loot", x + 8, cy + 4, TEXT_HEADING, false);
        String capText = lootRows * 9 + " slots";
        gfx.text(font, capText, x + GUI_W - 8 - font.width(capText), cy + 4, TEXT_DIM, false);
        gfx.fill(x + 4, cy + 14, x + GUI_W - 4, cy + 15, SEPARATOR);

        // Loot slot visuals (background at slot.x-1, slot.y-1 relative to GUI origin)
        int lootSlotX = 22;
        int lootSlotY = 30;
        for (int row = 0; row < lootRows; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(gfx, x + lootSlotX - 1 + col * 18, topPos + lootSlotY - 1 + row * 18);
            }
        }

        // Inventory separator
        int invSepY = topPos + lootSlotY + lootRows * 18 + 4;
        gfx.fill(x + 4, invSepY, x + GUI_W - 4, invSepY + 1, SEPARATOR);
        gfx.text(font, "Inventory", x + 8, invSepY + 3, TEXT_DIM, false);

        // Player inventory slot visuals
        int playerInvY = lootSlotY + lootRows * 18 + 14;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(gfx, x + lootSlotX - 1 + col * 18, topPos + playerInvY - 1 + row * 18);
            }
        }

        // Hotbar
        int hotbarY = playerInvY + 58;
        for (int col = 0; col < 9; col++) {
            drawSlot(gfx, x + lootSlotX - 1 + col * 18, topPos + hotbarY - 1);
        }
    }

    // ??????????? HELPERS ???????????
    private void drawSlot(GuiGraphicsExtractor gfx, int sx, int sy) {
        gfx.fill(sx, sy, sx + 18, sy + 1, SLOT_DARK);
        gfx.fill(sx, sy, sx + 1, sy + 18, SLOT_DARK);
        gfx.fill(sx + 1, sy + 17, sx + 18, sy + 18, SLOT_LIGHT);
        gfx.fill(sx + 17, sy + 1, sx + 18, sy + 18, SLOT_LIGHT);
        gfx.fill(sx + 1, sy + 1, sx + 17, sy + 17, SLOT_BG);
    }

    private void drawBorder(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int light, int dark) {
        gfx.fill(x, y, x + w, y + 1, light);
        gfx.fill(x, y + h - 1, x + w, y + h, dark);
        gfx.fill(x, y, x + 1, y + h, light);
        gfx.fill(x + w - 1, y, x + w, y + h, dark);
    }

    private void centeredText(GuiGraphicsExtractor gfx, String text, int cx, int y, int color) {
        gfx.text(font, text, cx - font.width(text) / 2, y, color, false);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor gfx, int mx, int my) {
        // Handled per-tab
    }

    private Component getPatrolText() {
        boolean on = menu.getData().get(0) != 0;
        return Component.literal("Patrol: " + (on ? "\u00a7aON" : "\u00a7cOFF"));
    }

    private Component getThreatText() {
        int mode = menu.getData().get(1);
        String name = SecurityGolemEntity.ThreatMode.fromOrdinal(mode).name();
        return Component.literal("Threat Mode: \u00a7e" + name);
    }

    private Component getCameraText() {
        boolean on = menu.getData().get(2) != 0;
        return Component.literal("Camera: " + (on ? "\u00a7aON" : "\u00a77OFF"));
    }
}
