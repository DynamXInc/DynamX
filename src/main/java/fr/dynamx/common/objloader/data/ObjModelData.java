package fr.dynamx.common.objloader.data;

import com.jme3.math.Vector3f;
import fr.dynamx.api.dxmodel.DxModelPath;
import fr.dynamx.common.objloader.OBJLoader;
import fr.dynamx.utils.DynamXUtils;
import lombok.Getter;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * An obj model not able to be rendered, used for collisions generation
 */
public class ObjModelData extends DxModelData {
    @Getter
    private final List<ObjObjectData> objObjects = new ArrayList<>();
    @Getter
    private final Map<String, Material> materials = new HashMap<>();

    public ObjModelData(DxModelPath path) {
        super(path);
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
    public float[] getVerticesPos(String objectName) {
        float[] pos = new float[0];
        for (ObjObjectData objObject : objObjects) {
            if (!objObject.getName().toLowerCase().contains(objectName.toLowerCase())) {
                continue;
            }
            pos = new float[objObject.getVertices().length * 3];
            for (int i = 0; i < objObject.getVertices().length; i++) {
                pos[i * 3] = objObject.getVertices()[i].getPos().x;
                pos[i * 3 + 1] = objObject.getVertices()[i].getPos().y;
                pos[i * 3 + 2] = objObject.getVertices()[i].getPos().z;
            }
        }
        return pos;
    }

    @Override
    public int[] getMeshIndices(String objectName) {
        return getObjObject(objectName).getIndices();
    }

    @Override
    public Vector3f getMeshMin(String name, @Nullable Vector3f result) {
        ObjObjectData objObject = getObjObject(name);
        return objObject == null ? null : objObject.min(result);
    }

    @Override
    public Vector3f getMeshMax(String name, @Nullable Vector3f result) {
        ObjObjectData objObject = getObjObject(name);
        return objObject == null ? null : objObject.max(result);
    }

    @Override
    public Vector3f getMinOfModel(@Nullable Vector3f result) {
        Vector3f firstMin = objObjects.get(0).min(result);
        float minX = firstMin.x;
        float minY = firstMin.y;
        float minZ = firstMin.z;
        for (int i = 1; i < objObjects.size(); i++) {
            Vector3f min = objObjects.get(i).min(result);
            if (min.x < minX) minX = min.x;
            if (min.y < minY) minY = min.y;
            if (min.z < minZ) minZ = min.z;
        }
        if(result == null)
            return new Vector3f(minX, minY, minZ);
        return result.set(minX, minY, minZ);
    }

    @Override
    public Vector3f getMaxOfModel(@Nullable Vector3f result) {
        Vector3f firstMax = objObjects.get(0).max(result);
        float maxX = firstMax.x;
        float maxY = firstMax.y;
        float maxZ = firstMax.z;
        for (int i = 1; i < objObjects.size(); i++) {
            Vector3f max = objObjects.get(i).max(result);
            if (max.x > maxX) maxX = max.x;
            if (max.y > maxY) maxY = max.y;
            if (max.z > maxZ) maxZ = max.z;
        }
        if(result == null)
            return new Vector3f(maxX, maxY, maxZ);
        return result.set(maxX, maxY, maxZ);
    }

    public ObjObjectData getObjObject(String objectName) {
        return objObjects.stream().filter(objObject -> objObject.getName().equalsIgnoreCase(objectName)).findFirst().orElse(null);
    }

    @Override
    public List<String> getMeshNames() {
        return objObjects.stream().map(o -> o.getName().toLowerCase()).collect(Collectors.toList());
    }
}
