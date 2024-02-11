package fr.dynamx.api.events;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.SceneBuilder;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.contentpack.type.objects.ItemObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.relauncher.Side;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import java.util.List;

@Getter
public class DynamXItemEvent extends Event {
    private final Side side;
    private final ItemStack stack;

    public DynamXItemEvent(Side side, ItemStack stack) {
        this.stack = stack;
        this.side = side;
    }

    /**
     * Fired when creating the {@link SceneNode} of an item ({@link fr.dynamx.common.contentpack.type.objects.ItemObject} <br>
     * This event can be used to override the scene graph of an item, or edit its drawable parts before creating the scene graph <br>
     * This event is fired before the {@link CreatePartScene} event
     */
    @Getter
    @RequiredArgsConstructor
    public static class BuildSceneGraph extends DynamXEntityRenderEvents {
        /**
         * The scene builder
         */
        private final SceneBuilder<BaseRenderContext.ItemRenderContext, ItemObject<?>> sceneBuilder;
        /**
         * The pack info that is being compiled into a scene graph
         */
        private final ItemObject<?> packInfo;
        /**
         * The drawable parts of the pack info
         */
        private final List<IDrawablePart<ItemStack, ItemObject<?>>> drawableParts;
        /**
         * The scene graph that will be used to render the pack info. Can be overridden. <br>
         * Null by default (will be created by the pack info - default behavior)
         */
        @Setter
        private SceneNode<BaseRenderContext.ItemRenderContext, ItemObject<?>> overrideSceneNode;

        /**
         * @return The scene graph that will be used to render the pack info. If overrideSceneGraph is null, it will be created by the pack info.
         */
        @Nonnull
        public SceneNode<BaseRenderContext.ItemRenderContext, ItemObject<?>> getSceneGraphResult() {
            if (overrideSceneNode == null) {
                overrideSceneNode = sceneBuilder.buildItemSceneGraph(packInfo, (List) drawableParts, Vector3f.UNIT_XYZ);
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
    public static class Render extends DynamXItemEvent {
        private final EventStage stage;
        private final ItemCameraTransforms.TransformType transformType;
        private final Matrix4f transform;

        public Render(ItemStack stack, EventStage stage, ItemCameraTransforms.TransformType transformType, Matrix4f transform) {
            super(Side.CLIENT, stack);
            this.stage = stage;
            this.transformType = transformType;
            this.transform = transform;
        }
    }
}
