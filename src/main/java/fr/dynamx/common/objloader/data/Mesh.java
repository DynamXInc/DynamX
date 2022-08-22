package fr.dynamx.common.objloader.data;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.math.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Structure of an {@link ObjObjectData}
 */
public class Mesh {
    public int[] indices;
    public Vertex[] vertices;
    public Material[] materialForEachVertex;

    public List<Material> materialsList = new ArrayList<>();

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

    /**
     * Return maximum point in the mesh.
     */
    public Vector3f max() {
        if (vertices.length == 0) return new Vector3f();
        float maxX = vertices[0].getPos().x;
        float maxY = vertices[0].getPos().y;
        float maxZ = vertices[0].getPos().z;
        for (Vertex vertex : vertices) {
            if (vertex.getPos().x > maxX) maxX = vertex.getPos().x;
            if (vertex.getPos().y > maxY) maxY = vertex.getPos().y;
            if (vertex.getPos().z > maxZ) maxZ = vertex.getPos().z;
        }
        return new Vector3f(maxX, maxY, maxZ);
    }

    /**
     * Return minimum point in the mesh.
     */
    public Vector3f min() {
        if (vertices.length == 0) return new Vector3f(0f, 0f, 0f);
        float minX = vertices[0].getPos().x;
        float minY = vertices[0].getPos().y;
        float minZ = vertices[0].getPos().z;
        for (Vertex vertex : vertices) {
            if (vertex.getPos().x < minX) minX = vertex.getPos().x;
            if (vertex.getPos().y < minY) minY = vertex.getPos().y;
            if (vertex.getPos().z < minZ) minZ = vertex.getPos().z;
        }
        return new Vector3f(minX, minY, minZ);
    }


    /**
     * Return the center point of the mesh.
     */
    public Vector3f getCenter() {
        Vector3f max = max();
        Vector3f min = min();
        return new Vector3f((max.x + min.x) / 2f, (max.y + min.y) / 2f, (max.z + min.z) / 2f);
    }

    /**
     * Returns the dimensions of this mesh.
     */
    public Vector3f getDimension() {
        Vector3f max = max();
        Vector3f min = min();
        return new Vector3f((max.x - min.x) / 2, (max.y - min.y) / 2, (max.z - min.z) / 2);
    }

    public void addCollisionShape(CompoundCollisionShape to, Vector3f objectScale) {
        Vector3f half = getDimension().multLocal(objectScale);
        if (half.x != 0 || half.y != 0 || half.z != 0) {
            to.addChildShape(new BoxCollisionShape(half), getCenter());
        }
    }
}
