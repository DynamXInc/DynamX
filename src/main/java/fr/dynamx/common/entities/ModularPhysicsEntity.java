package fr.dynamx.common.entities;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.callbacks.ModularEntityInitCallback;
import fr.dynamx.api.entities.callbacks.ModularEntityPhysicsInitCallback;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.api.network.sync.SynchronizedEntityVariableRegistry;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;
import fr.dynamx.common.physics.entities.PackEntityPhysicsHandler;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Base implementation for all modular entities <br>
 * The modularity is made for addons, and allows to create very different objects and vehicles
 *
 * @param <T> The physics handler type
 * @see IPhysicsModule
 * @see PackEntityPhysicsHandler For the physics implementation
 */
public abstract class ModularPhysicsEntity<T extends AbstractEntityPhysicsHandler<?, ?>> extends PhysicsEntity<T> {
    protected final List<IPhysicsModule<?>> moduleList = new ArrayList<>();
    protected final List<IPhysicsModule.IEntityUpdateListener> updateEntityListeners = new ArrayList<>();
    protected final List<IPhysicsModule.IEntityPosUpdateListener> updateEntityPosListeners = new ArrayList<>();
    protected final List<IPhysicsModule.IPhysicsUpdateListener> updatePhysicsListeners = new ArrayList<>();

    /**
     * Entity init callback
     */
    protected ModularEntityInitCallback initCallback;
    protected ModularEntityPhysicsInitCallback physicsInitCallback;

    public ModularPhysicsEntity(World worldIn) {
        super(worldIn);
    }

    public ModularPhysicsEntity(World world, Vector3f pos, float spawnRotationAngle) {
        super(world, pos, spawnRotationAngle);
    }

    /**
     * Sets the entity init callback, should be called before entity init (first tick of its existence)
     *
     * @param initCallback The new {@link ModularEntityInitCallback}
     */
    public ModularPhysicsEntity<T> setInitCallback(ModularEntityInitCallback initCallback) {
        this.initCallback = initCallback;
        return this;
    }

    /**
     * @return The physics init callback
     */
    @Nullable
    public ModularEntityInitCallback getInitCallback() {
        return initCallback;
    }

    /**
     * Sets the physics init callback
     *
     * @param physicsInitCallback The new {@link ModularEntityInitCallback}
     */
    public ModularPhysicsEntity<T> setPhysicsInitCallback(ModularEntityPhysicsInitCallback physicsInitCallback) {
        if(initialized == EnumEntityInitState.ALL){
            physicsInitCallback.onPhysicsInit(this, physicsHandler);
            return this;
        }
        this.physicsInitCallback = physicsInitCallback;
        return this;
    }

    /**
     * @return The physics init callback
     */
    @Nullable
    public ModularEntityPhysicsInitCallback getPhysicsInitCallback() {
        return physicsInitCallback;
    }

    /**
     * Computes listeners of update methods
     */
    protected void sortModules() {
        updateEntityListeners.clear();
        updatePhysicsListeners.clear();
        moduleList.forEach(m -> {
            if (m instanceof IPhysicsModule.IEntityUpdateListener && ((IPhysicsModule.IEntityUpdateListener) m).listenEntityUpdates(world.isRemote ? Side.CLIENT : Side.SERVER))
                updateEntityListeners.add((IPhysicsModule.IEntityUpdateListener) m);
            if (m instanceof IPhysicsModule.IEntityPosUpdateListener && ((IPhysicsModule.IEntityPosUpdateListener) m).listenEntityPosUpdates(world.isRemote ? Side.CLIENT : Side.SERVER))
                updateEntityPosListeners.add((IPhysicsModule.IEntityPosUpdateListener) m);
            if (m instanceof IPhysicsModule.IPhysicsUpdateListener)
                updatePhysicsListeners.add((IPhysicsModule.IPhysicsUpdateListener) m);
        });
    }

    @Override
    public <Y extends IPhysicsModule<?>> Y getModuleByType(Class<Y> moduleClass) {
        return (Y) moduleList.stream().filter(m -> moduleClass.isAssignableFrom(m.getClass())).findFirst().orElse(null);
    }

    @Override
    public boolean hasModuleOfType(Class<? extends IPhysicsModule<?>> moduleClass) {
        return moduleList.stream().anyMatch(m -> moduleClass.isAssignableFrom(m.getClass()));
    }

    @Override
    public boolean initEntityProperties() {
        createModules(new ModuleListBuilder(moduleList));
        fireCreateModulesEvent(world.isRemote ? Side.CLIENT : Side.SERVER);
        moduleList.forEach(IPhysicsModule::initEntityProperties);
        if (initCallback != null) {
            initCallback.onEntityInit(this, moduleList);
            initCallback = null; //Free memory
        }
        //Init them before sorting because listened functions may change
        sortModules();
        //SynchronizedVariablesRegistry.setSyncVarsForContext(world.isRemote ? Side.CLIENT : Side.SERVER, new HashMap<>(), getNetwork());
        return true;
    }

    @Override
    public void initPhysicsEntity(boolean usePhysics) {
        if (usePhysics) physicsHandler = createPhysicsHandler();
        moduleList.forEach(m -> ((IPhysicsModule<T>) m).initPhysicsEntity(physicsHandler));
        if (usePhysics)
            physicsHandler.addToWorld(); //Add the physics handler to the physics world AFTER modules initialisation
        if (physicsInitCallback != null) {
            physicsInitCallback.onPhysicsInit(this, physicsHandler);
            physicsInitCallback = null; //Free memory
        }
    }

    /**
     * @return A new {@link PackEntityPhysicsHandler} for this entity
     */
    protected abstract T createPhysicsHandler();

    /**
     * Called to create and add modules to the module list <br>
     * Take care of their order : a propulsion module should be added before an engine module
     *
     * @param modules
     */
    protected abstract void createModules(ModuleListBuilder modules);

    /**
     * Fires the create modules event, with the right generic type <br>
     * If you override this function, you should make it final
     */
    protected abstract void fireCreateModulesEvent(Side side);

    @Override
    public void registerSynchronizedVariables() {
        super.registerSynchronizedVariables();
        int size = moduleList.size();
        for (int i = 0; i < size; i++) {
            SynchronizedEntityVariableRegistry.addVarsOf(this.getSynchronizer(), moduleList.get(i));
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tagCompound) {
        super.readEntityFromNBT(tagCompound);
        //Load the modules after because they are initialized in the super method
        int size = moduleList.size();
        for (int i = 0; i < size; i++) {
            moduleList.get(i).readFromNBT(tagCompound);
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tagCompound) {
        super.writeEntityToNBT(tagCompound);
        moduleList.forEach(m -> m.writeToNBT(tagCompound));
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        int size = updateEntityListeners.size();
        for (int i = 0; i < size; i++) {
            updateEntityListeners.get(i).updateEntity();
        }
    }

    @Override
    public void updateMinecraftPos() {
        super.updateMinecraftPos();
        int size = updateEntityPosListeners.size();
        for (int i = 0; i < size; i++) {
            updateEntityPosListeners.get(i).updateEntityPos();
        }
    }

    @Override
    public void preUpdatePhysics(boolean simulatingPhysics) {
        super.preUpdatePhysics(simulatingPhysics);
        int size = updatePhysicsListeners.size();
        for (int i = 0; i < size; i++) {
            updatePhysicsListeners.get(i).preUpdatePhysics(simulatingPhysics);
        }
    }

    @Override
    public void postUpdatePhysics(boolean simulatingPhysics) {
        super.postUpdatePhysics(simulatingPhysics);
        int size = updatePhysicsListeners.size();
        for (int i = 0; i < size; i++) {
            updatePhysicsListeners.get(i).postUpdatePhysics(simulatingPhysics);
        }
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        int size = moduleList.size();
        for (int i = 0; i < size; i++) {
            moduleList.get(i).addPassenger(passenger);
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        int size = moduleList.size();
        for (int i = 0; i < size; i++) {
            moduleList.get(i).removePassenger(passenger);
        }
    }

    @Override
    public void applyOrientationToEntity(Entity passenger) {
        if (this instanceof IModuleContainer.ISeatsContainer) {
            ((IModuleContainer.ISeatsContainer) this).getSeats().applyOrientationToEntity(passenger);
        } else {
            super.applyOrientationToEntity(passenger);
        }
    }

    @Override
    public void updatePassenger(Entity passenger) {
        if (this instanceof IModuleContainer.ISeatsContainer) {
            ((IModuleContainer.ISeatsContainer) this).getSeats().updatePassenger(passenger);
        } else {
            super.updatePassenger(passenger);
        }
    }

    @Nullable
    @Override
    public Entity getControllingPassenger() {
        if (this instanceof IModuleContainer.ISeatsContainer && ((IModuleContainer.ISeatsContainer) this).getSeats() != null) { //May be called before init of modules
            return ((IModuleContainer.ISeatsContainer) this).getSeats().getControllingPassenger();
        } else {
            return super.getControllingPassenger();
        }
    }

    public List<IPhysicsModule<?>> getModules() {
        return moduleList;
    }
}