package fr.dynamx.api.events;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.SceneBuilder;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.entities.PhysicsEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraftforge.fml.common.eventhandler.Event;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Events related to {@link SceneNode}s and DynamX entities rendering
 */
@Getter
public class DynamXEntityRenderEvents extends Event {

    /**
     * Fired when creating the {@link SceneNode} of a pack info ({@link fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo} or {@link fr.dynamx.common.contentpack.type.objects.PropObject} for eample) <br>
     * This event can be used to override the scene graph of a pack info, or edit its drawable parts before creating the scene graph <br>
     * This event is fired before the {@link CreatePartScene} event
     */
    @Getter
    @RequiredArgsConstructor
    public static class BuildSceneGraph extends DynamXEntityRenderEvents {
        /**
         * The scene builder
         */
        private final SceneBuilder<BaseRenderContext.EntityRenderContext, IPhysicsPackInfo> sceneBuilder;
        /**
         * The pack info that is being compiled into a scene graph
         */
        private final IPhysicsPackInfo packInfo;
        /**
         * The drawable parts of the pack info
         */
        private final List<IDrawablePart<? extends PhysicsEntity<?>, ? extends IPhysicsPackInfo>> drawableParts;
        /**
         * The scale of the model (of the pack info)
         */
        private final Vector3f modelScale;
        /**
         * The scene graph that will be used to render the pack info. Can be overridden. <br>
         * Null by default (will be created by the pack info - default behavior)
         */
        @Setter
        private SceneNode<BaseRenderContext.EntityRenderContext, ? extends IPhysicsPackInfo> overrideSceneNode;

        /**
         * @return The scene graph that will be used to render the pack info. If overrideSceneGraph is null, it will be created by the pack info.
         */
        @Nonnull
        public SceneNode<BaseRenderContext.EntityRenderContext, ? extends IPhysicsPackInfo> getSceneGraphResult() {
            if (overrideSceneNode == null) {
                overrideSceneNode = sceneBuilder.buildEntitySceneGraph(packInfo, (List) drawableParts, modelScale);
            }
            return overrideSceneNode;
        }
    }

    /**
     * Fired when creating the {@link SceneNode} of an {@link IDrawablePart} <br>
     * This event can be used to override the scene graph of a part <br>
     * You can also add a {@link SceneNode.SceneRenderListener} to the scene graph, allowing you to listen and cancel the rendering of the part
     */
    @Getter
    @RequiredArgsConstructor
    public static class CreatePartScene extends DynamXEntityRenderEvents {
        /**
         * The pack info containing the part
         */
        private final IModelPackObject packInfo;
        /**
         * The part that is being rendered
         */
        private final IDrawablePart<?, ?> part;
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

    /**
     * Fired when rendering a {@link PhysicsEntity} <br>
     * This event can be used to override the rendering of an entity, or to add custom rendering <br>
     * This event has different phases described by {@link Render#renderType}
     */
    @Getter
    public static class Render extends DynamXEntityRenderEvents {
        /**
         * The entity being rendered
         */
        @Nullable
        private final PhysicsEntity<?> entity;

        /**
         * The render context
         */
        private final BaseRenderContext.EntityRenderContext context;

        /**
         * The render type
         *
         * @see Type
         */
        private final Type renderType;

        /**
         * The render pass <br>
         * 0 for solid objects <br>
         * 1 for translucent objects <br>
         * Some types of render event are fired for both
         */
        private final int renderPass;

        public Render(PhysicsEntity<?> entity, BaseRenderContext.EntityRenderContext context, Type renderType, int renderPass) {
            this.entity = entity;
            this.context = context;
            this.renderType = renderType;
            this.renderPass = renderPass;
        }

        /**
         * The render type
         *
         * @see Type#ENTITY
         * @see Type#PARTICLES
         * @see Type#DEBUG
         * @see Type#POST
         */
        public enum Type {
            /**
             * Fired before rendering the entity. Cancellable.
             */
            ENTITY,
            /**
             * Fired before spawning particles. Cancellable.
             */
            PARTICLES,
            /**
             * Fired before rendering debug. Cancellable.
             */
            DEBUG,
            /**
             * Fired after rendering the entity. Not cancellable.
             */
            POST
        }
    }
}
