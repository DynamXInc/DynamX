package fr.dynamx.common.entities.modules.engines;

import fr.dynamx.api.audio.EnumSoundState;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.network.sync.SynchronizationRules;
import fr.dynamx.api.network.sync.SynchronizedEntityVariable;
import fr.dynamx.client.sound.EngineSound;
import fr.dynamx.common.contentpack.type.vehicle.BaseEngineInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.WheelsModule;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.modules.EnginePhysicsHandler;
import fr.dynamx.common.physics.entities.parts.engine.AutomaticGearboxHandler;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static fr.dynamx.client.ClientProxy.SOUND_HANDLER;

/**
 * Basic {@link } implementation for cars <br>
 * Works with an {@link AutomaticGearboxHandler} and a {@link WheelsModule}
 *
 * @see VehicleEntityProperties.EnumEngineProperties
 * @see EnginePhysicsHandler
 */
@SynchronizedEntityVariable.SynchronizedPhysicsModule(modid = DynamXConstants.ID)
public abstract class BasicEngineModule implements IPhysicsModule<BaseVehiclePhysicsHandler<?>>, IPhysicsModule.IPhysicsUpdateListener, IPhysicsModule.IEntityUpdateListener, IPackInfoReloadListener {

    protected final BaseVehicleEntity<? extends BaseVehiclePhysicsHandler<?>> entity;

    //Default value is 32 for the handbrake on spawn
    @SynchronizedEntityVariable(name = "controls")
    private final EntityVariable<Integer> controls = new EntityVariable<>(SynchronizationRules.CONTROLS_TO_SPECTATORS, 32);

    /**
     * @see VehicleEntityProperties.EnumEngineProperties
     */
    @SynchronizedEntityVariable(name = "engine_props") //TOD THINK OF IT
    private final EntityVariable<float[]> engineProperties = new EntityVariable<>(SynchronizationRules.PHYSICS_TO_SPECTATORS, new float[VehicleEntityProperties.EnumEngineProperties.values().length]);

    protected final Map<Integer, EngineSound> engineSounds = new HashMap<>();
    @Getter
    protected EngineSound currentEngineSound;
    protected EngineSound lastEngineSound;

    public BasicEngineModule(BaseVehicleEntity<? extends BaseVehiclePhysicsHandler<?>> entity) {
        this.entity = entity;
    }

    /**
     * These vars are automatically synchronised from server (or driver) to others
     *
     * @return All engine properties, see {@link VehicleEntityProperties.EnumEngineProperties}
     */
    public float[] getEngineProperties() {
        return engineProperties.get();
    }

    public float getEngineProperty(VehicleEntityProperties.EnumEngineProperties engineProperty) {
        return engineProperties.get()[engineProperty.ordinal()];
    }

    public boolean isAccelerating() {
        return EnginePhysicsHandler.inTestFullGo || (controls.get() & 2) == 2;
    }

    public boolean isReversing() {
        return (controls.get() & 4) == 4;
    }

    public boolean isTurningLeft() {
        return (controls.get() & 8) == 8;
    }

    public boolean isTurningRight() {
        return (controls.get() & 16) == 16;
    }

    public boolean isHandBraking() {
        return (getControls() & 32) == 32;
    }

    /**
     * @return The engine off/on state
     */
    public boolean isEngineStarted() {
        return (EnginePhysicsHandler.inTestFullGo) || ((controls.get() & 1) == 1);
    }

    /**
     * Set the engine off/on state <br>
     * Not used in DynamX, here for the addons
     *
     * @param started engine on/off state
     */
    public void setEngineStarted(boolean started) {
        setControls(started ? getControls() | 1 : getControls() & ~1);
    }

    public int getControls() {
        return controls.get();
    }


    /**
     * Set all engine controls <br>
     * If called on client side and if the engine is switched on, plays the starting sound
     */
    public void setControls(int controls) {
        if (!this.isEngineStarted() && (controls & 1) == 1)
            onEngineSwitchedOn();
        else if (isEngineStarted() && (controls & 1) != 1)
            onEngineSwitchedOff();
        this.controls.set(controls);
    }

    public void onEngineSwitchedOn() {
        if (entity.world.isRemote && entity.ticksExisted > 60) {
            playStartingSound();
        }
    }

    public void onEngineSwitchedOff() {

    }

    /**
     * Resets all controls except engine on/off state and handbrake
     */
    public void resetControls() {
        setControls(controls.get() & 1 | (controls.get() & 32));
    }

    @Nullable
    @Override
    public abstract IVehicleController createNewController();

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        if (tag.getBoolean("isEngineStarted"))
            setControls(controls.get() | 1); //set engine on
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setBoolean("isEngineStarted", isEngineStarted());
    }

    @Override
    public void postUpdatePhysics(boolean simulatingPhysics) {
        if (simulatingPhysics) {
            this.engineProperties.get()[VehicleEntityProperties.EnumEngineProperties.SPEED.ordinal()] = entity.physicsHandler.getSpeed(BaseVehiclePhysicsHandler.SpeedUnit.KMH);
        }
    }

    @Override
    public void onSetSimulationHolder(SimulationHolder simulationHolder, EntityPlayer simulationPlayerHolder, SimulationHolder.UpdateContext changeContext) {
        if (simulationPlayerHolder == null) {
            resetControls();
        }
    }

    //Sounds

    @SideOnly(Side.CLIENT)
    protected void playStartingSound() {
        boolean forInterior = Minecraft.getMinecraft().gameSettings.thirdPersonView == 0 && entity.isRidingOrBeingRiddenBy(Minecraft.getMinecraft().player);
        String sound = getStartingSound(forInterior);
        if (sound != null)
            SOUND_HANDLER.playSingleSound(entity.physicsPosition, sound, 1, 1);
    }

    @SideOnly(Side.CLIENT)
    public abstract BaseEngineInfo getEngineInfo();

    @Override
    public boolean listenEntityUpdates(Side side) {
        return side.isClient();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateEntity() {
        if (!MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.UpdateSounds(entity, this, PhysicsEntityEvent.Phase.PRE))) {
            if (entity.getPackInfo() != null) {
                updateSounds();
            }
            MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.UpdateSounds(entity, this, PhysicsEntityEvent.Phase.POST));
        }
    }

    @Override
    public void onPackInfosReloaded() {
        engineSounds.clear();
    }

    @SideOnly(Side.CLIENT)
    public String getStartingSound(boolean forInterior) {
        if (getEngineInfo() == null)
            return null;
        return forInterior ? getEngineInfo().startingSoundInterior : getEngineInfo().startingSoundExterior;
    }

    @SideOnly(Side.CLIENT)
    public void updateSounds() {
        BaseEngineInfo engineInfo = getEngineInfo();
        if (engineInfo != null && engineInfo.getEngineSounds() != null) {
            if (engineSounds.isEmpty()) { //Sounds are not initialized
                engineInfo.getEngineSounds().forEach(engineSound -> engineSounds.put(engineSound.id, new EngineSound(engineSound, entity, this)));
            }
            if (isEngineStarted()) {
                boolean forInterior = Minecraft.getMinecraft().gameSettings.thirdPersonView == 0 && entity.isRidingOrBeingRiddenBy(Minecraft.getMinecraft().player);
                float rpm = getEngineProperty(VehicleEntityProperties.EnumEngineProperties.REVS) * engineInfo.getMaxRevs();
                lastEngineSound = currentEngineSound;
                if (currentEngineSound == null || !currentEngineSound.shouldPlay(rpm, forInterior)) {
                    engineSounds.forEach((id, vehicleSound) -> {
                        if (vehicleSound.shouldPlay(rpm, forInterior)) {
                            this.currentEngineSound = vehicleSound;
                        }
                    });
                }
                if (currentEngineSound != lastEngineSound) //if playing sound changed
                {
                    if (lastEngineSound != null)
                        SOUND_HANDLER.stopSound(lastEngineSound);
                    if (currentEngineSound != null) {
                        if (currentEngineSound.getState() == EnumSoundState.STOPPING) //already playing
                            currentEngineSound.onStarted();
                        else
                            SOUND_HANDLER.playStreamingSound(Vector3fPool.get(currentEngineSound.getPosX(), currentEngineSound.getPosY(), currentEngineSound.getPosZ()), currentEngineSound);
                    }
                }
            } else {
                if (currentEngineSound != null)
                    SOUND_HANDLER.stopSound(currentEngineSound);
                currentEngineSound = lastEngineSound = null;
            }
        }
    }

    public float getSoundPitch() {
        return getEngineProperty(VehicleEntityProperties.EnumEngineProperties.REVS);
    }
}