package fr.dynamx.common.objloader;


import fr.dynamx.common.objloader.data.Material;
import fr.dynamx.common.objloader.data.ObjObjectData;
import fr.dynamx.common.objloader.data.Vertex;
import lombok.Getter;
import lombok.Setter;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class IndexedModel {
    @Getter
    private final List<Vector3f> vertices = new ArrayList<>();
    @Getter
    private final List<Vector2f> texCoords = new ArrayList<>();
    @Getter
    private final List<Vector3f> normals = new ArrayList<>();

    //TODO CLEAN
    @Getter
    @Setter
    private IntBuffer indices;
    @Getter
    private final List<OBJIndex> objIndices = new ArrayList<>();
    @Getter
    private final List<String> indicedMaterials = new ArrayList<>();
    @Getter
    public final Map<String, Material.IndexPair> materials = new HashMap<>();

    public void toMesh(ObjObjectData mesh) {
        int n = Math.min(vertices.size(), Math.min(texCoords.size(), normals.size()));
        Vertex[] verticesArray = IntStream.range(0, n).mapToObj(i -> new Vertex(vertices.get(i),
                texCoords.get(i),
                normals.get(i))).toArray(Vertex[]::new);

        mesh.setVertices(verticesArray);
        mesh.setIndices(indices);
        mesh.setMaterialForEachVertex(indicedMaterials.toArray(new String[0]));
        mesh.setMaterials(materials);
    }

    public void computeNormals() {
        for (int i = 0; i < indices.capacity(); i += 3) {
            int i0 = indices.get(i);
            int i1 = indices.get(i + 1);
            int i2 = indices.get(i + 2);

            Vector3f v = (Vector3f) vertices.get(i1).clone();
            v.sub(vertices.get(i0));
            Vector3f l0 = v;
            v = (Vector3f) vertices.get(i2).clone();
            v.sub(vertices.get(i0));
            Vector3f l1 = v;
            v = (Vector3f) l0.clone();
            v.cross(l0, l1);
            Vector3f normal = v;

            v = (Vector3f) normals.get(i0).clone();
            v.add(normal);
            normals.set(i0, v);
            v = (Vector3f) normals.get(i1).clone();
            v.add(normal);
            normals.set(i1, v);
            v = (Vector3f) normals.get(i2).clone();
            v.add(normal);
            normals.set(i2, v);
        }

        for (Vector3f normal : normals) normal.normalize();
    }

    public Vector3f computeCenter() {
        float x = 0;
        float y = 0;
        float z = 0;
        for (Vector3f position : vertices) {
            x += position.x;
            y += position.y;
            z += position.z;
        }
        x /= vertices.size();
        y /= vertices.size();
        z /= vertices.size();
        return new Vector3f(x, y, z);
    }

    public final static class OBJIndex {
        public int positionIndex;
        public int texCoordsIndex;
        public int normalIndex;

        public boolean equals(Object o) {
            if (o instanceof OBJIndex) {
                OBJIndex index = (OBJIndex) o;
                return index.normalIndex == normalIndex && index.positionIndex == positionIndex && index.texCoordsIndex == texCoordsIndex;
            }

            return false;
        }

        public int hashCode() {
            final int base = 17;
            final int multiplier = 31;

            int result = base;
            result = multiplier * result + positionIndex;
            result = multiplier * result + texCoordsIndex;
            result = multiplier * result + normalIndex;
            return result;
        }
    }
}
