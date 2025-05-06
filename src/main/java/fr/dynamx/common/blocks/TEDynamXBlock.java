package fr.dynamx.common.blocks;

import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.aym.acsguis.api.ACsGuiApi;
import fr.dynamx.api.blocks.IBlockEntityModule;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.client.gui.GuiBlockCustomization;
import fr.dynamx.client.renders.animations.DxAnimator;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.capability.DynamXChunkData;
import fr.dynamx.common.capability.DynamXChunkDataProvider;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.parts.PartBlockSeat;
import fr.dynamx.common.contentpack.type.ObjectCollisionsHelper;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.common.entities.IDynamXObject;
import fr.dynamx.common.entities.SeatEntity;
import fr.dynamx.common.handlers.CommonEventHandler;
import fr.dynamx.common.physics.terrain.chunk.ChunkLoadingTicket;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.VerticalChunkPos;
import fr.dynamx.utils.debug.ChunkGraph;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;

public class TEDynamXBlock extends TileEntity implements IDynamXObject, IPackInfoReloadListener, ITickable {
    @Getter
    private BlockObject<?> packInfo;
    @Getter
    private int rotation;
    @Getter
    @Setter
    private Vector3f relativeTranslation = new Vector3f();
    @Getter
    @Setter
    private Vector3f relativeRotation = new Vector3f();
    @Getter
    @Setter
    private Vector3f relativeScale = new Vector3f(1, 1, 1);

    private boolean hasSeats;
    @Getter
    private List<SeatEntity> seatEntities;

    /**
     * The cache of the block collisions, with position offset but no rotation
     */
    protected final List<MutableBoundingBox> unrotatedCollisionsCache = new ArrayList<>();
    /**
     * The hitbox for (mouse) interaction with the block
     */
    protected AxisAlignedBB boundingBoxCache;

    private EnumBlockEntityInitState initialized = EnumBlockEntityInitState.NOT_INITIALIZED;
    protected final List<IBlockEntityModule> moduleList = new ArrayList<>();
    protected final List<IBlockEntityModule.IBlockEntityUpdateListener> updateEntityListeners = new ArrayList<>();

    @Getter
    private final DxAnimator animator;

    public TEDynamXBlock() {
        animator = new DxAnimator();
    }

    public TEDynamXBlock(BlockObject<?> packInfo) {
        this();
        setPackInfo(packInfo);
        this.hasSeats = !packInfo.getPartsByType(PartBlockSeat.class).isEmpty();
    }

    public void setPackInfo(BlockObject<?> packInfo) {
        this.packInfo = packInfo;
        if (world != null)
            world.markBlockRangeForRenderUpdate(pos, pos);
        if (packInfo == null)
            return;
        this.hasSeats = !packInfo.getPartsByType(PartBlockSeat.class).isEmpty();
        if (!hasSeats && seatEntities != null) {
            seatEntities.forEach(Entity::setDead);
            seatEntities = null;
        }
    }

    /**
     * Computes listeners of update methods
     */
    protected void getListenerModules() {
        updateEntityListeners.clear();
        moduleList.forEach(m -> {
            if (m instanceof IBlockEntityModule.IBlockEntityUpdateListener && ((IBlockEntityModule.IBlockEntityUpdateListener) m).listenBlockEntityUpdates(world.isRemote ? Side.CLIENT : Side.SERVER))
                updateEntityListeners.add((IBlockEntityModule.IBlockEntityUpdateListener) m);
        });
        initialized = EnumBlockEntityInitState.ALL;
    }

    public <Y extends IPhysicsModule<?>> Y getModuleByType(Class<Y> moduleClass) {
        return (Y) moduleList.stream().filter(m -> moduleClass.isAssignableFrom(m.getClass())).findFirst().orElse(null);
    }

    public boolean hasModuleOfType(Class<? extends IPhysicsModule<?>> moduleClass) {
        return moduleList.stream().anyMatch(m -> moduleClass.isAssignableFrom(m.getClass()));
    }

    public void initBlockEntityModules() {
        if (packInfo == null)
            return;
        createModules(new ModuleListBuilder(moduleList));
        //TODO fireCreateModulesEvent(world.isRemote ? Side.CLIENT : Side.SERVER);
        moduleList.sort(Comparator.comparingInt(m -> -m.getInitPriority()));
        moduleList.forEach(IBlockEntityModule::initBlockEntityProperties);
        if (world != null) {
            getListenerModules();
        } else {
            initialized = EnumBlockEntityInitState.MODULES_CREATED;
        }
    }

    protected void createModules(ModuleListBuilder modules) {
        getPackInfo().addModules(this, modules);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (packInfo != null)
            compound.setString("BlockInfo", packInfo.getFullName());
        compound.setInteger("Rotation", rotation);
        compound.setFloat("TranslationX", relativeTranslation.x);
        compound.setFloat("TranslationY", relativeTranslation.y);
        compound.setFloat("TranslationZ", relativeTranslation.z);
        compound.setFloat("ScaleX", relativeScale.x);
        compound.setFloat("ScaleY", relativeScale.y);
        compound.setFloat("ScaleZ", relativeScale.z);
        compound.setFloat("RotationX", relativeRotation.x);
        compound.setFloat("RotationY", relativeRotation.y);
        compound.setFloat("RotationZ", relativeRotation.z);
        // Write modules data
        moduleList.forEach(m -> m.writeToNBT(compound));
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (!compound.hasKey("BlockInfo")) {
            DynamXMain.log.error("TEDynamXBlock at " + pos + " has no BlockInfo tag. Ignoring it.");
            return;
        }
        String info = compound.getString("BlockInfo");
        BlockObject<?> packInfo = DynamXObjectLoaders.BLOCKS.findInfo(info);
        if (packInfo == null) {
            DynamXMain.log.error("Block object info is null for te " + this + " at " + pos + " : " + info + " not found. Ignoring it.");
            return;
        }
        setPackInfo(DynamXObjectLoaders.BLOCKS.findInfo(compound.getString("BlockInfo")));
        this.hasSeats = packInfo != null && !packInfo.getPartsByType(PartBlockSeat.class).isEmpty();
        rotation = compound.getInteger("Rotation");
        relativeTranslation = new Vector3f(
                compound.getFloat("TranslationX"),
                compound.getFloat("TranslationY"),
                compound.getFloat("TranslationZ"));
        relativeScale = new Vector3f(
                compound.getFloat("ScaleX"),
                compound.getFloat("ScaleY"),
                compound.getFloat("ScaleZ"));
        relativeRotation = new Vector3f(
                compound.getFloat("RotationX"),
                compound.getFloat("RotationY"),
                compound.getFloat("RotationZ"));

        // Read modules data
        initBlockEntityModules();
        moduleList.forEach(iBlockEntityModule -> iBlockEntityModule.readFromNBT(compound));

        if (packInfo == null && world != null && !world.isRemote) {
            DynamXMain.log.warn("Block object info is null for te " + this + " at " + pos + ". Removing it.");
            world.setBlockToAir(pos);
        } else {
            markCollisionsDirty(false);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return packInfo == null || packInfo.getRenderDistanceSquared() == -1 ? super.getMaxRenderDistanceSquared() : packInfo.getRenderDistanceSquared();
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        this.writeToNBT(nbttagcompound);
        return new SPacketUpdateTileEntity(pos, 0, nbttagcompound);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        super.onDataPacket(net, pkt);
        this.readFromNBT(pkt.getNbtCompound());
        this.world.markBlockRangeForRenderUpdate(pos, pos);
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
        markCollisionsDirty(true);
    }

    @SideOnly(Side.CLIENT)
    public void openConfigGui() {
        ACsGuiApi.asyncLoadThenShowGui("Block Customization", () -> new GuiBlockCustomization(this));
    }

    /**
     * @return The block collision box when you try to hover it with the mouse and interact with it
     */
    public AxisAlignedBB computeBoundingBox() {
        if (boundingBoxCache == null) {
            QuaternionPool.openPool();
            Vector3fPool.openPool();
            List<IShapeInfo> boxes = getUnrotatedCollisionBoxes(); //Get PartShape boxes
            if (boxes.isEmpty()) {//If there is no boxes, create a default one
                boundingBoxCache = new AxisAlignedBB(0, 0, 0, 1, 1, 1);
            } else {
                MutableBoundingBox container;
                if (boxes.size() == 1) //If there is one, no more calculus to do !
                    container = new MutableBoundingBox(boxes.get(0).getBoundingBox());
                else {
                    container = new MutableBoundingBox(boxes.get(0).getBoundingBox());
                    for (int i = 1; i < boxes.size(); i++) { //Else create a bigger box containing all of the boxes
                        container.growTo(boxes.get(i).getBoundingBox());
                    }
                }
                //The container box corresponding to an unrotated entity, so rotate it !
                Quaternion physicsRotation = getCollidableRotation();
                container.scale(getRelativeScale().x != 0 ? getRelativeScale().x : 1, getRelativeScale().y != 0 ? getRelativeScale().y : 1, getRelativeScale().z != 0 ? getRelativeScale().z : 1);
                container.grow(0.1, 0.0, 0.1); //Grow it to avoid little glitches on the corners of the car
                container = DynamXContext.getCollisionHandler().rotateBB(Vector3fPool.get(0.5f, 0, 0.5f), container, physicsRotation);
                container.offset(getRelativeTranslation().x, getRelativeTranslation().y, getRelativeTranslation().z);
                boundingBoxCache = container.toBB();
            }
            Vector3fPool.closePool();
            QuaternionPool.closePool();
        }
        return boundingBoxCache;
    }

    @Override
    public List<MutableBoundingBox> getCollisionBoxes() {
        if (packInfo != null && unrotatedCollisionsCache.size() != getUnrotatedCollisionBoxes().size()) {
            synchronized (unrotatedCollisionsCache) {
                for (IShapeInfo shape : getUnrotatedCollisionBoxes()) {
                    MutableBoundingBox b = new MutableBoundingBox(shape.getBoundingBox());
                    b.scale(relativeScale.x != 0 ? relativeScale.x : 1, relativeScale.y != 0 ? relativeScale.y : 1, relativeScale.z != 0 ? relativeScale.z : 1);
                    b.offset(pos.getX() - 0.5, pos.getY(), pos.getZ() - 0.5);
                    unrotatedCollisionsCache.add(b);
                }
            }
        }
        return unrotatedCollisionsCache;
    }

    /**
     * @return The collision boxes composing this entity, with no rotation and no position applied <br>
     * Used for all collisions of the entity (with players, vehicles, etc) <br>
     * The list is not modified by callers of the function <br>
     * <strong>Note : </strong>The list is cached by the callers of this function, so you need to call markCollisionsDirty() to refresh them.
     */
    public List<IShapeInfo> getUnrotatedCollisionBoxes() {
        if (packInfo == null) {
            return Collections.emptyList();
        }
        return packInfo.getCollisionsHelper().getShapes();
    }

    /**
     * @return The collision of this tile entity in the PhysicsWorld, without rotation or custom translation
     */
    public CompoundCollisionShape getPhysicsCollision() {
        if (packInfo == null) {
            throw new IllegalStateException("BlockObjectInfo is null for te " + this + " at " + pos);
        }
        if (!packInfo.getCollisionsHelper().hasPhysicsCollisions()) {
            return ObjectCollisionsHelper.getEmptyCollisionShape();
        }
        return packInfo.getCollisionsHelper().getPhysicsCollisionShape();
    }

    /**
     * Invalidates the block collisions caches, and reloads them <br>
     * Will provoke lag if you call this each tick
     *
     * @param updatePhysicsTerrain True to recompute the {@link fr.dynamx.common.physics.terrain.chunk.ChunkCollisions} of the chunk, for collisions of the {@link fr.dynamx.api.physics.IPhysicsWorld}
     */
    public void markCollisionsDirty(boolean updatePhysicsTerrain) {
        if (world != null) {
            removeChunkCollisions();
        }
        boundingBoxCache = null;
        unrotatedCollisionsCache.clear();
        if (updatePhysicsTerrain && world != null && DynamXContext.usesPhysicsWorld(world)) {
            VerticalChunkPos pos1 = new VerticalChunkPos(getPos().getX() >> 4, getPos().getY() >> 4, getPos().getZ() >> 4);
            if (DynamXConfig.enableDebugTerrainManager) {
                ChunkLoadingTicket ticket = DynamXContext.getPhysicsWorld(world).getTerrainManager().getTicket(pos1);
                if (ticket != null)
                    ChunkGraph.addToGrah(pos1, ChunkGraph.ChunkActions.CHK_UPDATE, ChunkGraph.ActionLocation.MAIN, ticket.getCollisions(), "Chunk changed from DynamX TE markDirty opf " + getPackInfo() + " at " + getPos() + ". Ticket " + ticket);
            }
            DynamXContext.getPhysicsWorld(world).getTerrainManager().onChunkChanged(pos1);
        }
        if (world != null) {
            addChunkCollisions();
        }
    }

    @Override
    public Quaternion getCollidableRotation() {
        return packInfo == null ? QuaternionPool.get(0, 0, 0, 1) : DynamXGeometry.eulerToQuaternion((packInfo.getRotation().z - relativeRotation.z),
                ((packInfo.getRotation().y - relativeRotation.y + getRotation() * 22.5f) % 360),
                (packInfo.getRotation().x + relativeRotation.x));
    }

    @Override
    public Vector3f getCollisionOffset() {
        return Vector3fPool.get(0.5f, 0, 0.5f).addLocal(relativeTranslation);
    }

    @Override
    public void onLoad() {
        addChunkCollisions();
    }

    /**
     * Adds the block collisions to the chunk data using {@link DynamXChunkData} <br>
     * Used for large blocks that can be in multiple chunks
     */
    protected void addChunkCollisions() {
        List<Chunk> chunks = new ArrayList<>();
        AxisAlignedBB bb = computeBoundingBox().offset(pos);
        for (int x = (int) bb.minX; x <= bb.minX + Math.max(1, bb.maxX - bb.minX); x += (int) Math.max(1, Math.min(bb.maxX - bb.minX, 16))) {
            for (int z = (int) bb.minZ; z <= bb.minZ + Math.max(1, bb.maxZ - bb.minZ); z += (int) Math.max(1, Math.min(bb.maxZ - bb.minZ, 16))) {
                if (!world.isChunkGeneratedAt(x >> 4, z >> 4)) {
                    ChunkPos pos = new ChunkPos(x >> 4, z >> 4);
                    if (!CommonEventHandler.PENDING_CHUNKS_COLLISIONS.containsKey(pos))
                        CommonEventHandler.PENDING_CHUNKS_COLLISIONS.put(pos, new HashMap<>());
                    CommonEventHandler.PENDING_CHUNKS_COLLISIONS.get(pos).put(this.pos, this.computeBoundingBox().offset(this.pos));
                    continue;
                }
                Chunk chunk = world.getChunk(x >> 4, z >> 4);
                if (!chunks.contains(chunk)) {
                    chunks.add(chunk);
                    DynamXChunkData data = chunk.getCapability(DynamXChunkDataProvider.DYNAMX_CHUNK_DATA_CAPABILITY, null);
                    Objects.requireNonNull(data).getBlocksAABB().put(this.pos, computeBoundingBox().offset(this.pos));
                }
            }
        }
    }

    /**
     * Removes the block collisions from the chunk data using {@link DynamXChunkData} <br>
     * Used for large blocks that can be in multiple chunks
     */
    protected void removeChunkCollisions() {
        List<Chunk> chunks = new ArrayList<>();
        AxisAlignedBB bb = computeBoundingBox().offset(pos);
        for (int x = (int) bb.minX; x <= bb.minX + Math.max(1, bb.maxX - bb.minX); x += (int) Math.max(1, Math.min(bb.maxX - bb.minX, 16))) {
            for (int z = (int) bb.minZ; z <= bb.minZ + Math.max(1, bb.maxZ - bb.minZ); z += (int) Math.max(1, Math.min(bb.maxZ - bb.minZ, 16))) {
                ChunkPos pos = new ChunkPos(x >> 4, z >> 4);
                if (CommonEventHandler.PENDING_CHUNKS_COLLISIONS.containsKey(pos))
                    CommonEventHandler.PENDING_CHUNKS_COLLISIONS.get(pos).remove(this.pos);
                if (!world.isChunkGeneratedAt(x >> 4, z >> 4)) {
                    return;
                }
                Chunk chunk = world.getChunk(x >> 4, z >> 4);
                if (!chunks.contains(chunk)) {
                    chunks.add(chunk);
                    DynamXChunkData data = chunk.getCapability(DynamXChunkDataProvider.DYNAMX_CHUNK_DATA_CAPABILITY, null);
                    data.getBlocksAABB().remove(this.pos);
                }
            }
        }
    }

    @Override
    public void onPackInfosReloaded() {
        setPackInfo(DynamXObjectLoaders.BLOCKS.findInfo(packInfo.getFullName()));
        for (IBlockEntityModule module : moduleList) {
            if (module instanceof IPackInfoReloadListener)
                ((IPackInfoReloadListener) module).onPackInfosReloaded();
        }
    }

    @Override
    public void update() {
        if (hasSeats && (seatEntities == null || seatEntities.stream().anyMatch(e -> e.isDead)) && !world.isRemote) {
            if (seatEntities != null) {
                seatEntities.forEach(Entity::setDead);
                seatEntities.clear();
            } else
                seatEntities = new ArrayList<>();
            List<PartBlockSeat> seats = packInfo.getPartsByType(PartBlockSeat.class);
            for (PartBlockSeat seat : seats) {
                SeatEntity entity = new SeatEntity(world, seat.getId());
                entity.setPosition(pos.getX() + 0.5f, pos.getY(), pos.getZ() + 0.5f);
                world.spawnEntity(entity);
                seatEntities.add(entity);
            }
        }
        if (initialized != EnumBlockEntityInitState.ALL) {
            if (initialized == EnumBlockEntityInitState.MODULES_CREATED) {
                getListenerModules();
            } else {
                initBlockEntityModules();
            }
        }
        if (!updateEntityListeners.isEmpty()) {
            updateEntityListeners.forEach(IBlockEntityModule.IBlockEntityUpdateListener::updateBlockEntity);
        }
        if (packInfo == null && !world.isRemote) {
            DynamXMain.log.error(String.format("Block info is null for te %s at %s. Removing it.", this, pos));
            world.setBlockToAir(pos);
        }
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
                box = DynamXContext.getCollisionHandler().rotateBB(Vector3fPool.get(), box, getCollidableRotation());
                Vector3f partPos = DynamXGeometry.rotateVectorByQuaternion(part.getPosition(), getCollidableRotation());
                partPos.addLocal(getPos().getX() + getPackInfo().getTranslation().x + getCollisionOffset().x,
                        getPos().getY() + 1.5f + getPackInfo().getTranslation().y + getCollisionOffset().y,
                        getPos().getZ() + getPackInfo().getTranslation().z + getCollisionOffset().z);
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

    public List<IBlockEntityModule> getModules() {
        return moduleList;
    }

    public enum EnumBlockEntityInitState {
        NOT_INITIALIZED, MODULES_CREATED, ALL
    }
}