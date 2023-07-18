package fr.dynamx.api.events;

import fr.dynamx.client.renders.TESRDynamXBlock;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.blocks.TEDynamXBlock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.relauncher.Side;

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
