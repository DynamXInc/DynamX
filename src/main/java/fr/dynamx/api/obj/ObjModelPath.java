package fr.dynamx.api.obj;

import fr.dynamx.common.contentpack.PackInfo;
import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.List;

/**
 * Defines an obj model location <br>
 * Can be used to search it as a mc packs resource, or generate a File to find it on server
 */
public class ObjModelPath {
    private final List<PackInfo> packLocations;
    private final ResourceLocation modelPath;

    public ObjModelPath(PackInfo packLocation, ResourceLocation modelPath) {
        this.packLocations = Collections.singletonList(packLocation);
        this.modelPath = modelPath;
    }

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

    public String getPackName() {
        return packLocations.get(0).getFixedPackName();
    }
}
