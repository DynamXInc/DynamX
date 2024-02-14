package fr.dynamx.api.events;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.client.renders.TESRDynamXBlock;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.SceneBuilder;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nonnull;
import java.util.List;

@Getter
public class DynamXBlockEvent extends Event {
    private final Side side;
    private final DynamXBlock<?> block;
    private final World world;

    public DynamXBlockEvent(Side side, DynamXBlock<?> dynamXBlock, World world) {
        this.block = dynamXBlock;
        this.side = side;
        this.world = world;
    }

    @Setter
    @Getter
    public static class CreateTileEntity extends DynamXBlockEvent {
        private TEDynamXBlock tileEntity;

        public CreateTileEntity(Side side, DynamXBlock<?> dynamXBlock, World world, TEDynamXBlock tileEntity) {
            super(side, dynamXBlock, world);
            this.tileEntity = tileEntity;
        }
    }

      /**
     * Fired when creating the {@link SceneNode} of a block ({@link fr.dynamx.common.contentpack.type.objects.BlockObject} <br>
     * This event can be used to override the scene graph of a block, or edit its drawable parts before creating the scene graph <br>
     * This event is fired before the {@link CreatePartScene} event
     */
    @Getter
    @RequiredArgsConstructor
    public static class BuildSceneGraph extends DynamXEntityRenderEvents {
        /**
         * The scene builder
         */
        private final SceneBuilder<BaseRenderContext.BlockRenderContext, BlockObject<?>> sceneBuilder;
        /**
         * The pack info that is being compiled into a scene graph
         */
        private final BlockObject<?> packInfo;
        /**
         * The drawable parts of the pack info
         */
        private final List<IDrawablePart<TEDynamXBlock, BlockObject<?>>> drawableParts;
        /**
         * The scale of the model (of the pack info)
         */
        private final Vector3f modelScale;
        /**
         * The scene graph that will be used to render the pack info. Can be overridden. <br>
         * Null by default (will be created by the pack info - default behavior)
         */
        @Setter
        private SceneNode<BaseRenderContext.BlockRenderContext, BlockObject<?>> overrideSceneNode;

        /**
         * @return The scene graph that will be used to render the pack info. If overrideSceneGraph is null, it will be created by the pack info.
         */
        @Nonnull
        public SceneNode<BaseRenderContext.BlockRenderContext, BlockObject<?>> getSceneGraphResult() {
            if (overrideSceneNode == null) {
                overrideSceneNode = (SceneNode) sceneBuilder.buildBlockSceneGraph(packInfo, (List) drawableParts, modelScale);
            }
            return overrideSceneNode;
        }
    }

    @Getter
    @Cancelable
    public static class RenderTileEntity extends DynamXBlockEvent {
        private final TEDynamXBlock tileEntity;
        private final TESRDynamXBlock<?> renderer;
        private final org.joml.Vector3f renderPos;
        private final float partialTicks;
        private final int destroyStage;
        private final float alpha;
        private final EventStage stage;

        public RenderTileEntity(DynamXBlock<?> dynamXBlock, World world, TEDynamXBlock tileEntity, TESRDynamXBlock<?> renderer, org.joml.Vector3f renderPos, float partialTicks, int destroyStage, float alpha, EventStage stage) {
            super(Side.CLIENT, dynamXBlock, world);
            this.tileEntity = tileEntity;
            this.renderer = renderer;
            this.renderPos = renderPos;
            this.partialTicks = partialTicks;
            this.destroyStage = destroyStage;
            this.alpha = alpha;
            this.stage = stage;
        }
    }
}
