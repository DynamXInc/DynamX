package fr.hermes.forge;

import fr.dynamx.core.client.renders.RenderPhysicsEntity;
import fr.dynamx.core.common.items.DynamXItemRegistry;
import fr.dynamx.core.utils.maths.DynamXGeometry;
import fr.dynamx.core.utils.optimization.MutableBoundingBox;
import fr.dynamx.core.utils.optimization.SubClassPool;
import fr.dynamx.core.utils.optimization.Vector3fPool;
import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.items.HmItemStack;
import fr.hermes.api.mc.world.HmServerWorld;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3i;

import javax.annotation.Nullable;
import java.util.Collection;

public class ForgeEntityWrapper<E extends HmEntity> extends Entity implements IEntityAdditionalSpawnData, HmEntity {
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
        return modEntity.isInRangeToRenderDist(range);
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
    protected void readEntityFromNBT(NBTTagCompound tag) {
        readFromNbt(tag);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        writeToNbt(tag);
    }

    @Override
    public void readFromNbt(NBTTagCompound tag) {
        modEntity.readFromNbt(tag);
    }

    @Override
    public void writeToNbt(NBTTagCompound tag) {
        modEntity.writeToNbt(tag);
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
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        onAddPassenger(passenger);
    }

    @Override
    public void onAddPassenger(HmEntity passenger) {
        modEntity.onAddPassenger(passenger);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        onRemovePassenger(passenger);
    }

    @Override
    public void onRemovePassenger(HmEntity passenger) {
        modEntity.onRemovePassenger(passenger);
    }

    @Override
    public void updatePassenger(Entity passenger) {
        if(!updatePassenger(passenger)) {
            super.updatePassenger(passenger);
        }
    }

    @Override
    public boolean updatePassenger(HmEntity passenger) {
        return modEntity.updatePassenger(passenger);
    }

    @Override
    public void applyOrientationToEntity(Entity entityToUpdate) {
        if(!updatePassengerRotation(entityToUpdate)) {
            super.applyOrientationToEntity(entityToUpdate);
        }
    }

    @Override
    public boolean updatePassengerRotation(HmEntity passenger) {
        return modEntity.updatePassengerRotation(passenger);
    }

    @Override
    public MutableBoundingBox getHmBoundingBox() {
        return modEntity.getHmBoundingBox();
    }

    @Override
    public AxisAlignedBB getEntityBoundingBox() {
        return getHmBoundingBox().toBB(); //TODO CACHE THE RESULT!!!
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

    @Override
    public boolean isDead() {
        return isDead;
    }

    @Override
    public int getTicksExisted() {
        return ticksExisted;
    }

    @Override
    public float getDistanceSq(HmEntity entity) {
        return super.getDistanceSq(entity);
    }

    // ====== From HmEntity ======

    @Override
    public HmServerWorld hm$getWorld() {
        //FIXME TODO
        return null;
    }

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
    public float getPrevRotationYaw() {
        return prevRotationYaw;
    }

    @Override
    public float getRotationPitch() {
        return rotationPitch;
    }

    @Override
    public float getPrevRotationPitch() {
        return prevRotationPitch;
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

    @Override
    public Collection<HmEntity> getHmPassengers() {
        return super.getPassengers();
    }

    @Nullable
    @Override
    public Entity getControllingPassenger() {
        HmEntity entity = getHmControllingPassenger();
        return entity != null ? entity : super.getControllingPassenger();
    }

    @Nullable
    @Override
    public HmEntity getHmControllingPassenger() {
        return modEntity.getHmControllingPassenger();
    }

    @Override
    public ItemStack getPickedResult(RayTraceResult target) {
        return getHmPickedResult();
    }

    @Override
    public HmItemStack getHmPickedResult() {
        return modEntity.getHmPickedResult();
    }

    @Override
    protected boolean canFitPassenger(Entity passenger) {
        return canFitPassenger(passenger); //FIXME TODO
    }

    @Override
    public boolean canFitPassenger(HmEntity passenger) {
        return modEntity.canFitPassenger(passenger);
    }

    @Override
    public int getBrightnessForRender() {
        return modEntity.getBrightnessForRender();
    }

    @Override
    public Vector3f getHmLook() {
        Vec3d look = getLook(1);
        return Vector3fPool.get(look.x, look.y, look.z);
    }

    @Override
    public Vector3f getHmPosition() {
        //TODO POOL ?
        return new Vector3f((float) posX, (float) (posY), (float) posZ);
    }

    @Override
    public Vector3i getHmBlockPosition() {
        // same logic as getPosition() from Mc
        //TODO POOL ?
        return new Vector3i((int) posX, (int) (posY + 0.5f), (int) posZ);
    }

    @Override
    public int getChunkX() {
        return chunkCoordX;
    }

    @Override
    public int getChunkY() {
        return chunkCoordY;
    }

    @Override
    public int getChunkZ() {
        return chunkCoordZ;
    }

    @Override
    public void setMotionX(float motionX) {
        this.motionX = motionX;
    }

    @Override
    public void setMotionY(float motionY) {
        this.motionY = motionY;
    }

    @Override
    public void setMotionZ(float motionZ) {
        this.motionZ = motionZ;
    }

    @Override
    public double getPrevPosX() {
        return prevPosX;
    }

    @Override
    public double getPrevPosY() {
        return prevPosY;
    }

    @Override
    public double getPrevPosZ() {
        return prevPosZ;
    }

    @Override
    public boolean startRiding(HmEntity riddenEntity) {
        //FIXME IMPLEMENT
        return false;
    }

    @Override
    public boolean startRiding(HmEntity riddenEntity, boolean force) {
        //FIXME IMPLEMENT
        return false;
    }

    @Override
    public Vector3f getEyesPosition() {
        return getHmPosition().add(0, getEyeHeight(), 0);
    }

    // === no from Hm ===

    @Override
    public boolean shouldRiderSit() {
        return RenderPhysicsEntity.shouldRenderPlayerSitting;
    }

    @Override
    public boolean canPassengerSteer() {
        return false;
    }
}
