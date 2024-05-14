package fr.dynamx.common.entities.modules.engines;

import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.network.sync.SynchronizationRules;
import fr.dynamx.api.network.sync.SynchronizedEntityVariable;
import fr.dynamx.client.handlers.hud.HelicopterController;
import fr.dynamx.client.handlers.hud.PlaneController;
import fr.dynamx.common.contentpack.type.vehicle.BaseEngineInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.network.sync.variables.EntityFloatArrayVariable;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.utils.DynamXConstants;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SynchronizedEntityVariable.SynchronizedPhysicsModule(modid = DynamXConstants.ID)
public class PlaneEngineModule extends BasicEngineModule {
    @Getter
    @SynchronizedEntityVariable(name = "roll_controls")
    private EntityFloatArrayVariable rollControls = new EntityFloatArrayVariable(SynchronizationRules.CONTROLS_TO_SPECTATORS, new float[2]);
    //@SynchronizedEntityVariable(name = "power")
    //private EntityVariable<Float> power = new EntityVariable<Float>(SynchronizationRules.CONTROLS_TO_SPECTATORS, 0f);
    //private int engineStartupTime;
    //private int startupTimer;
    @Getter
    private BaseEngineInfo engineInfo;

    public PlaneEngineModule(BaseVehicleEntity<? extends BaseVehiclePhysicsHandler<?>> entity) {
        super(entity);
        onPackInfosReloaded();
    }

    @Override
    public void onPackInfosReloaded() {
        super.onPackInfosReloaded();
        engineInfo = entity.getPackInfo().getSubPropertyByType(BaseEngineInfo.class);
        //engineStartupTime = entity.getPackInfo().getSubPropertyByType(HelicopterPhysicsInfo.class).getEngineStartupTime();
    }

    /*public void setPower(float power) {
        this.power.set(MathHelper.clamp(power, 0, 1));
    }

    public float getPower() {
        return power.get();
    }*/

    @Override
    public void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (entity.getControllingPassenger() == null && passenger instanceof EntityPlayer && !((EntityPlayer) passenger).capabilities.isCreativeMode) {
            //power.set(0f);
        }
    }

    @Override
    public void onEngineSwitchedOn() {
        super.onEngineSwitchedOn();
        //startupTimer = engineStartupTime;
        //power.set(1f/engineStartupTime);
    }

    @Override
    public void onEngineSwitchedOff() {
        super.onEngineSwitchedOff();
        //startupTimer = (int) (-20 * power.get());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IVehicleController createNewController() {
        return new PlaneController(entity, this);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        //power.set(tag.getFloat("power"));
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        //tag.setFloat("power", power.get());
    }

    @Override
    public boolean listenEntityUpdates(Side side) {
        return true;
    }

    @Override
    public void updateEntity() {
        /*if(startupTimer > 0) {
            startupTimer--;
            power.set(1f - (float)startupTimer/engineStartupTime);
        } else if(startupTimer < 0) {
            startupTimer++;
            power.set(-(float)startupTimer/20);
        }*/
        if (entity.world.isRemote) //sounds
            super.updateEntity();
    }

    @Override
    public float getSoundPitch() {
        return 1;//power.get();
    }
}