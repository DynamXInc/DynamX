package fr.dynamx.core.common.physics.terrain.element;

import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.api.physics.terrain.ITerrainElement;
import fr.dynamx.core.common.DynamXMain;
import fr.dynamx.core.common.blocks.TEDynamXBlock;
import fr.dynamx.core.utils.VerticalChunkPos;
import fr.dynamx.core.utils.debug.DynamXDebugOptions;
import fr.dynamx.core.utils.debug.TerrainDebugData;
import fr.dynamx.core.utils.debug.TerrainDebugRenderer;
import fr.dynamx.core.utils.optimization.BoundingBoxPool;
import fr.dynamx.core.utils.optimization.QuaternionPool;
import fr.dynamx.core.utils.optimization.SubClassPool;
import fr.hermes.api.mc.blocks.HmBlockEntity;
import fr.hermes.api.mc.world.HmWorld;
import fr.dynamx.core.utils.optimization.JmeVector3fPool;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * A terrain element containing multiple boxes
 */
public class DynamXBlockTerrainElement implements ITerrainElement {
    private int x, y, z;
    private BlockPos pos;
    private PhysicsRigidBody body;
    private TerrainDebugData debugData;

    public DynamXBlockTerrainElement() {
    }

    public DynamXBlockTerrainElement(int x, int y, int z, BlockPos pos) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.pos = new BlockPos(pos);
    }

    @Override
    public void save(TerrainSaveType type, ObjectOutputStream out) throws IOException {
        out.writeInt(x);
        out.writeInt(y);
        out.writeInt(z);
        out.writeInt(pos.getX());
        out.writeInt(pos.getY());
        out.writeInt(pos.getZ());
    }

    @Override
    public boolean load(TerrainSaveType type, ObjectInputStream in, VerticalChunkPos pos) throws IOException, ClassNotFoundException {
        x = in.readInt();
        y = in.readInt();
        z = in.readInt();
        this.pos = new BlockPos(in.readInt(), in.readInt(), in.readInt());
        return true;
    }

    @Override
    public PhysicsRigidBody build(HmWorld world, Vector3f pos) {
        HmBlockEntity te = world.hm$getTileEntity(this.pos);
        if (!(te instanceof TEDynamXBlock)) { //Not generated, should not happen because this should be removed from chunk
            DynamXMain.log.warn("[CHUNK DEBUG] Outdated DynamX block collisions found at: {}: TE not found. Maybe your packs have changed. The chunk will be reloaded", this.pos);
            return null;
        }
        PhysicsRigidBody p = new PhysicsRigidBody(((TEDynamXBlock) te).getPhysicsCollision(), 0);
        p.setPhysicsLocation(pos.add(JmeVector3fPool.get(x + 0.5f, y + 1.5f, z + 0.5f)).addLocal(((TEDynamXBlock) te).getRelativeTranslation()));
        p.setPhysicsRotation(((TEDynamXBlock) te).getCollidableRotation());
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
    public void addDebugToWorld(HmWorld mcWorld, Vector3f pos) {
        JmeVector3fPool.openPool();
        QuaternionPool.openPool();
        BoundingBoxPool.getPool().openSubPool(SubClassPool.BOUNDING_BOX_DEFAULT);

        BoundingBox b = body.getCollisionShape().boundingBox(body.getPhysicsLocation(JmeVector3fPool.get()), body.getPhysicsRotation(QuaternionPool.get()), BoundingBoxPool.get());
        Vector3f min = b.getMin(JmeVector3fPool.get());
        Vector3f max = b.getMax(JmeVector3fPool.get());
        debugData = new TerrainDebugData(TerrainDebugRenderer.DYNAMXBLOCKS, new float[]{min.x, min.y, min.z, max.x, max.y, max.z});
        (mcWorld.hm$isClient() ? DynamXDebugOptions.CLIENT_BLOCK_BOXES : DynamXDebugOptions.BLOCK_BOXES).getDataIn().put(debugData.getUuid(), debugData);

        BoundingBoxPool.getPool().closeSubPool();
        QuaternionPool.closePool();
        JmeVector3fPool.closePool();
    }

    @Override
    public void removeDebugFromWorld(HmWorld mcWorld) {
        if (debugData != null) {
            (mcWorld.hm$isClient() ? DynamXDebugOptions.CLIENT_BLOCK_BOXES : DynamXDebugOptions.BLOCK_BOXES).getDataIn().remove(debugData.getUuid());
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
