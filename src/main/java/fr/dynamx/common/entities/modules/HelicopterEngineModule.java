package fr.dynamx.common.entities.modules;

import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SynchronizationRules;
import fr.dynamx.api.network.sync.SynchronizedEntityVariable;
import fr.dynamx.client.handlers.hud.HelicopterController;
import fr.dynamx.client.sound.EngineSound;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.network.sync.variables.EntityFloatArrayVariable;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.modules.EnginePhysicsHandler;
import fr.dynamx.common.physics.entities.parts.engine.AutomaticGearboxHandler;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;

/**
 * Basic {@link IEngineModule} implementation for cars <br>
 * Works with an {@link AutomaticGearboxHandler} and a {@link WheelsModule}
 *
 * @see VehicleEntityProperties.EnumEngineProperties
 * @see EnginePhysicsHandler
 */
@SynchronizedEntityVariable.SynchronizedPhysicsModule
public class HelicopterEngineModule extends BasicEngineModule {
    @Getter
    @SynchronizedEntityVariable(name = "roll_controls")
    private EntityFloatArrayVariable rollControls = new EntityFloatArrayVariable(SynchronizationRules.CONTROLS_TO_SPECTATORS, new float[2]);
    @SynchronizedEntityVariable(name = "power")
    private EntityVariable<Float> power = new EntityVariable<Float>(SynchronizationRules.CONTROLS_TO_SPECTATORS, 0f);

    public HelicopterEngineModule(BaseVehicleEntity<? extends BaseVehiclePhysicsHandler<?>> entity) {
        super(entity);
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

    //TODO HELICOPTER SOUNDS

    @Override
    @SideOnly(Side.CLIENT)
    protected String getStartingSound(boolean forInterior) {
        return "todo";
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void updateSounds() {

    }

    @Override
    @SideOnly(Side.CLIENT)
    public float getSoundPitch() {
        //TODO
        return 0;
    }
}