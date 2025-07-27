package fr.dynamx.common.physics.terrain.element;

import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.collision.shapes.infos.IndexedMesh;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.api.physics.terrain.ITerrainElement;
import fr.dynamx.utils.VerticalChunkPos;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.debug.TerrainDebugData;
import fr.dynamx.utils.debug.TerrainDebugRenderer;
import net.minecraft.world.World;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

public class VehicleTerrainElement implements ITerrainElement {
    private final IndexedMesh mesh;
    private PhysicsRigidBody body;
    private final Map<Integer, TerrainDebugData> debugData = new HashMap<>();

    public VehicleTerrainElement(IndexedMesh mesh) {
        this.mesh = mesh;
    }

    @Override
    public PhysicsRigidBody build(World world, Vector3f pos) {
        if (mesh != null) {
            MeshCollisionShape shape = new MeshCollisionShape(false, mesh);
            shape.setMargin(0.04f);
            body = new PhysicsRigidBody(shape, 0);
            body.setRestitution(1f);
            body.setFriction(0.8f);
            body.setUserObject(new BulletShapeType<>(EnumBulletShapeType.TERRAIN, this));

           // body.setPhysicsLocation(pos);
        }
        return body;
    }

    @Override
    public PhysicsRigidBody getBody() {
        return body;
    }

    @Override
    public void save(TerrainSaveType type, ObjectOutputStream to) throws IOException {

    }

    @Override
    public boolean load(TerrainSaveType type, ObjectInputStream from, VerticalChunkPos pos) throws IOException, ClassNotFoundException {
        return false;
    }

    @Override
    public void clear() {
        body = null;
        debugData.clear();
    }

    @Override
    public TerrainElementsFactory getFactory() {
        return TerrainElementsFactory.CUSTOM_SLOPE;
    }

    @Override
    public int[] getMaxSize() {
        return ITerrainElement.DEFAULT_SIZE;
    }

    @Override
    public void addDebugToWorld(World world, Vector3f pos) {
        if (mesh != null && DynamXDebugOptions.VEHICLE_MESH_WIREFRAME.isActive()) {
            FloatBuffer pb = mesh.copyVertexPositions();
            IntBuffer ib = mesh.copyIndices();
            pb.rewind();
            ib.rewind();
            float[] data = new float[ib.limit() * 3];
            for (int i = 0; i < ib.limit() / 3; i++) {
                for(int j=0;j<3;j++) {
                    int vertIndex = ib.get(i*3+j)*3;
                    data[i * 9 + j * 3] = pb.get(vertIndex);
                    data[i * 9 + j * 3 + 1] = pb.get(vertIndex + 1);
                    data[i * 9 + j * 3 + 2] = pb.get(vertIndex + 2);
                }
            }

            TerrainDebugData tdd = new TerrainDebugData(TerrainDebugRenderer.VEHICLE_MESH, data);
            debugData.put(tdd.getUuid(), tdd);
            (DynamXDebugOptions.VEHICLE_MESH_WIREFRAME).getDataIn().put(tdd.getUuid(), tdd);
        }
    }

    @Override
    public void removeDebugFromWorld(World world) {
        for (Integer uuid : debugData.keySet()) {
            (DynamXDebugOptions.VEHICLE_MESH_WIREFRAME).getDataIn().remove(uuid);
        }
        debugData.clear();
    }
}
