package fr.dynamx.common.entities.modules;

import fr.dynamx.api.audio.EnumSoundState;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SynchronizationRules;
import fr.dynamx.client.handlers.hud.CarController;
import fr.dynamx.client.sound.EngineSound;
import fr.dynamx.common.contentpack.type.vehicle.CarEngineInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.api.network.sync.SynchronizedEntityVariable;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.modules.EnginePhysicsHandler;
import fr.dynamx.common.physics.entities.parts.engine.AutomaticGearboxHandler;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static fr.dynamx.client.ClientProxy.SOUND_HANDLER;

/**
 * Basic {@link IEngineModule} implementation for cars <br>
 * Works with an {@link AutomaticGearboxHandler} and a {@link fr.dynamx.common.entities.modules.WheelsModule}
 *
 * @see fr.dynamx.api.entities.VehicleEntityProperties.EnumEngineProperties
 * @see EnginePhysicsHandler
 */
@SynchronizedEntityVariable.SynchronizedPhysicsModule(modid = DynamXConstants.ID)
public class CarEngineModule extends BasicEngineModule implements IPackInfoReloadListener {
    //TODO CLEAN ENGINE CODE
    protected CarEngineInfo engineInfo;
    protected EnginePhysicsHandler physicsHandler;

    /**
     * The active speed limit, or Float.MAX_VALUE
     */
    @SynchronizedEntityVariable(name = "speed_limit")
    private final EntityVariable<Float> speedLimit = new EntityVariable<>(SynchronizationRules.CONTROLS_TO_SPECTATORS, Float.MAX_VALUE);

    public CarEngineModule(BaseVehicleEntity<? extends BaseVehiclePhysicsHandler<?>> entity, CarEngineInfo engineInfo) {
        super(entity);
        this.engineInfo = engineInfo;
    }

    public CarEngineInfo getEngineInfo() {
        return engineInfo;
    }

    @Override
    public void onPackInfosReloaded() {
        this.engineInfo = entity.getPackInfo().getSubPropertyByType(CarEngineInfo.class);
        if (physicsHandler != null)
            physicsHandler.onPackInfosReloaded();
        sounds.clear();
    }

    public EnginePhysicsHandler getPhysicsHandler() {
        return physicsHandler;
    }

    @Override
    public float getSoundPitch() {
        return getEngineProperty(VehicleEntityProperties.EnumEngineProperties.REVS);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IVehicleController createNewController() {
        return new CarController(entity, this);
    }

    @Override
    public void initPhysicsEntity(@Nullable BaseVehiclePhysicsHandler<?> handler) {
        if (handler != null) {
            physicsHandler = new EnginePhysicsHandler(this, handler, handler.getWheels());
        }
    }

    @Override
    public void preUpdatePhysics(boolean simulatePhysics) {
        if (simulatePhysics) {
            physicsHandler.update();
        }
    }

    @Override
    public void postUpdatePhysics(boolean simulatingPhysics) {
        super.postUpdatePhysics(simulatingPhysics);
        if (simulatingPhysics) {
            this.getEngineProperties()[VehicleEntityProperties.EnumEngineProperties.REVS.ordinal()] = physicsHandler.getEngine().getRevs();
            this.getEngineProperties()[VehicleEntityProperties.EnumEngineProperties.ACTIVE_GEAR.ordinal()] = physicsHandler.getGearBox().getActiveGearNum();
        }
    }

    /**
     * @return The max speed, set by the current driver, or from the vehicle info
     */
    public float getRealSpeedLimit() {
        return speedLimit.get() == Float.MAX_VALUE ? entity.getPackInfo().getVehicleMaxSpeed() : speedLimit.get();
    }

    //Sounds

    public final Map<Integer, EngineSound> sounds = new HashMap<>();
    private EngineSound lastVehicleSound;
    private EngineSound currentVehicleSound;

    public EngineSound getCurrentEngineSound() {
        return currentVehicleSound;
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected String getStartingSound(boolean forInterior) {
        return forInterior ? engineInfo.startingSoundInterior : engineInfo.startingSoundExterior;
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void updateSounds() {
        if (engineInfo != null && engineInfo.getEngineSounds() != null) {
            if (sounds.isEmpty()) { //Sounds are not initialized
                engineInfo.getEngineSounds().forEach(engineSound -> sounds.put(engineSound.id, new EngineSound(engineSound, entity, this)));
            }
            if (isEngineStarted()) {
                boolean forInterior = Minecraft.getMinecraft().gameSettings.thirdPersonView == 0 && entity.isRidingOrBeingRiddenBy(Minecraft.getMinecraft().player);
                float rpm = getEngineProperty(VehicleEntityProperties.EnumEngineProperties.REVS) * engineInfo.getMaxRevs();
                lastVehicleSound = currentVehicleSound;
                if (currentVehicleSound == null || !currentVehicleSound.shouldPlay(rpm, forInterior)) {
                    sounds.forEach((id, vehicleSound) -> {
                        if (vehicleSound.shouldPlay(rpm, forInterior)) {
                            this.currentVehicleSound = vehicleSound;
                        }
                    });
                }
                if (currentVehicleSound != lastVehicleSound) //if playing sound changed
                {
                    if (lastVehicleSound != null)
                        SOUND_HANDLER.stopSound(lastVehicleSound);
                    if (currentVehicleSound != null) {
                        if (currentVehicleSound.getState() == EnumSoundState.STOPPING) //already playing
                            currentVehicleSound.onStarted();
                        else
                            SOUND_HANDLER.playStreamingSound(Vector3fPool.get(currentVehicleSound.getPosX(), currentVehicleSound.getPosY(), currentVehicleSound.getPosZ()), currentVehicleSound);
                    }
                }
            } else {
                if (currentVehicleSound != null)
                    SOUND_HANDLER.stopSound(currentVehicleSound);
                currentVehicleSound = lastVehicleSound = null;
            }
        }
    }

    /**
     * The active speed limit, or Float.MAX_VALUE
     */
    public float getSpeedLimit() {
        return speedLimit.get();
    }

    public void setSpeedLimit(float speedLimit) {
        this.speedLimit.set(speedLimit);
    }
}