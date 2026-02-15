package fr.dynamx.core.common.entities;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.callbacks.ModularEntityInitCallback;
import fr.dynamx.api.entities.callbacks.ModularEntityPhysicsInitCallback;
import fr.dynamx.core.common.entities.vehicles.BoatEntity;
import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.entities.HmEntityFactory;
import fr.hermes.api.mc.entities.HmEntityLogic;
import fr.hermes.api.mc.world.HmWorld;
import lombok.Getter;
import lombok.Setter;

public abstract class PhysicsEntitiesFactory implements HmEntityFactory {
    protected final String packName;
    @Getter
    protected final Vector3f pos;
    protected final float spawnRotation;
    protected final int metadata;

    @Setter
    protected ModularEntityInitCallback entityInitCallback;
    @Setter
    protected ModularEntityPhysicsInitCallback physicsInitCallback;

    protected PhysicsEntitiesFactory(String packName, Vector3f pos, float spawnRotation, int metadata) {
        this.packName = packName;
        this.pos = pos;
        this.spawnRotation = spawnRotation;
        this.metadata = metadata;
    }

    protected void applyCallbacks(ModularPhysicsEntity<?> modularEntity) {
        modularEntity.setInitCallback(entityInitCallback);
        modularEntity.setPhysicsInitCallback(physicsInitCallback);
    }

    public static class Props extends PhysicsEntitiesFactory {
        public Props(String packName, Vector3f pos, float spawnRotation, int metadata) {
            super(packName, pos, spawnRotation, metadata);
        }

        @Override
        public HmEntityLogic createEntityLogic(HmWorld world, HmEntity entity) {
            PropsEntity<?> logic = new PropsEntity<>(packName, entity, pos, spawnRotation, metadata);
            applyCallbacks(logic);
            return logic;
        }
    }

    public static class Boat extends PhysicsEntitiesFactory {
        public Boat(String packName, Vector3f pos, float spawnRotation, int metadata) {
            super(packName, pos, spawnRotation, metadata);
        }

        @Override
        public HmEntityLogic createEntityLogic(HmWorld world, HmEntity entity) {
            BoatEntity<?> logic = new BoatEntity<>(packName, entity, pos, spawnRotation, metadata);
            applyCallbacks(logic);
            return logic;
        }
    }
}
