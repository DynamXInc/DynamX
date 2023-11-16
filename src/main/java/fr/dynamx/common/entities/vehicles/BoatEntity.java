package fr.dynamx.common.entities.vehicles;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.engines.BoatPropellerModule;
import fr.dynamx.common.entities.modules.SeatsModule;
import fr.dynamx.common.physics.entities.BoatPhysicsHandler;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class BoatEntity<T extends BoatPhysicsHandler<?>> extends BaseVehicleEntity<T> implements IModuleContainer.ISeatsContainer {
    private SeatsModule seats;
    private BoatPropellerModule propeller;

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
        super.createModules(modules);
        seats = getModuleByType(SeatsModule.class);
        propeller = getModuleByType(BoatPropellerModule.class);
    }

    @Nonnull
    public BoatPropellerModule getPropeller() {
        return propeller;
    }

    @Nonnull
    @Override
    public SeatsModule getSeats() {
        return seats;
    }

    @Override
    public PackPhysicsEntity<?, ?> cast() {
        return this;
    }

    @Override
    public ModularVehicleInfo createInfo(String infoName) {
        return DynamXObjectLoaders.BOATS.findInfo(infoName);
    }

    @Override
    public void onPackInfosReloaded() {
        super.onPackInfosReloaded();
        if (physicsHandler != null)
            physicsHandler.onPackInfosReloaded();
    }

}
