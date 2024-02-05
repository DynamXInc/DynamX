package fr.dynamx.client.renders.scene;

import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.common.entities.IDynamXObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * A scene graph is a tree of {@link Node} that can be rendered by a {@link fr.dynamx.client.renders.RenderPhysicsEntity}. <br>
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

    Matrix4f getTransform();

    SceneGraph<T, A> getParent();
    void setParent(SceneGraph<T, A>  parent);


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

        @Override
        public Matrix4f getTransform() {
            return encapsulatedScene.getTransform();
        }

        @Override
        public SceneGraph<T, A> getParent() {
            return encapsulatedScene;
        }

        @Override
        public void setParent(SceneGraph<T, A> parent) {
            encapsulatedScene.setParent(parent);
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
