package fr.dynamx.common.entities.modules;

import fr.dynamx.api.audio.EnumSoundState;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.entities.modules.IEngineModule;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.physics.entities.IEnginePhysicsHandler;
import fr.dynamx.client.ClientProxy;
import fr.dynamx.client.handlers.hud.HelicopterController;
import fr.dynamx.client.sound.EngineSound;
import fr.dynamx.common.contentpack.type.vehicle.EngineInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.network.sync.vars.VehicleSynchronizedVariables;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.modules.EnginePhysicsHandler;
import fr.dynamx.common.physics.entities.modules.HelicopterEnginePhysicsHandler;
import fr.dynamx.common.physics.entities.parts.engine.AutomaticGearboxHandler;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.dynamx.client.ClientProxy.SOUND_HANDLER;

/**
 * Basic {@link IEngineModule} implementation for cars <br>
 * Works with an {@link AutomaticGearboxHandler} and a {@link WheelsModule}
 *
 * @see VehicleEntityProperties.EnumEngineProperties
 * @see EnginePhysicsHandler
 */
public class HelicopterEngineModule implements IEngineModule<AbstractEntityPhysicsHandler<?, ?>>, IPhysicsModule.IPhysicsUpdateListener, IPhysicsModule.IEntityUpdateListener {
    protected final BaseVehicleEntity<? extends BaseVehiclePhysicsHandler<?>> entity;
    protected final EngineInfo engineInfo;
    protected HelicopterEnginePhysicsHandler physicsHandler;

    //Handbrake on spawn
    private int controls = 2;
    private float power = 0;


    public void setPower(float power) {
        this.power = MathHelper.clamp(power, 0, 1);
    }

    public float getPower() {
        return power;
    }

    /**
     * 0  = Speed(KM/H) , 1 = RPM, 2 = maxRPM, 3 = gearNumber, 4 = MaxSpeed, 5 = power, 6 = brake
     */
    private float[] engineProperties = new float[VehicleEntityProperties.EnumEngineProperties.values().length];

    public HelicopterEngineModule(BaseVehicleEntity<? extends BaseVehiclePhysicsHandler<?>> entity, EngineInfo engineInfo) {
        this.entity = entity;
        this.engineInfo = engineInfo;
    }

    /**
     * These vars are automatically synchronised from server (or driver) to others
     *
     * @return All engine properties, see {@link VehicleEntityProperties.EnumEngineProperties}
     */
    @Override
    public float[] getEngineProperties() {
        return engineProperties;
    }

    public float getEngineProperty(VehicleEntityProperties.EnumEngineProperties engineProperty){
        return engineProperties[engineProperty.ordinal()];
    }

    public EngineInfo getEngineInfo() {
        return engineInfo;
    }

    @Override
    public IEnginePhysicsHandler getPhysicsHandler() {
        return physicsHandler;
    }

    /**
     * Used for synchronization, don't use this
     */
    @Override
    public void setEngineProperties(float[] engineProperties) {
        this.engineProperties = engineProperties;
    }

    public boolean isAccelerating() {
        return (EnginePhysicsHandler.inTestFullGo) || (isEngineStarted() && (controls & 1) == 1);
    }

    public boolean isHandBraking() {
        return (controls & 2) == 2;
    }

    public boolean isReversing() {
        return isEngineStarted() && (controls & 4) == 4;
    }

    public boolean isTurningLeft() {
        return (controls & 8) == 8;
    }

    public boolean isTurningRight() {
        return (controls & 16) == 16;
    }

    @Override
    public void setEngineStarted(boolean started) {
        if (started) {
            setControls(controls | 32);
        } else {
            setControls((Integer.MAX_VALUE - 32) & controls);
        }
    }

    @Override
    public boolean isEngineStarted() {
        return (EnginePhysicsHandler.inTestFullGo) || ((controls & 32) == 32);
    }

    public int getControls() {
        return controls;
    }

    /**
     * Set all engine controls <br>
     * If called on client side and if the engine is switched on, plays the starting sound
     */
    public void setControls(int controls) {
        if (entity.world.isRemote && entity.ticksExisted > 60 && !this.isEngineStarted() && (controls & 32) == 32)
            playStartingSound();
        this.controls = controls;
    }

    /**
     * Resets all controls except engine on/off state
     */
    public void resetControls() {
        setControls(controls & 32 | (controls & 2));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IVehicleController createNewController() {
        return new HelicopterController(entity, this);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        if (tag.getBoolean("isEngineStarted"))
            setControls(controls | 32); //set engine on
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setBoolean("isEngineStarted", isEngineStarted());
    }

    @Override
    public void initPhysicsEntity(AbstractEntityPhysicsHandler<?, ?> handler) {
        if (handler != null) {
            physicsHandler = new HelicopterEnginePhysicsHandler(this, (BaseVehiclePhysicsHandler<?>) handler, ((BaseVehiclePhysicsHandler<?>) handler).getPropulsion());
        }
    }

    @Override
    public void preUpdatePhysics(boolean simulatingPhysics) {
        if(simulatingPhysics)
            physicsHandler.update();
    }

    @Override
    public void postUpdatePhysics(boolean simulatingPhysics) {
        if (simulatingPhysics) {
            this.engineProperties[VehicleEntityProperties.EnumEngineProperties.SPEED.ordinal()] = entity.physicsHandler.getSpeed(BaseVehiclePhysicsHandler.SpeedUnit.KMH);
            this.engineProperties[VehicleEntityProperties.EnumEngineProperties.REVS.ordinal()] = physicsHandler.getEngine().getRevs();
            //this.engineProperties[VehicleEntityProperties.EnumEngineProperties.MAXREVS.ordinal()] = physicEntity.getEngine().getMaxRevs();
            this.engineProperties[VehicleEntityProperties.EnumEngineProperties.ACTIVE_GEAR.ordinal()] = physicsHandler.upForce;//physicsHandler.getGearBox().getActiveGearNum();
            //this.engineProperties[VehicleEntityProperties.EnumEngineProperties.MAXSPEED.ordinal()] = physicEntity.getGearBox().getMaxSpeed(Vehicle.SpeedUnit.KMH);
            //this.engineProperties[VehicleEntityProperties.EnumEngineProperties.POWER.ordinal()] = physicsHandler.getEngine().getPower();
            //this.engineProperties[VehicleEntityProperties.EnumEngineProperties.BRAKING.ordinal()] = physicsHandler.getEngine().getBraking();
        }
    }

    @Override
    public void removePassenger(Entity passenger) {
        if (entity.getControllingPassenger() == null) {
            resetControls();
        }
    }

    @Override
    public void addSynchronizedVariables(Side side, SimulationHolder simulationHolder, List<ResourceLocation> variables) {
        if (simulationHolder.isPhysicsAuthority(side) || simulationHolder.ownsControls(side))
            variables.add(VehicleSynchronizedVariables.HelicopterControls.NAME);
    }

    //Sounds

    public final Map<Integer, EngineSound> sounds = new HashMap<>();
    private EngineSound lastVehicleSound;
    private EngineSound currentVehicleSound;

    public EngineSound getCurrentEngineSound() {
        return currentVehicleSound;
    }

    @SideOnly(Side.CLIENT)
    private void playStartingSound() {
        boolean forInterior = Minecraft.getMinecraft().gameSettings.thirdPersonView == 0 && entity.isRidingOrBeingRiddenBy(Minecraft.getMinecraft().player);
        ClientProxy.SOUND_HANDLER.playSingleSound(entity.physicsPosition, forInterior ? engineInfo.startingSoundInterior : engineInfo.startingSoundExterior, 1, 1);
    }

    @Override
    public boolean listenEntityUpdates(Side side) {
        return side.isClient();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateEntity() {
        if (!MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.UpdateVehicleSoundEntityEvent(entity, this, PhysicsEntityEvent.Phase.PRE))) {
            if (entity.getPackInfo() != null) {
                if (engineInfo != null && engineInfo.getEngineSounds() != null) {
                    if (sounds.isEmpty()) { //Sounds are not initialized
                        engineInfo.getEngineSounds().forEach(engineSound -> sounds.put(engineSound.id, new EngineSound(engineSound, entity, this)));
                    }
                    if (isEngineStarted()) {
                        if (engineProperties != null) {
                            boolean forInterior = Minecraft.getMinecraft().gameSettings.thirdPersonView == 0 && entity.isRidingOrBeingRiddenBy(Minecraft.getMinecraft().player);
                            float rpm = engineProperties[VehicleEntityProperties.EnumEngineProperties.REVS.ordinal()] * engineInfo.getMaxRevs();
                            lastVehicleSound = currentVehicleSound;
                            if (currentVehicleSound == null || !currentVehicleSound.shouldPlay(rpm, forInterior)) {
                                sounds.forEach((id, vehicleSound) -> {
                                    if (vehicleSound.shouldPlay(rpm, forInterior)) {
                                        this.currentVehicleSound = vehicleSound;
                                    }
                                });
                            }
                        }
                        if (currentVehicleSound != lastVehicleSound) //if playing sound changed
                        {
                            if (lastVehicleSound != null)
                                SOUND_HANDLER.stopSound(lastVehicleSound);
                            if (currentVehicleSound != null) {
                                if (currentVehicleSound.getState() == EnumSoundState.STOPPING) //already playing
                                    currentVehicleSound.onStarted();
                                else
                                    SOUND_HANDLER.playLoopingSound(Vector3fPool.get(currentVehicleSound.getPosX(), currentVehicleSound.getPosY(), currentVehicleSound.getPosZ()), currentVehicleSound);
                            }
                        }
                    } else {
                        if (currentVehicleSound != null)
                            SOUND_HANDLER.stopSound(currentVehicleSound);
                        currentVehicleSound = lastVehicleSound = null;
                    }
                }
            }
            MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.UpdateVehicleSoundEntityEvent(entity, this, PhysicsEntityEvent.Phase.POST));
        }
    }
}