package fr.hermes.forge;

import fr.aym.acsguis.api.ACsGuiApiService;
import fr.aym.acslib.ACsLib;
import fr.aym.acslib.api.services.ThreadedLoadingService;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.aym.mps.utils.UserErrorMessageException;
import fr.dynamx.core.client.command.DynamXClientCommand;
import fr.dynamx.core.client.handlers.ClientEventHandler;
import fr.dynamx.core.client.handlers.KeyHandler;
import fr.dynamx.core.client.renders.RenderProp;
import fr.dynamx.core.client.renders.RenderRagdoll;
import fr.dynamx.core.client.renders.RenderSeatEntity;
import fr.dynamx.core.client.renders.TESRDynamXBlock;
import fr.dynamx.core.client.renders.vehicle.RenderBaseVehicle;
import fr.dynamx.core.client.renders.vehicle.RenderDoor;
import fr.dynamx.core.common.DynamXMain;
import fr.dynamx.core.common.blocks.TEDynamXBlock;
import fr.dynamx.core.common.entities.PropsEntity;
import fr.dynamx.core.common.entities.RagdollEntity;
import fr.dynamx.core.common.entities.SeatEntity;
import fr.dynamx.core.common.entities.vehicles.*;
import fr.dynamx.core.utils.DynamXConstants;
import fr.dynamx.core.utils.DynamXLoadingTasks;
import fr.dynamx.core.utils.client.DynamXRenderUtils;
import fr.dynamx.core.utils.errors.DynamXErrorManager;
import fr.dynamx.core.utils.optimization.SubClassPool;
import fr.hermes.api.mod.HermesUtils;
import fr.hermes.api.mod.HermesUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.resource.IResourceType;
import net.minecraftforge.client.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.client.resource.VanillaResourceType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.FMLModContainer;
import net.minecraftforge.fml.common.MetadataCollection;
import net.minecraftforge.fml.common.discovery.ContainerType;
import net.minecraftforge.fml.common.discovery.ModCandidate;
import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion;
import net.minecraftforge.fml.common.versioning.InvalidVersionSpecificationException;
import net.minecraftforge.fml.common.versioning.VersionRange;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.function.Predicate;

public class HmUtilsClient extends HmUtils implements HermesUtilsClient, ISelectiveResourceReloadListener {
    private byte loadingState;

    //TODO SHOULD BE MOVED TO SOME CLIENT UTILS METHOD
    @Override
    public boolean addFileResources(File file) {
        try {
            HashMap<String, Object> map = new HashMap<>();
            map.put("modid", DynamXConstants.ID);
            map.put("name", "DynamX assets : " + file.getName());
            map.put("version", "1.0");
            FMLModContainer container = new FMLModContainer("fr.dynamx.common.DynamXMain", new ModCandidate(file, file, file.isDirectory() ? ContainerType.DIR : ContainerType.JAR), map);
            container.bindMetadata(MetadataCollection.from(null, ""));
            FMLClientHandler.instance().addModAsResource(container);
            return true;
        } catch (Throwable e) {
            DynamXMain.log.error("Failed to load textures and models of DynamX pack : {}", file.getName());
            DynamXMain.log.throwing(e);
            if (!(e instanceof Exception)) //todo clean
                e = new RuntimeException("encapsulated error", e);
            DynamXErrorManager.addError(file.getName(), DynamXErrorManager.INIT_ERRORS, "res_pack_load_fail", ErrorLevel.FATAL, "assets", "Failed to register as resource pack", (Exception) e, 700);
            return false;
        }
    }

    //TODO SHOULD BE MOVED TO SOME CLIENT UTILS METHOD
    @Override
    public void reloadLanguageResources() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager()));
    }

    @Override
    public void initializeDynamXPacks() {
        try {
            VersionRange versionRange = VersionRange.createFromVersionSpec(DynamXConstants.ACSGUIS_REQUIRED_VERSION);
            ACsGuiApiService service = ACsLib.getPlatform().provideService(ACsGuiApiService.class);
            if (!versionRange.containsVersion(new DefaultArtifactVersion(service.getVersion()))) {
                DynamXMain.log.fatal("Invalid version of ACsGuis found: {}. Expected to be in {}. Halting game loading at pre init.", service.getVersion(), versionRange);
                DynamXMain.getInstance().setMemoizedConstructionError(new UserErrorMessageException("Invalid ACsGuis version " + service.getVersion(), null,
                        "Invalid ACsGuis version " + service.getVersion(),
                        "This version of DynamX requires a version of ACsGuis in range " + versionRange + ".",
                        "We advise you to install version " + DynamXConstants.DEFAULT_ACSGUIS_VERSION + " of ACsGuis."));
                return;
            }
        } catch (InvalidVersionSpecificationException e) {
            throw new RuntimeException("Bad ACSGUIS_REQUIRED_VERSION", e);
        }

        //This event handler needs to be registered before mc's sound system init
        //TODO HUM HUM
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());

        ((SimpleReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener((ISelectiveResourceReloadListener) (resourceManager, resourcePredicate) -> {
            if (loadingState == 0) {
                loadingState++;
            } else if (loadingState == 1) {
                loadingState++;
                ThreadedLoadingService loadingService = ACsLib.getPlatform().provideService(ThreadedLoadingService.class);
                loadingService.addTask(ThreadedLoadingService.ModLoadingSteps.BLOCK_REGISTRY, "packsload", () -> {
                    JmeVector3fPool.openPool(SubClassPool.PACK_MODEL_LOAD); //Open a pool for the loading of entities
                    DynamXLoadingTasks.reload(DynamXLoadingTasks.TaskContext.MC_INIT, DynamXLoadingTasks.PACK);
                    JmeVector3fPool.closePool();

                    //Must follow addons init
                    loadingService.addTask(ThreadedLoadingService.ModLoadingSteps.INIT, "proxy preinit", this::preInit);
                });
            }
        });
    }

    @Override
    public void registerMinecraftRenderingHandlers() {
        // TODO not an ideal solution but works for now
        RenderingRegistry.registerEntityRenderingHandler(CarEntity.class, RenderBaseVehicle.RenderCar::new);
        RenderingRegistry.registerEntityRenderingHandler(BoatEntity.class, RenderBaseVehicle.RenderBoat::new);
        RenderingRegistry.registerEntityRenderingHandler(TrailerEntity.class, RenderBaseVehicle.RenderTrailer::new);
        RenderingRegistry.registerEntityRenderingHandler(HelicopterEntity.class, RenderBaseVehicle.RenderHelicopter::new);
        RenderingRegistry.registerEntityRenderingHandler(PropsEntity.class, RenderProp::new);
        RenderingRegistry.registerEntityRenderingHandler(DoorEntity.class, RenderDoor::new);
        RenderingRegistry.registerEntityRenderingHandler(RagdollEntity.class, RenderRagdoll::new);
        RenderingRegistry.registerEntityRenderingHandler(SeatEntity.class, RenderSeatEntity::new);

        ((SimpleReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(this);

        // Code below was in ClientProxy.init() instead of preInit() now
        MinecraftForge.EVENT_BUS.register(new KeyHandler(FMLClientHandler.instance().getClient()));
        ClientCommandHandler.instance.registerCommand(new DynamXClientCommand());

        ClientRegistry.bindTileEntitySpecialRenderer(TEDynamXBlock.class, new TESRDynamXBlock<>());
        if (!Minecraft.getMinecraft().getFramebuffer().isStencilEnabled()) {
            Minecraft.getMinecraft().getFramebuffer().enableStencil();
        }
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate) {
        if (resourcePredicate.test(VanillaResourceType.MODELS)) {
            DynamXRenderUtils.initGlMeshes();
            DynamXLoadingTasks.reload(DynamXLoadingTasks.TaskContext.CLIENT, DynamXLoadingTasks.MODEL).thenAccept(empty -> {
                if (Minecraft.getMinecraft().player != null && DynamXErrorManager.getErrorManager().hasErrors(DynamXErrorManager.MODEL_ERRORS))
                    Minecraft.getMinecraft().player.sendMessage(new TextComponentTranslation("dynamx.reload.models.errors"));
            });
        }
    }
}
