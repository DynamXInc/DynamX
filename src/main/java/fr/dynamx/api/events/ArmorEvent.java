package fr.dynamx.api.events;

import fr.dynamx.api.obj.IObjObject;
import fr.dynamx.client.renders.model.ModelObjArmor;
import fr.dynamx.client.renders.model.ObjModelClient;
import lombok.Getter;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

public class ArmorEvent extends Event {
    @Cancelable
    public static class Render extends ArmorEvent {
        @Getter
        private final ModelObjArmor armorModel;
        @Getter
        private final ObjModelClient objModel;
        @Getter
        private final IObjObject objObject;
        @Getter
        private final PhysicsEntityEvent.Phase eventPhase;
        @Getter
        private final Type renderType;

        public Render(ModelObjArmor armorModel, ObjModelClient objModel, IObjObject objObject, PhysicsEntityEvent.Phase phase, Type renderType) {
            this.armorModel = armorModel;
            this.objModel = objModel;
            this.objObject = objObject;
            this.eventPhase = phase;
            this.renderType = renderType;
        }

        public enum Type {
            WITH_ROTATION, NORMAL
        }
    }
}
