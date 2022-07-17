package fr.dynamx.api.obj;

import fr.dynamx.api.contentpack.object.INamedObject;
import net.minecraft.util.ResourceLocation;

/**
 * Defines an obj model location <br>
 * Can be used to search it as a mc packs resource, or generate a File to find it on server
 */
public class ObjModelPath implements INamedObject {
    private final String packName;
    private final ResourceLocation modelPath;

    public ObjModelPath(String packName, ResourceLocation modelPath) {
        this.packName = packName;
        this.modelPath = modelPath;
    }

    /**
     * @return The path of the model inside of the pack, excluding the modid (typically models/mymodels/myfirstmodel.obj)
     */
    @Override
    public String getName() {
        return modelPath.getPath();
    }

    @Override
    public String getPackName() {
        return packName;
    }

    /**
     * @return The path of the model inside of the pack (typically dynamxmod:models/mymodels/myfirstmodel.obj)
     */
    public ResourceLocation getModelPath() {
        return modelPath;
    }

    @Override
    public String toString() {
        return "ObjModelPath{" +
                "packName='" + packName + '\'' +
                ", modelPath=" + modelPath +
                '}';
    }
}
