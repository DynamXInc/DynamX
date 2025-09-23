package fr.dynamx.core.common.entities;

import com.jme3.math.Vector3f;
import fr.dynamx.core.common.blocks.TEDynamXBlock;
import fr.dynamx.core.common.contentpack.parts.PartBlockSeat;
import fr.dynamx.core.utils.EnumSeatPlayerPosition;
import fr.dynamx.core.utils.maths.DynamXGeometry;
import fr.hermes.forge.JmeVector3fPool;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

public class SeatEntity extends Entity implements IEntityAdditionalSpawnData {
    protected TEDynamXBlock block;
    protected PartBlockSeat<?> mySeat;
    protected byte seatID;

    public SeatEntity(World worldIn, byte seatID) {
        super(worldIn);
        this.seatID = seatID;
        this.noClip = true;
        this.setNoGravity(true);
    }

    public SeatEntity(World worldIn) {
        super(worldIn);
    }

    @Override
    protected void entityInit() {
        setSize(0.6f, 0.6f);
    }

    @Override
    public void updatePassenger(Entity passenger) {
        if (block == null || mySeat == null) {
            return;
        }
        JmeVector3fPool.openPool();
        Vector3f posVec = DynamXGeometry.rotateVectorByQuaternion(mySeat.getPosition(), block.getCollidableRotation());
        posVec.addLocal(block.getRelativeTranslation());
        passenger.setPosition(posX + posVec.x, posY + posVec.y, posZ + posVec.z);
        JmeVector3fPool.closePool();
    }

    /**
     * Rotates the passenger, limiting his field of view to avoid stiff necks
     */
    @Override
    public void applyOrientationToEntity(Entity passenger) {
        if (mySeat == null || !mySeat.shouldLimitFieldOfView()) {
            return;
        }
        float blockYaw = block.getPackInfo() == null ? 0 : (block.getPackInfo().getRotation().y - block.getRelativeRotation().y + block.getRotation() * 22.5f);
        passenger.setRenderYawOffset(blockYaw);
        float f = MathHelper.wrapDegrees(passenger.rotationYaw - blockYaw);
        float f1 = MathHelper.clamp(f, mySeat.getMinYaw(), mySeat.getMaxYaw());
        passenger.prevRotationYaw += f1 - f;
        passenger.rotationYaw += f1 - f;
        passenger.setRotationYawHead(passenger.rotationYaw);

        float f2 = MathHelper.wrapDegrees(passenger.rotationPitch);
        float f3 = MathHelper.clamp(f2, mySeat.getMinPitch(), mySeat.getMaxPitch());
        passenger.rotationPitch = f3;
        f2 = MathHelper.wrapDegrees(passenger.prevRotationPitch);
        f3 = MathHelper.clamp(f2, mySeat.getMinPitch(), mySeat.getMaxPitch());
        passenger.prevRotationPitch = f3;
    }

    @Override
    public boolean canPassengerSteer() {
        return false;
    }

    @Override
    public boolean shouldRiderSit() {
        return mySeat == null || mySeat.getPlayerPosition() == EnumSeatPlayerPosition.SITTING;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (ticksExisted % 20 != 0) {
            return;
        }
        TileEntity te = world.getTileEntity(getPosition());
        if (te instanceof TEDynamXBlock) {
            block = (TEDynamXBlock) te;
            mySeat = (PartBlockSeat<?>) block.getPackInfo().getPartsByType(PartBlockSeat.class).stream().filter(s -> ((PartBlockSeat<?>) s).getId() == seatID).findFirst().orElse(null);
            if (mySeat != null)
                return;
        }
        setDead();
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbtTagCompound) {
        seatID = nbtTagCompound.getByte("SeatID");
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbtTagCompound) {
        nbtTagCompound.setByte("SeatID", seatID);
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeByte(seatID);
    }

    @Override
    public void readSpawnData(ByteBuf additionalData) {
        seatID = additionalData.readByte();
    }
}
