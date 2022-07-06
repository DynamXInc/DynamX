package fr.dynamx.client;

import fr.aym.acslib.ACsLib;
import fr.aym.acslib.api.services.ErrorTrackingService;
import fr.aym.acslib.api.services.ThreadedLoadingService;
import fr.dynamx.api.obj.IModelTextureSupplier;
import fr.dynamx.api.obj.IObjModelRegistry;
import fr.dynamx.client.renders.model.MissingObjModel;
import fr.dynamx.client.renders.model.ObjItemModelLoader;
import fr.dynamx.client.renders.model.ObjModelClient;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.type.objects.ArmorObject;
import fr.dynamx.common.obj.eximpl.MtlMaterialLib;
import fr.dynamx.common.obj.eximpl.OBJLoader;
import fr.dynamx.common.obj.eximpl.QuickObjObject;
import fr.dynamx.utils.DynamXLoadingTasks;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.RegistryNameSetter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.ProgressManager;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.dynamx.common.DynamXMain.log;

public class DynamXModelRegistry implements IObjModelRegistry {
    private static final ObjItemModelLoader OBJ_ITEM_MODEL_LOADER = new ObjItemModelLoader();
    private static final Map<String, IModelTextureSupplier> MODELS_REGISTRY = new HashMap<>();
    private static final Map<String, ObjModelClient> MODELS = new HashMap<>();
    private static final List<String> ERRORED_MODELS = new ArrayList<>();

    /**
     * A missing model rendered when the right model isn't found
     */
    public static final ObjModelClient MISSING_MODEL = new MissingObjModel();

    private static boolean REGISTRY_CLOSED;

    @Override
    public void registerModel(String location) {
        registerModel(location, null);
    }

    @Override
    public void registerModel(String location, IModelTextureSupplier customTextures) {
        if (REGISTRY_CLOSED)
            throw new IllegalStateException("Model registry closed, you should register your model before DynamX pre-initialization");
        if (!MODELS_REGISTRY.containsKey(location)) {
            MODELS_REGISTRY.put(location, customTextures);
        } else if (customTextures != null && customTextures.hasCustomTextures()) {
            IModelTextureSupplier previousSupplier = MODELS_REGISTRY.get(location);
            if (previousSupplier == null || !previousSupplier.hasCustomTextures()) {
                log.debug("Replacing model texture supplier of '" + location + "' from '" + previousSupplier + "' to '" + customTextures + "' : the previous doesn't have custom textures");
                MODELS_REGISTRY.put(location, customTextures);
            } else {
                log.error("Tried to register the model '" + location + "' two times with custom textures '" + previousSupplier + "' and '" + customTextures + "' ! Ignoring " + customTextures);
                DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.PACK, customTextures.getPackName(), "Model "+location,
                        "Tried to register the model '" + location + "' two times with custom textures '" + previousSupplier + "' and '" + customTextures + "' ! Ignoring " + customTextures, ErrorTrackingService.TrackedErrorLevel.HIGH);
            }
        }
    }

    @Override
    public ObjModelClient getModel(String name) {
        if (!MODELS.containsKey(name)) {
            if (!ERRORED_MODELS.contains(name)) {
                log.error("Obj model " + name + " isn't registered !");
                ERRORED_MODELS.add(name);
            }
            return MISSING_MODEL;
        }
        return MODELS.get(name);
    }

    /**
     * Reloads all obj models <br>
     * <strong>DON'T CALL THIS, USE {@link DynamXLoadingTasks} !</strong>
     */
    @Override
    public void reloadModels() {
        log.info("Reloading all models...");
        REGISTRY_CLOSED = true;
        MODELS.clear();
        ERRORED_MODELS.clear();
        DynamXContext.getErrorTracker().clear(DynamXLoadingTasks.MODEL);

        ACsLib.getPlatform().provideService(ThreadedLoadingService.class).addTask(ThreadedLoadingService.ModLoadingSteps.FINISH_LOAD, "model_load", () -> {
            //bar.step("Loading model files");
            for (Map.Entry<String, IModelTextureSupplier> name : MODELS_REGISTRY.entrySet()) {
                log.debug("Loading tessellator model " + name.getKey());

                ObjModelClient model = ObjModelClient.createObjModel(RegistryNameSetter.getDynamXModelResourceLocation(name.getKey()), name.getValue());
                if(model != null) {
                    MODELS.put(name.getKey(), model);
                } else {
                    MODELS.put(name.getKey(), MISSING_MODEL);
                }
            }

            long time = System.currentTimeMillis();
            while (Minecraft.getMinecraft().getTextureManager() == null) { //Don't listen idea, it can be null
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            DynamXMain.log.debug("Tex manager wait took " + (System.currentTimeMillis() - time) + " ms");

            OBJLoader.getMaterialLibs().forEach(MtlMaterialLib::loadTextures);
        }, () -> {
            ProgressManager.ProgressBar bar = ProgressManager.push("Loading obj models", 3);
            log.info("Loading model textures and compiling display lists...");
            //Loads all textures of models, cannot be done before because the TextureManager is not initialized
            bar.step("Uploading textures");
            OBJLoader.getMaterialLibs().forEach(MtlMaterialLib::uploadTextures);
            bar.step("Compiling models");
            MODELS.forEach((s, t) -> t.setupModel());
            bar.step("Compiling armors");
            for (ArmorObject<?> info : DynamXObjectLoaders.ARMORS.getInfos().values()) {
                ObjModelClient model = getModel(info.getModel());
                info.getObjArmor().init(model);
            }
            ProgressManager.pop(bar);

            DynamXLoadingTasks.endTask(DynamXLoadingTasks.MODEL);
        });
    }

    @Override
    public IObjItemModelRenderer getItemRenderer() {
        return OBJ_ITEM_MODEL_LOADER;
    }

    @Override
    public int getLoadedModelCount() {
        return MODELS_REGISTRY.size();
    }
}
