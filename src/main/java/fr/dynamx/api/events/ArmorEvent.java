package fr.dynamx.api.events;

import fr.dynamx.client.renders.model.ModelObjArmor;
import fr.dynamx.client.renders.model.ObjModelRenderer;
import fr.dynamx.client.renders.model.ObjObjectRenderer;
import lombok.Getter;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

public class ArmorEvent extends Event {
    @Cancelable
    public static class RenderArmorEvent extends ArmorEvent {
        @Getter
        private final ModelObjArmor armorModel;
        @Getter
        private final ObjModelRenderer objModel;
        @Getter
        private final ObjObjectRenderer objObjectRenderer;
        @Getter
        private final PhysicsEntityEvent.Phase eventPhase;
        @Getter
        private final Type renderType;

        public RenderArmorEvent(ModelObjArmor armorModel, ObjModelRenderer objModel, ObjObjectRenderer objObjectRenderer, PhysicsEntityEvent.Phase phase, Type renderType) {
            this.armorModel = armorModel;
            this.objModel = objModel;
            this.objObjectRenderer = objObjectRenderer;
            this.eventPhase = phase;
            this.renderType = renderType;
        }

        public enum Type {
            WITH_ROTATION, NORMAL
        }
    }
}
