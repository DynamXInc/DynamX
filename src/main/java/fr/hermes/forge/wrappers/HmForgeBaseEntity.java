package fr.hermes.forge.wrappers;

import fr.dynamx.core.utils.optimization.MutableBoundingBox;
import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.entities.HmEntityFactory;
import fr.hermes.api.mc.entities.HmEntityLogic;
import fr.hermes.api.mc.entities.HmModEntity;
import fr.hermes.api.mc.items.HmItemStack;
import fr.hermes.api.mc.world.HmWorld;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

// TODO REGISTER, HOW TO HANDLE RENDERING, ETC
public class HmForgeBaseEntity<TLogic extends HmEntityLogic> extends Entity implements HmModEntity<TLogic>, IEntityAdditionalSpawnData {
    @Getter
    private TLogic logic;

    public HmForgeBaseEntity(World worldIn) {
        super(worldIn);
    }

    public HmForgeBaseEntity(World worldIn, HmEntityFactory<TLogic> entityFactory) {
        super(worldIn);
        logic = entityFactory.createEntityLogic((HmWorld) world, (HmEntity) this);
    }

    @Override
    protected void entityInit() {
        logic.onMcEntityInit();
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbtTagCompound) {
        // TODO logic instantiation
        logic.readFromNbt(nbtTagCompound);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbtTagCompound) {
        // TODO logic save
        logic.writeToNbt(nbtTagCompound);
    }

    @Override
    public void readSpawnData(ByteBuf byteBuf) {
        logic.readSpawnData(byteBuf);
    }

    @Override
    public void writeSpawnData(ByteBuf byteBuf) {
        logic.writeSpawnData(byteBuf);
    }

    @Override
    public void setDead() {
        super.setDead();
        logic.onSetDead();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        // TODO special prevPos logic from original PhysicsEntity wasn't kept
        logic.onUpdate();
    }

    @Override
    public AxisAlignedBB getEntityBoundingBox() {
        MutableBoundingBox bb = logic.getBoundingBox();
        if(bb != null) {
            // TODO needs optimization
            return bb.toBB();
        }
        return super.getEntityBoundingBox();
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        logic.onRemovedFromWorld();
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        logic.onAddPassenger((HmEntity) passenger);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        logic.onRemovePassenger((HmEntity) passenger);
    }

    @Override
    public void updatePassenger(Entity passenger) {
        if (!logic.updatePassenger((HmEntity) passenger)) {
            super.updatePassenger(passenger);
        }
    }

    @Override
    public void applyOrientationToEntity(Entity entityToUpdate) {
        if (!logic.updatePassengerRotation((HmEntity) entityToUpdate)) {
            super.applyOrientationToEntity(entityToUpdate);
        }
    }

    @Override
    public boolean canPassengerSteer() {
        return logic.canPassengerSteer() && super.canPassengerSteer();
    }

    @Override
    public boolean shouldRiderSit() {
        return logic.shouldPassengersSit();
    }

    @Override
    protected boolean canFitPassenger(Entity passenger) {
        Boolean canFit = logic.canFitPassenger((HmEntity) passenger);
        if(canFit != null) {
            return canFit;
        }
        return super.canFitPassenger(passenger);
    }

    @Override
    public ItemStack getPickedResult(RayTraceResult target) {
        HmItemStack itemStack = logic.getPickedResult();
        return itemStack != null ? (ItemStack) (Object) itemStack : super.getPickedResult(target);
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        Boolean inRange = logic.isInRangeToRenderDist(distance);
        if(inRange != null) {
            return inRange;
        }
        return super.isInRangeToRenderDist(distance);
    }

    @Override
    public int getBrightnessForRender() {
        int brightness = super.getBrightnessForRender();
        return brightness != -1 ? brightness : super.getBrightnessForRender();
    }

    @Override
    public String getName() {
        String name = logic.getName();
        if(name != null) {
            return name;
        }
        return super.getName();
    }
}
