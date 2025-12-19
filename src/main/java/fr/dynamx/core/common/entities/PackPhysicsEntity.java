package fr.dynamx.core.common.entities;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.contentpack.object.IPartContainer;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SynchronizationRules;
import fr.dynamx.api.network.sync.SynchronizedEntityVariable;
import fr.dynamx.core.common.DynamXContext;
import fr.dynamx.core.common.DynamXMain;
import fr.dynamx.core.common.contentpack.parts.BasePartSeat;
import fr.dynamx.core.common.entities.modules.MovableModule;
import fr.dynamx.core.common.physics.entities.PackEntityPhysicsHandler;
import fr.dynamx.core.common.physics.joints.EntityJointsHandler;
import fr.dynamx.core.utils.DynamXConstants;
import fr.dynamx.core.utils.client.ClientDynamXUtils;
import fr.dynamx.core.utils.debug.Profiler;
import fr.dynamx.core.utils.maths.DynamXGeometry;
import fr.dynamx.core.utils.optimization.MutableBoundingBox;
import fr.dynamx.core.utils.optimization.SubClassPool;
import fr.dynamx.core.utils.optimization.Vector3fPool;
import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.items.HmItemStack;
import fr.hermes.forge.JmeVector3fPool;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;

/**
 * Base implementation for all pack-based entities
 *
 * @param <T> The physics handler type
 * @see IPhysicsModule
 * @see PackEntityPhysicsHandler For the physics implementation
 */
@SynchronizedEntityVariable.SynchronizedPhysicsModule(modid = DynamXConstants.ID)
public abstract class PackPhysicsEntity<T extends PackEntityPhysicsHandler<A, ?>, A extends IPhysicsPackInfo & IPartContainer<?>> extends ModularPhysicsEntity<T> implements IPackInfoReloadListener {
    @SynchronizedEntityVariable(name = "info_name")
    private final EntityVariable<String> infoName = new EntityVariable<>(SynchronizationRules.SERVER_TO_CLIENTS, "");

    @SynchronizedEntityVariable(name = "metadata")
    private final EntityVariable<Integer> metadata = new EntityVariable<>(SynchronizationRules.SERVER_TO_CLIENTS, 0);

    private int lastMetadata = -1;

    /**
     * -- GETTER --
     * The texture id depends on the entity's metadata <br>
     * If -1 is returned, the entity will not be rendered
     *
     * @return The texture id to use for drawing chassis
     */
    @Getter
    private byte entityTextureId = -1;

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

    public PackPhysicsEntity(HmEntity mcEntityWrapper) {
        super(mcEntityWrapper);
    }

    public PackPhysicsEntity(String infoName, HmEntity mcEntityWrapper, Vector3f pos, float spawnRotationAngle, int metadata) {
        super(mcEntityWrapper, pos, spawnRotationAngle);
        setInfoName(infoName);
        setMetadata(metadata);
    }

    public abstract A createInfo(String infoName);

    @Override
    public boolean initEntityProperties() {
        packInfo = createInfo(getInfoName());
        if (packInfo != null && packInfo.getCollisionsHelper().hasPhysicsCollisions())
            return super.initEntityProperties();
        DynamXMain.log.warn("Failed to find info of {}. Should be {}.", this, getInfoName());
        return false;
    }

    @Override
    public void onPackInfosReloaded() {
        A packInfo = createInfo(getInfoName());
        if (packInfo == null) {
            DynamXMain.log.warn("Failed to find info of {} after packs reload. Should be {}. Killing the entity.", this, getInfoName());
            setDead();
            return;
        }
        setPackInfo(packInfo);

        if (physicsHandler != null) {
            physicsHandler.onPackInfosReloaded();
        }

        for (IPhysicsModule<?> module : moduleList) {
            if (module instanceof IPackInfoReloadListener) {
                ((IPackInfoReloadListener) module).onPackInfosReloaded();
            }
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
    public void readFromNbt(NBTTagCompound tagCompound) {
        //Read info name before entity init in super method
        setInfoName(tagCompound.getString("vehicleName"));
        setMetadata(tagCompound.getInteger("Metadata"));
        super.readFromNbt(tagCompound);
    }

    @Override
    public void writeToNbt(NBTTagCompound tagCompound) {
        tagCompound.setString("vehicleName", getInfoName());
        tagCompound.setInteger("Metadata", getMetadata());
        super.writeToNbt(tagCompound);
    }

    @Override
    public void onUpdate() {
        if (getInfoName().isEmpty()) {
            setDead();
            return;
        }
        JmeVector3fPool.openPool(SubClassPool.TICK_ENTITY_MC);
        Profiler.get().start(Profiler.Profiles.TICK_ENTITIES);
        super.onUpdate();
        if (mcEntityWrapper.getHmWorld().isClient() && getMetadata() != lastMetadata && !isDead()) //Metadata has been sync, so update texture
        {
            lastMetadata = getMetadata();
            entityTextureId = (byte) getMetadata();
            getModules().forEach(m -> m.onTexturesChange(entityTextureId));
        }
        Profiler.get().end(Profiler.Profiles.TICK_ENTITIES);
        JmeVector3fPool.closePool();
    }

    @Override
    public HmItemStack getHmPickedResult() {
        return packInfo.getPickedResult(getMetadata());
    }

    /**
     * Cache
     */
    @Override
    public List<MutableBoundingBox> getCollisionBoxes() {
        if (getPackInfo() == null || physicsPosition == null)
            return new ArrayList<>(0);
        Vector3f pos = JmeVector3fPool.get(getPosX(), getPosY(), getPosZ());
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
    public InteractivePart<?, ?> getHitPart(HmEntity entity) {
        if (getPackInfo() == null) {
            return null;
        }
        org.joml.Vector3f lookVec = entity.getHmLook();
        org.joml.Vector3f hitVec = entity.getEyesPosition();
        InteractivePart<?, ?> nearest = null;
        Vector3f nearestPos = null;
        Vector3f playerPos = JmeVector3fPool.get((float) entity.getPosX(), (float) entity.getPosY(), (float) entity.getPosZ());
        MutableBoundingBox box = new MutableBoundingBox();
        for (float f = 1.0F; f < 4.0F; f += 0.1F) {
            for (InteractivePart<?, ?> part : getPackInfo().getInteractiveParts()) {
                part.getBox(box);
                box = DynamXContext.getCollisionHandler().rotateBB(JmeVector3fPool.get(), box, physicsRotation);
                Vector3f partPos = DynamXGeometry.rotateVectorByQuaternion(part.getPosition(), physicsRotation);
                partPos.addLocal(physicsPosition);
                box.offset(partPos);
                if ((nearestPos == null || DynamXGeometry.distanceBetween(partPos, playerPos) < DynamXGeometry.distanceBetween(nearestPos, playerPos))
                        && box.contains(hitVec)) {
                    nearest = part;
                    nearestPos = partPos;
                }
            }
            hitVec = hitVec.add(lookVec.x * 0.1F, lookVec.y * 0.1F, lookVec.z * 0.1F);
        }
        return nearest;
    }

    @Override
    public boolean canFitPassenger(HmEntity passenger) {
        return getHmPassengers().size() < getPackInfo().getPartsByType(BasePartSeat.class).size();
    }

    @Override
    public boolean isInRangeToRenderDist(double range) {
        if (getPackInfo() != null && getPackInfo().getRenderDistanceSquared() != -1) {
            return range < getPackInfo().getRenderDistanceSquared();
        }
        return super.isInRangeToRenderDist(range);
    }

    @Override
    public EntityJointsHandler getJointsHandler() {
        return jointsHandler;
    }

    public int getMetadata() {
        return metadata.get();
    }

    public void setMetadata(int metadata) {
        this.metadata.set(metadata);
    }

    @Override
    public int getBrightnessForRender() {
        return ClientDynamXUtils.getLightNear(getHmWorld(), getHmBlockPosition(), 1, 3);
    }

    @Override
    public String getName() {
        return "DynamXEntity:" + getInfoName() + ":" + getEntityId();
    }

    public String getInfoName() {
        return infoName.get();
    }

    private void setInfoName(String name) {
        this.infoName.set(name);
    }
}
