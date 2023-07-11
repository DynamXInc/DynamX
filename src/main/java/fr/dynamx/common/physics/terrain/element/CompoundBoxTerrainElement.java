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
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;

/**
 * A terrain element containing multiple boxes
 */
public class CompoundBoxTerrainElement implements ITerrainElement {
    private MeshCollisionShape shape;
    private List<MutableBoundingBox> collisions;
    private final Map<Integer, TerrainDebugData> debugData = new HashMap<>();

    private List<IndexedMesh> meshes;
    private PhysicsRigidBody body;

    public CompoundBoxTerrainElement() {
    }

    public CompoundBoxTerrainElement(int x, int y, int z, List<MutableBoundingBox> boxes) {
        IndexedMeshBuilder builder = new IndexedMeshBuilder(x, y, z, debugData);
        builder.addBoxes(boxes);
        this.meshes = builder.getMeshes();
        this.collisions = boxes;
    }

    @Override
    public String toString() {
        System.out.println("===============");
        System.out.println("Box data " + this.hashCode());
        System.out.println("Collisions " + collisions);
        System.out.println("--------- Collisions stats: ---------");
        if (collisions != null) {
            System.out.println("Count: " + collisions.size());
            Map<Double, Integer> perHeightMin = new HashMap<>();
            Map<Double, Integer> perHeightMax = new HashMap<>();
            for (MutableBoundingBox box : collisions) {
                perHeightMin.put(box.minY, perHeightMin.getOrDefault(box.minY, 0) + 1);
                perHeightMax.put(box.maxY, perHeightMax.getOrDefault(box.maxY, 0) + 1);
            }
            System.out.println("Min height repartition: " + perHeightMin);
            System.out.println("Max height repartition: " + perHeightMin);
        } else {
            System.out.println("No collisions loaded");
        }
        System.out.println("Shape " + shape);
        if (body != null) {
            System.out.println("Body transform " + body.getPhysicsLocation(Vector3fPool.get()) + " " + body.getPhysicsRotation(QuaternionPool.get()));
        } else {
            System.out.println("Body not initialized");
        }
        if (meshes == null) {
            System.out.println("Meshes null : wtf");
        } else {
            System.out.println("Indexed meshes :");
            for (IndexedMesh m : meshes) {
                System.out.println(m + " : " + m.countTriangles() + " " + m.countVertices());
            }
        }
        System.out.println("===============");
        return "CompoundBoxTerrainElement{" +
                "collisions: " + collisions.size() +
                '}';
    }

    @Override
    public void save(TerrainSaveType type, ObjectOutputStream out) throws IOException {
        if (meshes != null && meshes.isEmpty()) //No boxes (empty element)
        {
            out.writeInt(-1);
        } else {
            out.writeInt(collisions.size());
            for (MutableBoundingBox box : collisions) {
                //Write all collisions boxes as computed by the TerrainCollisionManager
                out.writeObject(box);
            }
            if (type.usesPlatformDependantOptimizations()) {
                //Write bullet's bvh data
                out.writeObject(shape.serializeBvh());
            }
        }
    }

    @Override
    public boolean load(TerrainSaveType type, ObjectInputStream in, VerticalChunkPos pos) throws IOException, ClassNotFoundException {
        List<MutableBoundingBox> boxes = new ArrayList<>();
        int size = in.readInt();
        if (size == -1) //No boxes (empty element)
        {
            meshes = Collections.EMPTY_LIST;
            shape = null;
        } else {
            //if(!type.usesPlatformDependantOptimizations())
            //  System.out.println("Size "+pos+" "+size);
            //  long start = System.currentTimeMillis();
            for (int i = 0; i < size; i++) {
                //Read all boxes as computed by the TerrainCollisionManager
                boxes.add((MutableBoundingBox) in.readObject());
            }
            collisions = boxes; //05/07/20 we now need it to serialise and send chunk over network

            // start = System.currentTimeMillis()-start;
            // long start2 = System.currentTimeMillis();
            //Generate corresponding IndexedMeshes
            IndexedMeshBuilder builder = new IndexedMeshBuilder(-pos.x * 16, -pos.y * 16, -pos.z * 16, debugData);
            builder.addBoxes(boxes);
            //  start2 = System.currentTimeMillis()-start2;

            // long start3 = System.currentTimeMillis();
            meshes = builder.getMeshes(); //TODO REMOVE AFTER DEBUG
            if (type.usesPlatformDependantOptimizations()) {
                //Read bullet's bvh data
                byte[] data = (byte[]) in.readObject();
                //Then create the shape
                shape = new MeshCollisionShape(data, builder.getMeshes().toArray(new IndexedMesh[0]));
            } else
                //Then create the shape
                shape = new MeshCollisionShape(true, builder.getMeshes().toArray(new IndexedMesh[0]));
            //start3 = System.currentTimeMillis()-start3;

            // System.out.println("Timings are "+start+" "+start2+" "+start3);
        }
        return true;
    }

    @Override
    public PhysicsRigidBody build(World world, Vector3f pos) {
        if (shape == null) { //Not generated
            if (meshes.isEmpty()) //No boxes (empty element)
                return null;
            shape = new MeshCollisionShape(true, meshes.toArray(new IndexedMesh[0]));
        }
        PhysicsRigidBody rigidBody = new PhysicsRigidBody(shape, 0);
        rigidBody.setPhysicsLocation(pos);
        rigidBody.setRestitution(1);
        rigidBody.setUserObject(new BulletShapeType<>(EnumBulletShapeType.TERRAIN, this, rigidBody.getCollisionShape()));
        body = rigidBody;
        return rigidBody;
    }

    @Nonnull
    @Override
    public PhysicsRigidBody getBody() {
        return body;
    }

    @Override
    public void addDebugToWorld(World mcWorld, Vector3f pos) {
        if (!debugData.isEmpty())
            (mcWorld.isRemote ? DynamXDebugOptions.CLIENT_BLOCK_BOXES : DynamXDebugOptions.BLOCK_BOXES).getDataIn().putAll(debugData);
    }

    @Override
    public void removeDebugFromWorld(World mcWorld) {
        for (Integer pos : debugData.keySet()) {
            (mcWorld.isRemote ? DynamXDebugOptions.CLIENT_BLOCK_BOXES : DynamXDebugOptions.BLOCK_BOXES).getDataIn().remove(pos);
        }
    }

    @Override
    public void clear() {
        //don't clear because used to save this shape = null;
        body = null;
        debugData.clear();
    }

    @Override
    public TerrainElementsFactory getFactory() {
        return TerrainElementsFactory.COMPOUND_BOX;
    }
}
