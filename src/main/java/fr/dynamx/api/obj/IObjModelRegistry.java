package fr.dynamx.api.obj;

import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.client.renders.model.ItemObjModel;
import fr.dynamx.client.renders.model.renderer.ObjModelRenderer;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The DynamX obj model loader <br>
 * All models should be registered here before DynamX pre initialization
 */
public interface IObjModelRegistry {
    /**
     * Registers a model <br>
     * The model should be in modid:models/path
     *
     * @param name The model in format modid:path
     */
    void registerModel(ObjModelPath name);

    /**
     * Registers a model <br>
     * The model should be in modid:models/path
     *
     * @param name           The model in format modid:path
     * @param customTextures A texture supplier for this model (allows multi-texturing)
     */
    void registerModel(ObjModelPath name, IModelTextureSupplier customTextures);

    /**
     * @return The model corresponding to the given name (the name used in registerModel)
     * @throws IllegalArgumentException If the model wasn't registered (should be done before DynamX pre initialization)
     */
    ObjModelRenderer getModel(ResourceLocation name);

    /**
     * Reloads all models, may take some time
     */
    void reloadModels();

    /**
     * @return The number of loaded models
     */
    int getLoadedModelCount();

    /**
     * @return The obj item renderer
     */
    IObjItemModelRenderer getItemRenderer();

    /**
     * An {@link ICustomModelLoader} for obj item models
     */
    interface IObjItemModelRenderer extends ICustomModelLoader {
        /**
         * Registers the obj model of the given item
         *
         * @param item     The item
         * @param meta     The metadata
         * @param location An identifier for the item model - typically modid:itemname
         */
        void registerItemModel(IResourcesOwner item, int meta, ResourceLocation location);

        /**
         * @return The model corresponding to the given item, or null
         */
        ItemObjModel getModel(Item of, byte meta);

        /**
         * Refreshes the obj models used to render the items <br>
         * Called after packs reload
         */
        @SideOnly(Side.CLIENT)
        void refreshItemInfos();
    }
}
