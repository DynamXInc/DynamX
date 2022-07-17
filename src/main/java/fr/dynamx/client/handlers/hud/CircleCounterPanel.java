package fr.dynamx.client.handlers.hud;

import fr.aym.acsguis.component.panel.GuiPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class CircleCounterPanel extends GuiPanel {
    protected final ResourceLocation texture;
    protected final boolean rightToLeft;
    protected final int width, height;
    protected final float scale;
    protected final float maxValue;
    protected float prevValue, value;

    public CircleCounterPanel(ResourceLocation texture, boolean rightToLeft, int width, int height, float scale, float maxValue) {
        this.texture = texture;
        this.rightToLeft = rightToLeft;
        this.width = width;
        this.height = height;
        this.scale = scale;
        this.maxValue = maxValue;
    }

    @Override
    public void drawBackground(int mouseX, int mouseY, float partialTicks) {
        super.drawBackground(mouseX, mouseY, partialTicks);

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

        GlStateManager.pushMatrix();
        GlStateManager.translate(getScreenX(), getScreenY(), 0);

        GlStateManager.scale(scale, scale, 1);

        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GL11.glColor4f(1, 1, 1, 1);

        float curRpm = prevValue + (value - prevValue) * partialTicks;
        float tierMaxRpm = maxValue / 3;

        float y = (curRpm * height) / tierMaxRpm;
        if (curRpm >= tierMaxRpm) {
            y = height;
        }

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_TEX);
        if (rightToLeft) {
            bufferbuilder.pos(width, height, 0).tex(1, 1).endVertex();
            bufferbuilder.pos(width, height - y, 0).tex(1, (height - y) / height).endVertex();
            bufferbuilder.pos(width / 2.0, height / 2.0, 0).tex(0.5, 0.5).endVertex();
        } else {
            bufferbuilder.pos(width / 2.0, height / 2.0, 0).tex(0.5, 0.5).endVertex();
            bufferbuilder.pos(0, height - y, 0).tex(0, (height - y) / height).endVertex();
            bufferbuilder.pos(0, height, 0).tex(0, 1).endVertex();
        }
        tessellator.draw();

        if (curRpm >= tierMaxRpm) {
            float x = ((curRpm - tierMaxRpm) * width) / tierMaxRpm;
            if (curRpm >= tierMaxRpm * 2) {
                x = width;
            }

            bufferbuilder.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_TEX);
            if (rightToLeft) {
                bufferbuilder.pos(width, 0, 0).tex(1, 0).endVertex();
                bufferbuilder.pos(width - x, 0, 0).tex((width - x) / width, 0).endVertex();
                bufferbuilder.pos(width / 2.0, height / 2.0, 0).tex(0.5, 0.5).endVertex();
            } else {
                bufferbuilder.pos(width / 2.0, height / 2.0, 0).tex(0.5, 0.5).endVertex();
                bufferbuilder.pos(x, 0, 0).tex(x / width, 0).endVertex();
                bufferbuilder.pos(0, 0, 0).tex(0, 0).endVertex();
            }
            tessellator.draw();
        }

        if (curRpm >= tierMaxRpm * 2) {
            y = ((curRpm - tierMaxRpm * 2) * 300) / tierMaxRpm;
            if (curRpm >= maxValue) {
                y = 300;
            }

            bufferbuilder.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_TEX);
            if (rightToLeft) {
                bufferbuilder.pos(0, 0, 0).tex(0, 0).endVertex();
                bufferbuilder.pos(0, y, 0).tex(0, y / height).endVertex();
                bufferbuilder.pos(width / 2.0, height / 2.0, 0).tex(0.5, 0.5).endVertex();
            } else {
                bufferbuilder.pos(width / 2.0, height / 2.0, 0).tex(0.5, 0.5).endVertex();
                bufferbuilder.pos(width, y, 0).tex(1, y / height).endVertex();
                bufferbuilder.pos(width, 0, 0).tex(1, 0).endVertex();
            }
            tessellator.draw();
        }

        GlStateManager.popMatrix();
    }
}
