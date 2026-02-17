package fr.dynamx.core.common.entities;

import com.jme3.math.Vector3f;
import fr.dynamx.core.common.blocks.TEDynamXBlock;
import fr.dynamx.core.common.contentpack.parts.PartBlockSeat;
import fr.dynamx.core.utils.EnumSeatPlayerPosition;
import fr.dynamx.core.utils.maths.DynamXGeometry;
import fr.hermes.api.mc.blocks.HmBlockEntity;
import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.entities.HmEntityFactory;
import fr.hermes.api.mc.entities.HmEntityLogic;
import fr.hermes.api.mc.world.HmWorld;
import fr.hermes.api.utils.HmBlockEntityLogicMatcher;
import fr.dynamx.core.utils.optimization.JmeVector3fPool;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;

public class SeatEntity implements HmEntityLogic {
    private final HmEntity mcEntity;
    protected TEDynamXBlock block;
    protected PartBlockSeat<?> mySeat;
    protected byte seatID;

    public SeatEntity(HmEntity mcEntity, TEDynamXBlock block, PartBlockSeat<?> seat) {
        this(mcEntity);
        this.block = block;
        this.mySeat = seat;
        this.seatID = seat.getId();
    }

    public SeatEntity(HmEntity mcEntity) {
        super();
        this.mcEntity = mcEntity;
        mcEntity.hm$setNoClip(true);
        mcEntity.hm$setNoGravity(true);
    }

    @Override
    public void onMcEntityInit() {
        mcEntity.hm$setSize(0.6f, 0.6f);
    }

    @Override
    public boolean updatePassenger(HmEntity passenger) {
        if (block == null || mySeat == null) {
            return false;
        }
        JmeVector3fPool.openPool();
        Vector3f posVec = DynamXGeometry.rotateVectorByQuaternion(mySeat.getPosition(), block.getCollidableRotation());
        posVec.addLocal(block.getRelativeTranslation());
        passenger.hm$setPosition((float) (mcEntity.hm$getPosX() + posVec.x), (float) (mcEntity.hm$getPosY() + posVec.y), (float) (mcEntity.hm$getPosZ() + posVec.z));
        JmeVector3fPool.closePool();
        return true;
    }

    /**
     * Rotates the passenger, limiting his field of view to avoid stiff necks
     */
    @Override
    public boolean updatePassengerRotation(HmEntity passenger) {
        if (mySeat == null || !mySeat.shouldLimitFieldOfView()) {
            return true;
        }
        float blockYaw = block.getPackInfo() == null ? 0 : (block.getPackInfo().getRotation().y - block.getRelativeRotation().y + block.getRotation() * 22.5f);
        passenger.hm$setRenderYawOffset(blockYaw);
        float f = MathHelper.wrapDegrees(passenger.hm$getRotationYaw() - blockYaw);
        float f1 = MathHelper.clamp(f, mySeat.getMinYaw(), mySeat.getMaxYaw());
        passenger.hm$setPrevRotationYaw(passenger.hm$getPrevRotationYaw() + f1 - f);
        passenger.hm$setRotationYaw(passenger.hm$getRotationYaw() + f1 - f);
        passenger.hm$setRotationYawHead(passenger.hm$getRotationYaw());

        float f2 = MathHelper.wrapDegrees(passenger.hm$getRotationPitch());
        float f3 = MathHelper.clamp(f2, mySeat.getMinPitch(), mySeat.getMaxPitch());
        passenger.hm$setRotationPitch(f3);
        f2 = MathHelper.wrapDegrees(passenger.hm$getPrevRotationPitch());
        f3 = MathHelper.clamp(f2, mySeat.getMinPitch(), mySeat.getMaxPitch());
        passenger.hm$setPrevRotationPitch(f3);
        return true;
    }

    @Override
    public boolean canPassengerSteer() {
        return false;
    }

    @Override
    public boolean shouldPassengersSit() {
        return mySeat == null || mySeat.getPlayerPosition() == EnumSeatPlayerPosition.SITTING;
    }

    @Override
    public void onUpdate() {
        if (mcEntity.hm$getTicksExisted() % 20 != 0) {
            return;
        }
        if(block != null && !block.isInvalid()) {
            return;
        }
        HmBlockEntity te = mcEntity.hm$getWorld().hm$getTileEntity(mcEntity.hm$getBlockPosition());
        if (HmBlockEntityLogicMatcher.is(te, TEDynamXBlock.class)) {
            block = HmBlockEntityLogicMatcher.cast(te, TEDynamXBlock.class);
            mySeat = (PartBlockSeat<?>) block.getPackInfo().getPartsByType(PartBlockSeat.class).stream()
                    .filter(s -> ((PartBlockSeat<?>) s).getId() == seatID).findFirst().orElse(null);
            if (mySeat != null) {
                return;
            }
        }
        mcEntity.hm$setDead();
    }

    @Override
    public void readFromNbt(NBTTagCompound nbtTagCompound) {
        seatID = nbtTagCompound.getByte("SeatID");
    }

    @Override
    public void writeToNbt(NBTTagCompound nbtTagCompound) {
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

    public static class Factory implements HmEntityFactory<SeatEntity> {
        private final TEDynamXBlock block;
        private final PartBlockSeat<?> seat;

        public Factory(TEDynamXBlock block, PartBlockSeat<?> seat) {
            this.block = block;
            this.seat = seat;
        }

        @Override
        public SeatEntity createEntityLogic(HmWorld world, HmEntity entity) {
            return new SeatEntity(entity, block, seat);
        }
    }
}
