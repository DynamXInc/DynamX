package fr.dynamx.common.entities.vehicles;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.ISeatsModule;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.BoatPropellerModule;
import fr.dynamx.common.entities.modules.EngineModule;
import fr.dynamx.common.entities.modules.BoatEngineModule;
import fr.dynamx.common.entities.modules.CarEngineModule;
import fr.dynamx.common.entities.modules.SeatsModule;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class BoatEntity<T extends BoatEntity.BoatPhysicsHandler<?>> extends BaseVehicleEntity<T> implements IModuleContainer.ISeatsContainer {
    private CarEngineModule engine;
    private SeatsModule seats;
    private BoatPropellerModule propulsion;

    public BoatEntity(World world) {
        super(world);
    }

    public BoatEntity(String name, World world, Vector3f pos, float spawnRotationAngle, int metadata) {
        super(name, world, pos, spawnRotationAngle, metadata);
    }

    @Override
    public T createPhysicsHandler() {
        return (T) new BoatPhysicsHandler(this);
    }

    @Override
    public void createModules(ModuleListBuilder modules) {
        //Take care to add seats BEFORE engine (the engine needs to detect dismounts)
        modules.add(seats = new SeatsModule(this));
        //Take care to add propulsion BEFORE engine (the engine needs a propulsion)
        modules.add(propulsion = new BoatPropellerModule(this));

        super.createModules(modules);
        engine = getModuleByType(CarEngineModule.class);
    }

    @Nonnull
    public CarEngineModule getEngine() {
        return engine;
    }

    @Nonnull
    public BoatEngineModule getPropulsion() {
        return propulsion;
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

    @Override
    public ModularVehicleInfo createInfo(String infoName) {
        return DynamXObjectLoaders.BOATS.findInfo(infoName);
    }

    @Override
    public void onPackInfosReloaded() {
        super.onPackInfosReloaded();
        if(physicsHandler != null)
            physicsHandler.onPackInfosReloaded();
    }

}
