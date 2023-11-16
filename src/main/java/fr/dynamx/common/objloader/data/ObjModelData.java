package fr.dynamx.common.objloader.data;

import com.jme3.math.Vector3f;
import de.javagl.jgltf.model.NodeModel;
import fr.aym.acslib.api.services.mps.IMpsClassLoader;
import fr.dynamx.api.dxmodel.DxModelPath;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.contentpack.PackInfo;
import fr.dynamx.common.objloader.OBJLoader;
import fr.dynamx.utils.DynamXUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An obj model not able to be rendered, used for collisions generation
 */
public class ObjModelData extends DxModelData {
    @Getter
    private final List<ObjObjectData> objObjects = new ArrayList<>();
    @Getter
    private final Map<String, Material> materials = new HashMap<>();

    //private final IMpsClassLoader mpsClassLoader; //FIXME DELETED ?

    public ObjModelData(DxModelPath path) {
        super(path);
        this.mpsClassLoader = ContentPackLoader.getProtectedResources(path.getPackName()).getSecureLoader();
        //fixme this.objModelPath = path;
        try {
            String content = new String(DynamXUtils.readInputStream(FMLCommonHandler.instance().getSide().isClient() ? client(path) : server(path)), StandardCharsets.UTF_8);
            ResourceLocation location = path.getModelPath();
            ResourceLocation startPath = new ResourceLocation(location.getNamespace(), location.getPath().substring(0, location.getPath().lastIndexOf("/") + 1));
            new OBJLoader(objObjects, materials).readAndLoadModel(FMLCommonHandler.instance().getSide().isClient() ? startPath : null, content);
        } catch (Exception e) {
            //Don't remove the throw - Aym
            throw new RuntimeException("Model " + path + " cannot be loaded ! " + e + " Has secure loader: " + (mpsClassLoader != null), e);
        }
    }

    @Override
    public float[] getVerticesPos() {
        List<float[]> posList = new ArrayList<>();
        int size = 0;
        for (ObjObjectData objObject : objObjects) {
            if (!objObject.getName().toLowerCase().contains("main")) {
                float[] pos = getVerticesPos(objObject.getName().toLowerCase());
                posList.add(pos);
                size += pos.length;
            }
        }
        float[] pos = new float[size];
        for (float[] floats : posList) {
            System.arraycopy(floats, 0, pos, 0, floats.length);
        }
        return pos;
    }

    public Vector3f[] getVectorVerticesPos() {
        List<Vector3f[]> posList = new ArrayList<>();
        int size = 0;

        for (ObjObjectData objObject : objObjects) {
            if (!objObject.getName().toLowerCase().contains("main")) {
                Vector3f[] pos = getVectorVerticesPos(objObject.getName().toLowerCase());
                posList.add(pos);
                size += pos.length;
            }
        }
        Vector3f[] pos = new Vector3f[size];
        for (Vector3f[] floats : posList) {
            System.arraycopy(floats, 0, pos, 0, floats.length);
        }
        return pos;
    }

    @Override
    public float[] getVerticesPos(String objectName) {
        float[] pos = new float[0];
        for (ObjObjectData objObject : objObjects) {
            if (objObject.getName().toLowerCase().contains(objectName.toLowerCase())) {
                pos = new float[objObject.getMesh().vertices.length * 3];
                for (int i = 0; i < objObject.getMesh().vertices.length; i++) {
                    pos[i * 3] = objObject.getMesh().vertices[i].getPos().x;
                    pos[i * 3 + 1] = objObject.getMesh().vertices[i].getPos().y;
                    pos[i * 3 + 2] = objObject.getMesh().vertices[i].getPos().z;
                }
            }
        }
        return pos;
    }

    @Override
    public Vector3f[] getVectorVerticesPos(String objectName) {
        Vector3f[] pos = new Vector3f[0];
        for (ObjObjectData objObject : objObjects) {
            if (objObject.getName().toLowerCase().contains(objectName.toLowerCase())) {
                pos = new Vector3f[objObject.getMesh().vertices.length];
                for (int i = 0; i < objObject.getMesh().vertices.length; i++) {
                    pos[i] = new Vector3f(objObject.getMesh().vertices[i].getPos().x,
                            objObject.getMesh().vertices[i].getPos().y,
                            objObject.getMesh().vertices[i].getPos().z);
                }
            }
        }
        return pos;
    }

    @Override
    public int[] getAllMeshIndices() {
        List<int[]> indicesList = new ArrayList<>();
        int size = 0;
        for (ObjObjectData objObject : objObjects) {
            if (!objObject.getName().toLowerCase().contains("main")) {
                int[] indices = getMeshIndices(objObject.getName().toLowerCase());
                indicesList.add(indices);
                size += indices.length;
            }
        }
        int[] indices = new int[size];
        for (int[] ints : indicesList) {
            System.arraycopy(ints, 0, indices, 0, ints.length);
        }
        return indices;
    }

    @Override
    public Vector3f getMinOfMesh(String name) {
        return Vector3f.ZERO;
    }

    @Override
    public Vector3f getMaxOfMesh(String name) {
        return Vector3f.ZERO;
    }

    @Override
    public int[] getMeshIndices(String objectName) {
        int[] indices = new int[0];
        for (ObjObjectData objObject : objObjects) {
            if (objObject.getName().toLowerCase().contains(objectName.toLowerCase())) {
                indices = objObject.getMesh().indices;
            }
        }
        return indices;
    }

    public ObjObjectData getObjObject(String objectName) {
        for (ObjObjectData objObject : objObjects) {
            if (objObject.getName().toLowerCase().contains(objectName.toLowerCase())) {
                return objObject;
            }
        }
        return null;
    }

    @Override
    public Vector3f getMinOfModel() {
        Vector3f firstMin = objObjects.get(0).getMesh().min();
        float minX = firstMin.x;
        float minY = firstMin.y;
        float minZ = firstMin.z;
        for (ObjObjectData objObject : objObjects) {
            if (objObject.getMesh().min().x < minX) minX = objObject.getMesh().min().x;
            if (objObject.getMesh().min().y < minY) minY = objObject.getMesh().min().y;
            if (objObject.getMesh().min().z < minZ) minZ = objObject.getMesh().min().z;
        }
        return new Vector3f(minX, minY, minZ);
    }

    @Override
    public Vector3f getMaxOfModel() {
        Vector3f firstMax = objObjects.get(0).getMesh().max();
        float maxX = firstMax.x;
        float maxY = firstMax.y;
        float maxZ = firstMax.z;
        for (ObjObjectData objObject : objObjects) {
            maxX = Math.max(objObject.getMesh().max().x, maxX);
            maxY = Math.max(objObject.getMesh().max().x, maxY);
            maxZ = Math.max(objObject.getMesh().max().x, maxZ);
        }
        return new Vector3f(maxX, maxY, maxZ);
    }

    @Override
    public Vector3f getDimension() {
        Vector3f max = getMaxOfModel();
        Vector3f min = getMinOfModel();
        return new Vector3f(max.x - min.x, max.y - min.y, max.z - min.z);
    }

    @Override
    public Vector3f getCenter() {
        Vector3f max = getMaxOfModel();
        Vector3f min = getMinOfModel();
        return new Vector3f((max.x + min.x) / 2f, (max.y + min.y) / 2f, (max.z + min.z) / 2f);
    }
}
