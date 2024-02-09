package fr.dynamx.api.contentpack.object.render;

import com.jme3.math.Vector3f;
import fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier;
import fr.dynamx.client.renders.model.ItemDxModel;
import fr.dynamx.client.renders.scene.node.AbstractItemNode;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.joml.Matrix4f;

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

    @Nullable
    @SideOnly(Side.CLIENT)
    default String getItemIcon() {
        return null;
    }

    /**
     * Applies the item transforms to the model <br>
     * <strong>Use the matrix to apply your transforms, NOT open gl</strong>
     *
     * @param renderType The render type (first person, third person, ..)
     * @param stack The stack that is being rendered
     * @param model The model of the item
     * @param transform The matrix to apply the transforms to
     */
    @SideOnly(Side.CLIENT)
    default void applyItemTransforms(ItemCameraTransforms.TransformType renderType, ItemStack stack, ItemDxModel model, Matrix4f transform) {
        switch (renderType) {
            case NONE:
                break;
            case THIRD_PERSON_LEFT_HAND:
            case THIRD_PERSON_RIGHT_HAND:
                transform.translate(0.5f, 0.3f, 0.3f);
                transform.rotate(-100 * DynamXMath.TO_RADIAN, 1, 0, 0);
                transform.rotate(200 * DynamXMath.TO_RADIAN, 0, 0, 1);
                break;
            case FIRST_PERSON_LEFT_HAND:
            case FIRST_PERSON_RIGHT_HAND:
                transform.translate(0.5f, 0.3f, -0.3f);
                transform.rotate(-100 * DynamXMath.TO_RADIAN, 1, 0, 0);
                transform.rotate(200 * DynamXMath.TO_RADIAN, 0, 0, 1);
                break;
            case HEAD:
                break;
            case GUI:
                transform.translate(0.5f, 0.32f, 0);

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
                transform.rotate(-150 * DynamXMath.TO_RADIAN, 1, 0, 0);
                transform.rotate(200 * DynamXMath.TO_RADIAN, 0, 0, 1);
                transform.rotate(-25 * DynamXMath.TO_RADIAN, 0, 1, 0);
                break;
            case GROUND:
                transform.translate(0.5f, 0.3f, 0.5f);
                break;
            case FIXED:
                transform.rotate(-100 * DynamXMath.TO_RADIAN, 1, 0, 0);
                transform.rotate(200 * DynamXMath.TO_RADIAN, 0, 0, 1);
                break;
        }
    }

    /**
     * @return The scene graph of this object <br>
     * <strong>Should implement {@link AbstractItemNode} if this object has an item</strong>
     */
    SceneNode<?, ?> getSceneGraph();
}
