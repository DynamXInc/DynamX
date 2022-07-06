package fr.dynamx.api.events;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

public class DynamXRenderEvent extends Event
{
    @Cancelable
    public static class RenderModel extends Event
    {
        private final EventStage stage;
        private final ResourceLocation location;
        private final String groupName;

        public RenderModel(EventStage stage, ResourceLocation location, String groupName) {
            this.stage = stage;
            this.location = location;
            this.groupName = groupName;
        }

        public EventStage getStage() {
            return stage;
        }

        public ResourceLocation getLocation() {
            return location;
        }

        public String getGroupName() {
            return groupName;
        }
    }
}
