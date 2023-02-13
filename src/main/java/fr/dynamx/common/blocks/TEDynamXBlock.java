package fr.dynamx.common.blocks;

import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.aym.acsguis.api.ACsGuiApi;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.events.DynamXBlockEvent;
import fr.dynamx.client.gui.GuiBlockCustomization;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.capability.DynamXChunkData;
import fr.dynamx.common.capability.DynamXChunkDataProvider;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.common.entities.ICollidableObject;
import fr.dynamx.common.handlers.CollisionInfo;
import fr.dynamx.utils.VerticalChunkPos;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TEDynamXBlock extends TileEntity implements ICollidableObject, ITickable, IPackInfoReloadListener {
    private BlockObject<?> blockObjectInfo;
    private int rotation;
    private Vector3f relativeTranslation = new Vector3f(), relativeScale = new Vector3f(), relativeRotation = new Vector3f();

    private CollisionInfo cachedCollisions;

    /**
     * The hitbox for (mouse) interaction with the block
     */
    protected AxisAlignedBB boundingBoxCache;

    public TEDynamXBlock() {
    }

    public TEDynamXBlock(BlockObject<?> blockObjectInfo) {
        this.blockObjectInfo = blockObjectInfo;
    }

    public BlockObject<?> getBlockObjectInfo() {
        return blockObjectInfo;
    }

    public void setBlockObjectInfo(BlockObject<?> blockObjectInfo) {
        this.blockObjectInfo = blockObjectInfo;
        this.world.markBlockRangeForRenderUpdate(this.pos, this.pos);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        if (blockObjectInfo != null)
            compound.setString("BlockInfo", blockObjectInfo.getFullName());
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
        super.writeToNBT(compound);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("BlockInfo"))
            blockObjectInfo = DynamXObjectLoaders.BLOCKS.findInfo(compound.getString("BlockInfo"));
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
        markCollisionsDirty();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return blockObjectInfo == null ? super.getMaxRenderDistanceSquared() : blockObjectInfo.getRenderDistance();
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

    public Vector3f getRelativeTranslation() {
        return relativeTranslation;
    }

    public void setRelativeTranslation(Vector3f relativeTranslation) {
        this.relativeTranslation = relativeTranslation;
    }

    public Vector3f getRelativeScale() {
        return relativeScale;
    }

    public void setRelativeScale(Vector3f relativeScale) {
        this.relativeScale = relativeScale;
    }

    public Vector3f getRelativeRotation() {
        return relativeRotation;
    }

    public void setRelativeRotation(Vector3f relativeRotation) {
        this.relativeRotation = relativeRotation;
    }

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
        markCollisionsDirty();
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
            QuaternionPool.getINSTANCE().openSubPool();
            Vector3fPool.openPool();
            List<AxisAlignedBB> boxes = getUnrotatedCollisionBoxes(); //Get PartShape boxes
            if (boxes.isEmpty()) {//If there is no boxes, create a default one
                boundingBoxCache = new AxisAlignedBB(0, 0, 0, 1, 1, 1);
            } else {
                MutableBoundingBox container;
                if (boxes.size() == 1) //If there is one, no more calculus to do !
                    container = new MutableBoundingBox(boxes.get(0));
                else {
                    container = new MutableBoundingBox(boxes.get(0));
                    for (int i = 1; i < boxes.size(); i++) { //Else create a bigger box containing all of the boxes
                        container.growTo(boxes.get(i));
                    }
                }
                //The container box corresponding to an unrotated entity, so rotate it !
                Quaternion physicsRotation = getCollisionInfo().getRotation();
                container = DynamXContext.getCollisionHandler().rotateBB(Vector3fPool.get(0.5f, 0, 0.5f), container, physicsRotation);
                container.grow(0.1, 0.0, 0.1); //Grow it to avoid little glitches on the corners of the car
                container.scale(getRelativeScale().x != 0 ? getRelativeScale().x : 1, getRelativeScale().y != 0 ? getRelativeScale().y : 1, getRelativeScale().z != 0 ? getRelativeScale().z : 1);
                container.offset(getRelativeTranslation().x, getRelativeTranslation().y, getRelativeTranslation().z);
                boundingBoxCache = container.toBB();
            }
            Vector3fPool.closePool();
            QuaternionPool.getINSTANCE().closeSubPool();
        }
        return boundingBoxCache;//.offset(pos);
    }

    /**
     * @return The collision boxes composing this entity, with no rotation and no position applied <br>
     * Used for all collisions of the entity (with players, vehicles, etc) <br>
     * The list is not modified by callers of the function <br>
     * <strong>Note : </strong>The list is cached by the callers of this function, so you need to call markCollisionsDirty() to refresh them.
     */
    public List<AxisAlignedBB> getUnrotatedCollisionBoxes() {
        if (blockObjectInfo == null) {
            return Collections.EMPTY_LIST;
        }
        return blockObjectInfo.getCollisionBoxes();
    }

    /**
     * @return The collision of this tile entity in the PhysicsWorld, without rotation or custom translation
     */
    public CompoundCollisionShape getPhysicsCollision() {
        return blockObjectInfo.getCompoundCollisionShape();
    }

    /**
     * Invalidates the block collisions caches, and reloads them <br>
     * Will provoke lag if you call this each tick
     */
    public void markCollisionsDirty() {
        cachedCollisions = null;
        boundingBoxCache = null;
        if (world != null && DynamXContext.usesPhysicsWorld(world)) {
            DynamXContext.getPhysicsWorld(world).getTerrainManager().onChunkChanged(new VerticalChunkPos(getPos().getX() >> 4, getPos().getY() >> 4, getPos().getZ() >> 4));
        }
        if (world != null) {
            DynamXChunkData data = world.getChunk(pos).getCapability(DynamXChunkDataProvider.DYNAM_X_CHUNK_DATA_CAPABILITY, null);
            data.getBlocksAABB().put(pos, computeBoundingBox().offset(pos));
        }
    }

    @Override
    public CollisionInfo getCollisionInfo() {
        if (blockObjectInfo != null && cachedCollisions == null) {
            List<AxisAlignedBB> unrotatedCollisionsCache = new ArrayList<>();
            for (AxisAlignedBB shape : getUnrotatedCollisionBoxes()) {
                MutableBoundingBox b = new MutableBoundingBox(shape);
                b.scale(relativeScale.x != 0 ? relativeScale.x : 1, relativeScale.y != 0 ? relativeScale.y : 1, relativeScale.z != 0 ? relativeScale.z : 1);
                b.offset(-0.5f, -0.5f, -0.5f);
                b.offset(relativeTranslation.x, relativeTranslation.y, relativeTranslation.z);
                unrotatedCollisionsCache.add(b.toBB());
            }
            Quaternion rotation = DynamXGeometry.eulerToQuaternion((blockObjectInfo.getRotation().z - relativeRotation.z),
                    ((blockObjectInfo.getRotation().y - relativeRotation.y + getRotation() * 22.5f) % 360),
                    (blockObjectInfo.getRotation().x + relativeRotation.x));
            System.out.println("NEW COLL WITH R " + rotation);
            cachedCollisions = new CollisionInfo(unrotatedCollisionsCache, new Vector3f(pos.getX(), pos.getY(), pos.getZ()).add(0.5f, 0.5f, 0.5f), rotation);
            //cachedCollisions = new CollisionInfo(Arrays.asList(new MutableBoundingBox(Block.FULL_BLOCK_AABB).offset(-0.5f, -0.5f, -0.5f)), new Vector3f(pos.getX()+0.5f, pos.getY()+0.5f, pos.getZ()+0.5f), rotation);
        }
        if(cachedCollisions == null) //IZNOGOOD
            return new CollisionInfo(Arrays.asList(Block.FULL_BLOCK_AABB.offset(-0.5f, -0.5f, -0.5f)), new Vector3f(pos.getX()+0.5f, pos.getY()+0.5f, pos.getZ()+0.5f), Quaternion.IDENTITY);
        return cachedCollisions;
    }

    @Override
    public void onLoad() {
        DynamXChunkData data = world.getChunk(pos).getCapability(DynamXChunkDataProvider.DYNAM_X_CHUNK_DATA_CAPABILITY, null);
        data.getBlocksAABB().put(pos, computeBoundingBox().offset(pos));
    }

    @Override
    public void update() {
        MinecraftForge.EVENT_BUS.post(new DynamXBlockEvent.TickTileEntity(Side.SERVER, (DynamXBlock<?>) this.getBlockType(), getWorld(), this));
    }

    @Override
    public void onPackInfosReloaded() {
        blockObjectInfo = DynamXObjectLoaders.BLOCKS.findInfo(blockObjectInfo.getFullName());
    }
}