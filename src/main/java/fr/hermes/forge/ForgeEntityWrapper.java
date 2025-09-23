package fr.hermes.forge;

import fr.dynamx.core.common.items.DynamXItemRegistry;
import fr.dynamx.core.utils.maths.DynamXGeometry;
import fr.dynamx.core.utils.optimization.MutableBoundingBox;
import fr.dynamx.core.utils.optimization.SubClassPool;
import fr.dynamx.core.utils.optimization.Vector3fPool;
import fr.hermes.api.mc.HmEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public abstract class ForgeEntityWrapper<E extends HmEntity> extends Entity implements IEntityAdditionalSpawnData, HmEntity {
    private final E modEntity;

    // TODO SUB-CLASSES FOR ALL FINAL ENTITIES

    public ForgeEntityWrapper(E modEntity, World worldIn) {
        super(worldIn);
        this.modEntity = modEntity;
    }

    // ====== Logic that should be brought to other implementations

    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 0 || pass == 1;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
    } //Avoid vanilla sync

    @Override
    public boolean isInRangeToRenderDist(double range) {
        double d = getEntityBoundingBox().getAverageEdgeLength() * 4.0D * 64.0D;
        return range < d * d;
    }

    @Override
    public void updateMinecraftPos(Vector3f physicsPosition, Quaternionf physicsRotation) {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;

        posX = physicsPosition.x;
        posY = physicsPosition.y;
        posZ = physicsPosition.z;

        motionX = (posX - prevPosX);
        motionY = (posY - prevPosY);
        motionZ = (posZ - prevPosZ);

        setPosition(posX, posY, posZ);

        prevRotationYaw = rotationYaw;
        prevRotationPitch = rotationPitch;

        alignRotation(physicsRotation);
    }

    /**
     * Computes yaw and pitch from the given quaternion
     */
    private void alignRotation(Quaternionf localQuat) {
        Vector3f rotatedForwardDirection = Vector3fPool.get();
        rotatedForwardDirection = localQuat.transform(DynamXGeometry.FORWARD_DIRECTION, rotatedForwardDirection);

        rotationPitch = DynamXGeometry.getPitchFromRotationVector(rotatedForwardDirection) % 360;

        rotationYaw = DynamXGeometry.getYawFromRotationVector(rotatedForwardDirection) % 360;
        if (rotationYaw - prevRotationYaw > 270)
            prevRotationYaw += 360;
        else if (prevRotationYaw - rotationYaw > 270)
            prevRotationYaw -= 360;
    }

    @Override
    public void onUpdate() {
        JmeVector3fPool.openPool(SubClassPool.TICK_ENTITY_MC);

        double d1 = prevPosX;
        double d2 = prevPosY;
        double d3 = prevPosZ;
        super.onUpdate();
        prevPosX = d1;
        prevPosY = d2;
        prevPosZ = d3;

        modEntity.onUpdate();
        JmeVector3fPool.closePool();
    }

    @Override
    public boolean attackEntityFrom(DamageSource damageSource, float amount) {
       /* TODO EVENTS if (MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.Attacked(this, damageSource.getTrueSource(), damageSource))) {
            return false;
        }*/
        if (damageSource.isExplosion()) {
            return false;
        }
        if (!this.world.isRemote && !this.isDead && damageSource.getImmediateSource() instanceof EntityPlayer && damageSource.getTrueSource().getRidingEntity() != this
                && (((EntityPlayer) damageSource.getImmediateSource()).capabilities.isCreativeMode
                || ((EntityPlayer) damageSource.getImmediateSource()).getHeldItemMainhand().getItem().equals(DynamXItemRegistry.ITEM_WRENCH))) {
            setDead();
            return true;
        }
        return false;
    }

    // ====== From Mc ======

    @Override
    protected void entityInit() {
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        modEntity.readFromNbt(compound);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        modEntity.writeToNbt(compound);
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        modEntity.writeSpawnData(buffer);
    }

    @Override
    public void readSpawnData(ByteBuf additionalData) {
        modEntity.readSpawnData(additionalData);
    }

    @Override
    public MutableBoundingBox getBoundingBox() {
        return modEntity.getBoundingBox();
    }

    @Override
    public AxisAlignedBB getEntityBoundingBox() {
        return getBoundingBox().toBB(); //TODO CACHE THE RESULT!!!
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        modEntity.onRemovedFromWorld();
    }

    @Override
    public String getName() {
        return modEntity.getName();
    }

    // ====== From HmEntity ======

    @Override
    public void setNoClip(boolean value) {
        noClip = value;
    }

    @Override
    public void setPreventEntitySpawning(boolean value) {
        preventEntitySpawning = value;
    }

    @Override
    public void setIgnoreFrustumCheck(boolean value) {
        ignoreFrustumCheck = value;
    }

    @Override
    public void setPosition(float x, float y, float z) {
        super.setPosition(x, y, z);
    }

    @Override
    public void setRotationYaw(float yaw) {
        rotationYaw = yaw;
    }

    @Override
    public double getPosX() {
        return posX;
    }

    @Override
    public double getPosY() {
        return posY;
    }

    @Override
    public double getPosZ() {
        return posZ;
    }

    @Override
    public float getRotationYaw() {
        return rotationYaw;
    }

    @Override
    public float getRotationPitch() {
        return rotationPitch;
    }

    @Override
    public double getMotionX() {
        return motionX;
    }

    @Override
    public double getMotionY() {
        return motionY;
    }

    @Override
    public double getMotionZ() {
        return motionZ;
    }

    @Override
    public void setDead() {
        super.setDead();
        onSetDead();
    }

    @Override
    public void onSetDead() {
        modEntity.onSetDead();
    }
}
