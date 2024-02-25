package fr.dynamx.api.events;

import lombok.Getter;
import net.minecraftforge.fml.common.eventhandler.Event;

public class ContentPackSystemEvent extends Event {
    public static class Load extends ContentPackSystemEvent {
        @Getter
        private final EventPhase eventPhase;

        public Load(EventPhase phase) {
            this.eventPhase = phase;
        }
    }
}
