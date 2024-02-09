package fr.dynamx.api.events;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.client.renders.model.ModelObjArmor;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.model.renderer.ObjObjectRenderer;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.SceneBuilder;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.contentpack.type.objects.ArmorObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

import javax.annotation.Nonnull;
import java.util.List;

public class DynamXArmorEvent extends Event {
    /**
     * Fired when creating the {@link SceneNode} of an armor ({@link fr.dynamx.common.contentpack.type.objects.ArmorObject} <br>
     * This event can be used to override the scene graph of an armor, or edit its drawable parts before creating the scene graph <br>
     * This event is fired before the {@link CreatePartScene} event
     */
    @Getter
    @RequiredArgsConstructor
    public static class BuildSceneGraph extends DynamXEntityRenderEvents {
        /**
         * The scene builder
         */
        private final SceneBuilder<BaseRenderContext.ArmorRenderContext, ArmorObject<?>> sceneBuilder;
        /**
         * The pack info that is being compiled into a scene graph
         */
        private final ArmorObject<?> packInfo;
        /**
         * The drawable parts of the pack info
         */
        private final List<IDrawablePart<?, ArmorObject<?>>> drawableParts;
        /**
         * The scene graph that will be used to render the pack info. Can be overridden. <br>
         * Null by default (will be created by the pack info - default behavior)
         */
        @Setter
        private SceneNode<BaseRenderContext.ArmorRenderContext, ArmorObject<?>> overrideSceneNode;

        /**
         * @return The scene graph that will be used to render the pack info. If overrideSceneGraph is null, it will be created by the pack info.
         */
        @Nonnull
        public SceneNode<BaseRenderContext.ArmorRenderContext, ArmorObject<?>> getSceneGraphResult() {
            if (overrideSceneNode == null) {
                overrideSceneNode = sceneBuilder.buildArmorSceneGraph(packInfo, (List) drawableParts, Vector3f.UNIT_XYZ);
            }
            return overrideSceneNode;
        }
    }

    /**
     * @deprecated Will change in a future version
     */
    @Getter
    @Cancelable
    @Deprecated
    public static class Render extends DynamXArmorEvent {
        private final ModelObjArmor armorModel;
        private final DxModelRenderer objModel;
        private final ObjObjectRenderer objObjectRenderer;
        private final PhysicsEntityEvent.Phase eventPhase;
        private final Type renderType;

        public Render(ModelObjArmor armorModel, DxModelRenderer objModel, ObjObjectRenderer objObject, PhysicsEntityEvent.Phase phase, Type renderType) {
            this.armorModel = armorModel;
            this.objModel = objModel;
            this.objObjectRenderer = objObject;
            this.eventPhase = phase;
            this.renderType = renderType;
        }

        public enum Type {
            WITH_ROTATION, NORMAL
        }
    }
}
