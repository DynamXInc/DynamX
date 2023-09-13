package fr.dynamx.api.events;

import lombok.Getter;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.relauncher.Side;

public class DynamXItemEvent extends Event {
    @Getter
    private final Side side;
    @Getter
    private final ItemStack stack;

    public DynamXItemEvent(Side side, ItemStack stack) {
        this.stack = stack;
        this.side = side;
    }

    @Cancelable
    public static class Render extends DynamXItemEvent {
        @Getter
        private final EventStage stage;
        @Getter
        private final ItemCameraTransforms.TransformType transformType;

        public Render(ItemStack stack, EventStage stage, ItemCameraTransforms.TransformType transformType) {
            super(Side.CLIENT, stack);
            this.stage = stage;
            this.transformType = transformType;
        }
    }
}
