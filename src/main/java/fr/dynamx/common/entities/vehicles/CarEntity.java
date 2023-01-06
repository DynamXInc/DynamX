package fr.dynamx.common.entities.vehicles;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.common.entities.modules.EngineModule;
import fr.dynamx.common.entities.modules.SeatsModule;
import fr.dynamx.common.entities.modules.WheelsModule;
import fr.dynamx.common.physics.entities.BaseWheeledVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.modules.WheelsPhysicsHandler;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class CarEntity<T extends CarEntity.CarPhysicsHandler<?>> extends BaseVehicleEntity<T> implements
        IModuleContainer.ISeatsContainer, IModuleContainer.IDoorContainer {
    private EngineModule engine;
    private SeatsModule seats;
    private WheelsModule propulsion;
    private DoorsModule doors;

    public CarEntity(World world) {
        super(world);
    }

    public CarEntity(String name, World world, Vector3f pos, float spawnRotationAngle, int metadata) {
        super(name, world, pos, spawnRotationAngle, metadata);
    }

    @Override
    public T createPhysicsHandler() {
        return (T) new CarPhysicsHandler(this);
    }

    @Override
    public void createModules(ModuleListBuilder modules) {
        //Take care to add seats BEFORE engine (the engine needs to detect dismounts)
        modules.add(seats = new SeatsModule(this));
        //Take care to add propulsion BEFORE engine (the engine needs a propulsion)
        modules.add(propulsion = new WheelsModule(this));

        super.createModules(modules);

        engine = getModuleByType(EngineModule.class);
        doors = getModuleByType(DoorsModule.class);
    }

    @Override
    public ModularVehicleInfo createInfo(String infoName) {
        return DynamXObjectLoaders.WHEELED_VEHICLES.findInfo(infoName);
    }

    @Nonnull
    public EngineModule getEngine() {
        return engine;
    }

    @Nonnull
    public WheelsModule getPropulsion() {
        return propulsion;
    }

    @Override
    public DoorsModule getDoors() {
        return doors;
    }

    @Nonnull
    @Override
    public SeatsModule getSeats() {
        if (seats == null) //We may need seats before modules are created, because of seats sync
            seats = new SeatsModule(this);
        return seats;
    }

    @Override
    public BaseVehicleEntity<?> cast() {
        return this;
    }

    public static class CarPhysicsHandler<A extends CarEntity<?>> extends BaseWheeledVehiclePhysicsHandler<A> {
        public CarPhysicsHandler(A entity) {
            super(entity);
        }

        public WheelsPhysicsHandler getWheels() {
            return getHandledEntity().getPropulsion().getPhysicsHandler(); //WHEELS
        }
    }
}
