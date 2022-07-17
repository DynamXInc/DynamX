package fr.dynamx.common.physics.terrain.element;

import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.collision.shapes.infos.IndexedMesh;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.api.physics.terrain.ITerrainElement;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.utils.VerticalChunkPos;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.debug.TerrainDebugData;
import fr.dynamx.utils.debug.TerrainDebugRenderer;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A terrain element containing multiple faces of one slope <br>
 * NOT USED. WIP.
 */
@Deprecated
public class SlopeTerrainElement implements ITerrainElement {
    protected final List<SlopeFace> slopes = new ArrayList<>();
    protected MeshCollisionShape shape;
    private long poolId;
    protected TerrainDebugData debugData;

    public PhysicsRigidBody body;

    public SlopeTerrainElement() {
    }

    public SlopeTerrainElement(long poolId, Vector3f pos) {
        this.poolId = poolId;
        try {
            Vector3fPool.getPool(poolId, "" + pos).openSubPool();
        } catch (Exception e) {
            DynamXMain.log.fatal("Debug info " + poolId + " " + Vector3fPool.get(poolId, "" + pos) + " " + Vector3fPool.getInstances());
            throw new RuntimeException("Cannot continue, send this to aym", e);
        }
    }

    public List<SlopeFace> getSlopes() {
        return slopes;
    }

    public long getPoolId() {
        return poolId;
    }

    public void addSlope(SlopeFace slope) {
        slopes.add(slope);
    }

    public boolean empty() {
        return slopes.isEmpty();
    }

    @Override
    public void save(TerrainSaveType type, ObjectOutputStream out) throws IOException {
        out.writeInt(slopes.size());
        for (SlopeFace box : slopes) {
            //Write all collisions boxes as computed by fr.dynamx.impl.TerrainCollisionManager.loadBlockCollisions
            out.writeObject(box);
        }
        if (type.usesPlatformDependantOptimizations()) {
            //Write bullet's bvh data
            out.writeObject(shape.serializeBvh());
        }
    }

    @Override
    public boolean load(TerrainSaveType type, ObjectInputStream in, VerticalChunkPos pos) throws IOException, ClassNotFoundException {
        slopes.clear();
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            //Read all boxes as computed by fr.dynamx.impl.TerrainCollisionManager.loadBlockCollisions
            Object o = in.readObject();
            if (o instanceof SlopeFace)
                slopes.add((SlopeFace) o);
        }
        //Generate corresponding IndexedMeshes
        List<Vector3f> triangles = new ArrayList<>();
        int[] indices = new int[6 * 2 * slopes.size()];

        for (int i = 0; i < slopes.size(); i++) {
            //System.out.println("Setting slope at "+slopes.get(i).getOffset()+" "+triangles.size()+" "+slopes.get(i));
            IndexedMeshBuilder.appendSlopePointsToMesh(slopes.get(i).getPoints(), triangles, indices, i);
        }
        Vector3f debugPos = Vector3fPool.get(pos.x * 16, pos.y * 16, pos.z * 16);
        debugData = new TerrainDebugData(TerrainDebugRenderer.SLOPES, IndexedMeshBuilder.computeDebug(debugPos, triangles, indices));

        if (type.usesPlatformDependantOptimizations()) {
            //Read bullet's bvh data
            byte[] data = (byte[]) in.readObject();
            //Then create the shape
            shape = new MeshCollisionShape(data, new IndexedMesh(triangles.toArray(new Vector3f[0]), indices));
        } else
            //Then create the shape
            shape = new MeshCollisionShape(true, new IndexedMesh(triangles.toArray(new Vector3f[0]), indices));
        return true;
    }

    @Override
    public PhysicsRigidBody build(Vector3f pos) {
        applyOffset(pos.multLocal(-1));
        pos.multLocal(-1);
        if (shape == null) { //Not generated
            List<Vector3f> triangles = new ArrayList<>();
            int[] indices = new int[6 * 2 * slopes.size()];

            for (int i = 0; i < slopes.size(); i++) {
                //System.out.println("Setting slope at "+slopes.get(i).getOffset()+" "+triangles.size()+" "+slopes.get(i));
                IndexedMeshBuilder.appendSlopePointsToMesh(slopes.get(i).getPoints(), triangles, indices, i);
            }
            shape = new MeshCollisionShape(true, new IndexedMesh(triangles.toArray(new Vector3f[0]), indices));
            debugData = new TerrainDebugData(TerrainDebugRenderer.SLOPES, IndexedMeshBuilder.computeDebug(pos, triangles, indices));
        }

        PhysicsRigidBody pr = new PhysicsRigidBody(shape, 0);
        pr.setPhysicsLocation(pos);
        pr.setFriction(1);
        pr.setUserObject(new BulletShapeType<>(EnumBulletShapeType.SLOPE, this));

        if (poolId != 0) {
            Vector3fPool.getPool(poolId, "ste").closeSubPool();
            Vector3fPool.disposePool(poolId);
            poolId = 0;
        }
        body = pr;
        return pr;
    }

    @Nonnull
    @Override
    public PhysicsRigidBody getBody() {
        return body;
    }

    @Override
    public void addDebugToWorld(World mcWorld, Vector3f pos) {
        //System.out.println("Add debug of "+poolId+" at "+pos);
        (mcWorld.isRemote ? DynamXDebugOptions.CLIENT_SLOPE_BOXES : DynamXDebugOptions.SLOPE_BOXES).getDataIn().put(debugData.getUuid(), debugData);
    }

    @Override
    public void removeDebugFromWorld(World mcWorld) {
        //System.out.println("Remove debug of "+poolId);
        (mcWorld.isRemote ? DynamXDebugOptions.CLIENT_SLOPE_BOXES : DynamXDebugOptions.SLOPE_BOXES).getDataIn().remove(debugData.getUuid());
    }

    @Override
    public void clear() {
        debugData = null;
        shape = null;
        body = null;
        slopes.clear();
        if (poolId != 0)
            Vector3fPool.disposePool(poolId);
    }

    @Override
    public TerrainElementsFactory getFactory() {
        return TerrainElementsFactory.AUTO_SLOPES;
    }

    private void applyOffset(Vector3f offset) {
        for (SlopeFace b : slopes) {
            for (Vector3f point : b.getPoints()) {
                point.addLocal(offset);
            }
        }
    }
}
