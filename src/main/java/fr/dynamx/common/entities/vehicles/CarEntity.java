package fr.dynamx.common.entities.vehicles;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.IEngineModule;
import fr.dynamx.api.entities.modules.ISeatsModule;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.api.physics.entities.IPropulsionHandler;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.common.entities.modules.EngineModule;
import fr.dynamx.common.entities.modules.SeatsModule;
import fr.dynamx.common.entities.modules.WheelsModule;
import fr.dynamx.common.physics.entities.BaseWheeledVehiclePhysicsHandler;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class CarEntity<T extends CarEntity.CarPhysicsHandler<?>> extends BaseVehicleEntity<T> implements
        IModuleContainer.IEngineContainer, IModuleContainer.IPropulsionContainer<WheelsModule>,
        IModuleContainer.ISeatsContainer, IModuleContainer.IDoorContainer {
    private IEngineModule<?> engine;
    private ISeatsModule seats;
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
    protected ModularVehicleInfo createInfo(String infoName) {
        return DynamXObjectLoaders.WHEELED_VEHICLES.findInfo(infoName);
    }

    @Nonnull
    @Override
    public IEngineModule<?> getEngine() {
        return engine;
    }

    @Nonnull
    @Override
    public WheelsModule getPropulsion() {
        return propulsion;
    }

    @Override
    public DoorsModule getDoors() {
        return doors;
    }

    @Nonnull
    @Override
    public ISeatsModule getSeats() {
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

        @Override
        public IPropulsionHandler getPropulsion() {
            return getHandledEntity().getPropulsion().getPhysicsHandler(); //WHEELS
        }
    }
}
