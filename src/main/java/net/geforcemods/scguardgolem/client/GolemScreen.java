package net.geforcemods.scguardgolem.client;

import java.util.List;

import net.geforcemods.scguardgolem.SCGuardGolem;
import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.geforcemods.scguardgolem.entity.SecurityGolemEntity.ThreatMode;
import net.geforcemods.scguardgolem.inventory.GolemMenu;
import net.geforcemods.scguardgolem.network.ModifyWaypoint;
import net.geforcemods.scguardgolem.network.SyncGolemSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;

/**
 * Client-side GUI for the Security Guard Golem.
 * Tabs: Modules | Patrol | Settings
 */
public class GolemScreen extends AbstractContainerScreen<GolemMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            SCGuardGolem.MODID, "textures/gui/golem_gui.png");

    private static final int TAB_MODULES = 0;
    private static final int TAB_PATROL = 1;
    private static final int TAB_SETTINGS = 2;

    private int currentTab = TAB_MODULES;

    // Settings tab widgets
    private EditBox passwordField;
    private EditBox patrolSpeedField;
    private Button threatModeButton;
    private Button patrolToggleButton;
    private Button cameraToggleButton;
    private Button saveButton;

    // Patrol tab widgets
    private Button addWaypointButton;
    private Button clearWaypointsButton;
    private int waypointScrollOffset = 0;

    // Tab buttons
    private Button tabModulesBtn;
    private Button tabPatrolBtn;
    private Button tabSettingsBtn;

    public GolemScreen(GolemMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 200;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        int x = leftPos;
        int y = topPos;

        // Tab buttons across the top
        tabModulesBtn = addRenderableWidget(Button.builder(
                Component.literal("Modules"), b -> switchTab(TAB_MODULES))
                .bounds(x, y - 18, 58, 18).build());
        tabPatrolBtn = addRenderableWidget(Button.builder(
                Component.literal("Patrol"), b -> switchTab(TAB_PATROL))
                .bounds(x + 59, y - 18, 58, 18).build());
        tabSettingsBtn = addRenderableWidget(Button.builder(
                Component.literal("Settings"), b -> switchTab(TAB_SETTINGS))
                .bounds(x + 118, y - 18, 58, 18).build());

        // --- Settings tab widgets ---
        SecurityGolemEntity golem = menu.getGolem();

        patrolSpeedField = new EditBox(font, x + 70, y + 26, 50, 16,
                Component.literal("Patrol Speed"));
        patrolSpeedField.setMaxLength(5);
        patrolSpeedField.setValue(golem != null ? String.format("%.1f", golem.getPatrolSpeed()) : "1.0");
        addWidget(patrolSpeedField);

        passwordField = new EditBox(font, x + 70, y + 48, 96, 16,
                Component.literal("Chest Password"));
        passwordField.setMaxLength(16);
        passwordField.setValue(golem != null ? golem.getChestPassword() : "");
        addWidget(passwordField);

        ThreatMode mode = golem != null ? golem.getThreatMode() : ThreatMode.WARN;
        threatModeButton = addRenderableWidget(Button.builder(
                Component.literal("Threat: " + mode.name()),
                b -> cycleThreatMode())
                .bounds(x + 8, y + 70, 80, 18).build());

        boolean patrolling = golem != null && golem.isPatrolling();
        patrolToggleButton = addRenderableWidget(Button.builder(
                Component.literal("Patrol: " + (patrolling ? "ON" : "OFF")),
                b -> togglePatrol())
                .bounds(x + 90, y + 70, 78, 18).build());

        boolean camEnabled = golem != null && golem.isCameraEnabled();
        cameraToggleButton = addRenderableWidget(Button.builder(
                Component.literal("Camera: " + (camEnabled ? "ON" : "OFF")),
                b -> toggleCamera())
                .bounds(x + 8, y + 92, 80, 18).build());

        saveButton = addRenderableWidget(Button.builder(
                Component.literal("Save Settings"),
                b -> sendSettings())
                .bounds(x + 90, y + 92, 78, 18).build());

        // --- Patrol tab widgets ---
        addWaypointButton = addRenderableWidget(Button.builder(
                Component.literal("+ Add Here"),
                b -> addWaypointHere())
                .bounds(x + 8, y + 20, 76, 18).build());

        clearWaypointsButton = addRenderableWidget(Button.builder(
                Component.literal("Clear All"),
                b -> clearWaypoints())
                .bounds(x + 90, y + 20, 76, 18).build());

        switchTab(TAB_MODULES);
    }

    private void switchTab(int tab) {
        currentTab = tab;

        boolean isMod = tab == TAB_MODULES;
        boolean isPat = tab == TAB_PATROL;
        boolean isSet = tab == TAB_SETTINGS;

        // Settings widgets
        patrolSpeedField.setVisible(isSet);
        passwordField.setVisible(isSet);
        threatModeButton.visible = isSet;
        patrolToggleButton.visible = isSet;
        cameraToggleButton.visible = isSet;
        saveButton.visible = isSet;

        // Patrol widgets
        addWaypointButton.visible = isPat;
        clearWaypointsButton.visible = isPat;

        // Tab button emphasis
        tabModulesBtn.active = !isMod;
        tabPatrolBtn.active = !isPat;
        tabSettingsBtn.active = !isSet;
    }

    private void cycleThreatMode() {
        SecurityGolemEntity golem = menu.getGolem();
        if (golem == null) return;
        ThreatMode[] modes = ThreatMode.values();
        int next = (golem.getThreatMode().ordinal() + 1) % modes.length;
        golem.setThreatMode(modes[next]);
        threatModeButton.setMessage(Component.literal("Threat: " + modes[next].name()));
    }

    private void togglePatrol() {
        SecurityGolemEntity golem = menu.getGolem();
        if (golem == null) return;
        boolean newVal = !golem.isPatrolling();
        golem.setPatrolling(newVal);
        patrolToggleButton.setMessage(Component.literal("Patrol: " + (newVal ? "ON" : "OFF")));
    }

    private void toggleCamera() {
        SecurityGolemEntity golem = menu.getGolem();
        if (golem == null) return;
        boolean newVal = !golem.isCameraEnabled();
        golem.setCameraEnabled(newVal);
        cameraToggleButton.setMessage(Component.literal("Camera: " + (newVal ? "ON" : "OFF")));
    }

    private void sendSettings() {
        SecurityGolemEntity golem = menu.getGolem();
        if (golem == null) return;

        double speed = 1.0;
        try {
            speed = Double.parseDouble(patrolSpeedField.getValue());
        } catch (NumberFormatException ignored) {}

        if (minecraft != null && minecraft.getConnection() != null) {
            minecraft.getConnection().send(new ServerboundCustomPayloadPacket(
                    new SyncGolemSettings(
                            golem.getId(),
                            golem.getThreatMode().ordinal(),
                            speed,
                            golem.isPatrolling(),
                            passwordField.getValue(),
                            golem.isCameraEnabled())));
        }
    }

    private void addWaypointHere() {
        SecurityGolemEntity golem = menu.getGolem();
        if (golem == null || minecraft == null || minecraft.player == null) return;
        BlockPos pos = minecraft.player.blockPosition();
        if (minecraft != null && minecraft.getConnection() != null) {
            minecraft.getConnection().send(new ServerboundCustomPayloadPacket(
                    new ModifyWaypoint(golem.getId(), ModifyWaypoint.ACTION_ADD, pos)));
        }
    }

    private void clearWaypoints() {
        SecurityGolemEntity golem = menu.getGolem();
        if (golem == null) return;
        if (minecraft != null && minecraft.getConnection() != null) {
            minecraft.getConnection().send(new ServerboundCustomPayloadPacket(
                    new ModifyWaypoint(golem.getId(), ModifyWaypoint.ACTION_CLEAR, BlockPos.ZERO)));
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        if (currentTab == TAB_SETTINGS) {
            graphics.drawString(font, "Speed:", leftPos + 10, topPos + 30, 0x404040, false);
            graphics.drawString(font, "Password:", leftPos + 10, topPos + 52, 0x404040, false);
            patrolSpeedField.renderWidget(graphics, mouseX, mouseY, partialTick);
            passwordField.renderWidget(graphics, mouseX, mouseY, partialTick);
        }

        if (currentTab == TAB_PATROL) {
            renderWaypointList(graphics);
        }

        if (currentTab == TAB_MODULES) {
            String[] labels = {"Allow", "Deny", "Harm", "Speed", "Smart", "Store"};
            for (int i = 0; i < labels.length; i++) {
                graphics.drawString(font, labels[i], leftPos + 13 + i * 26, topPos + 10, 0x404040, false);
            }

            int unlocked = menu.getUnlockedLootSlots();
            String lootLabel = "Loot Storage (" + unlocked + "/" + GolemMenu.MAX_LOOT_SLOTS + " slots)";
            graphics.drawString(font, lootLabel, leftPos + 8, topPos + 42, 0x404040, false);
        }

        renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderWaypointList(GuiGraphics graphics) {
        SecurityGolemEntity golem = menu.getGolem();
        if (golem == null) return;

        List<BlockPos> waypoints = golem.getWaypoints();
        int y = topPos + 42;
        graphics.drawString(font, "Waypoints (" + waypoints.size() + "):", leftPos + 8, y, 0x404040, false);
        y += 12;

        int maxVisible = 6;
        for (int i = waypointScrollOffset; i < Math.min(waypoints.size(), waypointScrollOffset + maxVisible); i++) {
            BlockPos wp = waypoints.get(i);
            String text = (i + 1) + ". [" + wp.getX() + ", " + wp.getY() + ", " + wp.getZ() + "]";
            graphics.drawString(font, text, leftPos + 12, y, 0x606060, false);
            y += 11;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (currentTab == TAB_PATROL && menu.getGolem() != null) {
            int maxOffset = Math.max(0, menu.getGolem().getWaypoints().size() - 6);
            waypointScrollOffset = Math.max(0, Math.min(waypointScrollOffset - (int) scrollY, maxOffset));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        if (currentTab == TAB_MODULES) {
            graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (patrolSpeedField.isFocused() || passwordField.isFocused()) {
            if (keyCode == 256) { // Escape
                patrolSpeedField.setFocused(false);
                passwordField.setFocused(false);
                return true;
            }
            if (patrolSpeedField.isFocused())
                return patrolSpeedField.keyPressed(keyCode, scanCode, modifiers);
            return passwordField.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        sendSettings();
        super.removed();
    }
}
