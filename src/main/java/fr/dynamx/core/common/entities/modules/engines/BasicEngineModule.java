package fr.dynamx.core.common.entities.modules.engines;

import fr.dynamx.api.audio.EnumSoundState;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.network.sync.SynchronizationRules;
import fr.dynamx.api.network.sync.SynchronizedEntityVariable;
import fr.dynamx.core.client.sound.EngineSound;
import fr.dynamx.core.common.contentpack.type.vehicle.BaseEngineInfo;
import fr.dynamx.core.common.entities.BaseVehicleEntity;
import fr.dynamx.core.common.entities.modules.WheelsModule;
import fr.dynamx.core.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.core.common.physics.entities.modules.EnginePhysicsHandler;
import fr.dynamx.core.common.physics.entities.parts.engine.AutomaticGearboxHandler;
import fr.dynamx.core.utils.DynamXConstants;
import fr.hermes.api.mc.HmMinecraftClient;
import fr.hermes.api.mc.entities.HmPlayerEntity;
import fr.hermes.api.mod.HermesPlatform;
import fr.hermes.forge.JmeVector3fPool;
import lombok.Getter;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static fr.dynamx.core.client.ClientProxy.SOUND_HANDLER;

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
    private final EntityVariable<Integer> controls = new EntityVariable<>((var, value) -> {
        setControls(value); //This will call the change listeners like onEngineSwitchedOn
    }, SynchronizationRules.CONTROLS_TO_SPECTATORS, 32);

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
        if (entity.hm$getWorld().hm$isClient() && entity.getTicksExisted() > 60) {
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
    public void onSetSimulationHolder(SimulationHolder simulationHolder, HmPlayerEntity simulationPlayerHolder, SimulationHolder.UpdateContext changeContext) {
        if (simulationPlayerHolder == null) {
            resetControls();
        }
    }

    //Sounds

    //@SideOnly(Side.CLIENT)
    protected void playStartingSound() {
        boolean forInterior = HermesPlatform.getInstance().getClient().hm$getGameSettings().hm$isFirstPersonView() && entity.isRidingOrBeingRiddenBy(HermesPlatform.getInstance().getClient().hm$getPlayer());
        String sound = getStartingSound(forInterior);
        if (sound != null) {
            SOUND_HANDLER.playSingleSound(entity.physicsPosition, sound, 1, 1);
        }
    }

    //@SideOnly(Side.CLIENT)
    public abstract BaseEngineInfo getEngineInfo();

    @Override
    public boolean listenEntityUpdates(boolean isClient) {
        return isClient;
    }

    @Override
    //@SideOnly(Side.CLIENT)
    public void updateEntity() {
        //if (!MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.UpdateSounds(entity, this, EventPhase.PRE))) {
        if (entity.getPackInfo() != null) {
            updateSounds();
        }
        // TODO EVENTS MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.UpdateSounds(entity, this, EventPhase.POST));
        //}
    }

    @Override
    public void onPackInfosReloaded() {
        engineSounds.clear();
    }

    //@SideOnly(Side.CLIENT)
    public String getStartingSound(boolean forInterior) {
        if (getEngineInfo() == null)
            return null;
        return forInterior ? getEngineInfo().startingSoundInterior : getEngineInfo().startingSoundExterior;
    }

    //@SideOnly(Side.CLIENT)
    public void updateSounds() {
        BaseEngineInfo engineInfo = getEngineInfo();
        if (engineInfo == null || engineInfo.getEngineSounds() == null) {
            return;
        }

        if (engineSounds.isEmpty()) { //Sounds are not initialized
            engineInfo.getEngineSounds().forEach(engineSound -> engineSounds.put(engineSound.id, new EngineSound(engineSound, entity, this)));
        }

        if (!isEngineStarted()) {
            if (currentEngineSound != null) {
                SOUND_HANDLER.stopSound(currentEngineSound);
            }
            currentEngineSound = lastEngineSound = null;
            return;
        }
        // engine is started: check what sound should be playing

        HmMinecraftClient client = HermesPlatform.getInstance().getClient();
        boolean forInterior = client.hm$getGameSettings().hm$isFirstPersonView() && entity.isRidingOrBeingRiddenBy(client.hm$getPlayer());
        float rpm = getEngineProperty(VehicleEntityProperties.EnumEngineProperties.REVS) * engineInfo.getMaxRevs();
        lastEngineSound = currentEngineSound;
        if (currentEngineSound == null || !currentEngineSound.shouldPlay(rpm, forInterior)) {
            for (EngineSound sound : engineSounds.values()) {
                if (sound.shouldPlay(rpm, forInterior)) {
                    this.currentEngineSound = sound;
                    break;
                }
            }
        }

        if (currentEngineSound == lastEngineSound) {
            return;
        }
        // if playing sound changed

        if (lastEngineSound != null) {
            SOUND_HANDLER.stopSound(lastEngineSound);
        }
        if (currentEngineSound != null) {
            if (currentEngineSound.getState() == EnumSoundState.STOPPING) { //already playing
                currentEngineSound.onStarted();
            } else {
                SOUND_HANDLER.playStreamingSound(JmeVector3fPool.get(currentEngineSound.getPosX(), currentEngineSound.getPosY(), currentEngineSound.getPosZ()), currentEngineSound);
            }
        }
    }

    public float getSoundPitch() {
        return getEngineProperty(VehicleEntityProperties.EnumEngineProperties.REVS);
    }
}