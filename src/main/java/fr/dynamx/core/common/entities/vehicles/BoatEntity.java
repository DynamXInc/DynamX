package fr.dynamx.core.common.entities.vehicles;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.core.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.core.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.core.common.entities.BaseVehicleEntity;
import fr.dynamx.core.common.entities.PackPhysicsEntity;
import fr.dynamx.core.common.entities.PhysicsEntitiesFactory;
import fr.dynamx.core.common.entities.modules.SeatsModule;
import fr.dynamx.core.common.entities.modules.engines.BoatPropellerModule;
import fr.dynamx.core.common.physics.entities.BoatPhysicsHandler;
import fr.hermes.api.mc.entities.HmEntity;

import javax.annotation.Nonnull;

public class BoatEntity<T extends BoatPhysicsHandler<?>> extends BaseVehicleEntity<T> implements IModuleContainer.ISeatsContainer {
    private SeatsModule seats;
    private BoatPropellerModule propeller;

    public BoatEntity(HmEntity mcEntityWrapper) {
        super(mcEntityWrapper);
    }

    public BoatEntity(String name, HmEntity mcEntityWrapper, Vector3f pos, float spawnRotationAngle, int metadata) {
        super(name, mcEntityWrapper, pos, spawnRotationAngle, metadata);
    }

    @Override
    public T createPhysicsHandler() {
        return (T) new BoatPhysicsHandler(this);
    }

    @Override
    protected void getListenerModules() {
        super.getListenerModules();
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

    @Override
    public PhysicsEntitiesFactory createEntityFactory() {
        return new PhysicsEntitiesFactory.Boat(getInfoName(), physicsPosition, mcEntity.hm$getRotationYaw(), getMetadata());
    }
}
