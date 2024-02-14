package fr.dynamx.api.dxmodel;

import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.common.contentpack.PackInfo;
import lombok.Getter;
import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Defines an obj model location <br>
 * Can be used to search it as a mc packs resource, or generate a File to find it on server
 */
public class DxModelPath implements INamedObject {
    private final List<PackInfo> packLocations;
    private final ResourceLocation modelPath;

    @Getter
    private final EnumDxModelFormats format;

    public DxModelPath(PackInfo packLocation, ResourceLocation modelPath) {
        this(Collections.singletonList(packLocation), modelPath);
    }

    public DxModelPath(List<PackInfo> packInfos, ResourceLocation modelPath) {
        this.packLocations = packInfos;
        this.modelPath = modelPath;

        if(modelPath.getPath().endsWith(".obj")) format = EnumDxModelFormats.OBJ;
        else if(modelPath.getPath().endsWith(".gltf")) format = EnumDxModelFormats.GLTF;
        else throw new IllegalArgumentException("Model format not supported: " + modelPath.getPath());
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
        return modelPath + " in pack " + getPackName();
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
        DxModelPath that = (DxModelPath) o;
        return packLocations.equals(that.packLocations) && modelPath.equals(that.modelPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packLocations, modelPath);
    }
}
