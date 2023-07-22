package fr.dynamx.common.entities.modules;

import fr.dynamx.api.audio.EnumSoundState;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SynchronizationRules;
import fr.dynamx.api.network.sync.SynchronizedEntityVariable;
import fr.dynamx.client.handlers.hud.HelicopterController;
import fr.dynamx.client.sound.EngineSound;
import fr.dynamx.common.contentpack.type.vehicle.BaseEngineInfo;
import fr.dynamx.common.contentpack.type.vehicle.HelicopterPhysicsInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.network.sync.variables.EntityFloatArrayVariable;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.modules.EnginePhysicsHandler;
import fr.dynamx.common.physics.entities.parts.engine.AutomaticGearboxHandler;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

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
public class HelicopterEngineModule extends BasicEngineModule {
    @Getter
    @SynchronizedEntityVariable(name = "roll_controls")
    private EntityFloatArrayVariable rollControls = new EntityFloatArrayVariable(SynchronizationRules.CONTROLS_TO_SPECTATORS, new float[2]);
    @SynchronizedEntityVariable(name = "power")
    private EntityVariable<Float> power = new EntityVariable<Float>(SynchronizationRules.CONTROLS_TO_SPECTATORS, 0f);
    private HelicopterPhysicsInfo physicsInfo;
    private BaseEngineInfo engineInfo;

    public HelicopterEngineModule(BaseVehicleEntity<? extends BaseVehiclePhysicsHandler<?>> entity) {
        super(entity);
        physicsInfo = entity.getPackInfo().getSubPropertyByType(HelicopterPhysicsInfo.class);
        engineInfo = entity.getPackInfo().getSubPropertyByType(BaseEngineInfo.class);
    }

    public void setPower(float power) {
        this.power.set(MathHelper.clamp(power, 0, 1));
    }

    public float getPower() {
        return power.get();
    }

    @Override
    public void onEngineSwitchedOff() {
        super.onEngineSwitchedOff();
        power.set(0f);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IVehicleController createNewController() {
        return new HelicopterController(entity, this);
    }


    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        power.set(tag.getFloat("power"));
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setFloat("power", power.get());
    }

    public final Map<Integer, EngineSound> sounds = new HashMap<>();
    private EngineSound lastVehicleSound;
    private EngineSound currentVehicleSound;


    public EngineSound getCurrentEngineSound() {
        return currentVehicleSound;
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected String getStartingSound(boolean forInterior) {
        return engineInfo == null ? "null" : forInterior ? engineInfo.startingSoundInterior : engineInfo.startingSoundExterior;
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
                float power = this.power.get() * 1000;
                lastVehicleSound = currentVehicleSound;
                if (currentVehicleSound == null || !currentVehicleSound.shouldPlay(power, forInterior)) {
                    sounds.forEach((id, vehicleSound) -> {
                        if (vehicleSound.shouldPlay(power, forInterior)) {
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

    @Override
    public boolean listenEntityUpdates(Side side) {
        return side.isClient();
    }

    @Override
    public float getSoundPitch() {
        return power.get();
    }

}