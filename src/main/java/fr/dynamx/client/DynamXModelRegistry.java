package fr.dynamx.client;

import com.modularmods.mcgltf.MCglTF;
import fr.aym.acslib.ACsLib;
import fr.aym.acslib.api.services.ThreadedLoadingService;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.aym.acslib.services.impl.thrload.DynamXThreadedModLoader;
import fr.dynamx.api.contentpack.ContentPackType;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.api.dxmodel.EnumDxModelFormats;
import fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier;
import fr.dynamx.api.dxmodel.DxModelPath;
import fr.dynamx.client.handlers.ClientEventHandler;
import fr.dynamx.client.renders.model.MissingObjModel;
import fr.dynamx.client.renders.model.renderer.DxItemModelLoader;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.model.renderer.GltfModelRenderer;
import fr.dynamx.client.renders.model.renderer.ObjModelRenderer;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
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
import net.minecraftforge.fml.client.SplashProgress;
import net.minecraftforge.fml.common.ProgressManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static fr.dynamx.common.DynamXMain.log;

/**
 * The DynamX obj model loader <br>
 * All models should be registered here before DynamX pre initialization
 */
public class DynamXModelRegistry implements IPackInfoReloadListener {
    private static final int LOADER_POOL_SIZE = 6;

    private static final DxItemModelLoader OBJ_ITEM_MODEL_LOADER = new DxItemModelLoader();
    private static final Map<DxModelPath, IModelTextureVariantsSupplier> MODELS_REGISTRY = new HashMap<>();
    private static final Map<ResourceLocation, DxModelRenderer> MODELS = new ConcurrentHashMap<>();
    private static final List<ResourceLocation> FAULTY_MODELS = new ArrayList<>();

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
    public void registerModel(DxModelPath location) {
        registerModel(location, null);
    }

    /**
     * Registers a model
     *
     * @param location       The model location
     * @param customTextures A texture supplier for this model (allows multi-texturing)
     */
    public void registerModel(DxModelPath location, IModelTextureVariantsSupplier customTextures) {
        if (REGISTRY_CLOSED) {
            throw new IllegalStateException("Model registry closed, you should register your model before DynamX pre-initialization");
        }
        if (!MODELS_REGISTRY.containsKey(location)) {
            MODELS_REGISTRY.put(location, customTextures);
            if(location.getFormat() == EnumDxModelFormats.GLTF){
                MCglTF.getInstance().registerModel(location);
            }
        } else if (customTextures != null && customTextures.hasVaryingTextures()) {
            IModelTextureVariantsSupplier previousSupplier = MODELS_REGISTRY.get(location);
            if (previousSupplier == null || !previousSupplier.hasVaryingTextures()) {
                log.debug("Replacing model texture supplier of '" + location + "' from '" + previousSupplier + "' to '" + customTextures + "' : the previous doesn't have custom textures");
                MODELS_REGISTRY.put(location, customTextures);
                if(location.getFormat() == EnumDxModelFormats.GLTF){
                    MCglTF.getInstance().registerModel(location);
                }
            } else {
                DynamXErrorManager.addPackError(customTextures.getPackName(), "obj_duplicated_custom_textures", ErrorLevel.HIGH, location.getName(), "Tried to register the model '" + location + "' two times with custom textures '" + previousSupplier + "' and '" + customTextures + "' ! Ignoring " + customTextures);
            }
        }
    }

    /**
     * @return The model corresponding to the given name (the name used in registerModel)
     * @throws IllegalArgumentException If the model wasn't registered (should be done before DynamX pre initialization)
     */
    public DxModelRenderer getModel(ResourceLocation name) {
        if (!MODELS.containsKey(name)) {
            if (!FAULTY_MODELS.contains(name)) {
                log.error("Dx model " + name + " isn't registered !");
                FAULTY_MODELS.add(name);
            }
            return MISSING_MODEL;
        }
        return MODELS.get(name);
    }

    @Deprecated
    public DxModelRenderer getModel(String name) {
        return getModel(new ResourceLocation(DynamXConstants.ID, String.format("models/%s", name)));
    }

    /**
     * Reloads all obj models, may take some time <br>
     * <strong>DON'T CALL THIS, USE {@link DynamXLoadingTasks} !</strong>
     */
    public void reloadModels() {
        REGISTRY_CLOSED = true;
        MODELS.values().forEach(DxModelRenderer::clearVAOs);
        MODELS.values().removeIf(dxModelRenderer -> dxModelRenderer.getFormat() == EnumDxModelFormats.OBJ);
        FAULTY_MODELS.clear();
        //MCglTF.lookup.clear();
        DynamXContext.getDxModelDataCache().clear();
        DynamXErrorManager.getErrorManager().clear(DynamXErrorManager.MODEL_ERRORS);

        ThreadedLoadingService threadedLoadingService = ACsLib.getPlatform().provideService(ThreadedLoadingService.class);
        ExecutorService modelLoader = Executors.newScheduledThreadPool(LOADER_POOL_SIZE, new DynamXThreadedModLoader.DefaultThreadFactory("DnxModelLoader"));
        threadedLoadingService.addTask(ThreadedLoadingService.ModLoadingSteps.FINISH_LOAD, "model_load", () -> {
            try {
                /* == Load models == */
                List<Callable<?>> loadObjTasks = new ArrayList<>();
                for (Map.Entry<DxModelPath, IModelTextureVariantsSupplier> name : MODELS_REGISTRY.entrySet()) {
                    if(MODELS.containsKey(name.getKey().getModelPath())){
                        continue;
                    }
                    loadObjTasks.add(() -> {
                        log.debug("Loading dx model " + name.getKey());
                        DxModelRenderer model = null;
                        switch (name.getKey().getFormat()) {
                            case OBJ:
                                model = ObjModelRenderer.loadObjModel(name.getKey(), name.getValue());
                                break;
                            case GLTF:
                                try {
                                    model = new GltfModelRenderer(name.getKey(), name.getValue());
                                    MCglTF.getInstance().attachReceivers(name.getKey(), (GltfModelRenderer) model);
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                                break;
                        }
                        MODELS.put(name.getKey().getModelPath(), model != null ? model : MISSING_MODEL);
                        return null;
                    });
                }
                long start = System.currentTimeMillis();
                modelLoader.invokeAll((List) loadObjTasks);
                log.info("Took " + (System.currentTimeMillis() - start) + " ms to load " + loadObjTasks.size() + " dx models");

                /* == Wait for Mc's texture manager == */
                long time = System.currentTimeMillis();
                while (Minecraft.getMinecraft().getTextureManager() == null) { //Don't listen to idea, it can be null
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                DynamXMain.log.debug("Tex manager wait took " + (System.currentTimeMillis() - time) + " ms");

                /* == Load textures == */
                List<Callable<?>> loadTexturesTasks = new ArrayList<>();
                //Obj loader specific
                OBJLoader.getMtlLoaders().forEach(mtlLoader -> loadTexturesTasks.add(() -> {
                    mtlLoader.loadTextures();
                    return null;
                }));
                start = System.currentTimeMillis();
                modelLoader.invokeAll((List) loadTexturesTasks);
                log.info("Took " + (System.currentTimeMillis() - start) + " ms to load " + loadTexturesTasks.size() + " obj materials");
                modelLoader.shutdown();

                /* == Init armors == */
                DynamXObjectLoaders.ARMORS.getInfos().values().forEach(ArmorObject::initArmorModel);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, () -> {
            ProgressManager.ProgressBar bar = ProgressManager.push("Loading GLTF models", 1);
            log.info("Loading GLTF models...");
            long start = System.currentTimeMillis();
            if (!threadedLoadingService.mcLoadingFinished()) {
                SplashProgress.pause();
                MCglTF.getInstance().createShaderSkinningProgram();
            }
            try {
                MCglTF.getInstance().reloadModels();
            }catch (Exception e) {
                e.printStackTrace();
            }finally {
                if(!threadedLoadingService.mcLoadingFinished()) {
                    SplashProgress.resume();
                }

            }
            log.info("MCgLTF took " + (System.currentTimeMillis() - start) + " ms to load " + MCglTF.lookup.size() + " gltf models");
            log.info("Loading model textures...");
            //Loads all textures of models, cannot be done before because the TextureManager is not initialized
            bar.step("Uploading textures");
            OBJLoader.getMtlLoaders().forEach(MTLLoader::uploadTextures);
            OBJLoader.getMtlLoaders().clear();

            if(ClientEventHandler.MC.world != null)
                uploadVAOs();

            ProgressManager.pop(bar);
            DynamXLoadingTasks.endTask(DynamXLoadingTasks.MODEL);
        });
    }

    public void uploadVAOs() {
        log.info("Loading model vaos...");
        long t1 = System.currentTimeMillis();
        MODELS.values().forEach(DxModelRenderer::uploadVAOs);
        DynamXMain.log.info("VAO upload took " + (System.currentTimeMillis() - t1) + "ms");
    }

    /**
     * @return The obj item renderer
     */
    public DxItemModelLoader getItemRenderer() {
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
        MODELS_REGISTRY.keySet().removeIf(path -> path.getPackLocations().get(0).getPackType() != ContentPackType.BUILTIN);
        REGISTRY_CLOSED = false;
        //Registers all models avoiding duplicates
        //This doesn't load them, its done by the MC's resource manager
        for (InfoLoader<?> infoLoader : DynamXObjectLoaders.getLoaders()) {
            for (INamedObject namedObject : infoLoader.getInfos().values()) {
                if (namedObject instanceof IModelPackObject && ((IModelPackObject) namedObject).shouldRegisterModel()) {
                    DxModelPath modelPath = DynamXUtils.getModelPath(namedObject.getPackName(), ((IModelPackObject) namedObject).getModel());
                    registerModel(modelPath, (IModelTextureVariantsSupplier) namedObject);
                }
            }
        }
        REGISTRY_CLOSED = true;
        log.info("Registered " + getLoadedModelCount() + " dx models");
    }
}
