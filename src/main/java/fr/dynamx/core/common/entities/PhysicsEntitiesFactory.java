package fr.dynamx.core.common.entities;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.callbacks.ModularEntityInitCallback;
import fr.dynamx.api.entities.callbacks.ModularEntityPhysicsInitCallback;
import fr.dynamx.core.common.entities.vehicles.BoatEntity;
import fr.dynamx.core.common.entities.vehicles.CarEntity;
import fr.dynamx.core.common.entities.vehicles.HelicopterEntity;
import fr.dynamx.core.common.entities.vehicles.TrailerEntity;
import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.entities.HmEntityFactory;
import fr.hermes.api.mc.entities.HmEntityLogic;
import fr.hermes.api.mc.world.HmWorld;
import lombok.Getter;
import lombok.Setter;

public abstract class PhysicsEntitiesFactory<TLogic extends HmEntityLogic> implements HmEntityFactory<TLogic> {
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

    protected abstract TLogic createEntityLogicInternal(HmWorld world, HmEntity entity);

    @Override
    public TLogic createEntityLogic(HmWorld world, HmEntity entity) {
        TLogic logic = createEntityLogicInternal(world, entity);
        if (logic instanceof ModularPhysicsEntity) {
            applyCallbacks((ModularPhysicsEntity<?>) logic);
        }
        return logic;
    }

    protected void applyCallbacks(ModularPhysicsEntity<?> modularEntity) {
        modularEntity.setInitCallback(entityInitCallback);
        modularEntity.setPhysicsInitCallback(physicsInitCallback);
    }

    public static class Car extends PhysicsEntitiesFactory<CarEntity<?>> {
        public Car(String packName, Vector3f pos, float spawnRotation, int metadata) {
            super(packName, pos, spawnRotation, metadata);
        }

        @Override
        public CarEntity<?> createEntityLogicInternal(HmWorld world, HmEntity entity) {
            return new CarEntity<>(packName, entity, pos, spawnRotation, metadata);
        }
    }

    public static class Trailer extends PhysicsEntitiesFactory<TrailerEntity<?>> {
        public Trailer(String packName, Vector3f pos, float spawnRotation, int metadata) {
            super(packName, pos, spawnRotation, metadata);
        }

        @Override
        public TrailerEntity<?> createEntityLogicInternal(HmWorld world, HmEntity entity) {
            return new TrailerEntity<>(packName, entity, pos, spawnRotation, metadata);
        }
    }

    public static class Helicopter extends PhysicsEntitiesFactory<HelicopterEntity<?>> {
        public Helicopter(String packName, Vector3f pos, float spawnRotation, int metadata) {
            super(packName, pos, spawnRotation, metadata);
        }

        @Override
        public HelicopterEntity<?> createEntityLogicInternal(HmWorld world, HmEntity entity) {
            return new HelicopterEntity<>(packName, entity, pos, spawnRotation, metadata);
        }
    }

    public static class Boat extends PhysicsEntitiesFactory<BoatEntity<?>> {
        public Boat(String packName, Vector3f pos, float spawnRotation, int metadata) {
            super(packName, pos, spawnRotation, metadata);
        }

        @Override
        public BoatEntity<?> createEntityLogicInternal(HmWorld world, HmEntity entity) {
            return new BoatEntity<>(packName, entity, pos, spawnRotation, metadata);
        }
    }

    public static class Props extends PhysicsEntitiesFactory<PropsEntity<?>> {
        public Props(String packName, Vector3f pos, float spawnRotation, int metadata) {
            super(packName, pos, spawnRotation, metadata);
        }

        @Override
        public PropsEntity<?> createEntityLogicInternal(HmWorld world, HmEntity entity) {
            return new PropsEntity<>(packName, entity, pos, spawnRotation, metadata);
        }
    }
}
