package fr.dynamx.client.renders.scene;

import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.common.entities.ModularPhysicsEntity;
import fr.dynamx.utils.errors.DynamXErrorManager;
import lombok.Getter;
import net.minecraft.client.renderer.GlStateManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder for {@link SceneGraph}s
 *
 * @param <T> The type of the entity that is rendered
 * @param <A> The type of the pack info (the owner of the scene graph)
 */
@Getter
public class SceneBuilder<T extends ModularPhysicsEntity<?>, A extends IPhysicsPackInfo> {
    /**
     * Map of node name to Node
     */
    private final Map<String, Node> nodes = new HashMap<>();

    /**
     * Adds a node to the scene graph at the given path. <br>
     * If the path isn't completed at runtime (ie intermediate nodes are missing), an error will be reported and the node will be rendered anyway.
     *
     * @param part The part that will be rendered at this node
     * @param path The path to the node (ie the name of the parents of the node followed by the name of the node)
     */
    public void addNode(IDrawablePart<T, A> part, String... path) {
        if (path.length == 0)
            throw new IllegalArgumentException("Path cannot be empty");
        Node node = null;
        for (String s : path) {
            s = s.toLowerCase();
            node = (node == null ? nodes : node.nodes).computeIfAbsent(s, k -> new Node());
        }
        node.leaf = part;
    }

    /**
     * Builds the scene graph into a linked and unlinked nodes list of {@link SceneGraph}s. <br>
     * The nodes should have been added before calling this method.
     *
     * @param obj          The pack object that owns the scene graph, used for error reporting
     * @param modelScale   The scale of the model
     * @param linkedNodes  The list of linked nodes to fill
     * @param unlikedNodes The list of unlinked nodes to fill
     * @see SceneBuilder#buildEntitySceneGraph(IPhysicsPackInfo, List, Vector3f)
     */
    public void build(A obj, Vector3f modelScale, List<SceneGraph<T, A>> linkedNodes, List<SceneGraph<T, A>> unlikedNodes) {
        nodes.forEach((k, v) -> {
            validateNode(obj, k, v);
            List<SceneGraph<T, A>> list = v.leaf.isLinkedToEntity() ? linkedNodes : unlikedNodes;
            if (v.nodes.isEmpty()) {
                list.add(v.leaf.createSceneGraph(modelScale, null));
            } else {
                list.add(v.leaf.createSceneGraph(modelScale, v.generateScene(obj, modelScale)));
            }
        });
    }

    /**
     * Builds the scene graph of an entity
     *
     * @param obj           The pack object that owns the scene graph, used for error reporting
     * @param drawableParts The list of parts that should be rendered
     * @param modelScale    The scale of the model
     * @return The root node of the scene graph
     */
    public SceneGraph.EntityNode<T, A> buildEntitySceneGraph(A obj, List<IDrawablePart<T, A>> drawableParts, Vector3f modelScale) {
        List<SceneGraph<T, A>> linkedNodes = new ArrayList<>();
        List<SceneGraph<T, A>> unlinkedNodes = new ArrayList<>();
        drawableParts.forEach(part -> part.addToSceneGraph(obj, this));
        build(obj, modelScale, linkedNodes, unlinkedNodes);
        return new SceneGraph.EntityNode<>(linkedNodes, unlinkedNodes);
    }

    /**
     * Validates a node, and if it is invalid (no leaf is present), creates a fake leaf to avoid crashes
     *
     * @param obj      The pack object that owns the scene graph, used for error reporting
     * @param nodeName The name of the node
     * @param node     The node to validate
     */
    private void validateNode(A obj, String nodeName, Node node) {
        if (node.leaf == null) {
            String child = node.nodes.keySet().stream().findFirst().orElse("<error: no child>");
            DynamXErrorManager.addPackError(obj.getPackName(), "missing_depends_on_node", ErrorLevel.HIGH, obj.getName(), "Part " + child + " depend on " + nodeName + " but it doesn't exist. Creating a fake one.");
            node.leaf = new IDrawablePart<T, A>() {
                @Override
                public SceneGraph<T, A> createSceneGraph(Vector3f modelScale, List<SceneGraph<T, A>> childGraph) {
                    return new SceneGraph.Node<T, A>(null, null, modelScale, childGraph) {
                        @Override
                        public void render(@Nullable T entity, EntityRenderContext context, A packInfo) {
                            GlStateManager.pushMatrix();
                            transform();
                            renderChildren(entity, context, packInfo);
                            GlStateManager.popMatrix();
                        }
                    };
                }

                @Override
                public String getNodeName() {
                    return nodeName;
                }

                @Override
                public String getObjectName() {
                    return nodeName;
                }
            };
        }
    }

    /**
     * A node of the scene graph, with a leaf and children nodes
     */
    @Getter
    private class Node {
        /**
         * The leaf of this node, ie the part that will be rendered
         */
        private IDrawablePart<T, A> leaf;
        /**
         * The children of this node
         */
        private final Map<String, Node> nodes = new HashMap<>();

        /**
         * Generates the scene graph of this node
         *
         * @param obj        The pack object that owns the scene graph, used for error reporting
         * @param modelScale The scale of the model
         * @return The scene graph of this node
         */
        private List<SceneGraph<T, A>> generateScene(A obj, Vector3f modelScale) {
            List<SceneGraph<T, A>> list = new ArrayList<>();
            nodes.forEach((k, v) -> {
                validateNode(obj, k, v);
                if (v.nodes.isEmpty()) {
                    list.add(v.leaf.createSceneGraph(modelScale, null));
                } else {
                    list.add(v.leaf.createSceneGraph(modelScale, v.generateScene(obj, modelScale)));
                }
            });
            return list;
        }
    }
}
