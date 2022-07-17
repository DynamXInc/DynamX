package fr.dynamx.client.gui;

import com.jme3.math.FastMath;
import fr.aym.acsguis.api.GuiAPIClientHelper;
import fr.aym.acsguis.component.layout.GuiScaler;
import fr.aym.acsguis.component.panel.GuiFrame;
import fr.dynamx.common.items.tools.ItemWrench;
import fr.dynamx.common.items.tools.WrenchMode;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GuiWrenchSelection extends GuiFrame {

    WrenchMode currentMode;

    public GuiWrenchSelection() {
        super(new GuiScaler.Identity());
        setPauseGame(false);

        addClickListener((mouseX1, mouseY1, mouseButton1) -> {
            WrenchMode wrenchMode = getModeWithMousePos(mouseX1, mouseY1);
            WrenchMode.sendWrenchMode(wrenchMode);
            Minecraft.getMinecraft().displayGuiScreen(null);
        });

        ItemStack itemStack = mc.player.getHeldItemMainhand();
        if (itemStack.getItem() instanceof ItemWrench) {
            currentMode = WrenchMode.getCurrentMode(itemStack);
        }
    }

    public static void drawDisk(float x, float y, float innerRadius, float outerRadius, Color color, float alpha) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();

        int i, l;
        int triangleAmount = 100;

        float twicePi = 2 * FastMath.PI;

        GlStateManager.color(color.getRed(), color.getGreen(), color.getBlue(), alpha);

        for (l = 0; l <= 1; l++) {
            float radius = l == 0 ? innerRadius : outerRadius;
            GlStateManager.glBegin(GL11.GL_TRIANGLE_FAN);

            GL11.glVertex2f(x, y); // center of circle
            for (i = 0; i <= triangleAmount; i++) {
                GL11.glVertex2f(
                        x + (radius * FastMath.sin(i * twicePi / triangleAmount)),
                        y + (radius * FastMath.cos(i * twicePi / triangleAmount)));
            }
            GlStateManager.glEnd();
        }

        GlStateManager.color(1, 1, 1, 1);

        for (i = 1; i < WrenchMode.getWrenchModes().size(); i++) {
            int j = (i - 1) * (triangleAmount + 1) / (WrenchMode.getWrenchModes().size() - 1);
            float x1 = FastMath.sin(j * twicePi / triangleAmount);
            float y1 = FastMath.cos(j * twicePi / triangleAmount);
            float textX = FastMath.sin((j + 10) * twicePi / triangleAmount);
            float textY = FastMath.cos((j + 10) * twicePi / triangleAmount);
            float r2 = innerRadius + 0.5f * (outerRadius - innerRadius);
            {
                GlStateManager.enableTexture2D();
                String modeInitials = WrenchMode.getWrenchModes().get(i).getInitials();
                mc.fontRenderer.drawString(modeInitials, (int) (x + textX * r2) - mc.fontRenderer.getStringWidth(modeInitials) / 2, (int) (y + textY * r2) - mc.fontRenderer.FONT_HEIGHT / 2, Color.WHITE.getRGB());
                GlStateManager.disableTexture2D();
                GlStateManager.color(1, 1, 1, 1);
            }
            GlStateManager.glBegin(GL11.GL_LINE_STRIP);
            for (l = 0; l <= 1; l++) {
                float r = innerRadius + l * (outerRadius - innerRadius);
                GL11.glVertex2f(x + r * x1, y + r * y1);
            }
            GlStateManager.glEnd();
        }

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    List<String> infos = new ArrayList<>();

    @Override
    public void drawBackground(int mouseX, int mouseY, float partialTicks) {
        super.drawBackground(mouseX, mouseY, partialTicks);
        drawDisk(getWidth() / 2f, getHeight() / 2f, 110, 60, Color.BLACK, 0.5f);

        infos.clear();
        WrenchMode wrenchMode = getModeWithMousePos(mouseX, mouseY);
        infos.add(wrenchMode.getLabel());
        infos.add(wrenchMode.getMessage());
        GuiAPIClientHelper.drawHoveringText(infos, mouseX, mouseY);

        if (currentMode != null) {
            GlStateManager.scale(0.7f, 0.7f, 0);
            mc.fontRenderer.drawString("Current Mode : " + currentMode.getLabel(), (int) (getWidth() / (2 * 0.7f) - (mc.fontRenderer.getStringWidth("Current Mode : " + currentMode.getLabel()) / 2)), (int) (getHeight() / (2 * 0.7f) - (mc.fontRenderer.FONT_HEIGHT / 2)), Color.WHITE.getRGB());
        }

    }

    private WrenchMode getModeWithMousePos(int mouseX, int mouseY) {
        //TODO GENERALIZE MODE FINDING
        int mx = mouseX - getWidth() / 2;
        int my = mouseY - getHeight() / 2;
        int maxModes = WrenchMode.getWrenchModes().size() - 1;
        double theta = FastMath.atan2(my, mx);
        theta += FastMath.PI / maxModes;
        theta += FastMath.PI;
        theta = theta % (FastMath.PI * 2);

        int mode = (int) (theta / (2 * FastMath.PI / maxModes));

        return WrenchMode.getWrenchModes().get(mode == 5 ? 6 : maxModes - mode - 1);
    }

    @Override
    public List<ResourceLocation> getCssStyles() {
        return Collections.singletonList(new ResourceLocation(DynamXConstants.ID, "css/wrench_selection.css"));
    }

    @Override
    public boolean needsCssReload() {
        return true;
    }
}
