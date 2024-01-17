package fr.dynamx.client.renders.scene;

import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.utils.client.ClientDynamXUtils;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import fr.dynamx.utils.optimization.QuaternionPool;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import org.lwjgl.util.vector.Quaternion;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A type of root node, corresponding to an entity
 *
 * @param <T> The type of the entity that is rendered
 * @param <A> The type of the pack info (the owner of the scene graph)
 */
@Getter
@RequiredArgsConstructor
public
class EntityNode<T extends PhysicsEntity<?>, A extends IPhysicsPackInfo> implements SceneGraph<T, A> {
    /**
     * The children that are linked to the entity (ie that will be rendered with the entity transformations)
     */
    private final List<SceneGraph<T, A>> linkedChildren;
    /**
     * The children that are not linked to the entity (ie that will be rendered with the world transformations)
     */
    private final List<SceneGraph<T, A>> unlinkedChildren;

    @Override
    public void render(@Nullable T entity, EntityRenderContext context, A packInfo) {
        GlStateManager.pushMatrix();
        QuaternionPool.openPool();
        GlQuaternionPool.openPool();
        if (entity != null) {
            GlStateManager.translate(context.getX(), context.getY(), context.getZ());
            GlStateManager.rotate(ClientDynamXUtils.computeInterpolatedGlQuaternion(entity.prevRenderRotation, entity.renderRotation, context.getPartialTicks()));
        }
        // Scale to the config scale value
        GlStateManager.scale(packInfo.getScaleModifier().x, packInfo.getScaleModifier().y, packInfo.getScaleModifier().z);
        //Render the model
        context.getRender().renderMainModel(context.getModel(), entity, context.getTextureId(), context.isUseVanillaRender()); //TODO SIMPLIFY SCALE THINGS
        GlStateManager.scale(1 / packInfo.getScaleModifier().x, 1 / packInfo.getScaleModifier().y, 1 / packInfo.getScaleModifier().z);
        //Render the linked children
        linkedChildren.forEach(c -> c.render(entity, context, packInfo));
        //Render the unlinked children, if this is a static scene graph (not in the world)
        if (entity == null)
            unlinkedChildren.forEach(c -> c.render(null, context, packInfo));
        GlStateManager.popMatrix();
        //Render the unlinked children, if any
        if (entity != null && !unlinkedChildren.isEmpty()) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(
                    (float) context.getX() - (entity.prevPosX + (entity.posX - entity.prevPosX) * context.getPartialTicks()),
                    (float) context.getY() - (entity.prevPosY + (entity.posY - entity.prevPosY) * context.getPartialTicks()),
                    (float) context.getZ() - (entity.prevPosZ + (entity.posZ - entity.prevPosZ) * context.getPartialTicks()));
            unlinkedChildren.forEach(c -> c.render(entity, context, packInfo));
            GlStateManager.popMatrix();
        }
        GlQuaternionPool.closePool();
        QuaternionPool.closePool();
        DynamXRenderUtils.popGlAllAttribBits();
    }

    @Override
    public void renderDebug(@Nullable T entity, EntityRenderContext context, A packInfo) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(context.getX(), context.getY(), context.getZ());
        GlStateManager.pushMatrix();
        {
            Quaternion rotQuat = ClientDynamXUtils.computeInterpolatedGlQuaternion(
                    entity.prevRenderRotation,
                    entity.renderRotation,
                    context.getPartialTicks());
            GlStateManager.rotate(rotQuat);
            if (DynamXDebugOptions.CENTER_OF_MASS.isActive()) {
                RenderGlobal.drawBoundingBox(-((PackPhysicsEntity<?, ?>) entity).getPackInfo().getCenterOfMass().x - 0.05f, -((PackPhysicsEntity<?, ?>) entity).getPackInfo().getCenterOfMass().y - 0.05f,
                        -((PackPhysicsEntity<?, ?>) entity).getPackInfo().getCenterOfMass().z - 0.05f, -((PackPhysicsEntity<?, ?>) entity).getPackInfo().getCenterOfMass().x + 0.05f,
                        -((PackPhysicsEntity<?, ?>) entity).getPackInfo().getCenterOfMass().y + 0.05f, -((PackPhysicsEntity<?, ?>) entity).getPackInfo().getCenterOfMass().z + 0.05f,
                        1, 0, 1, 1);
            }
            linkedChildren.forEach(c -> c.renderDebug(entity, context, packInfo));
        }
        GlStateManager.popMatrix();
        unlinkedChildren.forEach(c -> c.renderDebug(entity, context, packInfo));
        GlStateManager.popMatrix();
    }
}
