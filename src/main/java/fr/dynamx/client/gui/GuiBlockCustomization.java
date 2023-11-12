package fr.dynamx.client.gui;

import com.jme3.math.Vector3f;
import fr.aym.acsguis.component.layout.GuiScaler;
import fr.aym.acsguis.component.panel.GuiFrame;
import fr.aym.acsguis.component.panel.GuiPanel;
import fr.aym.acsguis.component.textarea.GuiFloatField;
import fr.aym.acsguis.component.textarea.GuiLabel;
import fr.dynamx.client.renders.model.renderer.ObjModelRenderer;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.network.packets.MessageSyncBlockCustomization;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.client.DynamXRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

import java.util.Collections;
import java.util.List;

public class GuiBlockCustomization extends GuiFrame {
    public static final ResourceLocation STYLE = new ResourceLocation(DynamXConstants.ID, "css/block_custom.css");

    private static ObjModelRenderer model;
    private static TEDynamXBlock teBlock;
    private final GuiPanel preview;

    private final GuiFloatField translationX = new GuiFloatField(-10, 10);
    private final GuiFloatField translationY = new GuiFloatField(-10, 10);
    private final GuiFloatField translationZ = new GuiFloatField(-10, 10);
    private final GuiFloatField scaleX = new GuiFloatField(0.001f, 100);
    private final GuiFloatField scaleY = new GuiFloatField(0.001f, 100);
    private final GuiFloatField scaleZ = new GuiFloatField(0.001f, 100);
    private final GuiFloatField rotationX = new GuiFloatField(-360, 360);
    private final GuiFloatField rotationY = new GuiFloatField(-360, 360);
    private final GuiFloatField rotationZ = new GuiFloatField(-360, 360);

    private float angleX = -27, angleY = 18;
    private float scale = 20;
    private int scroll = 0;

    public GuiBlockCustomization(TEDynamXBlock te) {
        super(new GuiScaler.Identity());

        teBlock = te;
        model = DynamXContext.getObjModelRegistry().getModel(teBlock.getPackInfo().getModel());
        setCssClass("root");

        preview = new GuiPanel() {
            @Override
            public void drawForeground(int mouseX, int mouseY, float partialTicks) {
                super.drawForeground(mouseX, mouseY, partialTicks);
                Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
                int x = preview.getRenderMinX() + preview.getWidth();
                int y = preview.getRenderMinY() + preview.getHeight();
                drawModelOnScreen(x / 1.2f - 30, y / 1.2f - 30, mouseX, mouseY, model);

            }
        };
        preview.setCssClass("preview");
        GuiLabel rotationLabel = new GuiLabel("Rotation :");
        rotationLabel.setCssClass("rotation");
        GuiLabel scaleLabel = new GuiLabel("Scale :");
        scaleLabel.setCssClass("scale");
        GuiLabel translationLabel = new GuiLabel("Translation :");
        translationLabel.setCssClass("translation");

        translationX.setCssClass("translationX");
        translationX.setText(String.valueOf(teBlock.getRelativeTranslation().x));
        translationY.setCssClass("translationY");
        translationY.setText(String.valueOf(teBlock.getRelativeTranslation().y));
        translationZ.setCssClass("translationZ");
        translationZ.setText(String.valueOf(teBlock.getRelativeTranslation().z));

        scaleX.setCssClass("scaleX");
        scaleX.setText(teBlock.getRelativeScale().x != 0 ? String.valueOf(teBlock.getRelativeScale().x) : String.valueOf(1));
        scaleY.setCssClass("scaleY");
        scaleY.setText(teBlock.getRelativeScale().y != 0 ? String.valueOf(teBlock.getRelativeScale().y) : String.valueOf(1));
        scaleZ.setCssClass("scaleZ");
        scaleZ.setText(teBlock.getRelativeScale().z != 0 ? String.valueOf(teBlock.getRelativeScale().z) : String.valueOf(1));

        rotationX.setCssClass("rotationX");
        rotationX.setText(String.valueOf(teBlock.getRelativeRotation().x));
        rotationY.setCssClass("rotationY");
        rotationY.setText(String.valueOf(teBlock.getRelativeRotation().y));
        rotationZ.setCssClass("rotationZ");
        rotationZ.setText(String.valueOf(teBlock.getRelativeRotation().z));

        GuiLabel confirm = new GuiLabel("Confirm");
        confirm.setCssClass("confirm");

        confirm.addClickListener((mx, my, button) -> {
            Vector3f relativeTrans = new Vector3f(translationX.getValue(), translationY.getValue(), translationZ.getValue());
            Vector3f relativeScale = new Vector3f(scaleX.getValue(), scaleY.getValue(), scaleZ.getValue());
            Vector3f relativeRotation = new Vector3f(rotationX.getValue(), rotationY.getValue(), rotationZ.getValue());
            DynamXContext.getNetwork().sendToServer(new MessageSyncBlockCustomization(teBlock.getPos(), relativeTrans, relativeScale, relativeRotation));
            teBlock.setRelativeTranslation(relativeTrans);
            teBlock.setRelativeScale(relativeScale);
            teBlock.setRelativeRotation(relativeRotation);
            teBlock.markCollisionsDirty();
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
    }

    BlockRendererDispatcher blockRenderer = Minecraft.getMinecraft().getBlockRendererDispatcher();


    public void drawModelOnScreen(float posX, float posY, float mouseX, float mouseY, ObjModelRenderer model) {
        handleScaleAndRotation();

        GlStateManager.pushMatrix();

        GlStateManager.translate(posX, posY + 20, 200);

        GlStateManager.scale(100 + scale, 100 + scale, 100 + scale);

        renderGrid();

        //Wtf negative scale, don't remove it
        GlStateManager.scale(-1 / 5f, 1 / 5f, 1 / 5f);

        GlStateManager.rotate(180, 1, 0, 0);
        GlStateManager.rotate(angleX, 0, 1, 0);
        GlStateManager.rotate(-angleY, 1, 0, 0);

        GlStateManager.translate(-0.5, -1, 0.5);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        blockRenderer.renderBlockBrightness(teBlock.getWorld().getBlockState(teBlock.getPos().down()), 1.0F);

        renderModel();

        GlStateManager.popMatrix();

    }

    public void handleScaleAndRotation() {
        int i = Mouse.getEventDWheel() / 100;
        if (i != 0) {
            scroll += i;
            int maxScroll = 100;
            scroll = Math.max(Math.min(scroll, maxScroll), 0);
        }
        scale += scroll / 2f;
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
    }

    public void renderGrid() {

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();


        GlStateManager.rotate(angleX, 0, 1, 0);
        GlStateManager.rotate(-angleY, 1, 0, 0);

        GlStateManager.color(1, 1, 1, 1);

        GlStateManager.color(1, 0, 0, 1);
        GlStateManager.glLineWidth(5);
        DynamXRenderUtils.arrowMeshX.render();
        GlStateManager.color(0, 1, 0, 1);
        DynamXRenderUtils.arrowMeshY.render();
        GlStateManager.color(0, 0, 1, 1);
        DynamXRenderUtils.arrowMeshZ.render();

        GlStateManager.translate(-1f, 0f, -1f);

        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.glLineWidth(2);
        DynamXRenderUtils.gridMesh.render();


        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();

    }

    public void renderModel() {


        GlStateManager.translate(
                0.5 + teBlock.getPackInfo().getTranslation().x + translationX.getValue(),
                2.5D + teBlock.getPackInfo().getTranslation().y + translationY.getValue(),
                0.5 + teBlock.getPackInfo().getTranslation().z + translationZ.getValue());
        // Scale to the config scale value
        GlStateManager.scale(
                teBlock.getPackInfo().getScaleModifier().x * (scaleX.getValue() != 0 ? scaleX.getValue() : 1),
                teBlock.getPackInfo().getScaleModifier().y * (scaleY.getValue() != 0 ? scaleY.getValue() : 1),
                teBlock.getPackInfo().getScaleModifier().z * (scaleZ.getValue() != 0 ? scaleZ.getValue() : 1));
        // Correct rotation of the block
        GlStateManager.rotate(teBlock.getRotation() * 22.5f, 0.0F, -1.0F, 0.0F);
        float rotate = rotationX.getValue() + teBlock.getPackInfo().getRotation().x;
        if (rotate != 0)
            GlStateManager.rotate(rotate, 1, 0, 0);
        rotate = rotationY.getValue() + teBlock.getPackInfo().getRotation().y;
        if (rotate != 0)
            GlStateManager.rotate(rotate, 0, 1, 0);
        rotate = rotationZ.getValue() + teBlock.getPackInfo().getRotation().z;
        if (rotate != 0)
            GlStateManager.rotate(rotate, 0, 0, 1);

        model.renderModel();
    }

    @Override
    public List<ResourceLocation> getCssStyles() {
        return Collections.singletonList(STYLE);
    }

    @Override
    public boolean doesPauseGame() {
        return false;
    }

    @Override
    public boolean needsCssReload() {
        return false;
    }
}
