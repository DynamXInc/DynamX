package fr.dynamx.common.physics.terrain.element;

import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.api.physics.terrain.ITerrainElement;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.utils.VerticalChunkPos;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.debug.TerrainDebugData;
import fr.dynamx.utils.debug.TerrainDebugRenderer;
import fr.dynamx.utils.optimization.BoundingBoxPool;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * A terrain element containing multiple boxes
 */
public class DynamXBlockTerrainElement implements ITerrainElement
{
    private int x, y, z;
    private BlockPos pos;
    private PhysicsRigidBody body;
    private TerrainDebugData debugData;
    private Vector3f position; //TODO OPTI : THIS IS IN THE TE
    private Quaternion rotation;

    public DynamXBlockTerrainElement() {}

    public DynamXBlockTerrainElement(int x, int y, int z, BlockPos pos, Vector3f position, Quaternion rotation) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.position = position;
        this.rotation = rotation;
        this.pos = new BlockPos(pos);
    }

    @Override
    public void save(TerrainSaveType type, ObjectOutputStream out) throws IOException {
        out.writeInt(x);
        out.writeInt(y);
        out.writeInt(z);
        out.writeFloat(position.x);
        out.writeFloat(position.y);
        out.writeFloat(position.z);
        out.writeFloat(rotation.getX());
        out.writeFloat(rotation.getY());
        out.writeFloat(rotation.getZ());
        out.writeFloat(rotation.getW());
        out.writeInt(pos.getX());
        out.writeInt(pos.getY());
        out.writeInt(pos.getZ());
    }

    @Override
    public boolean load(TerrainSaveType type, ObjectInputStream in, VerticalChunkPos pos) throws IOException, ClassNotFoundException {
        x = in.readInt();
        y = in.readInt();
        z = in.readInt();
        position = new Vector3f(in.readFloat(), in.readFloat(), in.readFloat());
        rotation = new Quaternion(in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat());
        this.pos = new BlockPos(in.readInt(), in.readInt(), in.readInt());
        return true;
    }

    @Override
    public PhysicsRigidBody build(Vector3f pos) {
        TileEntity te = DynamXContext.getPhysicsWorld().getTerrainManager().getWorld().getTileEntity(this.pos);
        if(!(te instanceof TEDynamXBlock)) { //Not generated, should not happen because this should be removed from chunk
            throw new IllegalStateException("DynamX block TE failed to load at "+pos);
        }
        PhysicsRigidBody p = new PhysicsRigidBody(((TEDynamXBlock) te).getPhysicsCollision(), 0);
        p.setPhysicsLocation(pos.add(Vector3fPool.get(x+0.5f, y+1.5f, z+0.5f).addLocal(((TEDynamXBlock) te).getBlockObjectInfo().getTranslation())).addLocal(position));
        p.setPhysicsRotation(rotation);
        p.setUserObject(new BulletShapeType<>(EnumBulletShapeType.TERRAIN, this));
        body = p;
        return p;
    }

    @Nonnull
    @Override
    public PhysicsRigidBody getBody() {
        return body;
    }

    @Override
    public void addDebugToWorld(World mcWorld, Vector3f pos) {
        QuaternionPool.openPool();
        BoundingBoxPool.getPool().openSubPool();

        BoundingBox b = body.getCollisionShape().boundingBox(body.getPhysicsLocation(Vector3fPool.get()), body.getPhysicsRotation(QuaternionPool.get()), BoundingBoxPool.get());
        Vector3f min = b.getMin(Vector3fPool.get());
        Vector3f max = b.getMax(Vector3fPool.get());
        debugData = new TerrainDebugData(TerrainDebugRenderer.DYNAMXBLOCKS, new float[] {min.x, min.y, min.z, max.x, max.y, max.z});
        (mcWorld.isRemote ? DynamXDebugOptions.CLIENT_BLOCK_BOXES : DynamXDebugOptions.BLOCK_BOXES).getDataIn().put(debugData.getUuid(), debugData);
        BoundingBoxPool.getPool().closeSubPool();
        QuaternionPool.closePool();
    }

    @Override
    public void removeDebugFromWorld(World mcWorld) {
        if(debugData != null) {
            (mcWorld.isRemote ? DynamXDebugOptions.CLIENT_BLOCK_BOXES : DynamXDebugOptions.BLOCK_BOXES).getDataIn().remove(debugData.getUuid());
        }
    }

    @Override
    public void clear() {
        //don't clear because used to save this shape = null;
        body = null;
    }

    @Override
    public TerrainElementsFactory getFactory() {
        return TerrainElementsFactory.DYNAMX_BLOCK;
    }
}
