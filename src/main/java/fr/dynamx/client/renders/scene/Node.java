package fr.dynamx.client.renders.scene;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.common.entities.IDynamXObject;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.client.ClientDynamXUtils;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.GlStateManager;
import org.joml.Matrix4f;
import org.lwjgl.util.vector.Quaternion;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * A basic node of the scene graph, with a position, a rotation and a scale, that can have linked children nodes. <br>
 * The children will be rendered with the same transformations as the node.
 *
 * @param <T> The type of the entity that is rendered
 * @param <A> The type of the pack info (the owner of the scene graph)
 */
public abstract class Node<T extends IDynamXObject, A extends IModelPackObject> implements SceneGraph<T, A> {
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

    @Getter
    protected final Matrix4f transform = new Matrix4f();

    @Setter
    @Getter
    protected SceneGraph<T, A> parent;

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
        transform.set(parent.getTransform());
        if (translation != null)
            transform.translate(translation.x, translation.y, translation.z);
        if (rotation != null)
            transform.rotate(DynamXUtils.toQuaternion(rotation));

        transform.scale(scale.x, scale.y, scale.z);
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
                GlStateManager.translate(-translation.x / scale.x, -translation.y / scale.y, -translation.z / scale.z);
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
