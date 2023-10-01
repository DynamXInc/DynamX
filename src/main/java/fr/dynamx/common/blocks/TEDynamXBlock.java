package fr.dynamx.common.blocks;

import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.aym.acsguis.api.ACsGuiApi;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.client.gui.GuiBlockCustomization;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.capability.DynamXChunkData;
import fr.dynamx.common.capability.DynamXChunkDataProvider;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.type.ObjectCollisionsHelper;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.common.entities.ICollidableObject;
import fr.dynamx.common.entities.modules.AbstractLightsModule;
import fr.dynamx.utils.VerticalChunkPos;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TEDynamXBlock extends TileEntity implements ICollidableObject, IPackInfoReloadListener {
    private BlockObject<?> blockObjectInfo;
    private int rotation;
    private Vector3f relativeTranslation = new Vector3f(), relativeScale = new Vector3f(), relativeRotation = new Vector3f();

    /**
     * The cache of the block collisions, with position offset but no rotation
     */
    protected final List<MutableBoundingBox> unrotatedCollisionsCache = new ArrayList<>();
    /**
     * The hitbox for (mouse) interaction with the block
     */
    protected AxisAlignedBB boundingBoxCache;

    @Getter
    private AbstractLightsModule lightsModule;

    public TEDynamXBlock() {
    }

    public TEDynamXBlock(BlockObject<?> blockObjectInfo) {
        setBlockObjectInfo(blockObjectInfo);
    }

    public BlockObject<?> getBlockObjectInfo() {
        return blockObjectInfo;
    }

    public void setBlockObjectInfo(BlockObject<?> blockObjectInfo) {
        this.blockObjectInfo = blockObjectInfo;
        if(world != null)
            world.markBlockRangeForRenderUpdate(pos, pos);
        if(blockObjectInfo != null && !blockObjectInfo.getLightSources().isEmpty())
            lightsModule = new AbstractLightsModule.LightsModule(blockObjectInfo);
        else
            lightsModule = null;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        if (blockObjectInfo != null)
            compound.setString("BlockInfo", blockObjectInfo.getFullName());
        if(lightsModule != null)
            lightsModule.writeToNBT(compound);
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
            setBlockObjectInfo(DynamXObjectLoaders.BLOCKS.findInfo(compound.getString("BlockInfo")));
        if(lightsModule != null)
            lightsModule.readFromNBT(compound);
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

        if(blockObjectInfo == null && world != null && !world.isRemote) {
            DynamXMain.log.warn("Block object info is null for te " + this + " at " + pos+ ". Removing it.");
            world.setBlockToAir(pos);
        }
        else
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
            QuaternionPool.getINSTANCE().closeSubPool();
        }
        return boundingBoxCache;//.offset(pos);
    }

    @Override
    public List<MutableBoundingBox> getCollisionBoxes() {
        if (blockObjectInfo != null && unrotatedCollisionsCache.size() != getUnrotatedCollisionBoxes().size()) {
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
        if (blockObjectInfo == null) {
            return Collections.EMPTY_LIST;
        }
        return blockObjectInfo.getCollisionsHelper().getShapes();
    }

    /**
     * @return The collision of this tile entity in the PhysicsWorld, without rotation or custom translation
     */
    public CompoundCollisionShape getPhysicsCollision() {
        if (blockObjectInfo == null) {
            throw new IllegalStateException("BlockObjectInfo is null for te " + this + " at " + pos);
        }
        if (!blockObjectInfo.getCollisionsHelper().hasPhysicsCollisions()) {
            return ObjectCollisionsHelper.EMPTY_COLLISION_SHAPE;
        }
        return blockObjectInfo.getCollisionsHelper().getPhysicsCollisionShape();
    }

    /**
     * Invalidates the block collisions caches, and reloads them <br>
     * Will provoke lag if you call this each tick
     */
    public void markCollisionsDirty() {
        boundingBoxCache = null;
        unrotatedCollisionsCache.clear();
        if (world != null && DynamXContext.usesPhysicsWorld(world)) {
            DynamXContext.getPhysicsWorld(world).getTerrainManager().onChunkChanged(new VerticalChunkPos(getPos().getX() >> 4, getPos().getY() >> 4, getPos().getZ() >> 4));
        }
        if (world != null) {
            DynamXChunkData data = world.getChunk(pos).getCapability(DynamXChunkDataProvider.DYNAM_X_CHUNK_DATA_CAPABILITY, null);
            data.getBlocksAABB().put(pos, computeBoundingBox().offset(pos));
        }
    }

    @Override
    public Quaternion getCollidableRotation() {
        return blockObjectInfo == null ? QuaternionPool.get(0, 0, 0, 1) : DynamXGeometry.eulerToQuaternion((blockObjectInfo.getRotation().z - relativeRotation.z),
                ((blockObjectInfo.getRotation().y - relativeRotation.y + getRotation() * 22.5f) % 360),
                (blockObjectInfo.getRotation().x + relativeRotation.x));
    }

    @Override
    public Vector3f getCollisionOffset() {
        return Vector3fPool.get(0.5f, 0, 0.5f).addLocal(relativeTranslation);
    }

    @Override
    public void onLoad() {
        DynamXChunkData data = world.getChunk(pos).getCapability(DynamXChunkDataProvider.DYNAM_X_CHUNK_DATA_CAPABILITY, null);
        data.getBlocksAABB().put(pos, computeBoundingBox().offset(pos));
    }

    @Override
    public void onPackInfosReloaded() {
        setBlockObjectInfo(DynamXObjectLoaders.BLOCKS.findInfo(blockObjectInfo.getFullName()));
    }
}