package fr.dynamx.api.events;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.client.renders.TESRDynamXBlock;
import fr.dynamx.client.renders.scene.SceneBuilder;
import fr.dynamx.client.renders.scene.SceneGraph;
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

public class DynamXBlockEvent extends Event {
    @Getter
    private final Side side;
    @Getter
    private final DynamXBlock<?> block;
    @Getter
    private final World world;

    public DynamXBlockEvent(Side side, DynamXBlock<?> dynamXBlock, World world) {
        this.block = dynamXBlock;
        this.side = side;
        this.world = world;
    }

    public static class CreateTileEntity extends DynamXBlockEvent {
        @Getter
        @Setter
        private TEDynamXBlock tileEntity;

        public CreateTileEntity(Side side, DynamXBlock<?> dynamXBlock, World world, TEDynamXBlock tileEntity) {
            super(side, dynamXBlock, world);
            this.tileEntity = tileEntity;
        }
    }

      /**
     * Fired when creating the {@link SceneGraph} of a block ({@link fr.dynamx.common.contentpack.type.objects.BlockObject} <br>
     * This event can be used to override the scene graph of a block, or edit its drawable parts before creating the scene graph <br>
     * This event is fired before the {@link CreatePartScene} event
     */
    @Getter
    @RequiredArgsConstructor
    public static class BuildSceneGraph extends DynamXEntityRenderEvents {
        /**
         * The scene builder
         */
        private final SceneBuilder<?, ?> sceneBuilder;
        /**
         * The pack info that is being compiled into a scene graph
         */
        private final BlockObject<?> packInfo;
        /**
         * The drawable parts of the pack info
         */
        private final List<IDrawablePart<?, ?>> drawableParts;
        /**
         * The scale of the model (of the pack info)
         */
        private final Vector3f modelScale;
        /**
         * The scene graph that will be used to render the pack info. Can be overridden. <br>
         * Null by default (will be created by the pack info - default behavior)
         */
        @Setter
        private SceneGraph<?, ?> overrideSceneGraph;

        /**
         * @return The scene graph that will be used to render the pack info. If overrideSceneGraph is null, it will be created by the pack info.
         */
        @Nonnull
        public SceneGraph<?, ?> getSceneGraphResult() {
            if (overrideSceneGraph == null) {
                overrideSceneGraph = ((SceneBuilder<?, IPhysicsPackInfo>) sceneBuilder).buildBlockSceneGraph(packInfo, (List) drawableParts, modelScale);
            }
            return overrideSceneGraph;
        }
    }

    @Cancelable
    public static class RenderTileEntity extends DynamXBlockEvent {
        @Getter
        private final TEDynamXBlock tileEntity;
        @Getter
        private final TESRDynamXBlock<?> renderer;
        @Getter
        private final double x;
        @Getter
        private final double y;
        @Getter
        private final double z;
        @Getter
        private final float partialTicks;
        @Getter
        private final int destroyStage;
        @Getter
        private final float alpha;
        @Getter
        private final EventStage stage;

        public RenderTileEntity(DynamXBlock<?> dynamXBlock, World world, TEDynamXBlock tileEntity, TESRDynamXBlock<?> renderer, double x, double y, double z, float partialTicks, int destroyStage, float alpha, EventStage stage) {
            super(Side.CLIENT, dynamXBlock, world);
            this.tileEntity = tileEntity;
            this.renderer = renderer;
            this.x = x;
            this.y = y;
            this.z = z;
            this.partialTicks = partialTicks;
            this.destroyStage = destroyStage;
            this.alpha = alpha;
            this.stage = stage;
        }
    }
}
