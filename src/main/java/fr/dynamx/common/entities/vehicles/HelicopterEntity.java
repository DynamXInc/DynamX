package fr.dynamx.common.entities.vehicles;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.client.camera.CameraMode;
import fr.dynamx.client.handlers.hud.HelicopterController;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.common.entities.modules.SeatsModule;
import fr.dynamx.common.entities.modules.WheelsModule;
import fr.dynamx.common.physics.entities.HelicopterPhysicsHandler;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class HelicopterEntity<T extends HelicopterPhysicsHandler<?>> extends BaseVehicleEntity<T> implements
        IModuleContainer.ISeatsContainer, IModuleContainer.IDoorContainer {
    private SeatsModule seats;
    private DoorsModule doors;
    private WheelsModule wheels;

    public HelicopterEntity(World world) {
        super(world);
    }

    public HelicopterEntity(String name, World world, Vector3f pos, float spawnRotationAngle, int metadata) {
        super(name, world, pos, spawnRotationAngle, metadata);
    }

    @Override
    public T createPhysicsHandler() {
        return (T) new HelicopterPhysicsHandler(this);
    }

    @Override
    protected void createModules(ModuleListBuilder modules) {
        //Take care to add seats BEFORE engine (the engine needs to detect dismounts)
        modules.add(getSeats());
        //modules.add(wheels = new WheelsModule(this));
        super.createModules(modules);
        doors = getModuleByType(DoorsModule.class);
    }

    @Override
    public ModularVehicleInfo createInfo(String infoName) {
        return DynamXObjectLoaders.HELICOPTERS.findInfo(infoName);
    }

    @Override
    public DoorsModule getDoors() {
        return doors;
    }

    @Nonnull
    @Override
    public SeatsModule getSeats() {
        if (seats == null) //We may need seats before modules are created, because of seats sync
            seats = new SeatsModule(this, CameraMode.FIXED) {
                @Override
                public void applyOrientationToEntity(Entity passenger) {
                    if(seats != null && seats.getControllingPassenger() == passenger && HelicopterController.isMouseLocked()) {
                        passenger.rotationYaw = 0;
                        passenger.prevRotationYaw = 0;
                        passenger.rotationPitch = 0;
                        passenger.prevRotationPitch = 0;
                    } else {
                        super.applyOrientationToEntity(passenger);
                    }
                }
            };
        return seats;
    }

    public WheelsModule getWheels() {
        return wheels;
    }

    @Override
    public BaseVehicleEntity<?> cast() {
        return this;
    }
}
