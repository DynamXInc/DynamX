package fr.dynamx.common.entities.modules.engines;

import fr.dynamx.api.audio.EnumSoundState;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SynchronizationRules;
import fr.dynamx.api.network.sync.SynchronizedEntityVariable;
import fr.dynamx.client.handlers.hud.CarController;
import fr.dynamx.client.sound.ReversingSound;
import fr.dynamx.common.contentpack.type.vehicle.CarEngineInfo;
import fr.dynamx.common.contentpack.type.vehicle.CarInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.modules.EnginePhysicsHandler;
import fr.dynamx.common.physics.entities.parts.engine.AutomaticGearboxHandler;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

import static fr.dynamx.client.ClientProxy.SOUND_HANDLER;

/**
 * {@link BasicEngineModule} implementation for cars <br>
 * Works with an {@link AutomaticGearboxHandler} and a {@link fr.dynamx.common.entities.modules.WheelsModule}
 *
 * @see fr.dynamx.api.entities.VehicleEntityProperties.EnumEngineProperties
 * @see EnginePhysicsHandler
 */
@SynchronizedEntityVariable.SynchronizedPhysicsModule(modid = DynamXConstants.ID)
public class CarEngineModule extends BasicEngineModule implements IPackInfoReloadListener {
    //TODO CLEAN ENGINE CODE
    @Getter
    protected CarEngineInfo engineInfo;
    @Getter
    protected EnginePhysicsHandler physicsHandler;

    protected ReversingSound reversingSound;

    /**
     * The active speed limit, or Float.MAX_VALUE
     */
    @SynchronizedEntityVariable(name = "speed_limit")
    private final EntityVariable<Float> speedLimit = new EntityVariable<>(SynchronizationRules.CONTROLS_TO_SPECTATORS, Float.MAX_VALUE);

    public CarEngineModule(BaseVehicleEntity<? extends BaseVehiclePhysicsHandler<?>> entity, CarEngineInfo engineInfo) {
        super(entity);
        this.engineInfo = engineInfo;
    }

    @Override
    public void onPackInfosReloaded() {
        this.engineInfo = entity.getPackInfo().getSubPropertyByType(CarEngineInfo.class);
        if (physicsHandler != null)
            physicsHandler.onPackInfosReloaded();
        super.onPackInfosReloaded();
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

    /**
     * The active speed limit, or Float.MAX_VALUE
     */
    public float getSpeedLimit() {
        return speedLimit.get();
    }

    public void setSpeedLimit(float speedLimit) {
        this.speedLimit.set(speedLimit);
    }

    @Override
    public void setControls(int controls) {
        if (entity.world.isRemote && entity.ticksExisted > 60 && entity.getPackInfo() instanceof CarInfo) {
            if (!this.isHandBraking() && (controls & 32) == 32)
                playHandbrakeSound(true);
            else if (this.isHandBraking() && (controls & 32) != 32)
                playHandbrakeSound(false);
        }
        super.setControls(controls);
    }

    @Override
    public void updateSounds() {
        super.updateSounds();
        if (isReversing() && getEngineProperty(VehicleEntityProperties.EnumEngineProperties.ACTIVE_GEAR) == -1) {
            playReversingSound();
        }
    }

    @SideOnly(Side.CLIENT)
    protected void playHandbrakeSound(boolean on) {
        String sound = on ? ((CarInfo) entity.getPackInfo()).getHandbrakeSoundOn() : ((CarInfo) entity.getPackInfo()).getHandbrakeSoundOff(); // It is assumed that entity is a CarEntity, and that it has a CarInfo
        if (sound != null)
            SOUND_HANDLER.playSingleSound(entity.physicsPosition, sound, 1, 1);
    }

    @SideOnly(Side.CLIENT)
    protected void playReversingSound() {
        if (getEngineInfo() == null)
            return;
        String sound = ((CarInfo) entity.getPackInfo()).getReversingSound(); // It is assumed that entity is a CarEntity, and that it has a CarInfo
        if (sound == null)
            return;
        boolean forInterior = Minecraft.getMinecraft().gameSettings.thirdPersonView == 0 && entity.isRidingOrBeingRiddenBy(Minecraft.getMinecraft().player);
        if (reversingSound != null && reversingSound.getState() == EnumSoundState.PLAYING) {
            if (forInterior == reversingSound.isInterior())
                return;
            SOUND_HANDLER.stopSound(reversingSound);
        }
        reversingSound = new ReversingSound(sound, entity, this, forInterior);
        SOUND_HANDLER.playStreamingSound(Vector3fPool.get(reversingSound.getPosX(), reversingSound.getPosY(), reversingSound.getPosZ()), reversingSound);
    }
}
