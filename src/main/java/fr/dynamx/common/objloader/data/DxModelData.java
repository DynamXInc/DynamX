package fr.dynamx.common.objloader.data;

import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.mps.IMpsClassLoader;
import fr.dynamx.api.dxmodel.DxModelPath;
import fr.dynamx.api.dxmodel.EnumDxModelFormats;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.contentpack.PackInfo;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Tuple;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class DxModelData {


    @Getter
    private final DxModelPath objModelPath;
    protected final IMpsClassLoader mpsClassLoader;

    public DxModelData(DxModelPath path) {
        this.objModelPath = path;
        this.mpsClassLoader = ContentPackLoader.getProtectedResources(path.getPackName()).getSecureLoader();
    }

    @SideOnly(Side.CLIENT)
    protected InputStream getClientInputStream(DxModelPath path) throws IOException {
        return Minecraft.getMinecraft().getResourceManager().getResource(path.getModelPath()).getInputStream();
    }

    protected InputStream getServerInputStream(DxModelPath path) throws IOException {
        InputStream result = null;
        for (PackInfo packInfo : path.getPackLocations()) {
            result = packInfo.readFile(path.getModelPath());
            if (result != null)
                break;
        }
        if (result == null)
            throw new FileNotFoundException("Model not found : " + path + ". Pack : " + path.getPackName());
        return result;
    }

    /**
     * Search for the model file in the packs and return the first found
     *
     * @param path The model path
     * @return A tuple containing the pack info of the pack containing this model, and the model file input stream
     * @throws IOException If the model file cannot be found
     */
    public static Tuple<PackInfo, InputStream> getModelFile(DxModelPath path) throws IOException {
        InputStream result;
        for (PackInfo packInfo : path.getPackLocations()) {
            result = packInfo.readFile(path.getModelPath());
            if (result != null)
                return new Tuple<>(packInfo, result);
        }
        throw new FileNotFoundException("Model not found : " + path + ". Pack : " + path.getPackName());
    }

    public abstract float[] getVerticesPos(String objectName);

    public float[] getVerticesPos() {
        List<float[]> posList = new ArrayList<>();
        int size = 0;
        for (String meshName : getMeshNames()) {
            if (!meshName.equalsIgnoreCase("main")) {
                float[] pos = getVerticesPos(meshName.toLowerCase());
                posList.add(pos);
                size += pos.length;
            }
        }
        int index = 0;
        float[] pos = new float[size];
        for (float[] floats : posList) {
            System.arraycopy(floats, 0, pos, index, floats.length);
            index += floats.length;
        }
        return pos;
    }

    public abstract int[] getMeshIndices(String objectName);

    public int[] getAllMeshIndices() {
        return getMeshNames().stream()
                .filter(meshName -> !meshName.equalsIgnoreCase("main"))
                .map(meshName -> getMeshIndices(meshName.toLowerCase()))
                .flatMapToInt(Arrays::stream)
                .toArray();
    }

    public abstract Vector3f getMeshMin(String name, @Nullable Vector3f result);

    public abstract Vector3f getMeshMax(String name, @Nullable Vector3f result);

    public abstract Vector3f getMinOfModel(@Nullable Vector3f result);

    public abstract Vector3f getMaxOfModel(@Nullable Vector3f result);

    public abstract List<String> getMeshNames();

    public Vector3f getCenter(@Nullable Vector3f result) {
        Vector3f min = getMinOfModel(Vector3fPool.get());
        Vector3f max = getMaxOfModel(Vector3fPool.get());
        if (result == null)
            return new Vector3f((max.x + min.x) / 2f, (max.y + min.y) / 2f, (max.z + min.z) / 2f);
        return result.set((max.x + min.x) / 2f, (max.y + min.y) / 2f, (max.z + min.z) / 2f);
    }

    public Vector3f getDimension(@Nullable Vector3f result) {
        Vector3f min = getMinOfModel(Vector3fPool.get());
        Vector3f max = getMaxOfModel(Vector3fPool.get());
        if (result == null)
            return new Vector3f((max.x - min.x) / 2, (max.y - min.y) / 2, (max.z - min.z) / 2);
        return result.set((max.x - min.x) / 2, (max.y - min.y) / 2, (max.z - min.z) / 2);
    }

    public Vector3f getMeshCenter(String name, @Nullable Vector3f result) {
        Vector3f min = getMeshMin(name, Vector3fPool.get());
        Vector3f max = getMeshMax(name, Vector3fPool.get());
        if (result == null)
            return new Vector3f((max.x + min.x) / 2f, (max.y + min.y) / 2f, (max.z + min.z) / 2f);
        return result.set((max.x + min.x) / 2f, (max.y + min.y) / 2f, (max.z + min.z) / 2f);
    }

    public Vector3f getMeshDimension(String name, @Nullable Vector3f result) {
        Vector3f min = getMeshMin(name, Vector3fPool.get());
        Vector3f max = getMeshMax(name, Vector3fPool.get());
        if (result == null)
            return new Vector3f((max.x - min.x) / 2, (max.y - min.y) / 2, (max.z - min.z) / 2);
        return result.set((max.x - min.x) / 2, (max.y - min.y) / 2, (max.z - min.z) / 2);
    }

    public EnumDxModelFormats getFormat() {
        return objModelPath.getFormat();
    }
}
