package fr.dynamx.client.renders.scene.node;

import fr.dynamx.api.contentpack.object.render.Enum3DRenderLocation;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.api.events.client.DynamXRenderItemEvent;
import fr.dynamx.client.renders.model.ItemDxModel;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.IRenderContext;
import fr.dynamx.common.contentpack.type.ViewTransformsInfo;
import fr.dynamx.utils.client.ClientDynamXUtils;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Matrix4f;

/**
 * A {@link SceneNode} that can be rendered as an item, with an {@link fr.dynamx.client.renders.scene.BaseRenderContext.ItemRenderContext} <br>
 * The item render method can be customized by overriding {@link #renderItemModel(BaseRenderContext.ItemRenderContext, IModelPackObject, Matrix4f)}
 *
 * @param <C> The "base" type of the render context (when the node isn't rendered as an item)
 * @param <A> The type of the pack info (the owner of the scene graph)
 */
public abstract class AbstractItemNode<C extends IRenderContext, A extends IModelPackObject> implements SceneNode<C, A> {
    /**
     * Renders this node as an item with an {@link fr.dynamx.client.renders.scene.BaseRenderContext.ItemRenderContext} <br>
     * You normally don't need to override this method, {@link #renderItemModel(BaseRenderContext.ItemRenderContext, IModelPackObject, Matrix4f)} is here for that
     *
     * @param context  The context of the render call
     * @param packInfo The pack info of the scene graph
     */
    public void renderAsItemNode(BaseRenderContext.ItemRenderContext context, A packInfo) {
        ItemStack stack = context.getStack();
        ItemDxModel model = context.getItemModel();
        ItemCameraTransforms.TransformType renderType = context.getRenderType();
        if (packInfo.get3DItemRenderLocation() == Enum3DRenderLocation.NONE || (renderType == ItemCameraTransforms.TransformType.GUI && packInfo.get3DItemRenderLocation() == Enum3DRenderLocation.WORLD)) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.5F, 0.5F, 0.5F);
            Minecraft.getMinecraft().getRenderItem().renderItem(stack, model.getGuiBaked());
            GlStateManager.popMatrix();
        } else {
            Matrix4f transform = getTransform();
            transform.identity();
            Vector3fPool.openPool();
            QuaternionPool.openPool();
            GlQuaternionPool.openPool();
            if (!MinecraftForge.EVENT_BUS.post(new DynamXRenderItemEvent(context, this, DynamXRenderItemEvent.EventStage.TRANSFORM))) {
                packInfo.applyItemTransforms(renderType, stack, model, transform);
                ViewTransformsInfo transformsInfo = packInfo.getViewTransformsInfo(renderType);
                if(transformsInfo != null) {
                    transform.mul(transformsInfo.getTransformMatrix());
                } else {
                    float scale = packInfo.getItemScale();
                    transform.scale(scale, scale, scale);
                }
            }
            if (!MinecraftForge.EVENT_BUS.post(new DynamXRenderItemEvent(context, this, DynamXRenderItemEvent.EventStage.RENDER))) {
                renderItemModel(context, packInfo, transform);
            }
            GlQuaternionPool.closePool();
            QuaternionPool.closePool();
            Vector3fPool.closePool();
            DynamXRenderUtils.popGlAllAttribBits();
        }
    }

    /**
     * Renders the item model with the given context, pack info and transformation matrix <br>
     * Fired by {@link #renderAsItemNode(BaseRenderContext.ItemRenderContext, IModelPackObject)} <br>
     * This method doesn't render the linked children by default, you have to do it manually <br>
     * <strong>Use the matrix to apply your transforms, NOT open gl</strong>
     *
     * @param context   The context of the render call
     * @param packInfo  The pack info of the scene graph
     * @param transform The transformation matrix
     */
    public void renderItemModel(BaseRenderContext.ItemRenderContext context, A packInfo, Matrix4f transform) {
        GlStateManager.pushMatrix();
        GlStateManager.multMatrix(ClientDynamXUtils.getMatrixBuffer(transform));
        context.getModel().renderModel(context.getTextureId(), context.getRenderType() == ItemCameraTransforms.TransformType.GUI);
        GlStateManager.popMatrix();
    }
}
