package fr.dynamx.client.renders.scene;

import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.client.ClientDynamXUtils;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import fr.dynamx.utils.optimization.QuaternionPool;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import org.joml.Matrix4f;
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
public class EntityNode<T extends PhysicsEntity<?>, A extends IPhysicsPackInfo> implements SceneGraph<T, A> {
    /**
     * The children that are linked to the entity (ie that will be rendered with the entity transformations)
     */
    private final List<SceneGraph<T, A>> linkedChildren;
    /**
     * The children that are not linked to the entity (ie that will be rendered with the world transformations)
     */
    private final List<SceneGraph<T, A>> unlinkedChildren;

    private final Matrix4f transform = new Matrix4f();

    @Override
    public void render(@Nullable T entity, EntityRenderContext context, A packInfo) {
        QuaternionPool.openPool();
        GlQuaternionPool.openPool();
        transform.identity();
        if (entity != null) {
            transform.translate(context.getRenderPosition());
            transform.rotate(ClientDynamXUtils.computeInterpolatedJomlQuaternion(entity.prevRenderRotation, entity.renderRotation, context.getPartialTicks()));
        }

        // Scale to the config scale value
        transform.scale(DynamXUtils.toVector3f(packInfo.getScaleModifier()));
        //Render the model
        GlStateManager.pushMatrix();
        GlStateManager.multMatrix(ClientDynamXUtils.getMatrixBuffer(transform));
        context.getRender().renderMainModel(context.getModel(), entity, context.getTextureId(), context.isUseVanillaRender()); //TODO SIMPLIFY SCALE THINGS
        GlStateManager.popMatrix();
        transform.scale(1 / packInfo.getScaleModifier().x, 1 / packInfo.getScaleModifier().y, 1 / packInfo.getScaleModifier().z);
        //Render the linked children
        linkedChildren.forEach(c -> c.render(entity, context, packInfo));
        //Render the unlinked children, if this is a static scene graph (not in the world)
        if (entity == null)
            unlinkedChildren.forEach(c -> c.render(null, context, packInfo));
        //Render the unlinked children, if any
        if (entity != null && !unlinkedChildren.isEmpty()) {
            transform.translate((float) (context.getRenderPosition().x - (entity.prevPosX + (entity.posX - entity.prevPosX) * context.getPartialTicks())),
                    (float) (context.getRenderPosition().y - (entity.prevPosY + (entity.posY - entity.prevPosY) * context.getPartialTicks())),
                    (float) (context.getRenderPosition().z - (entity.prevPosZ + (entity.posZ - entity.prevPosZ) * context.getPartialTicks())));
            unlinkedChildren.forEach(c -> c.render(entity, context, packInfo));
        }
        GlQuaternionPool.closePool();
        QuaternionPool.closePool();
        DynamXRenderUtils.popGlAllAttribBits();
    }

    @Override
    public void renderDebug(@Nullable T entity, EntityRenderContext context, A packInfo) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(context.getRenderPosition().x, context.getRenderPosition().y, context.getRenderPosition().z);
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

    @Override
    public SceneGraph<T, A> getParent() {
        throw new UnsupportedOperationException("This node is a root node, it can't have a parent");
    }

    @Override
    public void setParent(SceneGraph<T, A> parent) {
        throw new UnsupportedOperationException("This node is a root node, it can't have a parent");
    }
}
