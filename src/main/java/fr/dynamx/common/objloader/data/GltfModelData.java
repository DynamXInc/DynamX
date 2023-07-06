package fr.dynamx.common.objloader.data;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.math.Vector3f;
import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.NodeModel;
import fr.dynamx.api.dxmodel.DxModelPath;
import lombok.Getter;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class GltfModelData extends DxModelData {

    @Getter
    private final List<NodeModel> nodeModels = new ArrayList<>();

    public GltfModelData(DxModelPath objModelPath) {
        super(objModelPath);
    }

    public float[] getVerticesPos() {
        List<float[]> posList = new ArrayList<>();
        int size = 0;
        for (NodeModel meshModel : nodeModels) {
            float[] pos = getVerticesPos(meshModel.getName().toLowerCase());
            posList.add(pos);
            size += pos.length;
        }
        float[] pos = new float[size];
        for (float[] floats : posList) {
            System.arraycopy(floats, 0, pos, 0, floats.length);
        }
        return pos;
    }

    public float[] getVerticesPos(String objectName) {
        float[] pos = new float[0];
        for (NodeModel nodeModel : nodeModels) {
            if (nodeModel.getName().toLowerCase().contains(objectName.toLowerCase())) {
                for (MeshModel meshModel : nodeModel.getMeshModels()) {
                    for (MeshPrimitiveModel meshPrimitiveModel : meshModel.getMeshPrimitiveModels()) {
                        AccessorModel accessorModel = meshPrimitiveModel.getAttributes().get("POSITION");
                        if (accessorModel.getComponentType() != 5126) {
                            return pos;
                        }
                        FloatBuffer floatBuffer = accessorModel.getBufferViewModel().getBufferModel().getBufferData().asFloatBuffer();
                        pos = new float[floatBuffer.limit()];
                        floatBuffer.get(pos);
                    }
                }
            }
        }
        return pos;
    }

    public Vector3f[] getVectorVerticesPos(String objectName) {
        float[] verticesPos = getVerticesPos(objectName);
        Vector3f[] vectorPos = new Vector3f[verticesPos.length / 3];
        for (int i = 0; i < verticesPos.length / 3; i++) {
            vectorPos[i / 3] = new Vector3f(verticesPos[i / 3], verticesPos[i / 3 + 1], verticesPos[i / 3 + 2]);
        }
        return vectorPos;
    }

    public int[] getMeshIndices(String objectName) {
        String objectNameLower = objectName.toLowerCase();

        IntStream indicesStream = nodeModels.stream()
                .filter(nodeModel -> nodeModel.getName().toLowerCase().contains(objectNameLower))
                .flatMapToInt(nodeModel -> nodeModel.getMeshModels().stream()
                        .flatMapToInt(meshModel -> meshModel.getMeshPrimitiveModels().stream()
                                .flatMapToInt(primitiveModel -> {
                                    IntBuffer intBuffer = primitiveModel.getIndices().getBufferViewModel()
                                            .getBufferModel()
                                            .getBufferData()
                                            .asIntBuffer();
                                    int[] meshIndices = new int[intBuffer.limit()];
                                    intBuffer.get(meshIndices);
                                    return IntStream.of(meshIndices);
                                })
                        )
                );
        return indicesStream.toArray();
    }

    public int[] getAllMeshIndices() {
        List<int[]> indicesList = new ArrayList<>();
        int size = 0;
        for (NodeModel nodeModel : nodeModels) {
            int[] indices = getMeshIndices(nodeModel.getName().toLowerCase());
            indicesList.add(indices);
            size += indices.length;
        }
        int[] indices = new int[size];
        for (int[] ints : indicesList) {
            System.arraycopy(ints, 0, indices, 0, ints.length);
        }
        return indices;
    }

    public Vector3f getMinOfMesh(String name) {
        Vector3f[] verticesPos = getVectorVerticesPos(name);
        if (verticesPos.length == 0) return Vector3f.ZERO;
        float minX = verticesPos[0].x;
        float minY = verticesPos[0].y;
        float minZ = verticesPos[0].z;
        for (Vector3f vertex : verticesPos) {
            if (vertex.x < minX) minX = vertex.x;
            if (vertex.y < minY) minY = vertex.y;
            if (vertex.z < minZ) minZ = vertex.z;
        }
        return new Vector3f(minX, minY, minZ);
    }

    public Vector3f getMaxOfMesh(String name) {
        Vector3f[] verticesPos = getVectorVerticesPos(name);
        if (verticesPos.length == 0) return Vector3f.ZERO;
        float maxX = verticesPos[0].x;
        float maxY = verticesPos[0].y;
        float maxZ = verticesPos[0].z;
        for (Vector3f vertex : verticesPos) {
            if (vertex.x > maxX) maxX = vertex.x;
            if (vertex.y > maxY) maxY = vertex.y;
            if (vertex.z > maxZ) maxZ = vertex.z;
        }
        return new Vector3f(maxX, maxY, maxZ);
    }

    @Override
    public Vector3f getMinOfModel() {
        return Vector3f.ZERO;
    }

    @Override
    public Vector3f getMaxOfModel() {
        return Vector3f.ZERO;
    }

    @Override
    public Vector3f getDimension() {
        return Vector3f.ZERO;
    }

    @Override
    public Vector3f getCenter() {
        return Vector3f.ZERO;
    }

    public NodeModel getNodeModel(String objectName) {
        for (NodeModel nodeModel : nodeModels) {
            if (nodeModel.getName().toLowerCase().contains(objectName.toLowerCase())) {
                return nodeModel;
            }
        }
        return null;
    }
}
