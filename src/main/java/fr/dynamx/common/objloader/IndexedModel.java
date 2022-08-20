package fr.dynamx.common.objloader;


import lombok.Getter;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class IndexedModel {

    @Getter
    private final List<Vector3f> vertices = new ArrayList<>();
    @Getter
    private final List<Vector2f> texCoords = new ArrayList<>();
    @Getter
    private final List<Vector3f> normals = new ArrayList<>();
    @Getter
    private final List<Vector3f> tangents = new ArrayList<>();
    @Getter
    private final List<Integer> indices = new ArrayList<>();
    @Getter
    private final List<OBJIndex> objIndices = new ArrayList<>();
    @Getter
    private final List<Material> materials = new ArrayList<>();

    public void toMesh(Mesh mesh) {
        int n = Math.min(vertices.size(), Math.min(texCoords.size(), normals.size()));
        Integer[] indicesArray = indices.toArray(new Integer[0]);
        Vertex[] verticesArray = IntStream.range(0, n).mapToObj(i -> new Vertex(vertices.get(i),
                texCoords.get(i),
                normals.get(i), new Vector3f())).toArray(Vertex[]::new);
        int[] indicesArrayInt = Arrays.stream(indicesArray).mapToInt(integer -> integer).toArray();
        mesh.vertices = verticesArray;
        mesh.indices = indicesArrayInt;
        mesh.materialForEachVertex = materials.toArray(new Material[0]);

        List<Material> materialArrayList = new ArrayList<>();
        if (mesh.materialForEachVertex.length > 0) {
            materialArrayList.add(mesh.materialForEachVertex[0]);
            Material firstMaterial = mesh.materialForEachVertex[0];
            for (int i = 0; i < mesh.materialForEachVertex.length; i++) {
                if (mesh.materialForEachVertex[i] != firstMaterial) {
                    firstMaterial = mesh.materialForEachVertex[i];
                    materialArrayList.add(firstMaterial);
                }
            }
        }
        mesh.materialsList = materialArrayList;
    }

    public void computeNormals() {
        for (int i = 0; i < indices.size(); i += 3) {
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

    public void computeTangents() {
        tangents.clear();
        for (int i = 0; i < vertices.size(); i++)
            tangents.add(new Vector3f());

        for (int i = 0; i < indices.size(); i += 3) {
            int i0 = indices.get(i);
            int i1 = indices.get(i + 1);
            int i2 = indices.get(i + 2);

            Vector3f v = (Vector3f) vertices.get(i1).clone();
            v.sub(vertices.get(i0));
            Vector3f edge1 = v;
            v = (Vector3f) vertices.get(i2).clone();
            v.sub(vertices.get(i0));
            Vector3f edge2 = v;

            double deltaU1 = texCoords.get(i1).x - texCoords.get(i0).x;
            double deltaU2 = texCoords.get(i2).x - texCoords.get(i0).x;
            double deltaV1 = texCoords.get(i1).y - texCoords.get(i0).y;
            double deltaV2 = texCoords.get(i2).y - texCoords.get(i0).y;

            double dividend = (deltaU1 * deltaV2 - deltaU2 * deltaV1);
            double f = dividend == 0.0f ? 0.0f : 1.0f / dividend;

            Vector3f tangent = new Vector3f((float) (f * (deltaV2 * edge1.x - deltaV1 * edge2.x)), (float) (f * (deltaV2 * edge1.y - deltaV1 * edge2.y)), (float) (f * (deltaV2 * edge1.z - deltaV1 * edge2.z)));

            v = (Vector3f) tangents.get(i0).clone();
            v.add(tangent);
            tangents.set(i0, v);
            v = (Vector3f) tangents.get(i1).clone();
            v.add(tangent);
            tangents.set(i1, v);
            v = (Vector3f) tangents.get(i2).clone();
            v.add(tangent);
            tangents.set(i2, v);
        }

        for (Vector3f tangent : tangents) tangent.normalize();
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
