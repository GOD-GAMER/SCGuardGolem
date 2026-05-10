package net.geforcemods.scguardgolem.client;

import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.geforcemods.scguardgolem.inventory.GolemMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GolemScreen extends AbstractContainerScreen<GolemMenu> {

    // ?? Sprites ??
    private static final ResourceLocation PANEL_SPRITE = ResourceLocation.parse("scguardgolem:scg_panel");
    private static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot");
    private static final ResourceLocation TAB_SPRITE = ResourceLocation.withDefaultNamespace("widget/tab");
    private static final ResourceLocation TAB_SELECTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/tab_selected");
    private static final ResourceLocation TAB_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/tab_highlighted");
    private static final ResourceLocation TAB_SEL_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/tab_selected_highlighted");
    private static final ResourceLocation SCROLLER_BG_SPRITE = ResourceLocation.withDefaultNamespace("widget/scroller_background");
    private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("widget/scroller");

    // ?? Text colors (ARGB) ??
    private static final int C_TITLE = 0xFF404040;
    private static final int C_DIM   = 0xFF666666;
    private static final int C_SEP   = 0xFFAAAAAA;

    // ?? Dimensions ??
    private static final int W = 176;
    private static final int TAB_H = 20;
    private static final int SCROLLER_W = 8;
    private final int H;

    // Toggle buttons for config tab
    private Button patrolBtn, threatBtn, clearRouteBtn;
    // Lists tab: picker scroll offset
    private int pickerScroll = 0;
    private List<PickerEntry> pickerEntries = List.of();
    // Dynamic buttons for Lists tab
    private final List<Button> listButtons = new ArrayList<>();
    private boolean listButtonsDirty = true;
    private int lastIgnoreSize = -1;
    private int lastAttackSize = -1;

    public GolemScreen(GolemMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = W;
        this.imageHeight = menu.getGuiHeight();
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

        // ?? Config tab: 3 toggle buttons below modules ??
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
            Button.builder(Component.literal("\u00a7cClear"), b -> clickButton(2))
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
        if (tab == GolemMenu.TAB_LISTS) {
            pickerScroll = 0;
            allowScroll = 0;
            denyScroll = 0;
            refreshPickerEntries();
            listButtonsDirty = true;
        } else if (tab == GolemMenu.TAB_WAYPOINTS) {
            listButtonsDirty = true;
        }
        clickButton(200 + tab);
    }

    private void clickButton(int id) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    // ?? Tab click handling ??
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
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
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int x(int i, int tabW) { return leftPos + i * tabW; }
    private int tabWidth(int i, int tabW) { return (i == 3) ? W - tabW * 3 : tabW; }

    // ?? Scroll support ??
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (menu.getCurrentTab() == GolemMenu.TAB_LISTS) {
            SecurityGolemEntity golem = menu.getGolem();
            int allowSize = golem.getIgnoreListNames().size();
            int denySize = golem.getAlwaysAttackListNames().size();
            int delta = scrollY > 0 ? -1 : 1;

            // Determine Y regions
            int allowStartY = topPos + LIST_START_Y + LIST_ENTRY_H;
            int allowEndY = allowStartY + Math.max(1, Math.min(allowSize, LIST_MAX_VISIBLE)) * LIST_ENTRY_H;
            int denyStartY = allowEndY + 4 + LIST_ENTRY_H;
            int denyEndY = denyStartY + Math.max(1, Math.min(denySize, LIST_MAX_VISIBLE)) * LIST_ENTRY_H;
            int pickerStartY = denyEndY + 4 + 2 + LIST_ENTRY_H;

            if (mouseY >= allowStartY && mouseY < allowEndY && allowSize > LIST_MAX_VISIBLE) {
                allowScroll = Math.max(0, Math.min(allowScroll + delta, allowSize - LIST_MAX_VISIBLE));
                listButtonsDirty = true;
                return true;
            }
            if (mouseY >= denyStartY && mouseY < denyEndY && denySize > LIST_MAX_VISIBLE) {
                denyScroll = Math.max(0, Math.min(denyScroll + delta, denySize - LIST_MAX_VISIBLE));
                listButtonsDirty = true;
                return true;
            }
            if (mouseY >= pickerStartY) {
                int maxPickerScroll = Math.max(0, pickerEntries.size() - getPickerVisibleCount(calcPickerStartY(topPos)));
                if (maxPickerScroll > 0) {
                    pickerScroll = Math.max(0, Math.min(pickerScroll + delta, maxPickerScroll));
                    listButtonsDirty = true;
                    return true;
                }
            }
        }
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
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        patrolBtn.setMessage(getPatrolText());
        threatBtn.setMessage(getThreatText());
        renderTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos;
        int y = topPos;
        int tabY = y - TAB_H;
        int curTab = menu.getCurrentTab();

        // ?? Draw 4 tabs ??
        String[] tabLabels = {"Config", "Loot", "Lists", "Route"};
        int tabW = W / 4;
        for (int i = 0; i < 4; i++) {
            int tx = x + i * tabW;
            int tw = tabWidth(i, tabW);
            boolean sel = (curTab == i);
            boolean hover = mx >= tx && mx < tx + tw && my >= tabY && my < y;
            ResourceLocation spr;
            if (sel) spr = hover ? TAB_SEL_HIGHLIGHTED_SPRITE : TAB_SELECTED_SPRITE;
            else spr = hover ? TAB_HIGHLIGHTED_SPRITE : TAB_SPRITE;

            if (!sel) g.blitSprite(spr, tx, tabY, tw, TAB_H + 3);
        }

        // ?? Panel background ??
        g.blitSprite(PANEL_SPRITE, x, y, W, H);

        // ?? Selected tab on top ??
        for (int i = 0; i < 4; i++) {
            if (curTab != i) continue;
            int tx = x + i * tabW;
            int tw = tabWidth(i, tabW);
            boolean hover = mx >= tx && mx < tx + tw && my >= tabY && my < y;
            ResourceLocation spr = hover ? TAB_SEL_HIGHLIGHTED_SPRITE : TAB_SELECTED_SPRITE;
            g.blitSprite(spr, tx, tabY, tw, TAB_H + 3);
        }

        // ?? Tab labels ??
        int tabTextY = tabY + (TAB_H - font.lineHeight) / 2;
        for (int i = 0; i < 4; i++) {
            int tx = x + i * tabW;
            int tw = tabWidth(i, tabW);
            g.drawString(font, tabLabels[i],
                tx + (tw - font.width(tabLabels[i])) / 2, tabTextY,
                curTab == i ? C_TITLE : C_DIM, false);
        }

        // ?? Title ??
        String titleText = switch (curTab) {
            case GolemMenu.TAB_CONFIG -> "Security Golem";
            case GolemMenu.TAB_LOOT -> "Collected Loot";
            case GolemMenu.TAB_LISTS -> "Allow / Deny Lists";
            case GolemMenu.TAB_WAYPOINTS -> "Patrol Route";
            default -> "";
        };
        g.drawString(font, titleText, x + 8, y + 6, C_TITLE, false);

        // ?? Tab content ??
        switch (curTab) {
            case GolemMenu.TAB_CONFIG -> drawConfigTab(g, x, y);
            case GolemMenu.TAB_LOOT -> drawLootTab(g, x, y);
            case GolemMenu.TAB_LISTS -> drawListsTab(g, x, y);
            case GolemMenu.TAB_WAYPOINTS -> drawWaypointsTab(g, x, y);
        }

        // ?? Player inventory ??
        drawPlayerInv(g, x, y);
    }

    // ?????????? CONFIG TAB ??????????
    private void drawConfigTab(GuiGraphics g, int x, int y) {
        String[] labels = {"Harm", "Speed", "Smart", "Store"};
        for (int i = 0; i < 4; i++) {
            int col = i % 2;
            int row = i / 2;
            int sx = x + GolemMenu.MOD_X - 1 + col * GolemMenu.MOD_COL;
            int sy = y + GolemMenu.MOD_Y - 1 + row * GolemMenu.MOD_ROW;

            g.blitSprite(SLOT_SPRITE, sx, sy, 18, 18);
            g.drawString(font, labels[i], sx + 1, sy + 19, C_DIM, false);
        }

        // ?? Status info below buttons ??
        int modBottom = GolemMenu.MOD_Y + 2 * GolemMenu.MOD_ROW;
        int statusY = y + modBottom + 28;

        String owner = menu.getGolem().getOwnerName();
        g.drawString(font, "Owner: " + (owner.isEmpty() ? "None" : owner), x + 8, statusY, C_DIM, false);

        String hp = String.format("HP: %.0f/%.0f", menu.getGolem().getHealth(), menu.getGolem().getMaxHealth());
        g.drawString(font, hp, x + 90, statusY, C_DIM, false);

        SecurityGolemEntity golem = menu.getGolem();
        double dmg = 15.0 + golem.getDamageUpgrade() * SecurityGolemEntity.DAMAGE_PER_LEVEL;
        double spd = 0.25 + golem.getSpeedUpgrade() * SecurityGolemEntity.SPEED_PER_LEVEL;
        double det = golem.getEffectiveDetectionRadius();
        g.drawString(font, String.format("Dmg: %.0f", dmg), x + 8, statusY + 12, C_DIM, false);
        g.drawString(font, String.format("Spd: %.2f", spd), x + 60, statusY + 12, C_DIM, false);
        g.drawString(font, String.format("Det: %.0f", det), x + 118, statusY + 12, C_DIM, false);

        int lootCap = menu.getLootRows() * 9;
        g.drawString(font, "Loot: " + lootCap + " slots", x + 8, statusY + 24, C_DIM, false);

        String mode = SecurityGolemEntity.ThreatMode.fromOrdinal(menu.getData().get(1)).name();
        g.drawString(font, "Mode: " + mode, x + 90, statusY + 24, C_DIM, false);
    }

    // ?????????? LOOT TAB ??????????
    private void drawLootTab(GuiGraphics g, int x, int y) {
        int totalRows = menu.getLootRows();
        int visibleRows = Math.min(totalRows, GolemMenu.VISIBLE_LOOT_ROWS);

        for (int row = 0; row < visibleRows; row++) {
            for (int col = 0; col < 9; col++) {
                g.blitSprite(SLOT_SPRITE,
                    x + GolemMenu.LOOT_X - 1 + col * 18,
                    y + GolemMenu.LOOT_Y - 1 + row * 18, 18, 18);
            }
        }

        int scrollOff = menu.getScrollOffset();
        String rowInfo = "Rows " + (scrollOff + 1) + "-" + (scrollOff + visibleRows) + " of " + totalRows;
        if (totalRows <= visibleRows) rowInfo = totalRows + " row" + (totalRows == 1 ? "" : "s");
        g.drawString(font, rowInfo, x + 8, y + GolemMenu.LOOT_Y + visibleRows * 18 + 2, C_DIM, false);

        if (totalRows > GolemMenu.VISIBLE_LOOT_ROWS) {
            drawScrollBar(g, x, y, totalRows, visibleRows, scrollOff);
        }
    }

    private void drawScrollBar(GuiGraphics g, int x, int y,
                                int totalRows, int visibleRows, int scrollOff) {
        int trackX = x + GolemMenu.LOOT_X + 9 * 18 + 2;
        int trackY = y + GolemMenu.LOOT_Y;
        int trackH = visibleRows * 18;

        g.blitSprite(SCROLLER_BG_SPRITE,
            trackX, trackY, SCROLLER_W, trackH);

        int maxScroll = menu.getMaxScroll();
        int thumbH = Math.max(10, trackH * visibleRows / totalRows);
        int thumbRange = trackH - thumbH;
        int thumbY = trackY + (maxScroll > 0 ? thumbRange * scrollOff / maxScroll : 0);

        g.blitSprite(SCROLLER_SPRITE,
            trackX, thumbY, SCROLLER_W, thumbH);
    }

    // ---------- LISTS TAB ----------
    private static final int LIST_ENTRY_H = 12;
    private static final int LIST_START_Y = 20;
    private static final int LIST_X = 10;
    private static final int PICKER_MAX_VISIBLE = 6;
    private static final int LIST_MAX_VISIBLE = 4;
    private int allowScroll = 0;
    private int denyScroll = 0;

    private record PickerEntry(String name) {}

    @Override
    public void containerTick() {
        super.containerTick();
        int curTab = menu.getCurrentTab();
        if (curTab == GolemMenu.TAB_LISTS) {
            SecurityGolemEntity golem = menu.getGolem();
            int ig = golem.getIgnoreListNames().size();
            int at = golem.getAlwaysAttackListNames().size();
            if (ig != lastIgnoreSize || at != lastAttackSize) {
                lastIgnoreSize = ig;
                lastAttackSize = at;
                listButtonsDirty = true;
            }
            refreshPickerEntries();
            if (listButtonsDirty) rebuildListButtons();
        } else if (curTab == GolemMenu.TAB_WAYPOINTS) {
            if (listButtonsDirty) {
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

    private void rebuildListButtons() {
        clearListButtons();
        SecurityGolemEntity golem = menu.getGolem();
        List<String> ignoreNames = new ArrayList<>(golem.getIgnoreListNames());
        List<String> attackNames = new ArrayList<>(golem.getAlwaysAttackListNames());
        int x = leftPos;
        int btnH = LIST_ENTRY_H;

        // Clamp scroll offsets
        allowScroll = Math.max(0, Math.min(allowScroll, Math.max(0, ignoreNames.size() - LIST_MAX_VISIBLE)));
        denyScroll = Math.max(0, Math.min(denyScroll, Math.max(0, attackNames.size() - LIST_MAX_VISIBLE)));

        // [x] remove from ignore list (visible entries only)
        int curY = topPos + LIST_START_Y + LIST_ENTRY_H;
        int visAllow = Math.min(ignoreNames.size(), LIST_MAX_VISIBLE);
        for (int i = 0; i < visAllow; i++) {
            final int actualIdx = allowScroll + i;
            if (actualIdx >= ignoreNames.size()) break;
            Button b = Button.builder(Component.literal("\u00a7cx"), btn -> { clickButton(300 + actualIdx); listButtonsDirty = true; })
                .bounds(x + W - 22, curY, 14, btnH).build();
            listButtons.add(addRenderableWidget(b));
            curY += LIST_ENTRY_H;
        }
        if (ignoreNames.isEmpty()) curY += LIST_ENTRY_H;

        curY += 4 + LIST_ENTRY_H; // gap + Deny label
        int visDeny = Math.min(attackNames.size(), LIST_MAX_VISIBLE);
        for (int i = 0; i < visDeny; i++) {
            final int actualIdx = denyScroll + i;
            if (actualIdx >= attackNames.size()) break;
            Button b = Button.builder(Component.literal("\u00a7cx"), btn -> { clickButton(400 + actualIdx); listButtonsDirty = true; })
                .bounds(x + W - 22, curY, 14, btnH).build();
            listButtons.add(addRenderableWidget(b));
            curY += LIST_ENTRY_H;
        }
        if (attackNames.isEmpty()) curY += LIST_ENTRY_H;

        // Picker [A]/[D]
        curY += 4 + 2 + LIST_ENTRY_H; // gap + separator + Entities label
        int vis = getPickerVisibleCount(curY);
        for (int i = 0; i < vis; i++) {
            int idx = pickerScroll + i;
            if (idx >= pickerEntries.size()) break;
            int ey = curY + i * LIST_ENTRY_H;
            final int fIdx = idx;
            Button aBtn = Button.builder(Component.literal("\u00a7aA"), btn -> { clickButton(500 + fIdx); listButtonsDirty = true; })
                .bounds(x + W - 36, ey, 14, btnH).build();
            Button dBtn = Button.builder(Component.literal("\u00a7cD"), btn -> { clickButton(600 + fIdx); listButtonsDirty = true; })
                .bounds(x + W - 18, ey, 14, btnH).build();
            listButtons.add(addRenderableWidget(aBtn));
            listButtons.add(addRenderableWidget(dBtn));
        }
        listButtonsDirty = false;
    }

    private void refreshPickerEntries() {
        if (minecraft == null || minecraft.level == null) { pickerEntries = List.of(); return; }
        SecurityGolemEntity golem = menu.getGolem();
        Set<String> existing = new LinkedHashSet<>();
        existing.addAll(golem.getIgnoreListNames());
        existing.addAll(golem.getAlwaysAttackListNames());

        Set<String> seen = new LinkedHashSet<>();
        List<PickerEntry> entries = new ArrayList<>();

        // All online players
        if (minecraft.getConnection() != null) {
            for (PlayerInfo info : minecraft.getConnection().getOnlinePlayers()) {
                String name = info.getProfile().getName();
                if (!existing.contains(name) && seen.add(name)) {
                    entries.add(new PickerEntry(name));
                }
            }
        }

        // All living entities in loaded chunks
        for (var e : minecraft.level.entitiesForRendering()) {
            if (e instanceof LivingEntity le && le.isAlive() && le != golem && !(le instanceof Player)) {
                String name = le.getName().getString();
                if (!existing.contains(name) && seen.add(name)) {
                    entries.add(new PickerEntry(name));
                }
            }
        }
        pickerEntries = entries;
    }

    private int getPickerVisibleCount(int pickerStartY) {
        int available = (topPos + menu.getPlayerInvY() - 22 - pickerStartY) / LIST_ENTRY_H;
        return Math.max(1, Math.min(available, PICKER_MAX_VISIBLE));
    }

    private int calcPickerStartY(int y) {
        SecurityGolemEntity golem = menu.getGolem();
        int h = LIST_START_Y;
        h += LIST_ENTRY_H; // Allow label
        h += Math.max(1, Math.min(golem.getIgnoreListNames().size(), LIST_MAX_VISIBLE)) * LIST_ENTRY_H;
        h += 4;
        h += LIST_ENTRY_H; // Deny label
        h += Math.max(1, Math.min(golem.getAlwaysAttackListNames().size(), LIST_MAX_VISIBLE)) * LIST_ENTRY_H;
        h += 4 + 2 + LIST_ENTRY_H; // gap + separator + Entities label
        return y + h;
    }

    private void drawListsTab(GuiGraphics g, int x, int y) {
        refreshPickerEntries();
        SecurityGolemEntity golem = menu.getGolem();
        List<String> ignoreNames = new ArrayList<>(golem.getIgnoreListNames());
        List<String> attackNames = new ArrayList<>(golem.getAlwaysAttackListNames());

        // Allow section
        int curY = y + LIST_START_Y;
        String allowLabel = "\u00a7aAllow (" + ignoreNames.size() + "):";
        g.drawString(font, allowLabel, x + LIST_X, curY, 0xFF55FF55, false);
        if (ignoreNames.size() > LIST_MAX_VISIBLE)
            g.drawString(font, "\u00a78\u2191\u2193", x + W - 22, curY, C_DIM, false);
        curY += LIST_ENTRY_H;
        if (ignoreNames.isEmpty()) {
            g.drawString(font, "\u00a77(empty)", x + LIST_X + 4, curY, C_DIM, false);
            curY += LIST_ENTRY_H;
        } else {
            int visAllow = Math.min(ignoreNames.size(), LIST_MAX_VISIBLE);
            for (int i = 0; i < visAllow; i++) {
                int idx = allowScroll + i;
                if (idx >= ignoreNames.size()) break;
                g.drawString(font, "\u00a7a\u25CF " + ignoreNames.get(idx), x + LIST_X + 2, curY, 0xFF55FF55, false);
                g.drawString(font, "\u00a77[\u00a7cx\u00a77]", x + W - 24, curY, 0xFFAAAAAA, false);
                curY += LIST_ENTRY_H;
            }
        }

        curY += 4;
        // Deny section
        String denyLabel = "\u00a7cDeny (" + attackNames.size() + "):";
        g.drawString(font, denyLabel, x + LIST_X, curY, 0xFFFF5555, false);
        if (attackNames.size() > LIST_MAX_VISIBLE)
            g.drawString(font, "\u00a78\u2191\u2193", x + W - 22, curY, C_DIM, false);
        curY += LIST_ENTRY_H;
        if (attackNames.isEmpty()) {
            g.drawString(font, "\u00a77(empty)", x + LIST_X + 4, curY, C_DIM, false);
            curY += LIST_ENTRY_H;
        } else {
            int visDeny = Math.min(attackNames.size(), LIST_MAX_VISIBLE);
            for (int i = 0; i < visDeny; i++) {
                int idx = denyScroll + i;
                if (idx >= attackNames.size()) break;
                g.drawString(font, "\u00a7c\u25CF " + attackNames.get(idx), x + LIST_X + 2, curY, 0xFFFF5555, false);
                g.drawString(font, "\u00a77[\u00a7cx\u00a77]", x + W - 24, curY, 0xFFAAAAAA, false);
                curY += LIST_ENTRY_H;
            }
        }

        // Nearby entities picker
        curY += 4;
        g.fill(x + 7, curY, x + W - 7, curY + 1, C_SEP);
        curY += 2;
        g.drawString(font, "\u00a7fEntities:", x + LIST_X, curY, 0xFFFFFFFF, false);
        curY += LIST_ENTRY_H;

        if (pickerEntries.isEmpty()) {
            g.drawString(font, "\u00a77(none)", x + LIST_X + 4, curY, C_DIM, false);
        } else {
            int visible = getPickerVisibleCount(curY);
            for (int i = 0; i < visible; i++) {
                int idx = pickerScroll + i;
                if (idx >= pickerEntries.size()) break;
                PickerEntry pe = pickerEntries.get(idx);
                g.drawString(font, "\u00a7f" + pe.name(), x + LIST_X + 2, curY, 0xFFFFFFFF, false);
                g.drawString(font, "\u00a7a[A]", x + W - 36, curY, 0xFF55FF55, false);
                g.drawString(font, "\u00a7c[D]", x + W - 18, curY, 0xFFFF5555, false);
                curY += LIST_ENTRY_H;
            }
            if (pickerEntries.size() > visible) {
                g.drawString(font, "\u00a78\u2191\u2193 scroll", x + LIST_X, curY, C_DIM, false);
            }
        }
    }

    // ---------- PLAYER INVENTORY (both tabs) ----------
    private void drawPlayerInv(GuiGraphics g, int x, int y) {
        int pInvY = menu.getPlayerInvY();

        int sepY = y + pInvY - 12;
        g.fill(x + 7, sepY, x + W - 7, sepY + 1, C_SEP);
        g.drawString(font, "Inventory", x + 8, sepY + 3, C_DIM, false);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                g.blitSprite(SLOT_SPRITE,
                    x + GolemMenu.PLAYER_INV_X - 1 + col * 18,
                    y + pInvY - 1 + row * 18, 18, 18);
            }
        }

        int hotbarY = pInvY + 58;
        for (int col = 0; col < 9; col++) {
            g.blitSprite(SLOT_SPRITE,
                x + GolemMenu.PLAYER_INV_X - 1 + col * 18,
                y + hotbarY - 1, 18, 18);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        // handled in renderBg per-tab
    }

    private Component getPatrolText() {
        boolean on = menu.getData().get(0) != 0;
        return Component.literal(on ? "\u00a72Patrol" : "\u00a74Patrol");
    }

    private Component getThreatText() {
        int mode = menu.getData().get(1);
        String name = SecurityGolemEntity.ThreatMode.fromOrdinal(mode).name();
        return Component.literal(name);
    }

    // ---------- WAYPOINTS TAB ----------
    private Button dwellDecBtn, dwellIncBtn;

    private static final int WP_DWELL_ROW_Y  = 18;
    private static final int WP_DWELL_ROW_H  = 16;
    private static final int WP_SEP_Y        = WP_DWELL_ROW_Y + WP_DWELL_ROW_H + 2;
    private static final int WP_LIST_START_Y = WP_SEP_Y + 6;

    private void drawWaypointsTab(GuiGraphics g, int x, int y) {
        SecurityGolemEntity golem = menu.getGolem();
        List<BlockPos> waypoints = golem.getWaypoints();

        // Dwell time row
        int dwellY = y + WP_DWELL_ROW_Y;
        int dwell = menu.getGolem().getDwellTicks();
        int dwellSec = dwell / 20;
        g.drawString(font, "Dwell time:", x + 8, dwellY + 3, C_TITLE, false);
        String dwellVal = dwellSec + "s";
        int valX = x + W - 62;
        g.drawString(font, dwellVal, valX + (20 - font.width(dwellVal)) / 2, dwellY + 3, 0xFFFFFFFF, false);

        // Separator
        g.fill(x + 7, y + WP_SEP_Y, x + W - 7, y + WP_SEP_Y + 1, C_SEP);

        // Waypoint list
        int listY = y + WP_LIST_START_Y;
        if (waypoints.isEmpty()) {
            g.drawString(font, "No waypoints set.", x + 8, listY, C_DIM, false);
            listY += 11;
            g.drawString(font, "\u00a77Hold Wire Cutters and", x + 8, listY, C_DIM, false);
            listY += 10;
            g.drawString(font, "\u00a77crouch twice to add one.", x + 8, listY, C_DIM, false);
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
                g.drawString(font, (current ? "\u25CF " : "\u25CB ") + label, x + 8, listY, col, false);
                g.drawString(font, "\u00a7c[x]", x + W - 24, listY, 0xFFFF5555, false);
                listY += 12;
            }
            if (waypoints.size() > (menu.getPlayerInvY() - WP_LIST_START_Y - 10) / 12) {
                g.drawString(font, "\u00a78... +" + (waypoints.size() - (menu.getPlayerInvY() - WP_LIST_START_Y - 10) / 12) + " more",
                        x + 8, listY, C_DIM, false);
            }
        }
    }

    private void rebuildWaypointButtons() {
        SecurityGolemEntity golem = menu.getGolem();
        List<BlockPos> waypoints = golem.getWaypoints();
        int x = leftPos;

        // Dwell [-] and [+] buttons
        int dwellBtnY = topPos + WP_DWELL_ROW_Y;
        int dwellBtnH = WP_DWELL_ROW_H - 2;
        dwellDecBtn = addRenderableWidget(Button.builder(Component.literal("-"),
                btn -> { clickButton(800); menu.getGolem().setDwellTicks(menu.getGolem().getDwellTicks() - 20); })
            .bounds(x + W - 44, dwellBtnY, 20, dwellBtnH).build());
        dwellIncBtn = addRenderableWidget(Button.builder(Component.literal("+"),
                btn -> { clickButton(801); menu.getGolem().setDwellTicks(menu.getGolem().getDwellTicks() + 20); })
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
                        menu.getGolem().removeWaypoint(idx);
                        listButtonsDirty = true;
                    })
                .bounds(x + W - 22, ey - 1, 14, 12).build();
            listButtons.add(addRenderableWidget(b));
        }
    }
}
