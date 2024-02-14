package fr.dynamx.api.contentpack.object.part;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.client.renders.scene.IRenderContext;
import fr.dynamx.client.renders.scene.SceneBuilder;
import fr.dynamx.client.renders.scene.node.SceneNode;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * A part that can be rendered in the world with the 3D model of the entity
 *
 * @param <T> The entity type
 * @param <A> The type of the owner of this part
 */
public interface IDrawablePart<T, A extends IModelPackObject> {
    /**
     * Called to update textures of this part (egg for wheels) according to the new entity's metadata
     *
     * @param entity The entity
     */
    @SideOnly(Side.CLIENT)
    default void onTexturesChange(T entity) {
    }

    /**
     * Prevents the added parts from being rendered with the main obj model of the vehicle. <br>
     * The SceneGraph create in {@link IDrawablePart#createSceneGraph} be used to render the part.
     *
     * @return The parts to hide when rendering the main obj model
     */
    default String[] getRenderedParts() {
        String objectName = getObjectName();
        return objectName == null ? new String[0] : new String[]{objectName};
    }

    /**
     * Adds this part to the scene graph. <br>
     * Override this if you want to change the path (hierarchy) of the part in the scene graph. By default, the part is added to the root of the scene graph.
     *
     * @param packInfo     The pack info of the entity (owner of the part)
     * @param sceneBuilder The scene builder
     */
    @SideOnly(Side.CLIENT)
    default void addToSceneGraph(A packInfo, SceneBuilder<IRenderContext, A> sceneBuilder) {
        sceneBuilder.addNode(packInfo, this);
    }

    /**
     * @return Whether this part should be linked to the entity or not. If false, the part will be rendered at the same position as the entity, but will not be affected by the entity's rotation.
     */
    @SideOnly(Side.CLIENT)
    default boolean isLinkedToEntity() {
        return true;
    }

    /**
     * Creates the scene node of this part
     *
     * @param modelScale The scale of the model (usually the scaleModifier of the packInfo)
     * @param childGraph The child scene graph (parts that are linked to this part)
     * @return The scene node of this part
     */
    @SideOnly(Side.CLIENT)
    SceneNode<IRenderContext, A> createSceneGraph(Vector3f modelScale, List<SceneNode<IRenderContext, A>> childGraph);

    /**
     * @return The node name in the scene graph. Use this name in the scene path when you want to attach a part to this part. This can be the part name defined in the pack.
     */
    @SideOnly(Side.CLIENT)
    String getNodeName();

    /**
     * @return The name of the object in the 3D model. This is NOT the name of the part and this is NOT the node name.
     */
    String getObjectName();
}
