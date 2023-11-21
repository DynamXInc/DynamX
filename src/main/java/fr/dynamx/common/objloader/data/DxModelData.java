package fr.dynamx.common.objloader.data;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.mps.IMpsClassLoader;
import fr.dynamx.api.dxmodel.DxModelPath;
import fr.dynamx.api.dxmodel.EnumDxModelFormats;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.contentpack.PackInfo;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public abstract class DxModelData {


    @Getter
    private final DxModelPath objModelPath;
    protected final IMpsClassLoader mpsClassLoader;

    public DxModelData(DxModelPath path) {
        this.objModelPath = path;
        this.mpsClassLoader = ContentPackLoader.getProtectedResources(path.getPackName()).getSecureLoader();
    }

    @SideOnly(Side.CLIENT)
    protected InputStream client(DxModelPath path) throws IOException {
        return Minecraft.getMinecraft().getResourceManager().getResource(path.getModelPath()).getInputStream();
    }

    public static InputStream server(DxModelPath path) throws IOException {
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

    public abstract float[] getVerticesPos();

    public abstract float[] getVerticesPos(String objectName);

    public abstract Vector3f[] getVectorVerticesPos(String objectName);

    public abstract int[] getMeshIndices(String objectName);

    public abstract int[] getAllMeshIndices();

    public abstract Vector3f getMinOfMesh(String name);
    public abstract Vector3f getMaxOfMesh(String name);

    public abstract Vector3f getMinOfModel();

    public abstract Vector3f getMaxOfModel();

    public abstract Vector3f getDimension();

    public abstract Vector3f getCenter();

    public EnumDxModelFormats getFormat(){
        return objModelPath.getFormat();
    }

    public void addCollisionShape(CompoundCollisionShape to, Vector3f objectScale) {
        Vector3f half = getDimension().multLocal(objectScale);
        if (half.x != 0 || half.y != 0 || half.z != 0) {
            to.addChildShape(new BoxCollisionShape(half), getCenter());
        }
    }

}
