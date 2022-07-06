package fr.dynamx.client.gui;

import com.jme3.math.Vector3f;
import fr.aym.acsguis.component.layout.GuiScaler;
import fr.aym.acsguis.component.panel.GuiFrame;
import fr.aym.acsguis.component.panel.GuiPanel;
import fr.aym.acsguis.component.textarea.GuiFloatField;
import fr.aym.acsguis.component.textarea.GuiLabel;
import fr.dynamx.client.renders.model.ObjModelClient;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.network.packets.MessageSyncBlockCustomization;
import fr.dynamx.common.obj.eximpl.TessellatorModelClient;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

import java.util.Collections;
import java.util.List;

public class GuiBlockCustomization extends GuiFrame {

    private static ObjModelClient model;
    private static TEDynamXBlock teBlock;
    GuiPanel preview = new GuiPanel();

    GuiFloatField translationX = new GuiFloatField(-100, 100);
    GuiFloatField translationY = new GuiFloatField(-100, 100);
    GuiFloatField translationZ = new GuiFloatField(-100, 100);
    GuiFloatField scaleX = new GuiFloatField(0, 100);
    GuiFloatField scaleY = new GuiFloatField(0, 100);
    GuiFloatField scaleZ = new GuiFloatField(0, 100);
    GuiFloatField rotationX = new GuiFloatField(-360, 360);
    GuiFloatField rotationY = new GuiFloatField(-360, 360);
    GuiFloatField rotationZ = new GuiFloatField(-360, 360);

    float angleX, angleY;
    int scroll = 0;
    int maxScroll = 100;

    public GuiBlockCustomization(TEDynamXBlock te) {
        super(new GuiScaler.Identity());
        teBlock = te;
        model = DynamXContext.getObjModelRegistry().getModel(te.getBlockObjectInfo().getModel());
        setCssClass("root");

        preview.setCssClass("preview");
        GuiLabel rotationLabel = new GuiLabel("Rotation :");
        rotationLabel.setCssClass("rotation");
        GuiLabel scaleLabel = new GuiLabel("Scale :");
        scaleLabel.setCssClass("scale");
        GuiLabel translationLabel = new GuiLabel("Translation :");
        translationLabel.setCssClass("translation");

        translationX.setCssClass("translationX");
        translationX.setText(String.valueOf(te.getRelativeTranslation().x));
        translationY.setCssClass("translationY");
        translationY.setText(String.valueOf(te.getRelativeTranslation().y));
        translationZ.setCssClass("translationZ");
        translationZ.setText(String.valueOf(te.getRelativeTranslation().z));

        scaleX.setCssClass("scaleX");
        scaleX.setText(te.getRelativeScale().x != 0 ? String.valueOf(te.getRelativeScale().x) : String.valueOf(1));
        scaleY.setCssClass("scaleY");
        scaleY.setText(te.getRelativeScale().y != 0 ? String.valueOf(te.getRelativeScale().y) : String.valueOf(1));
        scaleZ.setCssClass("scaleZ");
        scaleZ.setText(te.getRelativeScale().z != 0 ? String.valueOf(te.getRelativeScale().z) : String.valueOf(1));

        rotationX.setCssClass("rotationX");
        rotationX.setText(String.valueOf(te.getRelativeRotation().x));
        rotationY.setCssClass("rotationY");
        rotationY.setText(String.valueOf(te.getRelativeRotation().y));
        rotationZ.setCssClass("rotationZ");
        rotationZ.setText(String.valueOf(te.getRelativeRotation().z));

        GuiLabel confirm = new GuiLabel("Confirm");
        confirm.setCssClass("confirm");

        confirm.addClickListener((mx, my, button) -> {
            Vector3f relativeTrans = new Vector3f(translationX.getValue(), translationY.getValue(), translationZ.getValue());
            Vector3f relativeScale = new Vector3f(scaleX.getValue(), scaleY.getValue(), scaleZ.getValue());
            Vector3f relativeRotation = new Vector3f(rotationX.getValue(), rotationY.getValue(), rotationZ.getValue());
            DynamXContext.getNetwork().sendToServer(new MessageSyncBlockCustomization(te.getPos(), relativeTrans, relativeScale, relativeRotation));
            te.setRelativeTranslation(relativeTrans);
            te.setRelativeScale(relativeScale);
            te.setRelativeRotation(relativeRotation);
            te.markCollisionsDirty();
        });


        add(rotationLabel);
        add(scaleLabel);
        add(translationLabel);
        add(translationX);
        add(translationY);
        add(translationZ);
        add(rotationX);
        add(rotationY);
        add(rotationZ);
        add(scaleX);
        add(scaleY);
        add(scaleZ);
        add(confirm);
        add(preview);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void drawForeground(int mouseX, int mouseY, float partialTicks) {
        super.drawForeground(mouseX, mouseY, partialTicks);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        int x = preview.getRenderMinX() + preview.getWidth();
        int y = preview.getRenderMinY() + preview.getHeight();
        drawModelOnScreen(x / 1.2f - 30, y / 1.2f - 30, 20, mouseX, mouseY, (TessellatorModelClient) model);
    }

    public void drawModelOnScreen(float posX, float posY, float scale, float mouseX, float mouseY, TessellatorModelClient model) {
        unbindLayerBounds();

        BlockRendererDispatcher blockRenderer = Minecraft.getMinecraft().getBlockRendererDispatcher();
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();

        GlStateManager.translate(posX, posY, 50);
        int i = Mouse.getEventDWheel() / 100;
        if (i != 0) {
            scroll += i;
            scroll = Math.max(Math.min(scroll, maxScroll), 0);
        }
        scale += scroll / 2f;
        GlStateManager.scale(-scale, scale, scale);
        GlStateManager.rotate(180, 1, 0, 0);
        GlStateManager.rotate(45, 0, 1, 0);
        GlStateManager.rotate(angleX, 0, 1, 0);
        GlStateManager.rotate(-angleY, 1, 0, 0);

        if (Mouse.isButtonDown(0)) {
            if (mouseX > preview.getRenderMinX() && mouseX < preview.getRenderMaxX()) {
                if (mouseY > preview.getRenderMinY() && mouseY < preview.getRenderMaxY()) {
                    angleX += Mouse.getDX() / 2f;
                    angleY -= Mouse.getDY() / 2f;
                }
            }
        }
        if (angleX >= 360) angleX = 0;
        if (angleY <= -360) angleY = 0;
        GlStateManager.translate(-0.5, -1, 0.5);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1);
        blockRenderer.renderBlockBrightness(Blocks.GRASS.getDefaultState(), 1.0F);
        GlStateManager.translate(0.5, 1.5f, 0.5);

        GlStateManager.rotate(teBlock.getRotation() * 22.5f, 0, -1, 0);
        GlStateManager.translate(translationX.getValue(), translationY.getValue(), translationZ.getValue());
        GlStateManager.scale(scaleX.getValue(), scaleY.getValue(), scaleZ.getValue());
        GlStateManager.rotate(rotationX.getValue(), 1, 0, 0);
        GlStateManager.rotate(rotationY.getValue(), 0, 1, 0);
        GlStateManager.rotate(rotationZ.getValue(), 0, 0, 1);

        model.renderModel();
        GlStateManager.popMatrix();
    }

    @Override
    public List<ResourceLocation> getCssStyles() {
        return Collections.singletonList(new ResourceLocation(DynamXConstants.ID, "css/block_custom.css"));
    }

    @Override
    public boolean doesPauseGame() {
        return false;
    }

    @Override
    public boolean needsCssReload() {
        return true;
    }
}
