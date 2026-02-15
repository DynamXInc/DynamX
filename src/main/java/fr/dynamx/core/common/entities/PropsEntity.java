package fr.dynamx.core.common.entities;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.core.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.core.common.contentpack.type.objects.PropObject;
import fr.dynamx.core.common.entities.modules.SeatsModule;
import fr.dynamx.core.common.physics.entities.PackEntityPhysicsHandler;
import fr.dynamx.core.common.physics.entities.PropPhysicsHandler;
import fr.dynamx.forge.DynamXConfig;
import fr.hermes.api.mc.entities.HmEntity;

import javax.annotation.Nonnull;

public class PropsEntity<T extends PackEntityPhysicsHandler<PropObject<?>, ?>> extends PackPhysicsEntity<T, PropObject<?>> implements IModuleContainer.ISeatsContainer {
    private SeatsModule seats;

    public PropsEntity(HmEntity mcEntityWrapper) {
        super(mcEntityWrapper);
    }

    public PropsEntity(String infoName, HmEntity mcEntityWrapper, Vector3f pos, float spawnRotationAngle, int metadata) {
        super(infoName, mcEntityWrapper, pos, spawnRotationAngle, metadata);
    }

    @Override
    public PropObject<?> createInfo(String infoName) {
        return DynamXObjectLoaders.PROPS.findInfo(infoName);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (getPackInfo() == null) {
            return;
        }
        if (getPackInfo().getDespawnTime() != -1) {
            if ((getTicksExisted() % getPackInfo().getDespawnTime()) == 0) {
                mcEntity.hm$setDead();
            }
        }
    }

    @Override
    public T createPhysicsHandler() {
        return (T) new PropPhysicsHandler(this);
    }

    /* todo events @Override
    protected final void fireCreateModulesEvent(Side side) {
        //Don't simplify the generic type, for fml
        MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.CreateModules<>(PropsEntity.class, this, moduleList, side));
    }*/

    @Override
    public int getSyncTickRate() {
        return DynamXConfig.propsSyncTickRate;
    }

    @Override
    protected void getListenerModules() {
        super.getListenerModules();
        seats = getModuleByType(SeatsModule.class);
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
    public PhysicsEntitiesFactory createEntityFactory() {
        return new PhysicsEntitiesFactory.Props(getInfoName(), physicsPosition, mcEntity.hm$getRotationYaw(), getMetadata());
    }
}
