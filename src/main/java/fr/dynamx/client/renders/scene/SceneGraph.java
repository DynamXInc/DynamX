package fr.dynamx.client.renders.scene;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.common.entities.ModularPhysicsEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.utils.client.ClientDynamXUtils;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.util.vector.Quaternion;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * A scene graph is a tree of {@link SceneGraph.Node} that can be rendered by a {@link fr.dynamx.client.renders.RenderPhysicsEntity}. <br>
 * Each node match a {@link fr.dynamx.api.contentpack.object.part.IDrawablePart} that will be rendered at the node position. <br>
 * Each node can have children nodes that will be rendered after the node itself, with the same transformations.
 *
 * @param <T> The type of the entity that is rendered
 * @param <A> The type of the pack info (the owner of the scene graph)
 */
public interface SceneGraph<T extends PhysicsEntity<?>, A extends IPhysicsPackInfo> {
    //TODO PUT DEBUG RENDERING HERE !

    /**
     * Renders this subtree of the scene graph
     *
     * @param entity   The entity that is rendered, can be null if we are rendering a static scene graph (like in the inventory)
     * @param context  The render context
     * @param packInfo The pack info of the entity (the owner of the scene graph)
     */
    void render(@Nullable T entity, EntityRenderContext context, A packInfo);

    /**
     * A type of root node, corresponding to an entity
     *
     * @param <T> The type of the entity that is rendered
     * @param <A> The type of the pack info (the owner of the scene graph)
     */
    @RequiredArgsConstructor
    class EntityNode<T extends PhysicsEntity<?>, A extends IPhysicsPackInfo> implements SceneGraph<T, A> {
        /**
         * The children that are linked to the entity (ie that will be rendered with the entity transformations)
         */
        private final List<SceneGraph<T, A>> linkedChildren;
        /**
         * The children that are not linked to the entity (ie that will be rendered with the world transformations)
         */
        private final List<SceneGraph<T, A>> unlinkedChildren;

        /* TODO EVENTS
        if (!MinecraftForge.EVENT_BUS.post(new Render(VehicleEntityEvent.Render.Type.PARTS, this,carEntity, PhysicsEntityEvent.Phase.PRE, partialTicks, null))) {
        }
        MinecraftForge.EVENT_BUS.post(new Render(VehicleEntityEvent.Render.Type.PARTS, this,carEntity, PhysicsEntityEvent.Phase.POST, partialTicks, null));
        */

        @Override
        public void render(@Nullable T entity, EntityRenderContext context, A packInfo) {
            GlStateManager.pushMatrix();
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
        }
    }

    /**
     * A basic node of the scene graph, with a position, a rotation and a scale, that can have linked children nodes. <br>
     * The children will be rendered with the same transformations as the node.
     *
     * @param <T> The type of the entity that is rendered
     * @param <A> The type of the pack info (the owner of the scene graph)
     */
    @RequiredArgsConstructor
    abstract class Node<T extends ModularPhysicsEntity<?>, A extends IPhysicsPackInfo> implements SceneGraph<T, A> {
        /**
         * The translation of the node, relative to the previous node
         */
        @Nullable
        protected final Vector3f translation;
        /**
         * The rotation of the node, relative to the previous node
         */
        @Nullable
        protected final Quaternion rotation;
        /**
         * The scale of the node, absolute
         */
        @Nonnull
        protected final Vector3f scale;
        /**
         * The children of this node
         */
        @Nullable
        protected final List<SceneGraph<T, A>> linkedChildren;

        /**
         * Applies the transformations of this node
         */
        protected void transform() {
            if (translation != null)
                GlStateManager.translate(translation.x, translation.y, translation.z);
            if (rotation != null)
                GlStateManager.rotate(rotation);
            GlStateManager.scale(scale.x, scale.y, scale.z);
        }

        /**
         * Renders the children of this node (if any). <br>
         * This doesn't render the node itself, only the children. This should be called after the node transformations.
         *
         * @param entity   The entity that is rendered, can be null if we are rendering a static scene graph (like in the inventory)
         * @param context  The render context
         * @param packInfo The pack info of the entity (the owner of the scene graph)
         */
        protected void renderChildren(@Nullable T entity, EntityRenderContext context, A packInfo) {
            if (linkedChildren != null) {
                GlStateManager.scale(1 / scale.x, 1 / scale.y, 1 / scale.z);
                linkedChildren.forEach(c -> c.render(entity, context, packInfo));
            }
        }
    }
}
