package fr.dynamx.core.common.entities;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.core.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.core.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.core.utils.DynamXUtils;
import fr.dynamx.core.utils.EnumPlayerStandOnTop;
import fr.dynamx.forge.DynamXConfig;
import fr.hermes.api.mc.HmEntity;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Base implementation for all vehicles <br>
 * It's fully modular and allows to create very different vehicles
 *
 * @param <T> The physics handler type
 * @see IPhysicsModule
 * @see BaseVehiclePhysicsHandler For the physics implementation
 */
public abstract class BaseVehicleEntity<T extends BaseVehiclePhysicsHandler<?>> extends PackPhysicsEntity<T, ModularVehicleInfo> {
    public BaseVehicleEntity(HmEntity mcEntityWrapper) {
        super(mcEntityWrapper);
    }

    public BaseVehicleEntity(String name, HmEntity mcEntityWrapper, Vector3f pos, float spawnRotationAngle, int metadata) {
        super(name, mcEntityWrapper, pos, spawnRotationAngle, metadata);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
    }

    /* TODO EVENTS @Override
    protected final void fireCreateModulesEvent(Side side) {
        //Don't simplify the generic type, for fml
        MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.CreateModules<>(BaseVehicleEntity.class, this, moduleList, side));
    }*/

    @Override
    public void readFromNbt(NBTTagCompound tagCompound) {
        super.readFromNbt(tagCompound);

        setMetadata(tagCompound.getInteger("Metadata"));
        // TODO EVENTS MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.LoadFromNBT(tagCompound, this));
    }

    @Override
    public void writeToNbt(NBTTagCompound tagCompound) {
        super.writeToNbt(tagCompound);

        tagCompound.setInteger("Metadata", getMetadata());
        // TODO EVENTS MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.SaveToNBT(tagCompound, this));
    }

    @Override
    public String getName() {
        return "DynamXVehicle:" + getInfoName() + ":" + getEntityId();
    }

    @Override
    public int getSyncTickRate() { //TODO aym EXPLORE THIS
        return /*(getControllingPassenger() != null || isMovingQuickly()) ? DynamXConfig.mountedVehiclesSyncTickRate :*/ DynamXConfig.mountedVehiclesSyncTickRate;
    }

    @Override
    public boolean canPlayerStandOnTop() {
        EnumPlayerStandOnTop playerStandOnTop = this.getPackInfo().getPlayerStandOnTop();
        switch (playerStandOnTop) {
            case NEVER:
                return false;
            case PROGRESSIVE:
                return DynamXUtils.getSpeed(this) <= 30;
            default:
                return true;
        }
    }
}
