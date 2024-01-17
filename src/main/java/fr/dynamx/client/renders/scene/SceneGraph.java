package fr.dynamx.client.renders.scene;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.common.entities.IDynamXObject;
import fr.dynamx.utils.client.ClientDynamXUtils;
import fr.dynamx.utils.optimization.GlQuaternionPool;
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
public interface SceneGraph<T extends IDynamXObject, A extends IModelPackObject> {
    /**
     * Renders this subtree of the scene graph.
     *
     * @param entity   The entity that is rendered, can be null if we are rendering a static scene graph (like in the inventory)
     * @param context  The render context
     * @param packInfo The pack info of the entity (the owner of the scene graph)
     */
    void render(@Nullable T entity, EntityRenderContext context, A packInfo);

    /**
     * Renders the debug of this subtree of the scene graph. <br>
     * <strong>Note:</strong> unlike {@link SceneGraph#render(IDynamXObject, EntityRenderContext, IModelPackObject)}, the transformations of the parent nodes are not applied.
     *
     * @param entity   The entity that is rendered, can be null if we are rendering a static scene graph (like in the inventory)
     * @param context  The render context
     * @param packInfo The pack info of the entity (the owner of the scene graph)
     */
    void renderDebug(@Nullable T entity, EntityRenderContext context, A packInfo);

    /**
     * @return The children of the scene graph (attached parts)
     */
    List<SceneGraph<T, A>> getLinkedChildren();


    /**
     * A basic node of the scene graph, with a position, a rotation and a scale, that can have linked children nodes. <br>
     * The children will be rendered with the same transformations as the node.
     *
     * @param <T> The type of the entity that is rendered
     * @param <A> The type of the pack info (the owner of the scene graph)
     */
    abstract class Node<T extends IDynamXObject, A extends IModelPackObject> implements SceneGraph<T, A> {
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
         * Indicates if the position was read from the 3D model (true), or set by the user (false). <br>
         * Changes the behavior of the rendering in order to render the node at the right position with the right transformations.
         */
        protected final boolean isAutomaticPosition;
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
         * Creates a new node with the given manual transformations
         *
         * @param translation    The translation of the node, relative to the previous node, can be null
         * @param rotation       The rotation of the node, relative to the previous node, can be null
         * @param scale          The scale of the node, absolute, can't be null
         * @param linkedChildren The children of this node, can be null
         */
        public Node(@Nullable Vector3f translation, @Nullable Quaternion rotation, @Nonnull Vector3f scale, @Nullable List<SceneGraph<T, A>> linkedChildren) {
            this(translation, rotation, false, scale, linkedChildren);
        }

        /**
         * Creates a new node with the given transformations. You can tell if the position is automatic (read from the 3D model) or not.
         *
         * @param translation         The translation of the node, relative to the previous node, can be null
         * @param rotation            The rotation of the node, relative to the previous node, can be null
         * @param isAutomaticPosition Indicates if the position was read from the 3D model (true), or set by the user (false). <br>
         *                            Changes the behavior of the rendering in order to render the node at the right position with the right transformations.
         * @param scale               The scale of the node, absolute, can't be null
         * @param linkedChildren      The children of this node, can be null
         */
        public Node(@Nullable Vector3f translation, @Nullable Quaternion rotation, boolean isAutomaticPosition, @Nonnull Vector3f scale, @Nullable List<SceneGraph<T, A>> linkedChildren) {
            this.translation = translation;
            this.rotation = rotation;
            this.isAutomaticPosition = isAutomaticPosition;
            this.scale = scale;
            this.linkedChildren = linkedChildren;
        }

        /**
         * Applies the rotation point transformations of this node <br>
         * This should be called before applying "dynamic" transformations to the node (like the rotation of a wheel), and before transformToPartPos()
         */
        protected void transformToRotationPoint() {
            if (translation != null)
                GlStateManager.translate(translation.x, translation.y, translation.z);
            if (rotation != null)
                GlStateManager.rotate(rotation);
            GlStateManager.scale(scale.x, scale.y, scale.z);
        }

        /**
         * Applies the rendering transformations of this node <br>
         * If the position is automatic, this should be called after applying the "dynamic" transformations, and before rendering the node <br>
         * If the position isn't automatic, this doesn't need to be called.
         */
        protected void transformToPartPos() {
            if (isAutomaticPosition) {
                if (rotation != null)
                    GlStateManager.rotate(ClientDynamXUtils.inverseGlQuaternion(rotation, GlQuaternionPool.get()));
                if (translation != null)
                    GlStateManager.translate(-translation.x, -translation.y, -translation.z);
            }
        }

        /**
         * Applies the transformations of this node without the scale
         */
        protected void transformForDebug() {
            if (translation != null)
                GlStateManager.translate(translation.x, translation.y, translation.z);
            if (rotation != null)
                GlStateManager.rotate(rotation);
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

        @Override
        public void renderDebug(@Nullable T entity, EntityRenderContext context, A packInfo) {
            if (linkedChildren != null) {
                linkedChildren.forEach(c -> c.renderDebug(entity, context, packInfo));
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
    class SceneContainer<T extends IDynamXObject, A extends IModelPackObject> implements SceneGraph<T, A> {
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
        public void renderDebug(@Nullable T entity, EntityRenderContext context, A packInfo) {
            encapsulatedScene.renderDebug(entity, context, packInfo);
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
    interface SceneRenderListener<T extends IDynamXObject, A extends IModelPackObject> {
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
