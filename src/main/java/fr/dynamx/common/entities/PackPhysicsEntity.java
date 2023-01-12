package fr.dynamx.common.entities;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.entities.modules.IMovableModuleContainer;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.modules.MovableModule;
import fr.dynamx.common.physics.entities.PackEntityPhysicsHandler;
import fr.dynamx.common.physics.joints.EntityJointsHandler;
import fr.dynamx.utils.client.ClientDynamXUtils;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Base implementation for all pack-based entities
 *
 * @param <T> The physics handler type
 * @see IPhysicsModule
 * @see PackEntityPhysicsHandler For the physics implementation
 */
public abstract class PackPhysicsEntity<T extends PackEntityPhysicsHandler<A, ?>, A extends IPhysicsPackInfo> extends ModularPhysicsEntity<T> implements IMovableModuleContainer, IPackInfoReloadListener {
    private static final DataParameter<String> INFO_NAME = EntityDataManager.createKey(PackPhysicsEntity.class, DataSerializers.STRING);
    private static final DataParameter<Integer> METADATA = EntityDataManager.createKey(PackPhysicsEntity.class, DataSerializers.VARINT);
    private int lastMetadata = -1;
    private byte entityTextureID = -1;

    protected EntityJointsHandler jointsHandler = new EntityJointsHandler(this);
    private MovableModule movableModule;

    private A packInfo;

    public PackPhysicsEntity(World worldIn) {
        super(worldIn);
    }

    public PackPhysicsEntity(String infoName, World world, Vector3f pos, float spawnRotationAngle, int metadata) {
        super(world, pos, spawnRotationAngle);
        setInfoName(infoName);
        setMetadata(metadata);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.getDataManager().register(INFO_NAME, "");
        this.getDataManager().register(METADATA, -1);
    }

    public abstract A createInfo(String infoName);

    @Override
    public boolean initEntityProperties() {
        packInfo = createInfo(getInfoName());
        if (packInfo != null && packInfo.getPhysicsCollisionShape() != null)
            return super.initEntityProperties();
        DynamXMain.log.warn("Failed to find info of " + this + ". Should be " + getInfoName());
        return false;
    }

    @Override
    public void onPackInfosReloaded() {
        setPackInfo(createInfo(getInfoName()));
        for(IPhysicsModule<?> module : moduleList) {
            if(module instanceof IPackInfoReloadListener)
                ((IPackInfoReloadListener) module).onPackInfosReloaded();
        }
    }

    @Override
    protected void createModules(ModuleListBuilder modules) {
        moduleList.add(jointsHandler);
        moduleList.add(movableModule = new MovableModule(this));
        movableModule.initSubModules(modules, this);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tagCompound) {
        //Read info name before entity init in super method
        setInfoName(tagCompound.getString("vehicleName"));
        setMetadata(tagCompound.getInteger("Metadata"));
        super.readEntityFromNBT(tagCompound);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tagCompound) {
        tagCompound.setString("vehicleName", getInfoName());
        tagCompound.setInteger("Metadata", getMetadata());

        super.writeEntityToNBT(tagCompound);
    }

    @Override
    public void onUpdate() {
        if (getInfoName().isEmpty()) {
            setDead();
            return;
        }
        super.onUpdate();
        if (world.isRemote && getMetadata() != lastMetadata) //Metadata has been sync, so update texture
        {
            lastMetadata = getMetadata();
            entityTextureID = (byte) getMetadata();
            packInfo.getDrawableParts().forEach(m -> ((IDrawablePart<PackPhysicsEntity<?, ?>>) m).onTexturesChange(this));
        }
    }

    /**
     * Ray-traces to get hit part when interacting with the entity
     */
    public InteractivePart<?, ?> getHitPart(Entity entity) {
        if (getPackInfo() != null) {
            Vec3d lookVec = entity.getLook(1.0F);
            Vec3d hitVec = entity.getPositionVector().add(0, entity.getEyeHeight(), 0);
            InteractivePart<?, ?> nearest = null;
            Vector3f nearestPos = null;
            Vector3f vehiclePos = physicsPosition;
            Vector3f playerPos = Vector3fPool.get((float) entity.posX, (float) entity.posY, (float) entity.posZ);
            MutableBoundingBox box = new MutableBoundingBox();
            for (float f = 1.0F; f < 4.0F; f += 0.1F) {
                for (InteractivePart<?, ?> part : getPackInfo().getInteractiveParts()) {
                    part.getBox(box);
                    Vector3f partPos = DynamXGeometry.rotateVectorByQuaternion(part.getPosition(), physicsRotation);
                    partPos.addLocal(vehiclePos);
                    box.offset(partPos);
                    if ((nearestPos == null || DynamXGeometry.distanceBetween(partPos, playerPos) < DynamXGeometry.distanceBetween(nearestPos, playerPos)) && box.contains(hitVec)) {
                        nearest = part;
                        nearestPos = partPos;
                    }
                }
                hitVec = hitVec.add(lookVec.x * 0.1F, lookVec.y * 0.1F, lookVec.z * 0.1F);
            }
            return nearest;
        }
        return null;
    }

    @Override
    public EntityJointsHandler getJointsHandler() {
        return jointsHandler;
    }

    @Override
    public MovableModule getMovableModule() {
        return movableModule;
    }

    /**
     * The texture id depends on the entity's metadata <br>
     * If -1 is returned, the entity will not be rendered
     *
     * @return The texture id to use for drawing chassis
     */
    public byte getEntityTextureID() {
        return entityTextureID;
    }

    public int getMetadata() {
        return this.getDataManager().get(METADATA);
    }

    public void setMetadata(int metadata) {
        this.getDataManager().set(METADATA, metadata);
    }

    @Override
    public int getBrightnessForRender() {
        return ClientDynamXUtils.getLightNear(world, getPosition(), 1, 3);
    }

    @Override
    public String getName() {
        return "DynamXEntity:" + getInfoName() + ":" + getEntityId();
    }

    public String getInfoName() {
        return this.getDataManager().get(INFO_NAME);
    }

    private void setInfoName(String name) {
        this.getDataManager().set(INFO_NAME, name);
    }

    public A getPackInfo() {
        return packInfo;
    }

    public void setPackInfo(A packInfo) {
        this.packInfo = packInfo;
    }
}
