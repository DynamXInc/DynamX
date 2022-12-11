package fr.dynamx.api.obj;

import fr.dynamx.common.contentpack.PackInfo;
import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.List;

import java.util.Objects;

/**
 * Defines an obj model location <br>
 * Can be used to search it as a mc packs resource, or generate a File to find it on server
 */
//TODO RENAME OBJMODELPATH
public class ObjModelPath implements INamedObject {
    private final List<PackInfo> packLocations;
    private final ResourceLocation modelPath;

    public ObjModelPath(List<PackInfo> packInfos, ResourceLocation modelPath) {
        this.packLocations = packInfos;
        this.modelPath = modelPath;
    }

    public List<PackInfo> getPackLocations() {
        return packLocations;
    }

    /**
     * @return The path of the model inside the pack (typically dynamxmod:models/mymodels/myfirstmodel.obj)
     */
    public ResourceLocation getModelPath() {
        return modelPath;
    }

    @Override
    public String toString() {
        return "Model " + modelPath + " in pack " + getPackName();
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
        return packLocations.get(0).getFixedPackName();
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
