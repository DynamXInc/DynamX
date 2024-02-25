package fr.dynamx.api.events.client;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.client.renders.scene.node.SceneNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraftforge.fml.common.eventhandler.Event;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Fired when creating the {@link SceneNode} of a {@link IDrawablePart} <br>
 * This event can be used to override the scene graph of a part <br>
 * You can also add a {@link SceneNode.SceneRenderListener} to the scene graph, allowing you to listen and cancel the rendering of the part
 */
@Getter
@RequiredArgsConstructor
public class CreatePartSceneEvent extends Event {
    /**
     * The pack info containing the part
     */
    private final IModelPackObject packInfo;
    /**
     * The part that is being rendered
     */
    private final IDrawablePart<?> part;
    /**
     * The scale of the model (of the pack info)
     */
    private final Vector3f modelScale;
    /**
     * The children of the part <br>
     * Can be null if the node doesn't have any children
     */
    @Nullable
    private final List<SceneNode<?, ?>> childGraph;
    /**
     * The scene graph that will be used to render the part. Can be overridden. <br>
     * Null by default (will be created by the part - default behavior)
     */
    @Setter
    private SceneNode<?, ?> overrideSceneNode;

    /**
     * @return The scene graph that will be used to render the part. If overrideSceneGraph is null, it will be created by the part.
     */
    @Nonnull
    public SceneNode<?, ?> getSceneGraphResult() {
        if (overrideSceneNode == null) {
            overrideSceneNode = part.createSceneGraph(modelScale, (List) childGraph);
        }
        return overrideSceneNode;
    }

    /**
     * Adds a listener to the scene graph that will be used to render the part <br>
     * Multiple listeners can be added by addons, every listener will be called (except if one of them cancel the rendering), starting by the last added
     *
     * @param listener The listener to add
     */
    public void listenPartScene(SceneNode.SceneRenderListener<?, ?> listener) {
        overrideSceneNode = new SceneNode.SceneContainer(listener, part, getSceneGraphResult());
    }
}
