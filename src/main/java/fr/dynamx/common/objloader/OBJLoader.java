package fr.dynamx.common.objloader;

import fr.dynamx.common.objloader.data.Material;
import fr.dynamx.common.objloader.data.ObjObjectData;
import fr.dynamx.utils.DynamXUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class OBJLoader {

    private static final String COMMENT = "#";
    private static final String FACE = "f";
    private static final String POSITION = "v";
    private static final String TEX_COORDS = "vt";
    private static final String NORMAL = "vn";
    private static final String NEW_OBJECT = "o";
    private static final String NEW_GROUP = "g";
    private static final String USE_MATERIAL = "usemtl";
    private static final String NEW_MATERIAL = "mtllib";
    @Getter
    private static final List<MTLLoader> mtlLoaders = new ArrayList<>();

    private boolean hasNormals = false;
    private boolean hasTexCoords = false;
    private final List<ObjObjectData> objObjects;
    private final Map<String, Material> materials;

    public OBJLoader(List<ObjObjectData> objects, Map<String, Material> materials) {
        this.objObjects = objects;
        this.materials = materials;
    }

    public static String[] trim(String[] split) {
        List<String> strings = new ArrayList<>();
        for (String s : split)
            if (s != null && !s.trim().isEmpty())
                strings.add(s);
        return strings.toArray(new String[0]);
    }

    /**
     * Reads an obj models, including mtl files
     *
     * @param location   Path of the obj model directory. Null if it needs to ignore mtl files
     * @param objContent Content of the obj file
     */
    //TODO CLEAN THIS
    public void readAndLoadModel(ResourceLocation location, String objContent) {
        try {
            hasNormals = true;
            IndexedModel result = new IndexedModel();
            IndexedModel normalModel = new IndexedModel();
            String[] lines = objContent.split("[\n\r]");

            int posOffset = 0;
            int texOffset = 0;
            int normOffset = 0;
            List<Vector3f> positions = new ArrayList<>();
            List<Vector2f> texCoords = new ArrayList<>();
            List<Vector3f> normals = new ArrayList<>();
            List<IndexedModel.OBJIndex> indices = new ArrayList<>();
            List<String> indicedMaterials = new ArrayList<>();

            Map<IndexedModel.OBJIndex, Integer> resultIndexMap = new HashMap<>();
            Map<Integer, Integer> normalIndexMap = new HashMap<>();
            Map<Integer, Integer> indexMap = new HashMap<>();

            Map<ObjObjectData, IndexedModel> map = new HashMap<>();

            String currentMaterial = null;
            Map<ObjObjectData, IndexedModel[]> objects = new HashMap<>();
            for (int j = 0, linesLength = lines.length; j < linesLength; j++) {
                try {
                    String line = lines[j];
                    if (line != null && !line.trim().isEmpty()) {
                        String[] parts = trim(line.split(" "));
                        if (parts.length == 0)
                            continue;
                        if (!Objects.equals(parts[0].trim(), COMMENT) && parts.length == 1)
                            throw new IllegalArgumentException("Error at line " + j + ": '" + line + "' has no value(s)");
                        switch (parts[0]) {
                            case COMMENT:
                                continue;
                            case POSITION:
                                positions.add(new Vector3f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])));
                                break;
                            case FACE:
                                for (int i = 0; i < parts.length - 3; i++) {
                                    indicedMaterials.add(currentMaterial);
                                    indices.add(parseOBJIndex(parts[1], posOffset, texOffset, normOffset));
                                    indices.add(parseOBJIndex(parts[2 + i], posOffset, texOffset, normOffset));
                                    indices.add(parseOBJIndex(parts[3 + i], posOffset, texOffset, normOffset));
                                }
                                break;
                            case NORMAL:
                                normals.add(new Vector3f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])));
                                break;
                            case TEX_COORDS:
                                if (location != null) {
                                    texCoords.add(new Vector2f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2])));
                                    hasTexCoords = true;
                                }
                                break;
                            case NEW_MATERIAL:
                                if (location != null) {
                                    ResourceLocation path = new ResourceLocation(location.getNamespace(), location.getPath() + parts[1]);
                                    IResource resp = Minecraft.getMinecraft().getResourceManager().getResource(path);
                                    MTLLoader material = new MTLLoader();
                                    material.parse(location, new String(DynamXUtils.readInputStream(resp.getInputStream()), StandardCharsets.UTF_8));
                                    material.getMaterials().forEach(m -> materials.put(m.getName().toLowerCase(), m));
                                    mtlLoaders.add(material);
                                }
                                break;
                            case USE_MATERIAL:
                                setMaterialFinalIndex(indices, currentMaterial, result);
                                currentMaterial = parts[1].toLowerCase();
                                result.materials.put(currentMaterial, new Material.IndexPair(indices.size(), -1));
                                break;
                            case NEW_OBJECT:
                            case NEW_GROUP:
                                result.getObjIndices().addAll(indices);
                                result.getIndicedMaterials().addAll(indicedMaterials);
                                setMaterialFinalIndex(indices, currentMaterial, result);
                                normalModel.getObjIndices().addAll(indices);
                                result = new IndexedModel();
                                normalModel = new IndexedModel();
                                indices.clear();
                                indicedMaterials.clear();
                                objects.put(new ObjObjectData(parts[1]), new IndexedModel[]{result, normalModel});
                                break;
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error at line " + j, e);
                }
            }
            result.getObjIndices().addAll(indices);
            result.getIndicedMaterials().addAll(indicedMaterials);
            setMaterialFinalIndex(indices, currentMaterial, result);
            normalModel.getObjIndices().addAll(indices);

            for (ObjObjectData object : objects.keySet()) {
                result = objects.get(object)[0];
                normalModel = objects.get(object)[1];
                indices = result.getObjIndices();
                map.put(object, result);
                object.setCenter(result.computeCenter());
                for (IndexedModel.OBJIndex current : indices) {
                    Vector3f pos = positions.get(current.positionIndex);
                    Vector2f texCoord = hasTexCoords && location != null ? texCoords.get(current.texCoordsIndex) : new Vector2f();
                    Vector3f normal = hasNormals && current.normalIndex < normals.size() ? normals.get(current.normalIndex) : new Vector3f();

                    int modelVertexIndex = resultIndexMap.getOrDefault(current, -1);
                    if (modelVertexIndex == -1) {
                        resultIndexMap.put(current, result.getVertices().size());
                        modelVertexIndex = result.getVertices().size();

                        result.getVertices().add(pos);
                        result.getTexCoords().add(texCoord);
                        if (hasNormals)
                            result.getNormals().add(normal);
                        result.getTangents().add(new Vector3f());
                    }

                    int normalModelIndex = normalIndexMap.getOrDefault(current.positionIndex, -1);

                    if (normalModelIndex == -1) {
                        normalModelIndex = normalModel.getVertices().size();
                        normalIndexMap.put(current.positionIndex, normalModelIndex);

                        normalModel.getVertices().add(pos);
                        normalModel.getTexCoords().add(texCoord);
                        normalModel.getNormals().add(normal);
                        normalModel.getTangents().add(new Vector3f());
                    }

                    result.getIndices().add(modelVertexIndex);
                    normalModel.getIndices().add(normalModelIndex);
                    indexMap.put(modelVertexIndex, normalModelIndex);
                }

                if (!hasNormals) {
                    normalModel.computeNormals();

                    for (int i = 0; i < result.getNormals().size(); i++) {
                        result.getNormals().add(normalModel.getNormals().get(indexMap.getOrDefault(i, -1)));
                    }
                }
                object.setCenter(result.computeCenter());
            }

            objObjects.clear();
            Set<ObjObjectData> keys = map.keySet();
            for (ObjObjectData object : keys) {
                objObjects.add(object);
                map.get(object).toMesh(object);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while loading obj model", e);
        }
    }

    private void setMaterialFinalIndex(List<IndexedModel.OBJIndex> indices, String currentMaterial, IndexedModel currentObject) {
        if (currentMaterial == null) {
            return;
        }
        if (currentObject.materials.containsKey(currentMaterial)) {
            Material.IndexPair indexPair = currentObject.materials.get(currentMaterial);
            indexPair.setFinalIndex(indices.size());
        }
    }

    private Material getMaterial(List<Material> materials, String id) {
        for (Material mat : materials) {
            if (mat.getName().equals(id))
                return mat;
        }
        return null;
    }

    private IndexedModel.OBJIndex parseOBJIndex(String token, int posOffset, int texCoordsOffset, int normalOffset) {
        IndexedModel.OBJIndex index = new IndexedModel.OBJIndex();
        String[] values = token.split("/");

        index.positionIndex = Integer.parseInt(values[0]) - 1 - posOffset;
        if (values.length > 1) {
            if (values[1] != null && !values[1].equals("")) {
                index.texCoordsIndex = Integer.parseInt(values[1]) - 1 - texCoordsOffset;
            }
            if (values.length > 2) {
                index.normalIndex = Integer.parseInt(values[2]) - 1 - normalOffset;
                hasNormals = true;
            }
        }
        return index;
    }
}
