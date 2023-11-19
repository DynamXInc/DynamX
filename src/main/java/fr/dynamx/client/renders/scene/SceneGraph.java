package fr.dynamx.client.renders.scene;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.common.entities.ModularPhysicsEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.utils.client.ClientDynamXUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.util.vector.Quaternion;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
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
     * @return The children of the scene graph (attached parts)
     */
    List<SceneGraph<T, A>> getLinkedChildren();

    /**
     * A type of root node, corresponding to an entity
     *
     * @param <T> The type of the entity that is rendered
     * @param <A> The type of the pack info (the owner of the scene graph)
     */
    @Getter
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
        @Getter
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

    /**
     * A node encapsulating another scene graph, that can be used to listen and cancel the rendering of the encapsulated scene graph
     *
     * @param <T> The type of the entity that is rendered
     * @param <A> The type of the pack info (the owner of the scene graph)
     */
    @Getter
    @RequiredArgsConstructor
    class SceneContainer<T extends ModularPhysicsEntity<?>, A extends IPhysicsPackInfo> implements SceneGraph<T, A> {
        /**
         * The listener that will be called before and after the encapsulated scene graph is rendered
         */
        private final SceneRenderListener<T, A> listener;
        /**
         * The part that will be rendered (corresponding to the encapsulated scene graph)
         */
        private final IDrawablePart<T, A> part;
        /**
         * The encapsulated scene graph
         */
        private final SceneGraph<T, A> encapsulatedScene;

        @Override
        public void render(@Nullable T entity, EntityRenderContext context, A packInfo) {
            if (listener.beforeRender(encapsulatedScene, part, entity, context, packInfo)) {
                encapsulatedScene.render(entity, context, packInfo);
                listener.afterRender(encapsulatedScene, part, entity, context, packInfo);
            }
        }

        @Override
        public List<SceneGraph<T, A>> getLinkedChildren() {
            return Collections.singletonList(encapsulatedScene);
        }
    }

    /**
     * A scene listeners allows to listen and cancel when a scene graph is rendered
     *
     * @param <T> The type of the entity that is rendered
     * @param <A> The type of the pack info (the owner of the scene graph)
     */
    interface SceneRenderListener<T extends ModularPhysicsEntity<?>, A extends IPhysicsPackInfo> {
        /**
         * Called before the scene graph is rendered <br>
         * Return false to cancel the rendering
         *
         * @param renderedScene The scene graph that will be rendered (corresponding to the renderPart)
         * @param renderPart    The part that will be rendered
         * @param entity        The entity that is rendered, can be null if we are rendering a static scene graph (like in the inventory)
         * @param context       The render context
         * @param packInfo      The pack info of the entity (the owner of the scene graph)
         * @return True to render the scene graph, false to cancel the rendering
         */
        boolean beforeRender(SceneGraph<T, A> renderedScene, IDrawablePart<T, A> renderPart, @Nullable T entity, EntityRenderContext context, A packInfo);

        /**
         * Called after the scene graph is rendered (and after the children are rendered) <br>
         * Not called if the rendering was cancelled before
         *
         * @param renderedScene The scene graph that was rendered (corresponding to the renderPart)
         * @param renderPart    The part that was rendered
         * @param entity        The entity that is rendered, can be null if we are rendering a static scene graph (like in the inventory)
         * @param context       The render context
         * @param packInfo      The pack info of the entity (the owner of the scene graph)
         */
        void afterRender(SceneGraph<T, A> renderedScene, IDrawablePart<T, A> renderPart, @Nullable T entity, EntityRenderContext context, A packInfo);
    }
}
