package fr.dynamx.common.objloader.data;

import com.jme3.math.Vector3f;
import com.modularmods.mcgltf.MCglTF;
import de.javagl.jgltf.model.*;
import fr.dynamx.api.dxmodel.DxModelPath;
import lombok.Getter;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

public class GltfModelData extends DxModelData {

    @Getter
    private final GltfModel gltfModel;

    public GltfModelData(DxModelPath objModelPath) {
        super(objModelPath);
        gltfModel = MCglTF.getInstance().readModels(objModelPath);
    }

    public float[] getVerticesPos() {
        List<float[]> posList = new ArrayList<>();
        int size = 0;
        for (NodeModel meshModel : getNodeModels()) {
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
        String objectNameLower = objectName.toLowerCase();
        List<float[]> positionList = new ArrayList<>();
        int size = 0;

        for (NodeModel nodeModel : getNodeModels()) {
            if (nodeModel.getName().toLowerCase().contains(objectNameLower)) {
                for (MeshModel meshModel : nodeModel.getMeshModels()) {
                    for (MeshPrimitiveModel meshPrimitiveModel : meshModel.getMeshPrimitiveModels()) {
                        AccessorModel accessorModel = meshPrimitiveModel.getAttributes().get("POSITION");
                        if (accessorModel.getComponentType() != 5126) {
                            return new float[0];
                        }
                        FloatBuffer floatBuffer = accessorModel.getBufferViewModel().getBufferViewData().asFloatBuffer();
                        float[] pos = new float[floatBuffer.limit()];
                        size += floatBuffer.limit();
                        floatBuffer.get(pos);
                        for (int i = 0; i < pos.length; i+=3) {
                            float temp = pos[i + 1];
                            pos[i + 1] = -pos[i + 2];
                            pos[i + 2] = temp;
                        }
                        positionList.add(pos);
                    }
                }
            }
        }
        float[] pos = new float[size];
        int currentPosition = 0;
        for (float[] posi : positionList) {
            System.arraycopy(posi, 0, pos, currentPosition, posi.length);
            currentPosition += posi.length;
        }
        return pos;
    }

    public Vector3f[] getVectorVerticesPos(String objectName) {
        float[] verticesPos = getVerticesPos(objectName);
        Vector3f[] vectorPos = new Vector3f[verticesPos.length / 3];
        for (int i = 0; i < verticesPos.length / 3; i++) {
            vectorPos[i] = new Vector3f(verticesPos[i * 3], verticesPos[i * 3 + 1], verticesPos[i * 3 + 2]);
        }
        return vectorPos;
    }

    public int[] getMeshIndices(String objectName) {
        String objectNameLower = objectName.toLowerCase();
        List<int[]> indicesList = new ArrayList<>();
        int size = 0;

        for (NodeModel nodeModel : getNodeModels()) {
            if (nodeModel.getName().toLowerCase().contains(objectNameLower)) {
                for (MeshModel meshModel : nodeModel.getMeshModels()) {
                    for (MeshPrimitiveModel meshPrimitiveModel : meshModel.getMeshPrimitiveModels()) {
                        AccessorModel accessorModel = meshPrimitiveModel.getIndices();
                        if (accessorModel.getComponentType() != 5123) {
                            return new int[0];
                        }
                        ShortBuffer shortBuffer = accessorModel.getBufferViewModel().getBufferViewData().asShortBuffer();
                        int[] indices = new int[shortBuffer.limit()];
                        size += shortBuffer.limit();
                        for (int i = 0; i < shortBuffer.limit(); i++) {
                            indices[i] = shortBuffer.get();
                        }
                        indicesList.add(indices);
                    }
                }
            }
        }
        int[] indices = new int[size];
        int currentPosition = 0;
        for (int[] ints : indicesList) {
            System.arraycopy(ints, 0, indices, currentPosition, ints.length);
            currentPosition += ints.length;
        }
        return indices;
    }

    public int[] getAllMeshIndices() {
        List<int[]> indicesList = new ArrayList<>();
        int size = 0;
        for (NodeModel nodeModel : getNodeModels()) {
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

    //TODO YANIS: implement
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

    public List<NodeModel> getNodeModels() {
        return gltfModel == null ? new ArrayList<>() : gltfModel.getNodeModels();
    }

    public NodeModel getNodeModel(String objectName) {
        for (NodeModel nodeModel : getNodeModels()) {
            if (nodeModel.getName().toLowerCase().contains(objectName.toLowerCase())) {
                return nodeModel;
            }
        }
        return null;
    }
}
