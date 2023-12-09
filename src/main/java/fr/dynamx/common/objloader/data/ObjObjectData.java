package fr.dynamx.common.objloader.data;

import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import javax.vecmath.Vector3f;
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
    private int[] indices;
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

    public float[] getVerticesPos() {
        float[] pos = new float[vertices.length * 3];
        for (int i = 0; i < vertices.length; i++) {
            pos[i * 3] = vertices[i].getPos().x;
            pos[i * 3 + 1] = vertices[i].getPos().y;
            pos[i * 3 + 2] = vertices[i].getPos().z;
        }
        return pos;
    }

    public float[] getVerticesNormals() {
        float[] pos = new float[vertices.length * 3];
        for (int i = 0; i < vertices.length; i++) {
            pos[i * 3] = vertices[i].getNormal().x;
            pos[i * 3 + 1] = vertices[i].getNormal().y;
            pos[i * 3 + 2] = vertices[i].getNormal().z;
        }
        return pos;
    }

    public float[] getTextureCoords() {
        float[] pos = new float[vertices.length * 2];
        for (int i = 0; i < vertices.length; i++) {
            pos[i * 2] = vertices[i].getTexCoords().x;
            pos[i * 2 + 1] = 1 - vertices[i].getTexCoords().y;
        }
        return pos;
    }

    public com.jme3.math.Vector3f min(@Nullable com.jme3.math.Vector3f result) {
        if (vertices.length == 0) {
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
