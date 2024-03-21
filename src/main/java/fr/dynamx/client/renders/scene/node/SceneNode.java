package fr.dynamx.client.renders.scene.node;

import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.client.renders.scene.IRenderContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.joml.Matrix4f;

import java.util.Collections;
import java.util.List;

/**
 * A scene node is the basic element of a scene graph. <br>
 * A scene graph is a tree of {@link SimpleNode} that can be rendered by a {@link fr.dynamx.client.renders.RenderPhysicsEntity}. <br>
 * Each node match a {@link fr.dynamx.api.contentpack.object.part.IDrawablePart} that will be rendered at the node position. <br>
 * Each node can have children nodes that will be rendered after the node itself, with the same transformations.
 *
 * @param <C> The type of the render context
 * @param <A> The type of the pack info (the owner of the scene graph)
 * @see SimpleNode
 */
public interface SceneNode<C extends IRenderContext, A extends IModelPackObject> {

    /**
     * Renders this subtree of the scene node.
     *
     * @param context  The render context
     * @param packInfo The pack info of the entity (the owner of the scene graph)
     */
    void render(C context, A packInfo);

    /**
     * Renders the debug of this subtree of the scene node. <br>
     * <strong>Note:</strong> unlike {@link SceneNode#render(IRenderContext, IModelPackObject)}, the transformations of the parent nodes are not applied.
     *
     * @param context  The render context
     * @param packInfo The pack info of the entity (the owner of the scene graph)
     */
    void renderDebug(C context, A packInfo);

    /**
     * @return The children of the scene node (attached parts)
     */
    List<SceneNode<C, A>> getLinkedChildren();

    /**
     * Nodes are rendered using the transformations stored in the transform matrix, NOT using open gl transformations methods. <br>
     * Each child node will be rendered with the transformations of the parent node.
     *
     * @return The transformation matrix of this node
     */
    Matrix4f getTransform();

    /**
     * @return The parent of this node
     * @throws UnsupportedOperationException If the node is a root node
     */
    SceneNode<C, A> getParent();

    /**
     * Sets the parent of this node
     *
     * @param parent The parent of this node
     */
    void setParent(SceneNode<C, A> parent);


    /**
     * A node encapsulating another scene node, that can be used to listen and cancel the rendering of the encapsulated scene node
     *
     * @param <C> The type of the render context
     * @param <A> The type of the pack info (the owner of the scene graph)
     */
    @Getter
    @RequiredArgsConstructor
    class SceneContainer<C extends IRenderContext, A extends IModelPackObject> implements SceneNode<C, A> {
        /**
         * The listener that will be called before and after the encapsulated scene node is rendered
         */
        private final SceneRenderListener<C, A> listener;
        /**
         * The part that will be rendered (corresponding to the encapsulated scene node)
         */
        private final IDrawablePart<A> part;
        /**
         * The encapsulated scene node
         */
        private final SceneNode<C, A> encapsulatedScene;

        @Override
        public void render(C context, A packInfo) {
            if (listener.beforeRender(encapsulatedScene, part, context, packInfo)) {
                encapsulatedScene.render(context, packInfo);
                listener.afterRender(encapsulatedScene, part, context, packInfo);
            }
        }

        @Override
        public void renderDebug(C context, A packInfo) {
            encapsulatedScene.renderDebug(context, packInfo);
        }

        @Override
        public List<SceneNode<C, A>> getLinkedChildren() {
            return Collections.singletonList(encapsulatedScene);
        }

        @Override
        public Matrix4f getTransform() {
            return encapsulatedScene.getTransform();
        }

        @Override
        public SceneNode<C, A> getParent() {
            return encapsulatedScene;
        }

        @Override
        public void setParent(SceneNode<C, A> parent) {
            encapsulatedScene.setParent(parent);
        }
    }

    /**
     * A scene listeners allows to listen and cancel when a scene node is rendered
     *
     * @param <C> The type of the render context
     * @param <A> The type of the pack info (the owner of the scene graph)
     */
    interface SceneRenderListener<C extends IRenderContext, A extends IModelPackObject> {
        /**
         * Called before the scene node is rendered <br>
         * Return false to cancel the rendering
         *
         * @param renderedScene The scene node that will be rendered (corresponding to the renderPart)
         * @param renderPart    The part that will be rendered
         * @param context       The render context
         * @param packInfo      The pack info of the entity (the owner of the scene node)
         * @return True to render the scene node, false to cancel the rendering
         */
        boolean beforeRender(SceneNode<C, A> renderedScene, IDrawablePart<A> renderPart, C context, A packInfo);

        /**
         * Called after the scene node is rendered (and after the children are rendered) <br>
         * Not called if the rendering was cancelled before
         *
         * @param renderedScene The scene node that was rendered (corresponding to the renderPart)
         * @param renderPart    The part that was rendered
         * @param context       The render context
         * @param packInfo      The pack info of the entity (the owner of the scene node)
         */
        void afterRender(SceneNode<C, A> renderedScene, IDrawablePart<A> renderPart, C context, A packInfo);
    }
}
