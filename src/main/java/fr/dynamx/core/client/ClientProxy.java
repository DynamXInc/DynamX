package fr.dynamx.core.client;

import fr.aym.acsguis.api.ACsGuiApiService;
import fr.aym.acslib.ACsLib;
import fr.aym.acslib.api.services.ThreadedLoadingService;
import fr.aym.mps.utils.UserErrorMessageException;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.core.client.command.DynamXClientCommand;
import fr.dynamx.core.client.handlers.ClientEventHandler;
import fr.dynamx.core.client.handlers.KeyHandler;
import fr.dynamx.core.client.network.ClientPhysicsEntitySynchronizer;
import fr.dynamx.core.client.renders.RenderProp;
import fr.dynamx.core.client.renders.RenderRagdoll;
import fr.dynamx.core.client.renders.RenderSeatEntity;
import fr.dynamx.core.client.renders.TESRDynamXBlock;
import fr.dynamx.core.client.renders.vehicle.RenderBaseVehicle;
import fr.dynamx.core.client.renders.vehicle.RenderDoor;
import fr.dynamx.core.client.sound.DynamXSoundHandler;
import fr.dynamx.core.common.CommonProxy;
import fr.dynamx.core.common.DynamXContext;
import fr.dynamx.core.common.DynamXMain;
import fr.dynamx.core.common.blocks.TEDynamXBlock;
import fr.dynamx.core.common.entities.PhysicsEntity;
import fr.dynamx.core.common.entities.PropsEntity;
import fr.dynamx.core.common.entities.RagdollEntity;
import fr.dynamx.core.common.entities.SeatEntity;
import fr.dynamx.core.common.entities.vehicles.*;
import fr.dynamx.core.common.network.sync.PhysicsEntitySynchronizer;
import fr.dynamx.core.common.network.sync.SPPhysicsEntitySynchronizer;
import fr.dynamx.core.common.physics.entities.AbstractEntityPhysicsHandler;
import fr.dynamx.core.common.physics.world.BuiltinThreadedPhysicsWorld;
import fr.dynamx.core.utils.DynamXConstants;
import fr.dynamx.core.utils.DynamXLoadingTasks;
import fr.dynamx.core.utils.client.DynamXRenderUtils;
import fr.dynamx.core.utils.errors.DynamXErrorManager;
import fr.dynamx.core.utils.optimization.SubClassPool;
import fr.hermes.api.mc.world.HmClientWorld;
import fr.hermes.api.mc.world.HmServerWorld;
import fr.hermes.api.mc.world.HmWorld;
import fr.hermes.forge.JmeVector3fPool;

import java.util.function.Predicate;

public class ClientProxy extends CommonProxy implements ISelectiveResourceReloadListener {
    public static DynamXSoundHandler SOUND_HANDLER = new DynamXSoundHandler();

    public ClientProxy() {
        DynamXContext.initObjModelRegistry();
        ModelLoaderRegistry.registerLoader(DynamXContext.getDxModelRegistry().getItemRenderer());
    }

    @Override
    public boolean isDedicatedServer() {
        return false;
    }

    @Override
    public void scheduleTask(HmWorld mcWorld, Runnable task) {
        if (mcWorld.hm$isClient()) {
            ((HmClientWorld) mcWorld).hm$getMinecraftClient().addScheduledTask(task);
        } else {
            ((HmServerWorld) mcWorld).hm$getServer().hm$addScheduledTask(task);
        }
    }

    @Override
    public void preInit() {
        super.preInit();

        DynamXContext.getDxModelRegistry().onPackInfosReloaded();

        RenderingRegistry.registerEntityRenderingHandler(CarEntity.class, RenderBaseVehicle.RenderCar::new);
        RenderingRegistry.registerEntityRenderingHandler(BoatEntity.class, RenderBaseVehicle.RenderBoat::new);
        RenderingRegistry.registerEntityRenderingHandler(TrailerEntity.class, RenderBaseVehicle.RenderTrailer::new);
        RenderingRegistry.registerEntityRenderingHandler(HelicopterEntity.class, RenderBaseVehicle.RenderHelicopter::new);
        RenderingRegistry.registerEntityRenderingHandler(PropsEntity.class, RenderProp::new);
        RenderingRegistry.registerEntityRenderingHandler(DoorEntity.class, RenderDoor::new);
        RenderingRegistry.registerEntityRenderingHandler(RagdollEntity.class, RenderRagdoll::new);
        RenderingRegistry.registerEntityRenderingHandler(SeatEntity.class, RenderSeatEntity::new);

        ((SimpleReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(this);
    }

    @Override
    public void init() {
        super.init();

        MinecraftForge.EVENT_BUS.register(new KeyHandler(FMLClientHandler.instance().getClient()));
        ClientCommandHandler.instance.registerCommand(new DynamXClientCommand());

        ClientRegistry.bindTileEntitySpecialRenderer(TEDynamXBlock.class, new TESRDynamXBlock<>());
        if (!Minecraft.getMinecraft().getFramebuffer().isStencilEnabled())
            Minecraft.getMinecraft().getFramebuffer().enableStencil();
    }

    @Override
    public void completeInit() {
        super.completeInit();
        SplashProgress.pause();
        try {
            DynamXContext.getDxModelRegistry().uploadVAOs();
        } finally {
            SplashProgress.resume();
        }
    }

    @Override
    public HmWorld getClientWorld() {
        return (HmWorld) FMLClientHandler.instance().getClient().world;
    }

    @Override
    public HmWorld getServerWorld() {
        return (HmWorld) FMLCommonHandler.instance().getMinecraftServerInstance().getEntityWorld();
    }

    @Override
    public boolean shouldUseBulletSimulation(HmWorld world) {
        return super.shouldUseBulletSimulation(world) && world.hm$isClient();
    }

    @Override
    public <T extends AbstractEntityPhysicsHandler<?, ?>> PhysicsEntitySynchronizer<? extends PhysicsEntity<T>> getNetHandlerForEntity(PhysicsEntity<T> tPhysicsEntity) {
        if (tPhysicsEntity.getHmWorld().hm$isClient()) {
            if (FMLCommonHandler.instance().getMinecraftServerInstance() != null)
                return new SPPhysicsEntitySynchronizer<>(tPhysicsEntity, Side.CLIENT);
            else
                return new ClientPhysicsEntitySynchronizer<>(tPhysicsEntity);
        }
        return super.getNetHandlerForEntity(tPhysicsEntity);
    }

    @Override
    public int getTickTime() {
        return FMLClientHandler.instance().getClient().player.ticksExisted;
    }

    @Override
    public boolean ownsSimulation(PhysicsEntity<?> entity) {
        //TODO NEW SYNC CLEAN THIS
        if (entity.getSynchronizer().getSimulationHolder().ownsPhysics(entity.getHmWorld().hm$isClient())) {
            return true;
        }
        if (entity.getHmWorld().hm$isClient() && ClientEventHandler.MC.player.getRidingEntity() instanceof PhysicsEntity
                && ((PhysicsEntity<?>) ClientEventHandler.MC.player.getRidingEntity()).getSynchronizer().getSimulationHolder().ownsPhysics(true)) {
            return true;
        }
        return ClientEventHandler.MC.player != null && DynamXContext.getPlayerPickingObjects().containsKey(ClientEventHandler.MC.player.getEntityId()) &&
                DynamXContext.getPlayerPickingObjects().get(ClientEventHandler.MC.player.getEntityId()) == entity.getEntityId();
        //on client side : true if the player is driving a vehicle (in any entity)
        /*return entity.getNetwork().getSimulationHolder() == SimulationHolder.SERVER_SP || (!entity.world.isRemote && entity.getNetwork().getSimulationHolder() == SimulationHolder.SERVER)
                || ()
                || ));*/
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

    @Override
    public void initPhysicsWorld(HmWorld world) {
        if (DynamXContext.getPhysicsWorldPerDimensionMap().containsKey(world.hm$getDimension())) {
            // connecting to another server (e.g. with bungeecoord) : unload the previous world
            DynamXMain.log.info("Duplicate world load detected. Are using BungeeCoord ? Unloading old world.");
            IPhysicsWorld physicsWorld = DynamXContext.getPhysicsWorld(world);
            if (physicsWorld != null) {
                DynamXMain.log.debug("Clearing current physics world...");
                physicsWorld.clearAll();
                DynamXContext.getPlayerToCollision().clear();
            } else {
                throw new IllegalStateException("Physics world loaded but not found. Dim: " + world.hm$getDimension() + " World: " + world);
            }
        }
        DynamXContext.getPhysicsWorldPerDimensionMap().put(world.provider.getDimension(), new BuiltinThreadedPhysicsWorld(world, !ClientEventHandler.MC.isSingleplayer()));
    }

    private byte loadingState;

    @Override
    public void schedulePacksInit() {
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
}
