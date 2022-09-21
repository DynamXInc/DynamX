package fr.dynamx.api.obj;

import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.common.contentpack.PackInfo;
import net.minecraft.util.ResourceLocation;

import java.util.Objects;

/**
 * Defines an obj model location <br>
 * Can be used to search it as a mc packs resource, or generate a File to find it on server
 */
//TODO RENAME OBJMODELPATH
public class ObjModelPath implements INamedObject {
    private final PackInfo packInfo;
    private final ResourceLocation modelPath;

    public ObjModelPath(PackInfo packInfo, ResourceLocation modelPath) {
        this.packInfo = packInfo;
        this.modelPath = modelPath;
    }

    public PackInfo getPackInfo() {
        return packInfo;
    }

    /**
     * @return The path of the model inside the pack, excluding the modid (typically models/mymodels/myfirstmodel.obj)
     */
    @Override
    public String getName() {
        return modelPath.getPath();
    }

    @Override
    public String getPackName() {
        return packInfo.getPathName();
    }

    /**
     * @return The path of the model inside of the pack (typically dynamxmod:models/mymodels/myfirstmodel.obj)
     */
    public ResourceLocation getModelPath() {
        return modelPath;
    }

    @Override
    public String toString() {
        return "Model " + modelPath + " in pack " + packInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjModelPath that = (ObjModelPath) o;
        return packInfo.equals(that.packInfo) && modelPath.equals(that.modelPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packInfo, modelPath);
    }
}
