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

/**
 * An object that can be rendered as an item or in the world
 */
public interface IModelPackObject extends IModelTextureVariantsSupplier {
    /**
     * @return The model location of this object
     */
    ResourceLocation getModel();

    /**
     * @return True if this object has a model
     */
    default boolean isModelValid() {
        return getModel() != null && !getModel().getPath().toLowerCase().contains("disable_rendering");
    }

    /**
     * @return True if the model returned by {@link #getModel()} should be loaded by the {@link fr.dynamx.client.DynamXModelRegistry}
     */
    default boolean shouldRegisterModel() {
        return isModelValid() && !getModel().getPath().endsWith("json");
    }

    /**
     * @param viewType The item view type
     * @return The transforms info for the given view type
     */
    @SideOnly(Side.CLIENT)
    default ViewTransformsInfo getViewTransformsInfo(ItemCameraTransforms.TransformType viewType) {
        return null;
    }

    /**
     * @return The default scale applied to the item when getViewTransformsInfo returns null
     */
    @SideOnly(Side.CLIENT)
    default float getItemScale() {
        return 1;
    }

    /**
     * @return The 3D render location of this item
     */
    @SideOnly(Side.CLIENT)
    default Enum3DRenderLocation get3DItemRenderLocation() {
        return Enum3DRenderLocation.ALL;
    }

    /**
     * @return A text shown on the item in guis
     */
    @Nullable
    @SideOnly(Side.CLIENT)
    default String getItemIcon() {
        return null;
    }

    /**
     * Applies item transforms to the model, depending on the {@link net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType} <br>
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
                transform.rotate(-120 * DynamXMath.TO_RADIAN, 1, 0, 0);
                transform.rotate(180 * DynamXMath.TO_RADIAN, 0, 0, 1);
                break;
            case HEAD:
                break;
            case GUI:
                transform.translate(0.5f, 0.32f, 0);
                String tip = model.getOwner().getItemIcon();
                if (!StringUtils.isNullOrEmpty(tip)) {
                    GlStateManager.disableLighting();
                    Matrix4f textTransform = new Matrix4f(transform);
                    textTransform.translate(0, 0, 20);
                    textTransform.rotate(-150 * DynamXMath.TO_RADIAN, 1, 0, 0);
                    textTransform.scale(0.035f, 0.035f, 1);
                    GlStateManager.pushMatrix();
                    GlStateManager.multMatrix(ClientDynamXUtils.getMatrixBuffer(textTransform));
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
