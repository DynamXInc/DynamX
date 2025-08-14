package fr.dynamx.common.objloader.data;

import lombok.Getter;
import lombok.Setter;
import org.lwjgl.BufferUtils;

import javax.annotation.Nullable;
import javax.vecmath.Vector3f;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

public class ObjObjectData{
    @Getter
    private final String name;
    @Getter
    @Setter
    private Vector3f center;

    @Getter
    @Setter
    private IntBuffer indices;
    @Getter
    @Setter
    private Vertex[] vertices;
    @Getter
    @Setter
    private String[] materialForEachVertex;
    @Getter
    @Setter
    private Map<String, Material.IndexPair> materials = new HashMap<>();

    public ObjObjectData(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "ObjObjectData{" +
                "name='" + name + '\'' +
                '}';
    }

    public FloatBuffer getVerticesPos() {
        FloatBuffer pos = BufferUtils.createFloatBuffer(vertices.length * 3);
        for (Vertex vertex : vertices) {
            pos.put(vertex.getPos().x);
            pos.put(vertex.getPos().y);
            pos.put(vertex.getPos().z);
        }
        pos.flip();
        return pos;
    }

    public FloatBuffer getVerticesNormals() {
        FloatBuffer pos = BufferUtils.createFloatBuffer(vertices.length * 3);
        for (Vertex vertex : vertices) {
            pos.put(vertex.getNormal().x);
            pos.put(vertex.getNormal().y);
            pos.put(vertex.getNormal().z);
        }
        pos.flip();
        return pos;
    }

    public FloatBuffer getTextureCoords() {
        FloatBuffer pos = BufferUtils.createFloatBuffer(vertices.length * 2);
        for (Vertex vertex : vertices) {
            pos.put(vertex.getTexCoords().x);
            pos.put(1 - vertex.getTexCoords().y);
        }
        pos.flip();
        return pos;
    }

    public com.jme3.math.Vector3f min(@Nullable com.jme3.math.Vector3f result) {
        if (vertices == null || vertices.length == 0) {
            if (result == null)
                return new com.jme3.math.Vector3f();
            return result.set(0, 0, 0);
        }
        float minX = vertices[0].getPos().x;
        float minY = vertices[0].getPos().y;
        float minZ = vertices[0].getPos().z;
        for (Vertex vertex : vertices) {
            if (vertex.getPos().x < minX) minX = vertex.getPos().x;
            if (vertex.getPos().y < minY) minY = vertex.getPos().y;
            if (vertex.getPos().z < minZ) minZ = vertex.getPos().z;
        }
        if (result == null)
            return new com.jme3.math.Vector3f(minX, minY, minZ);
        return result.set(minX, minY, minZ);
    }

    public com.jme3.math.Vector3f max(@Nullable com.jme3.math.Vector3f result) {
        if (vertices.length == 0) {
            if (result == null)
                return new com.jme3.math.Vector3f();
            return result.set(0, 0, 0);
        }
        float maxX = vertices[0].getPos().x;
        float maxY = vertices[0].getPos().y;
        float maxZ = vertices[0].getPos().z;
        for (Vertex vertex : vertices) {
            if (vertex.getPos().x > maxX) maxX = vertex.getPos().x;
            if (vertex.getPos().y > maxY) maxY = vertex.getPos().y;
            if (vertex.getPos().z > maxZ) maxZ = vertex.getPos().z;
        }
        if (result == null)
            return new com.jme3.math.Vector3f(maxX, maxY, maxZ);
        return result.set(maxX, maxY, maxZ);
    }

    /**
     * Releases memory when the vao objects has been compiled, or the server started
     */
    public void clearData() {
        indices = null;
        vertices = null;
        materialForEachVertex = null;
    }
}
