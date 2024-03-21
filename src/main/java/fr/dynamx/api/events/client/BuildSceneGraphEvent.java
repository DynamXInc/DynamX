package fr.dynamx.api.events.client;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.IRenderContext;
import fr.dynamx.client.renders.scene.SceneBuilder;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.contentpack.type.objects.ArmorObject;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.common.contentpack.type.objects.ItemObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraftforge.fml.common.eventhandler.Event;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.BiFunction;


@Getter
@RequiredArgsConstructor
public abstract class BuildSceneGraphEvent<C extends IRenderContext, A extends IModelPackObject> extends Event {
    /**
     * The scene builder
     */
    protected final SceneBuilder<C, A> sceneBuilder = new SceneBuilder<>();
    /**
     * The pack info that is being compiled into a scene graph
     */
    protected final A packInfo;
    /**
     * The drawable parts of the pack info
     */
    protected final List<IDrawablePart<A>> drawableParts;
    /**
     * The scale of the model (of the pack info)
     */
    protected final Vector3f modelScale;
    /**
     * The scene graph that will be used to render the pack info. Can be overridden. <br>
     * Null by default (will be created by the pack info - default behavior)
     */
    @Setter
    protected SceneNode<C, A> overrideSceneNode;

    /**
     * @return The scene graph that will be used to render the pack info.
     * If overrideSceneGraph is null, a default scene graph will be created.
     */
    @Nonnull
    public abstract SceneNode<C, A> getSceneGraphResult();

    /**
     * Adds an isolated scene node to the pack info <br>
     * An isolated scene node is a scene node that is not part of the pack info's drawable parts <br>
     * It can be used to render additional things that are not part of the pack info's drawable parts
     *
     * @param nodeName  The name of the scene node in the scene graph
     * @param sceneNode The scene node creator (takes the model scale and the children of the scene node being created) (see {@link IDrawablePart#createSceneGraph(Vector3f, List)})
     */
    public void addSceneNode(String nodeName, BiFunction<Vector3f, List<SceneNode<C, A>>, SceneNode<C, A>> sceneNode) {
        drawableParts.add(new IDrawablePart<A>() {
            @Override
            public SceneNode<IRenderContext, A> createSceneGraph(Vector3f modelScale, List<SceneNode<IRenderContext, A>> childGraph) {
                return sceneNode.apply(modelScale, (List) childGraph);
            }

            @Override
            public String getNodeName() {
                return nodeName;
            }

            @Override
            public String getObjectName() {
                return null;
            }
        });
    }

    /**
     * Fired when creating the {@link SceneNode} of a pack info ({@link fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo} or {@link fr.dynamx.common.contentpack.type.objects.PropObject} for eample) <br>
     * This event can be used to override the scene graph of a pack info, or edit its drawable parts before creating the scene graph <br>
     * This event is fired before the {@link CreatePartSceneEvent} event
     */
    public static class BuildEntityScene extends BuildSceneGraphEvent<BaseRenderContext.EntityRenderContext, IPhysicsPackInfo> {
        public BuildEntityScene(IPhysicsPackInfo packInfo, List<IDrawablePart<IPhysicsPackInfo>> iDrawableParts, Vector3f modelScale) {
            super(packInfo, iDrawableParts, modelScale);
        }

        @Override
        @Nonnull
        public SceneNode<BaseRenderContext.EntityRenderContext, IPhysicsPackInfo> getSceneGraphResult() {
            if (overrideSceneNode == null) {
                overrideSceneNode = sceneBuilder.buildEntitySceneGraph(packInfo, (List) drawableParts, modelScale);
            }
            return overrideSceneNode;
        }
    }

    /**
     * Fired when creating the {@link SceneNode} of a block ({@link fr.dynamx.common.contentpack.type.objects.BlockObject} <br>
     * This event can be used to override the scene graph of a block, or edit its drawable parts before creating the scene graph <br>
     * This event is fired before the {@link CreatePartSceneEvent} event
     */
    public static class BuildBlockScene extends BuildSceneGraphEvent<BaseRenderContext.BlockRenderContext, BlockObject<?>> {
        public BuildBlockScene(BlockObject<?> packInfo, List<IDrawablePart<BlockObject<?>>> drawableParts, Vector3f modelScale) {
            super(packInfo, drawableParts, modelScale);
        }

        @Override
        @Nonnull
        public SceneNode<BaseRenderContext.BlockRenderContext, BlockObject<?>> getSceneGraphResult() {
            if (overrideSceneNode == null) {
                overrideSceneNode = sceneBuilder.buildBlockSceneGraph(packInfo, (List) drawableParts, modelScale);
            }
            return overrideSceneNode;
        }
    }

    /**
     * Fired when creating the {@link SceneNode} of an armor ({@link fr.dynamx.common.contentpack.type.objects.ArmorObject} <br>
     * This event can be used to override the scene graph of an armor, or edit its drawable parts before creating the scene graph <br>
     * This event is fired before the {@link CreatePartSceneEvent} event
     */
    public static class BuildArmorScene extends BuildSceneGraphEvent<BaseRenderContext.ArmorRenderContext, ArmorObject<?>> {
        public BuildArmorScene(ArmorObject<?> packInfo, List<IDrawablePart<ArmorObject<?>>> drawableParts) {
            super(packInfo, drawableParts, new Vector3f(1, 1, 1));
        }

        @Override
        @Nonnull
        public SceneNode<BaseRenderContext.ArmorRenderContext, ArmorObject<?>> getSceneGraphResult() {
            if (overrideSceneNode == null) {
                overrideSceneNode = sceneBuilder.buildArmorSceneGraph(packInfo, (List) drawableParts, modelScale);
            }
            return overrideSceneNode;
        }
    }

    /**
     * Fired when creating the {@link SceneNode} of an item ({@link fr.dynamx.common.contentpack.type.objects.ItemObject} <br>
     * This event can be used to override the scene graph of an item, or edit its drawable parts before creating the scene graph <br>
     * This event is fired before the {@link CreatePartSceneEvent} event
     */
    public static class BuildItemScene extends BuildSceneGraphEvent<BaseRenderContext.ItemRenderContext, ItemObject<?>> {
        public BuildItemScene(ItemObject<?> packInfo, List<IDrawablePart<ItemObject<?>>> drawableParts) {
            super(packInfo, drawableParts, new Vector3f(1, 1, 1));
        }

        @Override
        @Nonnull
        public SceneNode<BaseRenderContext.ItemRenderContext, ItemObject<?>> getSceneGraphResult() {
            if (overrideSceneNode == null) {
                overrideSceneNode = sceneBuilder.buildItemSceneGraph(packInfo, (List) drawableParts, modelScale);
            }
            return overrideSceneNode;
        }
    }
}
