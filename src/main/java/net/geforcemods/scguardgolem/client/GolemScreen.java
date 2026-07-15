package net.geforcemods.scguardgolem.client;

import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.geforcemods.scguardgolem.inventory.GolemMenu;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else
/*import net.minecraft.client.gui.GuiGraphics;*/
//? if >=1.21.10
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GolemScreen extends AbstractContainerScreen<GolemMenu> {

    // Sprites registered in the GUI atlas (flat-fill fallback on Forge 1.20.1, see Gfx)
    //? if 1.20.4 {
    /*private static final Identifier PANEL_SPRITE = new Identifier("scguardgolem", "scg_panel");
    private static final Identifier SLOT_SPRITE = new Identifier("minecraft", "container/slot");
    private static final Identifier TAB_SPRITE = new Identifier("minecraft", "widget/tab");
    private static final Identifier TAB_SELECTED_SPRITE = new Identifier("minecraft", "widget/tab_selected");
    private static final Identifier TAB_HIGHLIGHTED_SPRITE = new Identifier("minecraft", "widget/tab_highlighted");
    private static final Identifier TAB_SEL_HIGHLIGHTED_SPRITE = new Identifier("minecraft", "widget/tab_selected_highlighted");
    private static final Identifier SCROLLER_BG_SPRITE = new Identifier("minecraft", "widget/scroller_background");
    private static final Identifier SCROLLER_SPRITE = new Identifier("minecraft", "widget/scroller");
    private static final Identifier MOD_HARMING_SPRITE = new Identifier("scguardgolem", "mod_harming");
    private static final Identifier MOD_SPEED_SPRITE = new Identifier("scguardgolem", "mod_speed");
    private static final Identifier MOD_SMART_SPRITE = new Identifier("scguardgolem", "mod_smart");
    private static final Identifier MOD_STORAGE_SPRITE = new Identifier("scguardgolem", "mod_storage");
    private static final Identifier MOD_ALLOWLIST_SPRITE = new Identifier("scguardgolem", "mod_allowlist");
    private static final Identifier MOD_DENYLIST_SPRITE = new Identifier("scguardgolem", "mod_denylist");
    *///?} else {
    private static final Identifier PANEL_SPRITE = Identifier.parse("scguardgolem:scg_panel");
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final Identifier TAB_SPRITE = Identifier.withDefaultNamespace("widget/tab");
    private static final Identifier TAB_SELECTED_SPRITE = Identifier.withDefaultNamespace("widget/tab_selected");
    private static final Identifier TAB_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("widget/tab_highlighted");
    private static final Identifier TAB_SEL_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("widget/tab_selected_highlighted");
    private static final Identifier SCROLLER_BG_SPRITE = Identifier.withDefaultNamespace("widget/scroller_background");
    private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("widget/scroller");
    private static final Identifier MOD_HARMING_SPRITE = Identifier.parse("scguardgolem:mod_harming");
    private static final Identifier MOD_SPEED_SPRITE = Identifier.parse("scguardgolem:mod_speed");
    private static final Identifier MOD_SMART_SPRITE = Identifier.parse("scguardgolem:mod_smart");
    private static final Identifier MOD_STORAGE_SPRITE = Identifier.parse("scguardgolem:mod_storage");
    private static final Identifier MOD_ALLOWLIST_SPRITE = Identifier.parse("scguardgolem:mod_allowlist");
    private static final Identifier MOD_DENYLIST_SPRITE = Identifier.parse("scguardgolem:mod_denylist");
    //?}

    // Per-slot module icons, in acceptedModules() order (drawn as a ghost hint in each slot).
    private static final Identifier[] MOD_SPRITES = {
        MOD_HARMING_SPRITE, MOD_SPEED_SPRITE, MOD_SMART_SPRITE,
        MOD_STORAGE_SPRITE, MOD_ALLOWLIST_SPRITE, MOD_DENYLIST_SPRITE
    };

    // Text colors (ARGB)
    private static final int C_TITLE = 0xFF404040;
    private static final int C_DIM   = 0xFF666666;
    private static final int C_SEP   = 0xFFAAAAAA;

    // Dimensions
    private static final int W = 176;
    private static final int TAB_H = 20;
    private static final int SCROLLER_W = 8;
    private final int H;

    // Toggle buttons for config tab
    private Button patrolBtn, threatBtn, clearRouteBtn;
    // Lists tab: picker scroll offset
    // Dynamic buttons for the Route tab (rebuilt on change)
    private final List<Button> listButtons = new ArrayList<>();
    private boolean listButtonsDirty = true;
    private int lastWaypointSize = -1;

    public GolemScreen(GolemMenu menu, Inventory playerInv, Component title) {
        //? if >=26.1 {
        super(menu, playerInv, title, W, menu.getGuiHeight());
        //?} else {
        /*super(menu, playerInv, title);
        this.imageWidth = W;
        this.imageHeight = menu.getGuiHeight();
        *///?}
        this.H = menu.getGuiHeight();
        this.inventoryLabelY = 999;
        this.titleLabelY = 999;
    }

    @Override
    protected void init() {
        super.init();
        // Recenter vertically to account for tabs above the panel
        int totalH = H + TAB_H;
        topPos = Math.max(TAB_H, (height - totalH) / 2 + TAB_H);
        leftPos = (width - imageWidth) / 2;

        int x = leftPos;
        int y = topPos;

        // Config tab: 3 toggle buttons below modules
        int modBottom = GolemMenu.MOD_Y + 2 * GolemMenu.MOD_ROW;
        int btnY = y + modBottom + 4;
        int btnW = 54;
        int btnH = 20;
        int btnGap = 5;

        patrolBtn = addRenderableWidget(
            Button.builder(getPatrolText(), b -> { clickButton(0); b.setMessage(getPatrolText()); })
                .bounds(x + 8, btnY, btnW, btnH).build());
        threatBtn = addRenderableWidget(
            Button.builder(getThreatText(), b -> { clickButton(1); b.setMessage(getThreatText()); })
                .bounds(x + 8 + btnW + btnGap, btnY, btnW, btnH).build());
        clearRouteBtn = addRenderableWidget(
            Button.builder(Component.translatable("scguardgolem.gui.clear").withStyle(net.minecraft.ChatFormatting.RED), b -> clickButton(2))
                .bounds(x + 8 + (btnW + btnGap) * 2, btnY, 40, btnH).build());

        switchTab(GolemMenu.TAB_CONFIG);
    }

    private void switchTab(int tab) {
        menu.setTab(tab);
        boolean config = (tab == GolemMenu.TAB_CONFIG);
        patrolBtn.visible = config;
        threatBtn.visible = config;
        clearRouteBtn.visible = config;
        clearListButtons();
        if (tab == GolemMenu.TAB_WAYPOINTS) {
            listButtonsDirty = true;
        }
        clickButton(200 + tab);
    }

    private void clickButton(int id) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    // Tab click handling
    //? if >=1.21.10 {
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (!handled && event.button() == 0) {
            double mouseX = event.x();
            double mouseY = event.y();
            if (handleTabClick(mouseX, mouseY)) return true;
        }
        return super.mouseClicked(event, handled);
    }
    //?} else {
    /*@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && handleTabClick(mouseX, mouseY)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }
    *///?}

    private boolean handleTabClick(double mouseX, double mouseY) {
        int tabW = W / 4;
        int tabY = topPos - TAB_H;
        if (mouseY >= tabY && mouseY < topPos) {
            for (int i = 0; i < 4; i++) {
                int tx = x(i, tabW);
                int tw = tabWidth(i, tabW);
                if (mouseX >= tx && mouseX < tx + tw) {
                    switchTab(i);
                    return true;
                }
            }
        }
        return false;
    }

    private int x(int i, int tabW) { return leftPos + i * tabW; }
    private int tabWidth(int i, int tabW) { return (i == 3) ? W - tabW * 3 : tabW; }

    // Scroll support (horizontal scrollX param added in MC 1.20.2)
    //? if >=1.20.2 {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    //?} else {
    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
    *///?}
        if (menu.getCurrentTab() == GolemMenu.TAB_LOOT && menu.getMaxScroll() > 0) {
            int delta = scrollY > 0 ? -1 : 1;
            int newOffset = menu.getScrollOffset() + delta;
            newOffset = Math.max(0, Math.min(newOffset, menu.getMaxScroll()));
            if (newOffset != menu.getScrollOffset()) {
                menu.setScrollOffset(newOffset);
                if (minecraft != null && minecraft.gameMode != null)
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 100 + newOffset);
            }
            return true;
        }
        //? if >=1.20.2 {
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        //?} else
        /*return super.mouseScrolled(mouseX, mouseY, scrollY);*/
    }

    //? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        super.extractRenderState(g, mx, my, pt);
        updateButtonTexts();
    }

    @Override
    public void extractContents(GuiGraphicsExtractor g, int mx, int my, float pt) {
        drawAll(new Gfx(g), mx, my);
        // Vanilla slot rendering
        super.extractContents(g, mx, my, pt);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {
        // handled in drawAll per-tab
    }
    //?} else {
    /*@Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        updateButtonTexts();
        renderTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        drawAll(new Gfx(g), mx, my);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        // handled in drawAll per-tab
    }
    *///?}

    private void updateButtonTexts() {
        patrolBtn.setMessage(getPatrolText());
        threatBtn.setMessage(getThreatText());
    }

    private void drawAll(Gfx g, int mx, int my) {
        int x = leftPos;
        int y = topPos;
        int tabY = y - TAB_H;
        int curTab = menu.getCurrentTab();

        // Draw 4 tabs
        String[] tabLabels = {tr("scguardgolem.gui.tab.config"), tr("scguardgolem.gui.tab.loot"),
                tr("scguardgolem.gui.tab.lists"), tr("scguardgolem.gui.tab.route")};
        int tabW = W / 4;
        for (int i = 0; i < 4; i++) {
            int tx = x + i * tabW;
            int tw = tabWidth(i, tabW);
            boolean sel = (curTab == i);
            boolean hover = mx >= tx && mx < tx + tw && my >= tabY && my < y;
            Identifier spr;
            if (sel) spr = hover ? TAB_SEL_HIGHLIGHTED_SPRITE : TAB_SELECTED_SPRITE;
            else spr = hover ? TAB_HIGHLIGHTED_SPRITE : TAB_SPRITE;

            if (!sel) g.sprite(spr, tx, tabY, tw, TAB_H + 3);
        }

        // Panel background
        g.sprite(PANEL_SPRITE, x, y, W, H);

        // Selected tab on top
        for (int i = 0; i < 4; i++) {
            if (curTab != i) continue;
            int tx = x + i * tabW;
            int tw = tabWidth(i, tabW);
            boolean hover = mx >= tx && mx < tx + tw && my >= tabY && my < y;
            Identifier spr = hover ? TAB_SEL_HIGHLIGHTED_SPRITE : TAB_SELECTED_SPRITE;
            g.sprite(spr, tx, tabY, tw, TAB_H + 3);
        }

        // Tab labels
        int tabTextY = tabY + (TAB_H - font.lineHeight) / 2;
        for (int i = 0; i < 4; i++) {
            int tx = x + i * tabW;
            int tw = tabWidth(i, tabW);
            g.text(font, tabLabels[i],
                tx + (tw - font.width(tabLabels[i])) / 2, tabTextY,
                curTab == i ? C_TITLE : C_DIM, false);
        }

        // Title
        String titleText = switch (curTab) {
            case GolemMenu.TAB_CONFIG -> tr("scguardgolem.gui.title.config");
            case GolemMenu.TAB_LOOT -> tr("scguardgolem.gui.title.loot");
            case GolemMenu.TAB_LISTS -> tr("scguardgolem.gui.title.lists");
            case GolemMenu.TAB_WAYPOINTS -> tr("scguardgolem.gui.title.route");
            default -> "";
        };
        g.text(font, titleText, x + (W - font.width(titleText)) / 2, y + 6, C_TITLE, false);

        // Tab content
        switch (curTab) {
            case GolemMenu.TAB_CONFIG -> drawConfigTab(g, x, y);
            case GolemMenu.TAB_LOOT -> drawLootTab(g, x, y);
            case GolemMenu.TAB_LISTS -> drawListsTab(g, x, y);
            case GolemMenu.TAB_WAYPOINTS -> drawWaypointsTab(g, x, y);
        }

        // Player inventory
        drawPlayerInv(g, x, y);
    }

    // ---------- CONFIG TAB ----------
    private void drawConfigTab(Gfx g, int x, int y) {
        // 6 module slots in a 3x2 grid, matching acceptedModules() order.
        String[] labels = {tr("scguardgolem.gui.module.harming"), tr("scguardgolem.gui.module.speed"),
                tr("scguardgolem.gui.module.smart"), tr("scguardgolem.gui.module.storage"),
                tr("scguardgolem.gui.module.allowlist"), tr("scguardgolem.gui.module.denylist")};
        for (int i = 0; i < labels.length; i++) {
            int col = i % 3;
            int row = i / 3;
            int sx = x + GolemMenu.MOD_X - 1 + col * GolemMenu.MOD_COL_TIGHT;
            int sy = y + GolemMenu.MOD_Y - 1 + row * GolemMenu.MOD_ROW;

            g.sprite(SLOT_SPRITE, sx, sy, 18, 18);
            g.sprite(MOD_SPRITES[i], sx + 1, sy + 1, 16, 16);
            g.text(font, labels[i], sx + 1, sy + 19, C_DIM, false);
        }

        // Status info below buttons
        int modBottom = GolemMenu.MOD_Y + 2 * GolemMenu.MOD_ROW;
        int statusY = y + modBottom + 28;

        String owner = menu.getGolem().getOwnerName();
        g.text(font, tr("scguardgolem.gui.owner", owner.isEmpty() ? tr("scguardgolem.gui.owner_none") : owner), x + 8, statusY, C_DIM, false);

        g.text(font, tr("scguardgolem.gui.stat.hp", String.format("%.0f", menu.getGolem().getHealth()),
                String.format("%.0f", menu.getGolem().getMaxHealth())), x + 90, statusY, C_DIM, false);

        SecurityGolemEntity golem = menu.getGolem();
        double dmg = golem.hasHarmingModule() ? SecurityGolemEntity.HARMING_ATTACK_DAMAGE : SecurityGolemEntity.BASE_ATTACK_DAMAGE;
        double spd = golem.hasSpeedModule() ? SecurityGolemEntity.FAST_SPEED : SecurityGolemEntity.BASE_SPEED;
        double det = golem.getEffectiveDetectionRadius();
        g.text(font, tr("scguardgolem.gui.stat.dmg", String.format("%.0f", dmg)), x + 8, statusY + 12, C_DIM, false);
        g.text(font, tr("scguardgolem.gui.stat.spd", String.format("%.2f", spd)), x + 60, statusY + 12, C_DIM, false);
        g.text(font, tr("scguardgolem.gui.stat.det", String.format("%.0f", det)), x + 118, statusY + 12, C_DIM, false);

        int lootCap = menu.getLootRows() * 9;
        g.text(font, tr("scguardgolem.gui.loot_slots", lootCap), x + 8, statusY + 24, C_DIM, false);

        String mode = SecurityGolemEntity.ThreatMode.fromOrdinal(menu.getData().get(1)).name();
        g.text(font, tr("scguardgolem.gui.mode", tr("scguardgolem.gui.threat." + mode)), x + 90, statusY + 24, C_DIM, false);
    }

    // ---------- LOOT TAB ----------
    private void drawLootTab(Gfx g, int x, int y) {
        int totalRows = menu.getLootRows();
        int visibleRows = Math.min(totalRows, GolemMenu.VISIBLE_LOOT_ROWS);

        for (int row = 0; row < visibleRows; row++) {
            for (int col = 0; col < 9; col++) {
                g.sprite(SLOT_SPRITE,
                    x + GolemMenu.LOOT_X - 1 + col * 18,
                    y + GolemMenu.LOOT_Y - 1 + row * 18, 18, 18);
            }
        }

        int scrollOff = menu.getScrollOffset();
        String rowInfo = "Rows " + (scrollOff + 1) + "-" + (scrollOff + visibleRows) + " of " + totalRows;
        if (totalRows <= visibleRows) rowInfo = totalRows + " row" + (totalRows == 1 ? "" : "s");
        g.text(font, rowInfo, x + 8, y + GolemMenu.LOOT_Y + visibleRows * 18 + 2, C_DIM, false);

        if (totalRows > GolemMenu.VISIBLE_LOOT_ROWS) {
            drawScrollBar(g, x, y, totalRows, visibleRows, scrollOff);
        }
    }

    private void drawScrollBar(Gfx g, int x, int y,
                                int totalRows, int visibleRows, int scrollOff) {
        int trackX = x + GolemMenu.LOOT_X + 9 * 18 + 2;
        int trackY = y + GolemMenu.LOOT_Y;
        int trackH = visibleRows * 18;

        g.sprite(SCROLLER_BG_SPRITE, trackX, trackY, SCROLLER_W, trackH);

        int maxScroll = menu.getMaxScroll();
        int thumbH = Math.max(10, trackH * visibleRows / totalRows);
        int thumbRange = trackH - thumbH;
        int thumbY = trackY + (maxScroll > 0 ? thumbRange * scrollOff / maxScroll : 0);

        g.sprite(SCROLLER_SPRITE, trackX, thumbY, SCROLLER_W, thumbH);
    }

    // ---------- LISTS TAB (allow/deny is now SecurityCraft modules) ----------
    private static final int LIST_X = 10;

    private void drawListsTab(Gfx g, int x, int y) {
        int curY = y + 20;
        String allowState = tr(menu.getGolem().hasAllowlistModule() ? "scguardgolem.gui.lists.installed" : "scguardgolem.gui.lists.none");
        String denyState = tr(menu.getGolem().hasDenylistModule() ? "scguardgolem.gui.lists.installed" : "scguardgolem.gui.lists.none");
        g.text(font, tr("scguardgolem.gui.title.lists"), x + LIST_X, curY, C_TITLE, false);
        curY += 14;
        g.text(font, tr("scguardgolem.gui.lists.allow", allowState), x + LIST_X, curY, 0xFF55FF55, false);
        curY += 11;
        g.text(font, tr("scguardgolem.gui.lists.deny", denyState), x + LIST_X, curY, 0xFFFF5555, false);
        curY += 16;
        g.text(font, tr("scguardgolem.gui.lists.hint1"), x + LIST_X, curY, C_DIM, false); curY += 10;
        g.text(font, tr("scguardgolem.gui.lists.hint2"), x + LIST_X, curY, C_DIM, false); curY += 10;
        g.text(font, tr("scguardgolem.gui.lists.hint3"), x + LIST_X, curY, C_DIM, false); curY += 10;
        g.text(font, tr("scguardgolem.gui.lists.hint4"), x + LIST_X, curY, C_DIM, false);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (menu.getCurrentTab() == GolemMenu.TAB_WAYPOINTS) {
            int wpSize = menu.getGolem().getWaypoints().size();
            if (listButtonsDirty || wpSize != lastWaypointSize) {
                lastWaypointSize = wpSize;
                clearListButtons();
                rebuildWaypointButtons();
                listButtonsDirty = false;
            }
        }
    }

    private void clearListButtons() {
        for (Button b : listButtons) removeWidget(b);
        listButtons.clear();
    }

    // ---------- PLAYER INVENTORY (both tabs) ----------
    private void drawPlayerInv(Gfx g, int x, int y) {
        int pInvY = menu.getPlayerInvY();

        int sepY = y + pInvY - 12;
        g.fill(x + 7, sepY, x + W - 7, sepY + 1, C_SEP);
        g.text(font, tr("scguardgolem.gui.inventory"), x + 8, sepY + 3, C_DIM, false);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                g.sprite(SLOT_SPRITE,
                    x + GolemMenu.PLAYER_INV_X - 1 + col * 18,
                    y + pInvY - 1 + row * 18, 18, 18);
            }
        }

        int hotbarY = pInvY + 58;
        for (int col = 0; col < 9; col++) {
            g.sprite(SLOT_SPRITE,
                x + GolemMenu.PLAYER_INV_X - 1 + col * 18,
                y + hotbarY - 1, 18, 18);
        }
    }

    private Component getPatrolText() {
        boolean on = menu.getData().get(0) != 0;
        return Component.translatable("scguardgolem.gui.patrol")
                .withStyle(on ? net.minecraft.ChatFormatting.DARK_GREEN : net.minecraft.ChatFormatting.DARK_RED);
    }

    private Component getThreatText() {
        int mode = menu.getData().get(1);
        String name = SecurityGolemEntity.ThreatMode.fromOrdinal(mode).name();
        return Component.translatable("scguardgolem.gui.threat." + name);
    }

    // ---------- localization helpers ----------
    private static String tr(String key) { return Component.translatable(key).getString(); }
    private static String tr(String key, Object... args) { return Component.translatable(key, args).getString(); }

    // ---------- WAYPOINTS TAB ----------
    // Dwell-time buttons (rebuilt with list buttons)
    private Button dwellDecBtn, dwellIncBtn;

    // Y offset constants that both draw and button-rebuild use so they stay in sync
    private static final int WP_DWELL_ROW_Y    = 18;   // top of dwell row (relative to panel top)
    private static final int WP_DWELL_ROW_H    = 16;   // height of dwell row
    private static final int WP_SEP_Y          = WP_DWELL_ROW_Y + WP_DWELL_ROW_H + 2; // separator
    private static final int WP_LIST_START_Y   = WP_SEP_Y + 6; // first waypoint entry

    private void drawWaypointsTab(Gfx g, int x, int y) {
        SecurityGolemEntity golem = menu.getGolem();
        List<BlockPos> waypoints = golem.getWaypoints();

        // Dwell time row
        int dwellY = y + WP_DWELL_ROW_Y;
        // Read from synced ContainerData so client always shows the live value
        int dwell = menu.getSyncedDwellTicks();
        int dwellSec = dwell / 20;
        g.text(font, tr("scguardgolem.gui.dwell"), x + 8, dwellY + 3, C_TITLE, false);
        // The actual dwell value, centered between the two buttons drawn by rebuildWaypointButtons
        String dwellVal = dwellSec + "s";
        int valX = x + W - 62; // left edge of the value text area (buttons at W-44 and W-22)
        g.text(font, dwellVal, valX + (20 - font.width(dwellVal)) / 2, dwellY + 3, 0xFFFFFFFF, false);

        // Separator
        int sepY = y + WP_SEP_Y;
        g.fill(x + 7, sepY, x + W - 7, sepY + 1, C_SEP);

        // Waypoint list
        int listY = y + WP_LIST_START_Y;
        if (waypoints.isEmpty()) {
            g.text(font, tr("scguardgolem.gui.no_waypoints"), x + 8, listY, C_DIM, false);
            listY += 11;
            g.text(font, tr("scguardgolem.gui.waypoint_hint1"), x + 8, listY, C_DIM, false);
            listY += 10;
            g.text(font, tr("scguardgolem.gui.waypoint_hint2"), x + 8, listY, C_DIM, false);
        } else {
            int maxVisible = (menu.getPlayerInvY() - WP_LIST_START_Y - 10) / 12;
            for (int i = 0; i < Math.min(waypoints.size(), maxVisible); i++) {
                BlockPos wp = waypoints.get(i);
                String name = golem.getWaypointName(i);
                String label = (i + 1) + ". ";
                if (!name.isEmpty()) label += name + " ";
                label += "(" + wp.getX() + ", " + wp.getY() + ", " + wp.getZ() + ")";
                boolean current = (i == golem.getCurrentWaypointIndex());
                int col = current ? 0xFF55FF55 : C_TITLE;
                g.text(font, (current ? "● " : "○ ") + label, x + 8, listY, col, false);
                // [x] label hint beside the button
                g.text(font, "[x]", x + W - 24, listY, 0xFFFF5555, false);
                listY += 12;
            }
            if (waypoints.size() > (menu.getPlayerInvY() - WP_LIST_START_Y - 10) / 12) {
                g.text(font, tr("scguardgolem.gui.more", waypoints.size() - (menu.getPlayerInvY() - WP_LIST_START_Y - 10) / 12),
                        x + 8, listY, C_DIM, false);
            }
        }
    }

    /** Rebuild waypoint remove-buttons and dwell +/- buttons. */
    private void rebuildWaypointButtons() {
        SecurityGolemEntity golem = menu.getGolem();
        List<BlockPos> waypoints = golem.getWaypoints();
        int x = leftPos;

        // Dwell [-] and [+] buttons — right-aligned beside the dwell label
        int dwellBtnY = topPos + WP_DWELL_ROW_Y;
        int dwellBtnH  = WP_DWELL_ROW_H - 2;
        dwellDecBtn = addRenderableWidget(Button.builder(Component.literal("-"),
                btn -> clickButton(800))
            .bounds(x + W - 44, dwellBtnY, 20, dwellBtnH).build());
        dwellIncBtn = addRenderableWidget(Button.builder(Component.literal("+"),
                btn -> clickButton(801))
            .bounds(x + W - 22, dwellBtnY, 20, dwellBtnH).build());
        listButtons.add(dwellDecBtn);
        listButtons.add(dwellIncBtn);

        // Waypoint [x] remove buttons
        int maxVisible = (menu.getPlayerInvY() - WP_LIST_START_Y - 10) / 12;
        for (int i = 0; i < Math.min(waypoints.size(), maxVisible); i++) {
            final int idx = i;
            int ey = topPos + WP_LIST_START_Y + i * 12;
            Button b = Button.builder(Component.literal("x"),
                    btn -> {
                        clickButton(700 + idx);
                        // Optimistically update client-side entity so the list refreshes immediately
                        menu.getGolem().removeWaypoint(idx);
                        listButtonsDirty = true;
                    })
                .bounds(x + W - 22, ey - 1, 14, 12).build();
            listButtons.add(addRenderableWidget(b));
        }
    }

}
