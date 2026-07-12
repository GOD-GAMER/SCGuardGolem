package net.geforcemods.scguardgolem.client;

import net.minecraft.client.gui.Font;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else
/*import net.minecraft.client.gui.GuiGraphics;*/
//? if >=1.21.8 && !forge
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Thin per-version graphics adapter so the screen's drawing code is written
 * once. Absorbs: GuiGraphics -> GuiGraphicsExtractor (26.1 GUI rework),
 * the RenderPipelines blitSprite parameter (1.21.6+), drawString -> text
 * (26.1), and the missing sprite atlas on Forge 1.20.1 (flat-fill fallback,
 * same look as the original 1.20.1 branch).
 */
public final class Gfx {
    //? if >=26.1 {
    private final GuiGraphicsExtractor g;
    public Gfx(GuiGraphicsExtractor g) { this.g = g; }
    //?} else {
    /*private final GuiGraphics g;
    public Gfx(GuiGraphics g) { this.g = g; }
    *///?}

    public void sprite(Identifier sprite, int x, int y, int w, int h) {
        //? if forge {
        /*fillSprite(sprite, x, y, w, h);
        *///?} elif <1.21.8 {
        /*g.blitSprite(sprite, x, y, w, h);
        *///?} else {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, w, h);
        //?}
    }

    public void text(Font font, String s, int x, int y, int color, boolean shadow) {
        //? if >=26.1 {
        g.text(font, s, x, y, color, shadow);
        //?} else
        /*g.drawString(font, s, x, y, color, shadow);*/
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y2, color);
    }

    //? if forge {
    /*// Forge 1.20.1 predates the GUI sprite atlas — approximate every sprite
    // with flat fills (colors match the original mc/1.20.1 branch).
    private void fillSprite(Identifier sprite, int x, int y, int w, int h) {
        String p = sprite.getPath();
        if (p.contains("slot")) {
            g.fill(x, y, x + w, y + h, 0xFF8B8B8B);
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFD8D8D8);
        }
        else if (p.contains("tab")) {
            boolean sel = p.contains("selected");
            boolean hover = p.contains("highlighted");
            g.fill(x, y, x + w, y + h, sel ? 0xFFC6C6C6 : (hover ? 0xFFBBBBBB : 0xFFB0B0B0));
        }
        else if (p.contains("scroller_background"))
            g.fill(x, y, x + w, y + h, 0xFF666666);
        else if (p.contains("scroller"))
            g.fill(x, y, x + w, y + h, 0xFF888888);
        else // panel background
            g.fill(x, y, x + w, y + h, 0xFFC6C6C6);
    }
    *///?}
}
