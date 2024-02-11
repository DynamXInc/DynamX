package fr.dynamx.client.renders.scene.node;

import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.common.entities.ModularPhysicsEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
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

import java.util.List;

/**
 * A type of root node, corresponding to an entity
 *
 * @param <A> The type of the pack info (the owner of the scene graph)
 */
@Getter
@RequiredArgsConstructor
public class EntityNode<A extends IPhysicsPackInfo> extends AbstractItemNode<BaseRenderContext.EntityRenderContext, A> {
    private static final BaseRenderContext.EntityRenderContext context = new BaseRenderContext.EntityRenderContext(DynamXRenderUtils.getRenderBaseVehicle());

    /**
     * The children that are linked to the entity (ie that will be rendered with the entity transformations)
     */
    private final List<SceneNode<BaseRenderContext.EntityRenderContext, A>> linkedChildren;
    /**
     * The children that are not linked to the entity (ie that will be rendered with the world transformations)
     */
    private final List<SceneNode<BaseRenderContext.EntityRenderContext, A>> unlinkedChildren;

    /**
     * The transformation matrix of the node <br>
     * Stores the transformations of the node, and is used to render the node and its children <br>
     * Do not use GlStateManager to apply transformations, use this matrix instead
     */
    private final Matrix4f transform = new Matrix4f();

    @Override
    public void render(BaseRenderContext.EntityRenderContext context, A packInfo) {
        transform.identity();
        renderWithCurrentTransform(context, packInfo);
    }

    /**
     * Implementation of the render method, to allow the use of a modified transform matrix
     */
    protected void renderWithCurrentTransform(BaseRenderContext.EntityRenderContext context, A packInfo) {
        QuaternionPool.openPool();
        GlQuaternionPool.openPool();
        ModularPhysicsEntity<?> entity = context.getEntity();
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
        linkedChildren.forEach(c -> c.render(context, packInfo));
        //Render the unlinked children, if this is a static scene graph (not in the world)
        if (entity == null)
            unlinkedChildren.forEach(c -> c.render(context, packInfo));
        //Render the unlinked children, if any
        if (entity != null && !unlinkedChildren.isEmpty()) {
            transform.translate((float) (context.getRenderPosition().x - (entity.prevPosX + (entity.posX - entity.prevPosX) * context.getPartialTicks())),
                    (float) (context.getRenderPosition().y - (entity.prevPosY + (entity.posY - entity.prevPosY) * context.getPartialTicks())),
                    (float) (context.getRenderPosition().z - (entity.prevPosZ + (entity.posZ - entity.prevPosZ) * context.getPartialTicks())));
            unlinkedChildren.forEach(c -> c.render(context, packInfo));
        }
        GlQuaternionPool.closePool();
        QuaternionPool.closePool();
        DynamXRenderUtils.popGlAllAttribBits();
    }

    @Override
    public void renderDebug(BaseRenderContext.EntityRenderContext context, A packInfo) {
        ModularPhysicsEntity<?> entity = context.getEntity();
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
            linkedChildren.forEach(c -> c.renderDebug(context, packInfo));
        }
        GlStateManager.popMatrix();
        unlinkedChildren.forEach(c -> c.renderDebug(context, packInfo));
        GlStateManager.popMatrix();
    }

    @Override
    public SceneNode<BaseRenderContext.EntityRenderContext, A> getParent() {
        throw new UnsupportedOperationException("This node is a root node, it can't have a parent");
    }

    @Override
    public void setParent(SceneNode<BaseRenderContext.EntityRenderContext, A> parent) {
        throw new UnsupportedOperationException("This node is a root node, it can't have a parent");
    }

    @Override
    public void renderItemModel(BaseRenderContext.ItemRenderContext context, A packInfo, Matrix4f transform) {
        this.transform.set(transform);
        renderWithCurrentTransform((BaseRenderContext.EntityRenderContext) EntityNode.context.setRenderParams(0, 0, 0, context.getPartialTicks(), context.isUseVanillaRender()).setModelParams(context.getModel(), context.getTextureId()), packInfo);
    }
}
