package fr.dynamx.client.gui;

import com.jme3.math.Vector3f;
import fr.aym.acsguis.api.ACsGuiFrame;
import fr.aym.acsguis.component.layout.GuiScaler;
import fr.aym.acsguis.component.panel.GuiFrame;
import fr.aym.acsguis.component.panel.GuiPanel;
import fr.aym.acsguis.component.textarea.GuiFloatField;
import fr.aym.acsguis.component.textarea.GuiLabel;
import fr.aym.acsguis.event.listeners.mouse.IMouseMoveListener;
import fr.aym.acsguis.utils.ComponentRenderContext;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.network.packets.MessageSyncBlockCustomization;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.client.DynamXRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Mouse;

import java.util.Collections;
import java.util.List;

@ACsGuiFrame
public class GuiBlockCustomization extends GuiFrame {
    @ACsGuiFrame.RegisteredStyleSheet
    public static final ResourceLocation STYLE = new ResourceLocation(DynamXConstants.ID, "css/block_custom.css");

    private final DxModelRenderer model;
    private final TEDynamXBlock teBlock;
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

    private float angleX = -27, angleY = -18;
    private float scale = 20;
    private float targetScale = 0;

    public GuiBlockCustomization(TEDynamXBlock te) {
        super(new GuiScaler.Identity());
        this.teBlock = te;
        this.model = DynamXContext.getDxModelRegistry().getModel(te.getPackInfo().getModel());
        setCssClass("root");
        setPauseGame(false);

        preview = new GuiPanel() {
            @Override
            public void drawForeground(int mouseX, int mouseY, float partialTicks, ComponentRenderContext renderContext) {
                super.drawForeground(mouseX, mouseY, partialTicks, renderContext);
                Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
                float x = preview.getRenderMinX() + preview.getWidth() / 2;
                float y = preview.getRenderMinY() + preview.getHeight() / 2 + 30;
                drawModelOnScreen(x, y, mouseX, mouseY, model);
            }
        };
        preview.setCssClass("preview");
        preview.addWheelListener(dWheel -> {
            float amount = dWheel / 80f;
            if (dWheel > 0) {
                amount = MathHelper.clamp(amount, 1.111111F, 10);
            } else {
                amount = MathHelper.clamp(amount, -10, -1.111111F);
                amount = -1 / amount;
            }
            targetScale = targetScale * amount;
            targetScale = Math.max(1, targetScale);
        });
        preview.addMoveListener(new IMouseMoveListener() {
            @Override
            public void onMouseMoved(int mouseX, int mouseY) {
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

            @Override
            public void onMouseHover(int mouseX, int mouseY) {
            }

            @Override
            public void onMouseUnhover(int mouseX, int mouseY) {
            }
        });
        add(preview);

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
            teBlock.markCollisionsDirty(true);
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
    }

    private final BlockRendererDispatcher blockRenderer = Minecraft.getMinecraft().getBlockRendererDispatcher();

    public void drawModelOnScreen(float posX, float posY, float mouseX, float mouseY, DxModelRenderer model) {
        updateScale();

        GlStateManager.pushMatrix();
        GlStateManager.translate(posX, posY, 200);
        GlStateManager.scale(100 + scale, 100 + scale, 100 + scale);

        renderGrid();

        //Wtf negative scale, don't remove it
        GlStateManager.scale(-1 / 5f, 1 / 5f, 1 / 5f);

        GlStateManager.rotate(180, 1, 0, 0);
        GlStateManager.rotate(-angleY, 1, 0, 0);
        GlStateManager.rotate(angleX, 0, 1, 0);

        GlStateManager.translate(-0.5, -1, 0.5);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        blockRenderer.renderBlockBrightness(teBlock.getWorld().getBlockState(teBlock.getPos().down()), 1.0F);
        renderModel();

        GlStateManager.popMatrix();
    }

    public void updateScale() {
        scale = scale + (targetScale - scale) / 20;
        scale = Math.max(1, scale);
    }

    public void renderGrid() {
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();

        GlStateManager.rotate(-angleY, 1, 0, 0);
        GlStateManager.rotate(angleX, 0, 1, 0);

        GlStateManager.translate(-1f, 0f, -1f);

        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.glLineWidth(2);
        DynamXRenderUtils.gridMesh.render();
        GlStateManager.translate(1f, 0f, 1f);

        GlStateManager.color(1, 0, 0, 1);
        GlStateManager.glLineWidth(8);
        DynamXRenderUtils.arrowMeshX.render();
        GlStateManager.color(0, 1, 0, 1);
        DynamXRenderUtils.arrowMeshY.render();
        GlStateManager.color(0, 0, 1, 1);
        DynamXRenderUtils.arrowMeshZ.render();

        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    public void renderModel() {
        Vector3f curTrans = teBlock.getRelativeTranslation();
        teBlock.setRelativeTranslation(new Vector3f(translationX.getValue(), translationY.getValue(), translationZ.getValue()));
        Vector3f curRot = teBlock.getRelativeRotation();
        teBlock.setRelativeRotation(new Vector3f(rotationX.getValue(), rotationY.getValue(), rotationZ.getValue()));
        Vector3f curScale = teBlock.getRelativeScale();
        teBlock.setRelativeScale(new Vector3f(scaleX.getValue() != 0 ? scaleX.getValue() : 1, scaleY.getValue() != 0 ? scaleY.getValue() : 1, scaleZ.getValue() != 0 ? scaleZ.getValue() : 1));
        TileEntityRendererDispatcher.instance.render(teBlock, 0, 1, 0, 1);
        teBlock.setRelativeTranslation(curTrans);
        teBlock.setRelativeRotation(curRot);
        teBlock.setRelativeScale(curScale);
    }

    @Override
    public List<ResourceLocation> getCssStyles() {
        return Collections.singletonList(STYLE);
    }
}
