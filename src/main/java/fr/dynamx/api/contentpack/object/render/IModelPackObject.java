package fr.dynamx.api.contentpack.object.render;

import com.jme3.math.Vector3f;
import fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier;
import fr.dynamx.client.renders.model.ItemDxModel;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

public interface IModelPackObject extends IModelTextureVariantsSupplier {
    @SideOnly(Side.CLIENT)
    ResourceLocation getModel();

    default boolean isModelValid() {
        return getModel() != null && !getModel().getPath().toLowerCase().contains("disable_rendering");
    }

    default boolean shouldRegisterModel() {
        return isModelValid();
    }

    @SideOnly(Side.CLIENT)
    default float getItemScale() {
        return 1;
    }

    @SideOnly(Side.CLIENT)
    default Vector3f getItemRotate() {
        return Vector3fPool.get();
    }

    @SideOnly(Side.CLIENT)
    default Vector3f getItemTranslate() {
        return Vector3fPool.get();
    }

    @SideOnly(Side.CLIENT)
    default Enum3DRenderLocation get3DItemRenderLocation() {
        return Enum3DRenderLocation.ALL;
    }

    @SideOnly(Side.CLIENT)
    default void renderItem3D(ItemStack item, ItemCameraTransforms.TransformType renderType) {
        DynamXContext.getDxModelRegistry().getModel(getModel()).renderModel((byte) item.getMetadata());
    }

    @Nullable
    @SideOnly(Side.CLIENT)
    default String getItemIcon() {
        return null;
    }

    @SideOnly(Side.CLIENT)
    default void applyItemTransforms(ItemCameraTransforms.TransformType renderType, ItemStack stack, ItemDxModel model) {
        switch (renderType) {
            case NONE:
                break;
            case THIRD_PERSON_LEFT_HAND:
            case THIRD_PERSON_RIGHT_HAND:
                GlStateManager.translate(0.5, 0.3, 0.3);
                GlStateManager.rotate(-100, 1, 0, 0);
                GlStateManager.rotate(200, 0, 0, 1);
                break;
            case FIRST_PERSON_LEFT_HAND:
            case FIRST_PERSON_RIGHT_HAND:
                GlStateManager.translate(0.5, 0.3, -0.3);
                GlStateManager.rotate(-100, 1, 0, 0);
                GlStateManager.rotate(200, 0, 0, 1);
                break;
            case HEAD:
                break;
            case GUI:
                GlStateManager.translate(0.5, 0.32, 0);

                String tip = model.getOwner().getItemIcon();
                if (!StringUtils.isNullOrEmpty(tip)) {
                    GlStateManager.pushMatrix();
                    GlStateManager.disableLighting();
                    GlStateManager.translate(0, 0, 20);
                    GlStateManager.rotate(-150, 1, 0, 0);
                    GlStateManager.scale(0.035, 0.035f, 1);
                    Minecraft.getMinecraft().fontRenderer.drawString(tip, -13, -22, 0xFFFFFFFF);
                    GlStateManager.popMatrix();
                }
                GlStateManager.rotate(-150, 1, 0, 0);
                GlStateManager.rotate(200, 0, 0, 1);
                GlStateManager.rotate(-25, 0, 1, 0);
                break;
            case GROUND:
                GlStateManager.translate(0.5, 0.3, 0.5);
                break;
            case FIXED:
                GlStateManager.rotate(-100, 1, 0, 0);
                GlStateManager.rotate(200, 0, 0, 1);
                break;
        }
    }
}
