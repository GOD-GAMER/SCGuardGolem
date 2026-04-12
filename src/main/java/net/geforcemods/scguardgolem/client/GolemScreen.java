package net.geforcemods.scguardgolem.client;

import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.geforcemods.scguardgolem.inventory.GolemMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class GolemScreen extends AbstractContainerScreen<GolemMenu> {

    private static final Identifier CONTAINER_BG = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");

    private static final int MODULE_LABEL_COLOR = 0xFF55FFFF;
    private static final int SECTION_COLOR = 0xFFAAAAAA;
    private static final int MODULE_AREA_HEIGHT = 48;
    private static final int BUTTON_AREA_HEIGHT = 24;
    private static final int GAP = 4;

    private Button patrolButton;
    private Button threatButton;
    private Button cameraButton;

    public GolemScreen(GolemMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title, 176, calculateHeight(menu.getLootRows()));
        this.inventoryLabelY = this.imageHeight - 94;
    }

    private static int calculateHeight(int lootRows) {
        return 18 + MODULE_AREA_HEIGHT + GAP + BUTTON_AREA_HEIGHT + GAP
                + lootRows * 18 + 14 + 90 + 4;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos + 98;
        int y = topPos + 18;

        patrolButton = addRenderableWidget(
                Button.builder(getPatrolText(), btn -> {
                    clickButton(0);
                    btn.setMessage(getPatrolText());
                }).bounds(x, y, 70, 18).build());

        threatButton = addRenderableWidget(
                Button.builder(getThreatText(), btn -> {
                    clickButton(1);
                    btn.setMessage(getThreatText());
                }).bounds(x, y + 22, 70, 18).build());

        cameraButton = addRenderableWidget(
                Button.builder(getCameraText(), btn -> {
                    clickButton(2);
                    btn.setMessage(getCameraText());
                }).bounds(x, y + 44, 70, 18).build());
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

        // Update button labels each frame in case data changes
        patrolButton.setMessage(getPatrolText());
        threatButton.setMessage(getThreatText());
        cameraButton.setMessage(getCameraText());
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;

        // Draw dark background panel
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xCC101010);

        // Draw border
        graphics.fill(x, y, x + imageWidth, y + 1, 0xFF555555);
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF555555);
        graphics.fill(x, y, x + 1, y + imageHeight, 0xFF555555);
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF555555);

        // Module slot labels
        String[] topLabels = {"Harming", "Speed", "Smart"};
        String[] botLabels = {"Allowlist", "Denylist", "Storage"};
        for (int i = 0; i < 3; i++) {
            int slotX = x + 8 + i * 28;
            graphics.text(font, topLabels[i], slotX, y + 8, MODULE_LABEL_COLOR, false);
            graphics.text(font, botLabels[i], slotX, y + 30, MODULE_LABEL_COLOR, false);
        }

        // Loot section label
        int lootLabelY = y + 52;
        graphics.text(font, "Loot Chest", x + 8, lootLabelY, SECTION_COLOR, false);

        // Player inventory background (draw the standard inventory sprite from vanilla)
        int invY = y + imageHeight - 96;
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BG, x, invY, 0.0F, 126.0F, imageWidth, 96, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, 0, 0xFF55FF55, false);
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
