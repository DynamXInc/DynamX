package fr.dynamx.client.renders.scene;

import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.events.DynamXEntityRenderEvents;
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
     * Adds a node at the root of the scene graph
     *
     * @param obj  The pack object that owns the scene graph, used for error reporting
     * @param part The part that will be rendered at this node
     */
    public void addNode(A obj, IDrawablePart<T, A> part) {
        addNode(obj, nodes, part, part.getNodeName().toLowerCase());
    }

    /**
     * Adds a node to the scene graph with the given parent node. <br>
     * If the parent node doesn't exist, the child node will be added to the root of the scene graph, then the scene graph will be reordered when the parent node is added
     * (ie you can add nodes in any order). <br>
     * If the parent node is never added, an error will be reported and the node will be rendered anyway.
     *
     * @param obj        The pack object that owns the scene graph, used for error reporting
     * @param part       The part that will be rendered at this node
     * @param parentNode The name of the parent node
     */
    public void addNode(A obj, IDrawablePart<T, A> part, String parentNode) {
        if (!searchAddNode(obj, nodes, part, parentNode.toLowerCase(), part.getNodeName().toLowerCase())) {
            Node tempNode = new Node(null);
            nodes.put(parentNode.toLowerCase(), tempNode);
            tempNode.nodes.put(part.getNodeName().toLowerCase(), new Node(part));
        }
    }

    /**
     * Checks if a node exists in the scene graph
     *
     * @param nodeName The name of the node
     * @return True if the node exists in the scene graph
     */
    public boolean hasNode(String nodeName) {
        return hasNode(nodes, nodeName.toLowerCase());
    }

    /**
     * Recursively tests if a node exists in a subtree of the scene graph
     *
     * @param nodes    The subtree to search
     * @param nodeName The name of the node to search
     * @return True if the node exists in the subtree
     */
    private boolean hasNode(Map<String, Node> nodes, String nodeName) {
        if (nodes.containsKey(nodeName)) {
            return true;
        }
        for (Node n : nodes.values()) {
            if (hasNode(n.nodes, nodeName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the path of a node in the scene graph
     *
     * @param nodeName The name of the node to search
     * @return The path of the node, or null if it doesn't exist in the scene graph
     */
    public String getNodePath(String nodeName) {
        return getNodePath(nodes, nodeName.toLowerCase());
    }

    /**
     * Recursively searches for a node in a subtree of the scene graph and returns its path
     *
     * @param nodes    The subtree to search
     * @param nodeName The name of the node to search
     * @return The path of the node, or null if it doesn't exist in the subtree
     */
    private String getNodePath(Map<String, Node> nodes, String nodeName) {
        if (nodes.containsKey(nodeName)) {
            return nodeName;
        }
        for (Map.Entry<String, Node> entry : nodes.entrySet()) {
            String path = getNodePath(entry.getValue().nodes, nodeName);
            if (path != null) {
                return entry.getKey() + "/" + path;
            }
        }
        return null;
    }

    /**
     * Removes a node from the scene graph
     *
     * @param nodeName The name of the node to remove
     * @return True if the node was removed
     */
    public boolean removeNode(String nodeName) {
        return removeNode(nodes, nodeName.toLowerCase());
    }

    /**
     * Recursively searches for a node in a subtree of the scene graph and removes it
     *
     * @param nodes    The subtree to search
     * @param nodeName The name of the node to remove
     * @return True if the node was removed
     */
    private boolean removeNode(Map<String, Node> nodes, String nodeName) {
        if (nodes.containsKey(nodeName)) {
            nodes.remove(nodeName);
            return true;
        }
        for (Node n : nodes.values()) {
            if (removeNode(n.nodes, nodeName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a node to a subtree of the scene graph, searching for duplicates
     *
     * @param obj      The pack object that owns the scene graph, used for error reporting
     * @param nodes    The subtree to add the node to
     * @param part     The part that will be rendered at this node
     * @param nodeName The name of the node
     */
    private void addNode(A obj, Map<String, Node> nodes, IDrawablePart<T, A> part, String nodeName) {
        if (!nodes.containsKey(nodeName)) {
            nodes.put(nodeName, new Node(part));
            return;
        }
        Node n = nodes.get(nodeName);
        if (n.leaf != null) {
            DynamXErrorManager.addPackError(obj.getPackName(), "duplicate_scene_node", ErrorLevel.HIGH, obj.getName(), "Node " + nodeName);
        } else {
            n.leaf = part;
        }
    }

    /**
     * Adds a child node to a node of the scene graph, searching for duplicates <br>
     * If the parent node doesn't exist, the method will return false.
     *
     * @param obj        The pack object that owns the scene graph, used for error reporting
     * @param nodes      The subtree where the parent node is being searched
     * @param part       The part that will be rendered at this node
     * @param parentNode The name of the parent node to add the child to
     * @param nodeName   The name of the node
     * @return True if the node was added
     */
    private boolean searchAddNode(A obj, Map<String, Node> nodes, IDrawablePart<T, A> part, String parentNode, String nodeName) {
        if (nodes.containsKey(parentNode)) {
            addNode(obj, nodes.get(parentNode).nodes, part, nodeName);
            return true;
        } else {
            for (Node n : nodes.values()) {
                if (searchAddNode(obj, n.nodes, part, parentNode, nodeName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Creates a scene graph (subtree) from a node
     *
     * @param obj        The pack object that owns the scene graph, used for error reporting
     * @param nodeName   The name of the node
     * @param node       The node to create the scene graph from
     * @param modelScale The scale of the model (from the pack info)
     * @return The scene graph of the node
     */
    private SceneGraph<T, A> createSceneGraph(A obj, String nodeName, Node node, Vector3f modelScale) {
        validateNode(obj, nodeName, node);
        List<SceneGraph<T, A>> childGraph = node.nodes.isEmpty() ? null : node.generateScene(obj, modelScale);
        DynamXEntityRenderEvents.CreatePartScene createPartSceneEvent = new DynamXEntityRenderEvents.CreatePartScene(obj, node.leaf, modelScale, (List) childGraph);
        return (SceneGraph<T, A>) createPartSceneEvent.getSceneGraphResult();
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
            List<SceneGraph<T, A>> list = v.leaf.isLinkedToEntity() ? linkedNodes : unlikedNodes;
            list.add(createSceneGraph(obj, k, v, modelScale));
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
                            transformToRotationPoint();
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

        private Node(IDrawablePart<T, A> leaf) {
            this.leaf = leaf;
        }

        /**
         * Generates the scene graph of this node
         *
         * @param obj        The pack object that owns the scene graph, used for error reporting
         * @param modelScale The scale of the model
         * @return The scene graph of this node
         */
        private List<SceneGraph<T, A>> generateScene(A obj, Vector3f modelScale) {
            List<SceneGraph<T, A>> list = new ArrayList<>();
            nodes.forEach((k, v) -> list.add(createSceneGraph(obj, k, v, modelScale)));
            return list;
        }
    }
}
