package fr.dynamx.client;

import fr.aym.acslib.ACsLib;
import fr.aym.acslib.api.services.ThreadedLoadingService;
import fr.dynamx.api.audio.IDynamXSoundHandler;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.object.render.IObjPackObject;
import fr.dynamx.api.network.sync.v3.PhysicsEntitySynchronizer;
import fr.dynamx.api.obj.IModelTextureVariantsSupplier;
import fr.dynamx.api.obj.ObjModelPath;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.client.handlers.ClientEventHandler;
import fr.dynamx.client.handlers.KeyHandler;
import fr.dynamx.client.network.ClientPhysicsEntitySynchronizer;
import fr.dynamx.client.renders.RenderProp;
import fr.dynamx.client.renders.RenderRagdoll;
import fr.dynamx.client.renders.TESRDynamXBlock;
import fr.dynamx.client.renders.vehicle.RenderBaseVehicle;
import fr.dynamx.client.renders.vehicle.RenderCaterpillar;
import fr.dynamx.client.renders.vehicle.RenderDoor;
import fr.dynamx.client.sound.DynamXSoundHandler;
import fr.dynamx.common.CommonProxy;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.loader.InfoLoader;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.PropsEntity;
import fr.dynamx.common.entities.RagdollEntity;
import fr.dynamx.common.entities.vehicles.*;
import fr.dynamx.common.network.sync.SPPhysicsEntitySynchronizer;
import fr.dynamx.common.network.udp.CommandUdp;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;
import fr.dynamx.common.physics.world.BuiltinThreadedPhysicsWorld;
import fr.dynamx.utils.DynamXLoadingTasks;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.client.CommandNetworkDebug;
import fr.dynamx.utils.errors.DynamXErrorManager;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.resource.IResourceType;
import net.minecraftforge.client.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.client.resource.VanillaResourceType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.util.function.Predicate;

import static fr.dynamx.common.DynamXMain.log;

public class ClientProxy extends CommonProxy implements ISelectiveResourceReloadListener {
    public static IDynamXSoundHandler SOUND_HANDLER = new DynamXSoundHandler();

    public ClientProxy() {
        ModelLoaderRegistry.registerLoader(DynamXContext.getObjModelRegistry().getItemRenderer());
    }

    @Override
    public void scheduleTask(World mcWorld, Runnable task) {
        if (mcWorld.isRemote) {
            Minecraft.getMinecraft().addScheduledTask(task);
        } else {
            mcWorld.getMinecraftServer().addScheduledTask(task);
        }
    }

    @Override
    public void preInit() {
        super.preInit();

        //Loads all models avoiding duplicates
        for (InfoLoader<?> infoLoader : DynamXObjectLoaders.getLoaders()) {
            for (INamedObject namedObject : infoLoader.getInfos().values()) {
                if (namedObject instanceof IObjPackObject && ((IObjPackObject) namedObject).shouldRegisterModel()) {
                    ObjModelPath modelPath = DynamXUtils.getModelPath(namedObject.getPackName(), ((IObjPackObject) namedObject).getModel());
                    DynamXContext.getObjModelRegistry().registerModel(modelPath, (IModelTextureVariantsSupplier) namedObject);
                }
            }
        }
        log.info("Registered " + DynamXContext.getObjModelRegistry().getLoadedModelCount() + " obj models");

        RenderingRegistry.registerEntityRenderingHandler(CaterpillarEntity.class, RenderCaterpillar::new);
        RenderingRegistry.registerEntityRenderingHandler(CarEntity.class, RenderBaseVehicle.RenderCar::new);
        RenderingRegistry.registerEntityRenderingHandler(BoatEntity.class, RenderBaseVehicle.RenderBoat::new);
        RenderingRegistry.registerEntityRenderingHandler(TrailerEntity.class, RenderBaseVehicle.RenderTrailer::new);
        RenderingRegistry.registerEntityRenderingHandler(PropsEntity.class, RenderProp::new);
        RenderingRegistry.registerEntityRenderingHandler(DoorEntity.class, RenderDoor::new);
        RenderingRegistry.registerEntityRenderingHandler(RagdollEntity.class, RenderRagdoll::new);

        ((SimpleReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(this);
    }

    @Override
    public void init() {
        super.init();

        MinecraftForge.EVENT_BUS.register(new KeyHandler(FMLClientHandler.instance().getClient()));
        ClientCommandHandler.instance.registerCommand(new CommandUdp());
        ClientCommandHandler.instance.registerCommand(new CommandNetworkDebug());

        ClientRegistry.bindTileEntitySpecialRenderer(TEDynamXBlock.class, new TESRDynamXBlock<>());
    }

    @Override
    public World getClientWorld() {
        return FMLClientHandler.instance().getClient().world;
    }

    @Override
    public World getServerWorld() {
        return FMLCommonHandler.instance().getMinecraftServerInstance().getEntityWorld();
    }

    @Override
    public boolean shouldUseBulletSimulation(World world) {
        return super.shouldUseBulletSimulation(world) && world.isRemote;
    }

    @Override
    public <T extends AbstractEntityPhysicsHandler<?, ?>> PhysicsEntitySynchronizer<? extends PhysicsEntity<T>> getNetHandlerForEntity(PhysicsEntity<T> tPhysicsEntity) {
        if (tPhysicsEntity.world.isRemote) {
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
        if (entity.getSynchronizer().getSimulationHolder().ownsPhysics(entity.world.isRemote ? Side.CLIENT : Side.SERVER)) {
            return true;
        }
        if (entity.world.isRemote && ClientEventHandler.MC.player.getRidingEntity() instanceof PhysicsEntity
                && ((PhysicsEntity<?>) ClientEventHandler.MC.player.getRidingEntity()).getSynchronizer().getSimulationHolder().ownsPhysics(Side.CLIENT)) {
            return true;
        }
        return DynamXContext.getPlayerPickingObjects().containsKey(ClientEventHandler.MC.player.getEntityId()) &&
                DynamXContext.getPlayerPickingObjects().get(ClientEventHandler.MC.player.getEntityId()) == entity.getEntityId();
        //on client side : true if the player is driving a vehicle (in any entity)
        /*return entity.getNetwork().getSimulationHolder() == SimulationHolder.SERVER_SP || (!entity.world.isRemote && entity.getNetwork().getSimulationHolder() == SimulationHolder.SERVER)
                || ()
                || ));*/
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate) {
        if (resourcePredicate.test(VanillaResourceType.MODELS)) {
            DynamXLoadingTasks.reload(DynamXLoadingTasks.TaskContext.CLIENT, DynamXLoadingTasks.MODEL).thenAccept(empty -> {
                if (Minecraft.getMinecraft().player != null) {
                    if (DynamXErrorManager.getErrorManager().hasErrors(DynamXErrorManager.MODEL_ERRORS)) {
                        //TODO TRANSLATE
                        Minecraft.getMinecraft().player.sendMessage(new TextComponentString(TextFormatting.RED + "[DynamX] Certains modèles ont des problèmes, utilisez le menu de debug pour les voir"));
                    }
                }
            });
        }
    }

    @Override
    public void providePhysicsWorld(World world) {
        if (DynamXContext.getPhysicsWorldPerDimensionMap().containsKey(world.provider.getDimension()))
            throw new IllegalStateException("Physics world of " + world + " is already loaded !");
        DynamXContext.getPhysicsWorldPerDimensionMap().put(world.provider.getDimension(), new BuiltinThreadedPhysicsWorld(world, !ClientEventHandler.MC.isSingleplayer()));
    }

    private byte loadingState;

    @Override
    public void schedulePacksInit() {
        //This event handler needs to be registered before mc's sound system init
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());

        ((SimpleReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener((ISelectiveResourceReloadListener) (resourceManager, resourcePredicate) -> {
            if (loadingState == 0) {
                loadingState++;
            } else if (loadingState == 1) {
                loadingState++;
                ThreadedLoadingService loadingService = ACsLib.getPlatform().provideService(ThreadedLoadingService.class);
                loadingService.addTask(ThreadedLoadingService.ModLoadingSteps.BLOCK_REGISTRY, "packsload", () -> {
                    Vector3fPool.openPool(); //Open a pool for the loading of entities
                    DynamXLoadingTasks.reload(DynamXLoadingTasks.TaskContext.MC_INIT, DynamXLoadingTasks.PACK);

                    //Must follow addons init
                    loadingService.addTask(ThreadedLoadingService.ModLoadingSteps.INIT, "proxy preinit", this::preInit);
                });
            }
        });
    }
}
