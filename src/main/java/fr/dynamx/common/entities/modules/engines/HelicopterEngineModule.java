package fr.dynamx.common.entities.modules.engines;

import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SynchronizationRules;
import fr.dynamx.api.network.sync.SynchronizedEntityVariable;
import fr.dynamx.client.handlers.hud.HelicopterController;
import fr.dynamx.common.contentpack.type.vehicle.BaseEngineInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.network.sync.variables.EntityFloatArrayVariable;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.utils.DynamXConstants;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SynchronizedEntityVariable.SynchronizedPhysicsModule(modid = DynamXConstants.ID)
public class HelicopterEngineModule extends BasicEngineModule {
    @Getter
    @SynchronizedEntityVariable(name = "roll_controls")
    private EntityFloatArrayVariable rollControls = new EntityFloatArrayVariable(SynchronizationRules.CONTROLS_TO_SPECTATORS, new float[2]);
    @SynchronizedEntityVariable(name = "power")
    private EntityVariable<Float> power = new EntityVariable<Float>(SynchronizationRules.CONTROLS_TO_SPECTATORS, 0f);
    @Getter
    private BaseEngineInfo engineInfo;

    public HelicopterEngineModule(BaseVehicleEntity<? extends BaseVehiclePhysicsHandler<?>> entity) {
        super(entity);
        engineInfo = entity.getPackInfo().getSubPropertyByType(BaseEngineInfo.class);
    }

    public void setPower(float power) {
        this.power.set(MathHelper.clamp(power, 0, 1));
    }

    public float getPower() {
        return power.get();
    }

    @Override
    public void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (entity.getControllingPassenger() == null && passenger instanceof EntityPlayer && !((EntityPlayer) passenger).capabilities.isCreativeMode) {
            power.set(0f);
        }
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

    @Override
    public boolean listenEntityUpdates(Side side) {
        return side.isClient();
    }

    @Override
    public float getSoundPitch() {
        return power.get();
    }
}