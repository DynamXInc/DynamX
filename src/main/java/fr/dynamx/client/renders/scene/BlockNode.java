package fr.dynamx.client.renders.scene;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.model.renderer.GltfModelRenderer;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.contentpack.parts.PartStorage;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.utils.DynamXUtils;
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

import javax.annotation.Nullable;
import java.util.List;

/**
 * A type of root node, corresponding to a block
 *
 * @param <T> The type of the entity that is rendered
 * @param <A> The type of the pack info (the owner of the scene graph)
 */
@Getter
@RequiredArgsConstructor
public
class BlockNode<T extends TEDynamXBlock, A extends BlockObject<?>> implements SceneGraph<T, A> {
    /**
     * The children that are linked to the entity (ie that will be rendered with the entity transformations)
     */
    private final List<SceneGraph<T, A>> linkedChildren;

    @Override
    public void render(@Nullable T te, EntityRenderContext context, A packInfo) {
        if (te != null && te.getBlockType() instanceof DynamXBlock) { //the instanceof fixes a crash
            Vector3fPool.openPool();
            QuaternionPool.openPool();
            GlQuaternionPool.openPool();
            GlStateManager.pushMatrix();
            applyTransform(te, context.getX(), context.getY(), context.getZ());

            //Rendering the model
            DxModelRenderer model = DynamXContext.getDxModelRegistry().getModel(te.getPackInfo().getModel());
            if (model instanceof GltfModelRenderer) {
                te.getAnimator().update((GltfModelRenderer) model, context.getPartialTicks());

                te.getAnimator().setModelAnimations(((GltfModelRenderer) model).animations);
            }
            // Scale of the block object info scale modifier
            GlStateManager.scale(packInfo.getScaleModifier().x, packInfo.getScaleModifier().y, packInfo.getScaleModifier().z);
            model.renderDefaultParts(context.getTextureId(), context.isUseVanillaRender());
            //Render the linked children
            GlStateManager.scale(1 / packInfo.getScaleModifier().x, 1 / packInfo.getScaleModifier().y, 1 / packInfo.getScaleModifier().z);
            linkedChildren.forEach(c -> c.render(te, context, packInfo));

            Vector3f pos = DynamXUtils.toVector3f(te.getPos())
                    .add(te.getPackInfo().getTranslation().add(te.getRelativeTranslation()))
                    .add(0.5f, 1.5f, 0.5f);
            Vector3f rot = te.getRelativeRotation()
                    .add(packInfo.getRotation())
                    .add(0, te.getRotation() * 22.5f, 0);
            DynamXRenderUtils.spawnParticles(packInfo, te.getWorld(), pos, rot);
            GlStateManager.popMatrix();
            GlQuaternionPool.closePool();
            QuaternionPool.closePool();
            Vector3fPool.closePool();
            DynamXRenderUtils.popGlAllAttribBits();
        }
    }

    public void applyTransform(TEDynamXBlock te, double x, double y, double z) {
        // Translate to block render pos and add the config translate value
        GlStateManager.translate(
                x + 0.5D + te.getRelativeTranslation().x,
                y + 1.5D + te.getRelativeTranslation().y,
                z + 0.5D + te.getRelativeTranslation().z);
        // Rotate to the config rotation value
        GlStateManager.rotate(GlQuaternionPool.get(te.getCollidableRotation()));
        // Translate of the block object info translation
        if (te.getRelativeScale().x > 0 && te.getRelativeScale().y > 0 && te.getRelativeScale().z > 0) {
            GlStateManager.translate(-0.5, -1.5, -0.5);
            // Scale to the config scale value
            GlStateManager.scale(
                    (te.getRelativeScale().x != 0 ? te.getRelativeScale().x : 1),
                    (te.getRelativeScale().y != 0 ? te.getRelativeScale().y : 1),
                    (te.getRelativeScale().z != 0 ? te.getRelativeScale().z : 1));
            DynamXRenderUtils.glTranslate(te.getPackInfo().getTranslation());
            GlStateManager.translate(0.5D, 1.5D, 0.5D);
        } else
            DynamXRenderUtils.glTranslate(te.getPackInfo().getTranslation());
    }

    @Override
    public void renderDebug(@Nullable T te, EntityRenderContext context, A packInfo) {
        if (te == null)
            return;
        Vector3fPool.openPool();
        QuaternionPool.openPool();
        GlQuaternionPool.openPool();
        GlStateManager.pushMatrix();
        applyTransform(te, context.getX(), context.getY(), context.getZ());
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
        linkedChildren.forEach(c -> c.renderDebug(te, context, packInfo));
        GlStateManager.popMatrix();
        GlQuaternionPool.closePool();
        QuaternionPool.closePool();
        Vector3fPool.closePool();
    }
}
