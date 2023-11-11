package fr.dynamx.common.entities;

import com.jme3.math.Vector3f;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.contentpack.parts.PartBlockSeat;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class SeatEntity extends Entity {
    private TEDynamXBlock block;
    private byte seatID;

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

    }

    @Override
    public void updatePassenger(Entity passenger) {
        if(block == null) return;
        PartBlockSeat<?> seat = (PartBlockSeat<?>) block.getBlockObjectInfo().getPartsByType(PartBlockSeat.class).stream().filter(s -> ((PartBlockSeat<?>) s).getId() == seatID).findFirst().orElse(null);
        if(seat != null) {
            Vector3fPool.openPool();
            Vector3f posVec = DynamXGeometry.rotateVectorByQuaternion(seat.getPosition(), block.getCollidableRotation());
            passenger.setPosition(posX + posVec.x, posY + posVec.y, posZ + posVec.z);
            Vector3fPool.closePool();
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if(ticksExisted%20 == 0) {
            TileEntity te = world.getTileEntity(getPosition());
            if (te instanceof TEDynamXBlock)
                block = (TEDynamXBlock) te;
            else
                setDead();
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbtTagCompound) {
        seatID = nbtTagCompound.getByte("SeatID");
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbtTagCompound) {
        nbtTagCompound.setByte("SeatID", seatID);
    }
}
