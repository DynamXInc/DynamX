package fr.dynamx.common.entities;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.contentpack.object.IPartContainer;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.parts.BasePartSeat;
import fr.dynamx.common.entities.modules.MovableModule;
import fr.dynamx.common.physics.entities.PackEntityPhysicsHandler;
import fr.dynamx.common.physics.joints.EntityJointsHandler;
import fr.dynamx.utils.client.ClientDynamXUtils;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Base implementation for all pack-based entities
 *
 * @param <T> The physics handler type
 * @see IPhysicsModule
 * @see PackEntityPhysicsHandler For the physics implementation
 */
public abstract class PackPhysicsEntity<T extends PackEntityPhysicsHandler<A, ?>, A extends IPhysicsPackInfo & IPartContainer<?>> extends ModularPhysicsEntity<T> implements IPackInfoReloadListener {
    private static final DataParameter<String> INFO_NAME = EntityDataManager.createKey(PackPhysicsEntity.class, DataSerializers.STRING);
    private static final DataParameter<Integer> METADATA = EntityDataManager.createKey(PackPhysicsEntity.class, DataSerializers.VARINT);
    private int lastMetadata = -1;

    /**
     * -- GETTER --
     * The texture id depends on the entity's metadata <br>
     * If -1 is returned, the entity will not be rendered
     *
     * @return The texture id to use for drawing chassis
     */
    @Getter
    private byte entityTextureID = -1;

    protected EntityJointsHandler jointsHandler = new EntityJointsHandler(this);

    @Getter
    private MovableModule movableModule;

    @Getter
    @Setter
    private A packInfo;

    /**
     * Cache for collision boxes without rotation
     */
    private final List<MutableBoundingBox> rawBoxes = new ArrayList<>();

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
        if (packInfo != null && packInfo.getCollisionsHelper().hasPhysicsCollisions())
            return super.initEntityProperties();
        DynamXMain.log.warn("Failed to find info of " + this + ". Should be " + getInfoName());
        return false;
    }

    @Override
    public void onPackInfosReloaded() {
        setPackInfo(createInfo(getInfoName()));
        if (physicsHandler != null)
            physicsHandler.onPackInfosReloaded();
        for (IPhysicsModule<?> module : moduleList) {
            if (module instanceof IPackInfoReloadListener)
                ((IPackInfoReloadListener) module).onPackInfosReloaded();
        }
        rawBoxes.clear(); //Clear collisions cache
    }

    @Override
    protected void createModules(ModuleListBuilder modules) {
        moduleList.add(jointsHandler);
        moduleList.add(movableModule = new MovableModule(this));
        movableModule.initSubModules(modules, this);
        getPackInfo().addModules(this, modules);
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
            packInfo.getDrawableParts().forEach(m -> ((IDrawablePart<PackPhysicsEntity<?, ?>, A>) m).onTexturesChange(this));
        }
    }

    @Override
    public boolean isInRangeToRenderDist(double range) {
        return getPackInfo() != null && getPackInfo().getRenderDistance() >= range;
    }

    @Override
    public ItemStack getPickedResult(RayTraceResult target) {
        return packInfo.getPickedResult(getMetadata());
    }

    /**
     * Cache
     */
    @Override
    public List<MutableBoundingBox> getCollisionBoxes() {
        if (getPackInfo() == null || physicsPosition == null)
            return new ArrayList<>(0);
        Vector3f pos = Vector3fPool.get(posX, posY, posZ);
        if (rawBoxes.size() != getPackInfo().getCollisionsHelper().getShapes().size()) {
            rawBoxes.clear();
            for (IShapeInfo shape : getPackInfo().getCollisionsHelper().getShapes()) {
                MutableBoundingBox boundingBox = new MutableBoundingBox(shape.getBoundingBox());
                boundingBox.offset(pos);
                rawBoxes.add(boundingBox);
            }
        } else {
            for (int i = 0; i < getPackInfo().getCollisionsHelper().getShapes().size(); i++) {
                MutableBoundingBox boundingBox = rawBoxes.get(i);
                boundingBox.setTo(getPackInfo().getCollisionsHelper().getShapes().get(i).getBoundingBox());
                boundingBox.offset(pos);
            }
        }
        return rawBoxes;
    }

    /**
     * Ray-traces to get hit part when interacting with the entity
     */
    public InteractivePart<?, ?> getHitPart(Entity entity) {
        if (getPackInfo() == null) {
            return null;
        }
        Vec3d lookVec = entity.getLook(1.0F);
        Vec3d hitVec = entity.getPositionVector().add(0, entity.getEyeHeight(), 0);
        InteractivePart<?, ?> nearest = null;
        Vector3f nearestPos = null;
        Vector3f playerPos = Vector3fPool.get((float) entity.posX, (float) entity.posY, (float) entity.posZ);
        MutableBoundingBox box = new MutableBoundingBox();
        for (float f = 1.0F; f < 4.0F; f += 0.1F) {
            for (InteractivePart<?, ?> part : getPackInfo().getInteractiveParts()) {
                part.getBox(box);
                box = DynamXContext.getCollisionHandler().rotateBB(Vector3fPool.get(), box, physicsRotation);
                Vector3f partPos = DynamXGeometry.rotateVectorByQuaternion(part.getPosition(), physicsRotation);
                partPos.addLocal(physicsPosition);
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

    @Override
    protected boolean canFitPassenger(Entity passenger) {
        return this.getPassengers().size() < getPackInfo().getPartsByType(BasePartSeat.class).size();
    }

    @Override
    public boolean shouldRiderSit() {
        return RenderPhysicsEntity.shouldRenderPlayerSitting;
    }

    @Override
    public boolean canPassengerSteer() {
        return false;
    }

    @Override
    public EntityJointsHandler getJointsHandler() {
        return jointsHandler;
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
}
