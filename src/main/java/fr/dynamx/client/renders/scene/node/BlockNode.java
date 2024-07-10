package fr.dynamx.client.renders.scene.node;

import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.model.renderer.GltfModelRenderer;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.contentpack.parts.PartStorage;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.client.ClientDynamXUtils;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/**
 * A type of root node, corresponding to a block
 *
 * @param <A> The type of the pack info (the owner of the scene graph)
 */
@Getter
@RequiredArgsConstructor
public class BlockNode<A extends BlockObject<?>> extends AbstractItemNode<BaseRenderContext.BlockRenderContext, A> {
    /**
     * The children that are linked to the entity (ie that will be rendered with the entity transformations)
     */
    private final List<SceneNode<BaseRenderContext.BlockRenderContext, A>> linkedChildren;

    /**
     * The transformation matrix of the node <br>
     * Stores the transformations of the node, and is used to render the node and its children <br>
     * Do not use GlStateManager to apply transformations, use this matrix instead
     */
    @Getter
    private final Matrix4f transform = new Matrix4f();

    @Override
    public void render(BaseRenderContext.BlockRenderContext context, A packInfo) {
        if (context.getTileEntity() != null && context.getTileEntity().getBlockType() instanceof DynamXBlock) { //the instanceof fixes a crash
            transform.identity();
            Vector3fPool.openPool();
            QuaternionPool.openPool();
            GlQuaternionPool.openPool();
            TEDynamXBlock te = context.getTileEntity();
            applyTransform(te, context.getRenderPosition());

            //Rendering the model
            DxModelRenderer model = DynamXContext.getDxModelRegistry().getModel(te.getPackInfo().getModel());
            if (model instanceof GltfModelRenderer) {
                te.getAnimator().update((GltfModelRenderer) model, context.getPartialTicks());

                te.getAnimator().setModelAnimations(((GltfModelRenderer) model).animations);
            }
            // Scale of the block object info scale modifier
            transform.scale(DynamXUtils.toVector3f(packInfo.getScaleModifier()));
            GlStateManager.pushMatrix();
            GlStateManager.multMatrix(ClientDynamXUtils.getMatrixBuffer(transform));
            model.renderDefaultParts(context.getTextureId(), context.isUseVanillaRender());
            GlStateManager.popMatrix();
            //Render the linked children
            transform.scale(1 / packInfo.getScaleModifier().x, 1 / packInfo.getScaleModifier().y, 1 / packInfo.getScaleModifier().z);
            linkedChildren.forEach(c -> c.render(context, packInfo));

            GlQuaternionPool.closePool();
            QuaternionPool.closePool();
            Vector3fPool.closePool();
            DynamXRenderUtils.popGlAllAttribBits();
        }
    }

    public void applyTransform(TEDynamXBlock te, Vector3f renderPos) {
        // Translate to block render pos and add the config translate value
        transform.translate((renderPos.x + 0.5f + te.getRelativeTranslation().x),
                (renderPos.y + 0.5f + te.getRelativeTranslation().y),
                (renderPos.z + 0.5f + te.getRelativeTranslation().z));
        // Rotate to the config rotation value
        transform.rotate(DynamXUtils.toQuaternion(te.getCollidableRotation()));
        // Translate of the block object info translation
        if (te.getRelativeScale().x > 0 && te.getRelativeScale().y > 0 && te.getRelativeScale().z > 0) {
            transform.translate(-0.5f, 0.5f, -0.5f);
            // Scale to the config scale value
            transform.scale((te.getRelativeScale().x != 0 ? te.getRelativeScale().x : 1),
                    (te.getRelativeScale().y != 0 ? te.getRelativeScale().y : 1),
                    (te.getRelativeScale().z != 0 ? te.getRelativeScale().z : 1));
            transform.translate(0.5f, 0.5f, 0.5f);
        } else {
            // Backward-compatibility: old blocks were having 0, 0, 0 as default scale
            transform.translate(0, 1, 0);
        }
        transform.translate(DynamXUtils.toVector3f(te.getPackInfo().getTranslation()));
    }

    @Override
    public void renderDebug(BaseRenderContext.BlockRenderContext context, A packInfo) {
        if (!(context instanceof BaseRenderContext.BlockRenderContext))
            return;
        TEDynamXBlock te = context.getTileEntity();
        if (te == null)
            return;
        Vector3fPool.openPool();
        QuaternionPool.openPool();
        GlQuaternionPool.openPool();
        GlStateManager.pushMatrix();
        applyTransform(te, context.getRenderPosition());
        if (DynamXDebugOptions.PLAYER_TO_OBJECT_COLLISION_DEBUG.isActive()) {
            QuaternionPool.openPool();
            GlQuaternionPool.openPool();
            GlStateManager.pushMatrix();
            GlStateManager.translate(-0.5D, -1.5D, -0.5D);
            for (IShapeInfo partShape : te.getUnrotatedCollisionBoxes()) {
                RenderGlobal.drawBoundingBox(
                        (partShape.getPosition().x - partShape.getSize().x),
                        (partShape.getPosition().y - partShape.getSize().y),
                        (partShape.getPosition().z - partShape.getSize().z),
                        (partShape.getPosition().x + partShape.getSize().x),
                        (partShape.getPosition().y + partShape.getSize().y),
                        (partShape.getPosition().z + partShape.getSize().z),
                        0, 1, 1, 1);
            }
            GlStateManager.popMatrix();
            GlQuaternionPool.closePool();
            QuaternionPool.closePool();
        }
        MutableBoundingBox box = new MutableBoundingBox();
        if (DynamXDebugOptions.SEATS_AND_STORAGE.isActive()) {
            QuaternionPool.openPool();
            GlQuaternionPool.openPool();
            GlStateManager.pushMatrix();
            GlStateManager.translate(0D, -1.5D, 0D);
            for (PartStorage storage : (List<PartStorage>) packInfo.getPartsByType(PartStorage.class)) {
                storage.getBox(box);
                box.offset(storage.getPosition());
                RenderGlobal.drawBoundingBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                        1, 0.7f, 0, 1);
            }
            GlStateManager.popMatrix();
            GlQuaternionPool.closePool();
            QuaternionPool.closePool();
        }
        linkedChildren.forEach(c -> c.renderDebug((BaseRenderContext.BlockRenderContext) context, packInfo));
        GlStateManager.popMatrix();
        GlQuaternionPool.closePool();
        QuaternionPool.closePool();
        Vector3fPool.closePool();
    }

    @Override
    public SceneNode<BaseRenderContext.BlockRenderContext, A> getParent() {
        throw new UnsupportedOperationException("This node is a root node, it can't have a parent");
    }

    @Override
    public void setParent(SceneNode<BaseRenderContext.BlockRenderContext, A> parent) {
        throw new UnsupportedOperationException("This node is a root node, it can't have a parent");
    }
}
