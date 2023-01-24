package fr.dynamx.client;

import fr.aym.acslib.ACsLib;
import fr.aym.acslib.api.services.ThreadedLoadingService;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.aym.acslib.services.impl.thrload.DynamXThreadedModLoader;
import fr.dynamx.api.contentpack.ContentPackType;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.contentpack.object.render.IObjPackObject;
import fr.dynamx.api.obj.IModelTextureVariantsSupplier;
import fr.dynamx.api.obj.ObjModelPath;
import fr.dynamx.client.renders.model.MissingObjModel;
import fr.dynamx.client.renders.model.renderer.ObjItemModelLoader;
import fr.dynamx.client.renders.model.renderer.ObjModelRenderer;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.PackInfo;
import fr.dynamx.common.contentpack.loader.InfoLoader;
import fr.dynamx.common.contentpack.type.objects.ArmorObject;
import fr.dynamx.common.objloader.MTLLoader;
import fr.dynamx.common.objloader.OBJLoader;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXLoadingTasks;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.errors.DynamXErrorManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.ProgressManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static fr.dynamx.common.DynamXMain.log;

/**
 * The DynamX obj model loader <br>
 * All models should be registered here before DynamX pre initialization
 */
public class DynamXModelRegistry implements IPackInfoReloadListener {
    private static final int LOADER_POOL_SIZE = 6;

    private static final ObjItemModelLoader OBJ_ITEM_MODEL_LOADER = new ObjItemModelLoader();
    private static final Map<ObjModelPath, IModelTextureVariantsSupplier> MODELS_REGISTRY = new HashMap<>();
    private static final Map<ResourceLocation, ObjModelRenderer> MODELS = new ConcurrentHashMap<>();
    private static final List<ResourceLocation> ERRORED_MODELS = new ArrayList<>();

    /**
     * A missing model rendered when the right model isn't found
     */
    public static final ObjModelRenderer MISSING_MODEL = new MissingObjModel();

    private static boolean REGISTRY_CLOSED;

    /**
     * Registers a model
     *
     * @param location The model location
     */
    public void registerModel(ObjModelPath location) {
        registerModel(location, null);
    }

    public static final PackInfo BASE_PACKINFO = new PackInfo(DynamXConstants.ID, ContentPackType.BUILTIN);

    @Deprecated
    public void registerModel(String location) {
        registerModel(new ObjModelPath(BASE_PACKINFO, new ResourceLocation(DynamXConstants.ID, String.format("models/%s", location))), null);
    }

    @Deprecated
    public void registerModel(String location, IModelTextureVariantsSupplier customTextures) {
        registerModel(new ObjModelPath(BASE_PACKINFO, new ResourceLocation(DynamXConstants.ID, String.format("models/%s", location))), customTextures);
    }

    /**
     * Registers a model
     *
     * @param location       The model location
     * @param customTextures A texture supplier for this model (allows multi-texturing)
     */
    public void registerModel(ObjModelPath location, IModelTextureVariantsSupplier customTextures) {
        if (REGISTRY_CLOSED)
            throw new IllegalStateException("Model registry closed, you should register your model before DynamX pre-initialization");
        if (!MODELS_REGISTRY.containsKey(location)) {
            MODELS_REGISTRY.put(location, customTextures);
        } else if (customTextures != null && customTextures.hasVaryingTextures()) {
            IModelTextureVariantsSupplier previousSupplier = MODELS_REGISTRY.get(location);
            if (previousSupplier == null || !previousSupplier.hasVaryingTextures()) {
                log.debug("Replacing model texture supplier of '" + location + "' from '" + previousSupplier + "' to '" + customTextures + "' : the previous doesn't have custom textures");
                MODELS_REGISTRY.put(location, customTextures);
            } else {
                //log.error("Tried to register the model '" + location + "' two times with custom textures '" + previousSupplier + "' and '" + customTextures + "' ! Ignoring " + customTextures);
                //TODO FORMAT ERROR
                DynamXErrorManager.addPackError(customTextures.getPackName(), "obj_duplicated_custom_textures", ErrorLevel.HIGH, location.getName(), "Tried to register the model '" + location + "' two times with custom textures '" + previousSupplier + "' and '" + customTextures + "' ! Ignoring " + customTextures);
            }
        }
    }

    /**
     * @return The model corresponding to the given name (the name used in registerModel)
     * @throws IllegalArgumentException If the model wasn't registered (should be done before DynamX pre initialization)
     */
    public ObjModelRenderer getModel(ResourceLocation name) {
        if (!MODELS.containsKey(name)) {
            if (!ERRORED_MODELS.contains(name)) {
                log.error("Obj model " + name + " isn't registered !");
                ERRORED_MODELS.add(name);
            }
            return MISSING_MODEL;
        }
        return MODELS.get(name);
    }

    @Deprecated
    public ObjModelRenderer getModel(String name) {
        ResourceLocation path = new ResourceLocation(DynamXConstants.ID, String.format("models/%s", name));
        if (!MODELS.containsKey(path)) {
            if (!ERRORED_MODELS.contains(path)) {
                log.error("Obj model " + path + " isn't registered !");
                ERRORED_MODELS.add(path);
            }
            return MISSING_MODEL;
        }
        return MODELS.get(path);
    }

    /**
     * Reloads all obj models, may take some time <br>
     * <strong>DON'T CALL THIS, USE {@link DynamXLoadingTasks} !</strong>
     */
    public void reloadModels() {
        REGISTRY_CLOSED = true;
        MODELS.values().forEach(ObjModelRenderer::clearVAOs);
        MODELS.clear();
        ERRORED_MODELS.clear();
        DynamXContext.getObjModelDataCache().clear();
        DynamXErrorManager.getErrorManager().clear(DynamXErrorManager.MODEL_ERRORS);

        ExecutorService modelLoader = Executors.newScheduledThreadPool(LOADER_POOL_SIZE, new DynamXThreadedModLoader.DefaultThreadFactory("DnxModelLoader"));
        ACsLib.getPlatform().provideService(ThreadedLoadingService.class).addTask(ThreadedLoadingService.ModLoadingSteps.FINISH_LOAD, "model_load", () -> {
            try {
                /* == Load models == */
                List<Callable<?>> loadObjTasks = new ArrayList<>();
                for (Map.Entry<ObjModelPath, IModelTextureVariantsSupplier> name : MODELS_REGISTRY.entrySet()) {
                    loadObjTasks.add(() -> {
                        log.debug("Loading tessellator model " + name.getKey());
                        ObjModelRenderer model = ObjModelRenderer.loadObjModel(name.getKey(), name.getValue());
                        if (model != null) {
                            MODELS.put(name.getKey().getModelPath(), model);
                        } else {
                            MODELS.put(name.getKey().getModelPath(), MISSING_MODEL);
                        }
                        return null;
                    });
                }
                long start = System.currentTimeMillis();
                modelLoader.invokeAll((List) loadObjTasks);
                log.info("Took " + (System.currentTimeMillis() - start) + " ms to load " + loadObjTasks.size() + " obj models");

                /* == Wait for Mc's texture manager == */
                long time = System.currentTimeMillis();
                while (Minecraft.getMinecraft().getTextureManager() == null) { //Don't listen idea, it can be null
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                DynamXMain.log.debug("Tex manager wait took " + (System.currentTimeMillis() - time) + " ms");

                /* == Load textures == */
                List<Callable<?>> loadTexturesTasks = new ArrayList<>();
                OBJLoader.getMtlLoaders().forEach(mtlLoader -> loadTexturesTasks.add(() -> {
                    mtlLoader.loadTextures();
                    return null;
                }));
                start = System.currentTimeMillis();
                modelLoader.invokeAll((List) loadTexturesTasks);
                log.info("Took " + (System.currentTimeMillis() - start) + " ms to load " + loadTexturesTasks.size() + " obj materials");
                modelLoader.shutdown();

                /* == Init armors == */
                for (ArmorObject<?> info : DynamXObjectLoaders.ARMORS.getInfos().values()) {
                    ObjModelRenderer model = getModel(info.getModel());
                    info.getObjArmor().init(model);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, () -> {
            ProgressManager.ProgressBar bar = ProgressManager.push("Loading obj models", 1);
            log.info("Loading model textures...");
            //Loads all textures of models, cannot be done before because the TextureManager is not initialized
            bar.step("Uploading textures");
            OBJLoader.getMtlLoaders().forEach(MTLLoader::uploadTextures);
            OBJLoader.getMtlLoaders().clear();
            ProgressManager.pop(bar);
            DynamXLoadingTasks.endTask(DynamXLoadingTasks.MODEL);
        });
    }

    /**
     * @return The obj item renderer
     */
    public ObjItemModelLoader getItemRenderer() {
        return OBJ_ITEM_MODEL_LOADER;
    }

    /**
     * @return The number of loaded models
     */
    public int getLoadedModelCount() {
        return MODELS_REGISTRY.size();
    }

    @Override
    public void onPackInfosReloaded() {
        MODELS_REGISTRY.clear();
        //Registers all models avoiding duplicates
        //This doesn't load them, its done by the MC's resource manager
        for (InfoLoader<?> infoLoader : DynamXObjectLoaders.getLoaders()) {
            for (INamedObject namedObject : infoLoader.getInfos().values()) {
                if (namedObject instanceof IObjPackObject && ((IObjPackObject) namedObject).shouldRegisterModel()) {
                    ObjModelPath modelPath = DynamXUtils.getModelPath(namedObject.getPackName(), ((IObjPackObject) namedObject).getModel());
                    if (REGISTRY_CLOSED) {
                        // override old variants supplier
                        MODELS_REGISTRY.put(modelPath, (IModelTextureVariantsSupplier) namedObject);
                    } else {
                        registerModel(modelPath, (IModelTextureVariantsSupplier) namedObject);
                    }
                }
            }
        }
        log.info("Registered " + getLoadedModelCount() + " obj models");
    }
}
