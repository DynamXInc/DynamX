package fr.dynamx.common.physics.terrain.element;

import com.jme3.bullet.collision.shapes.SimplexCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.api.physics.terrain.ITerrainElement;
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
import java.util.Arrays;

public class CustomSlopeTerrainElement implements ITerrainElement.IPersistentTerrainElement {
    protected Vector3f[] points;
    protected PhysicsRigidBody body;
    private int[] maxSize = DEFAULT_SIZE;
    private final byte version;
    private TerrainDebugData debugData;

    public CustomSlopeTerrainElement() {
        this((byte) 0);
    }

    public CustomSlopeTerrainElement(byte version) {
        this.version = version;
    }

    public CustomSlopeTerrainElement(Vector3f[] points) {
        this((byte) 1);
        this.points = points;
    }

    @Override
    public PhysicsRigidBody build(World world, Vector3f pos) {
        SimplexCollisionShape shape = new SimplexCollisionShape(points);
        PhysicsRigidBody pr = new PhysicsRigidBody(shape, 0);
        Vector3f posFixed = Vector3fPool.get(pos).addLocal(8, -0.04f, 8);
        pr.setPhysicsLocation(posFixed);
        body = pr;
        pr.setFriction(1);
        pr.setUserObject(new BulletShapeType<>(EnumBulletShapeType.SLOPE, this));

        Vector3f half = shape.getHalfExtents(Vector3fPool.get());
        maxSize = new int[]{16 + (int) Math.floor(half.x), 16 + (int) Math.floor(half.y), 16 + (int) Math.floor(half.z)};
        //System.out.println("Got max size "+ Arrays.toString(maxSize) +" for "+this+" at "+pos);

        return pr;
    }

    @Nonnull
    @Override
    public PhysicsRigidBody getBody() {
        return body;
    }

    @Override
    public void save(TerrainSaveType type, ObjectOutputStream to) throws IOException {
        for (Vector3f v : points) {
            to.writeFloat(v.x);
            to.writeFloat(v.y);
            to.writeFloat(v.z);
            //System.out.println("Writing "+v);
        }
        //TODO FIX OUT OF [0,16[
        /*for(Vector3f v : points) {
            short compressedValue;
            int valuex = (int) (v.x * 2);
            compressedValue = (short) valuex;
            int valuey = (int) (v.y * 2);
            compressedValue = (short) (compressedValue | ((valuey) << 5));
            int valuez = (int) (v.z * 2);
            compressedValue = (short) (compressedValue | ((valuez) << 10));
            to.writeShort(compressedValue);

            System.out.println("Writing compressed "+v+" "+compressedValue);
        }*/
    }

    @Override
    public boolean load(TerrainSaveType type, ObjectInputStream from, VerticalChunkPos pos) throws IOException, ClassNotFoundException {
        points = new Vector3f[4];
        /*if(version == 1) {
            for (int i = 0; i < 4; i++) {
                short compressedValue = from.readShort();
                int valuex = compressedValue & 31;
                int valuey = compressedValue & 992;
                int valuez = compressedValue & 31744;
                points[i] = new Vector3f(valuex/2f, valuey/2f, valuez/2f);

                System.out.println("Reading compressed "+points[i]+" "+compressedValue+" at "+pos);
            }
        }
        else {*/
        for (int i = 0; i < 4; i++) {
            points[i] = new Vector3f(from.readFloat(), from.readFloat(), from.readFloat());
            //System.out.println("Reading "+points[i]+" at "+pos);
        }
        //}
        return true;
    }

    private static final int[] indices = new int[]{0, 1, 2, 2, 3, 0};

    @Override
    public void addDebugToWorld(World mcWorld, Vector3f pos) {
        float[] debugData = new float[indices.length * 3 + 3];
        for (int i = 0; i < indices.length; i++) {
            Vector3f pos1 = points[indices[i]];
            debugData[i * 3] = pos1.x + pos.x;
            debugData[i * 3 + 1] = pos1.y + pos.y;
            debugData[i * 3 + 2] = pos1.z + pos.z;
        }
        debugData[debugData.length - 3] = pos.x;
        debugData[debugData.length - 2] = pos.y + 8;
        debugData[debugData.length - 1] = pos.z;
        this.debugData = new TerrainDebugData(TerrainDebugRenderer.CUSTOM_SLOPE, debugData);
        (mcWorld.isRemote ? DynamXDebugOptions.CLIENT_SLOPE_BOXES : DynamXDebugOptions.SLOPE_BOXES).getDataIn().put(this.debugData.getUuid(), this.debugData);
    }

    @Override
    public void removeDebugFromWorld(World mcWorld) {
        if (this.debugData != null)
            (mcWorld.isRemote ? DynamXDebugOptions.CLIENT_SLOPE_BOXES : DynamXDebugOptions.SLOPE_BOXES).getDataIn().remove(this.debugData.getUuid());
    }

    @Override
    public void clear() {
        body = null;
    }

    @Override
    public TerrainElementsFactory getFactory() {
        return TerrainElementsFactory.CUSTOM_SLOPE;
    }

    private float[] debugDataCache;

    public float[] getDebugDataPreview(Vector3f pos) {
        if (debugDataCache == null) {
            debugDataCache = new float[indices.length * 3];
            for (int i = 0; i < indices.length; i++) {
                Vector3f pos1 = points[indices[i]];
                debugDataCache[i * 3] = pos1.x + pos.x;
                debugDataCache[i * 3 + 1] = pos1.y + pos.y;
                debugDataCache[i * 3 + 2] = pos1.z + pos.z;
            }
        }
        return debugDataCache;
    }

    @Override
    public int[] getMaxSize() {
        return maxSize;
    }

    @Override
    public String toString() {
        return "CustomSlopeTerrainElement{" +
                "points=" + Arrays.toString(points) +
                '}';
    }
}
