package fr.dynamx.common.entities;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.common.contentpack.ModularVehicleInfo;
import fr.dynamx.common.contentpack.parts.PartSeat;
import fr.dynamx.common.contentpack.parts.PartShape;
import fr.dynamx.common.network.sync.vars.VehicleSynchronizedVariables;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Base implementation for all vehicles <br>
 *     It's fully modular and allows to create very different vehicles
 *
 * @see IPhysicsModule
 * @see BaseVehiclePhysicsHandler For the physics implementation
 * @param <T> The physics handler type
 */
public abstract class BaseVehicleEntity<T extends BaseVehiclePhysicsHandler<?>> extends PackPhysicsEntity<T, ModularVehicleInfo<?>>
{
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
    public List<ResourceLocation> getSynchronizedVariables(Side side, SimulationHolder simulationHolder) {
        List<ResourceLocation> vars = super.getSynchronizedVariables(side, simulationHolder);
        if(this instanceof IModuleContainer.IEngineContainer && simulationHolder.isPhysicsAuthority(side)) {
            vars.add(VehicleSynchronizedVariables.Engine.NAME);
        }
        return vars;
    }

    @Override
    protected void createModules(ModuleListBuilder modules) {
        super.createModules(modules);
        getPackInfo().addModules(this, modules);
    }

    @Override
    protected final void fireCreateModulesEvent(Side side) {
        //Don't simplify the generic type, for fml
        MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.CreateEntityModulesEvent(BaseVehicleEntity.class, this, moduleList, side));
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tagCompound) {
        super.readEntityFromNBT(tagCompound);

        setMetadata(tagCompound.getInteger("Metadata"));
        MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.LoadVehicleEntityNBT(tagCompound, this));
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tagCompound) {
        super.writeEntityToNBT(tagCompound);

        tagCompound.setInteger("Metadata", getMetadata());
        MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.SaveVehicleEntityNBT(tagCompound, this));
    }

    @Override
    public void onUpdate() {
        Profiler.get().start(Profiler.Profiles.TICK_ENTITIES);
        Vector3fPool.openPool();
        super.onUpdate();
        Vector3fPool.closePool();
        Profiler.get().end(Profiler.Profiles.TICK_ENTITIES);
    }

    /**
     * Cache
     */
    private final List<MutableBoundingBox> unrotatedBoxes = new ArrayList<>();

    @Override
    public List<MutableBoundingBox> getCollisionBoxes() {
        if (getPackInfo() == null || physicsPosition == null)
            return new ArrayList<>(0);
        if (unrotatedBoxes.size() != getPackInfo().getPartShapes().size()) {
            unrotatedBoxes.clear();
            for (PartShape shape : getPackInfo().getPartShapes()) {
                MutableBoundingBox b = new MutableBoundingBox(shape.getBoundingBox());
                b.offset(physicsPosition);
                unrotatedBoxes.add(b);
            }
        } else {
            for (int i = 0; i < getPackInfo().getPartShapes().size(); i++) {
                MutableBoundingBox b = unrotatedBoxes.get(i);
                b.setTo(getPackInfo().getPartShapes().get(i).getBoundingBox());
                b.offset(physicsPosition);
                unrotatedBoxes.add(b);
            }
        }
        return unrotatedBoxes;
    }

    @Override
    protected boolean canFitPassenger(Entity passenger) {
        return this.getPassengers().size() < getPackInfo().getPartsByType(PartSeat.class).size();
    }

    @Override
    public boolean shouldRiderSit() {
        return true; //Passagers debouts
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
        return true;
    }
}
