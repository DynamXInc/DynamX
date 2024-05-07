package fr.dynamx.api.events;

import fr.dynamx.client.renders.TESRDynamXBlock;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.relauncher.Side;

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

    @Getter
    @Cancelable
    public static class RenderTileEntity extends DynamXBlockEvent {
        private final BaseRenderContext.BlockRenderContext renderContext;
        private final SceneNode<BaseRenderContext.BlockRenderContext, BlockObject<?>> sceneNode;
        private final TESRDynamXBlock<?> renderer;
        private final int destroyStage;
        private final float alpha;
        private final EventPhase eventPhase;

        public RenderTileEntity(DynamXBlock<?> dynamXBlock, BaseRenderContext.BlockRenderContext renderContext, SceneNode<BaseRenderContext.BlockRenderContext, BlockObject<?>> sceneNode, TESRDynamXBlock<?> renderer, int destroyStage, float alpha, EventPhase eventPhase) {
            super(Side.CLIENT, dynamXBlock, renderContext.getTileEntity().getWorld());
            this.renderContext = renderContext;
            this.sceneNode = sceneNode;
            this.renderer = renderer;
            this.destroyStage = destroyStage;
            this.alpha = alpha;
            this.eventPhase = eventPhase;
        }
    }
}
