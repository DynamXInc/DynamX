package fr.dynamx.api.events;

import fr.dynamx.client.renders.model.ModelObjArmor;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.model.renderer.ObjModelRenderer;
import fr.dynamx.client.renders.model.renderer.ObjObjectRenderer;
import lombok.Getter;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

public class ArmorEvent extends Event {
    @Cancelable
    public static class Render extends ArmorEvent {
        @Getter
        private final ModelObjArmor armorModel;
        @Getter
        private final DxModelRenderer objModel;
        @Getter
        private final ObjObjectRenderer objObjectRenderer;
        @Getter
        private final PhysicsEntityEvent.Phase eventPhase;
        @Getter
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
