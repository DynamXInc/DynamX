package fr.dynamx.common.entities;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.common.contentpack.parts.PartSeat;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.EnumPlayerStandOnTop;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.List;

/**
 * Base implementation for all vehicles <br>
 * It's fully modular and allows to create very different vehicles
 *
 * @param <T> The physics handler type
 * @see IPhysicsModule
 * @see BaseVehiclePhysicsHandler For the physics implementation
 */
public abstract class BaseVehicleEntity<T extends BaseVehiclePhysicsHandler<?>> extends PackPhysicsEntity<T, ModularVehicleInfo> {
    public BaseVehicleEntity(World worldIn) {
        super(worldIn);
    }

    public BaseVehicleEntity(String name, World world, Vector3f pos, float spawnRotationAngle, int metadata) {
        super(name, world, pos, spawnRotationAngle, metadata);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
    }

    @Override
    protected void createModules(ModuleListBuilder modules) {
        super.createModules(modules);
        getPackInfo().addModules(this, modules);
    }

    @Override
    protected final void fireCreateModulesEvent(Side side) {
        //Don't simplify the generic type, for fml
        MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.CreateModules<>(BaseVehicleEntity.class, this, moduleList, side));
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tagCompound) {
        super.readEntityFromNBT(tagCompound);

        setMetadata(tagCompound.getInteger("Metadata"));
        MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.LoadFromNBT(tagCompound, this));
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tagCompound) {
        super.writeEntityToNBT(tagCompound);

        tagCompound.setInteger("Metadata", getMetadata());
        MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.SaveToNBT(tagCompound, this));
    }

    @Override
    public void onUpdate() {
        Profiler.get().start(Profiler.Profiles.TICK_ENTITIES);
        Vector3fPool.openPool();
        super.onUpdate();
        Vector3fPool.closePool();
        Profiler.get().end(Profiler.Profiles.TICK_ENTITIES);
    }


    @Override
    protected boolean canFitPassenger(Entity passenger) {
        return this.getPassengers().size() < getPackInfo().getPartsByType(PartSeat.class).size();
    }

    @Override
    public boolean shouldRiderSit() {
        return RenderPhysicsEntity.shouldRenderPlayerSitting;
    }

    @Override
    public boolean canPassengerSteer() {
        return false;
    }

    @Override
    public void setDead() {
        super.setDead();
        moduleList.forEach(IPhysicsModule::onSetDead);
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        moduleList.forEach(IPhysicsModule::onRemovedFromWorld);
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
        if(playerStandOnTop == null)
            return true;
        else {
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
}
