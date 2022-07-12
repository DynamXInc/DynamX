package fr.dynamx.common.contentpack;

import fr.aym.acsguis.api.ACsGuiApi;
import fr.aym.acslib.api.services.ErrorTrackingService;
import fr.aym.acslib.api.services.mps.ModProtectionContainer;
import fr.dynamx.api.events.ContentPackSystemEvent;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.client.handlers.hud.CarController;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.loader.InfoLoader;
import fr.dynamx.common.contentpack.sync.PackSyncHandler;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXLoadingTasks;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLModContainer;
import net.minecraftforge.fml.common.MetadataCollection;
import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.fml.common.discovery.ContainerType;
import net.minecraftforge.fml.common.discovery.ModCandidate;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.versioning.ArtifactVersion;
import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static fr.dynamx.common.DynamXMain.log;

/**
 * Root of the DynamX packs system
 */
public class ContentPackLoader {
    public static final String PACK_FILE_EXTENSION = ".dnxpack";

    /**
     * For hot reload detection
     */
    private static boolean initialized;
    public static boolean isHotReloading;
    /**
     * Loaded BlockInfo
     */
    private static final Map<Block, float[]> BLOCKS_GRIP = new HashMap<>();
    private static final float[] DEFAULT_GRIP = new float[]{1, 0.9f};

    /**
     * Version of this loader
     */
    public static final ArtifactVersion LOADER_VERSION = new DefaultArtifactVersion("1.0.1");

    /**
     * Blocks where slopes can be placed
     */
    public static final List<Block> slopes = new ArrayList<>();

    public static boolean PLACE_SLOPES = false;
    public static int SLOPES_LENGTH = 20;

    /**
     * Protected resources of protected packs
     */
    private static final Map<String, ModProtectionContainer> protectedResources = new HashMap<>();

    /**
     * Inits the packs by finding the packs folder and adding resources to FML resources packs <br>
     * Additionally loads addons
     *
     * @param folderName The packs folder name
     * @return The chosen folder file
     */
    public static File init(FMLConstructionEvent event, ModProtectionContainer modProtectionContainer, String folderName, Side side) {
        //Production-environment
        File myDir = new File(folderName);
        if (!myDir.exists()) {
            if (myDir.getParentFile() != null) {
                //Dev-environment
                myDir = new File(myDir.getParentFile().getParentFile(), folderName);
                if (!myDir.exists()) {
                    //first-run of the mod, in production environment
                    myDir = new File(folderName);
                    myDir.mkdirs();
                }
            } else //First run, in production environment
                myDir.mkdirs();
        }
        //Discover addons
        AddonLoader.discoverAddons(event);
        //Discover resources
        if (side.isClient()) {
            int packCount = 0;
            for (File file : myDir.listFiles()) {
                if (file.isDirectory()) {
                    if (new File(file, "assets").exists()) {
                        try {
                            //not needed, only for java classes ((LaunchClassLoader)Thread.currentThread().getContextClassLoader()).addURL(file.toURI().toURL());
                            HashMap<String, Object> map = new HashMap<>();
                            map.put("modid", DynamXConstants.ID);
                            map.put("name", "DynamX pack : " + file.getName());
                            map.put("version", "1.0");
                            FMLModContainer container = new FMLModContainer("fr.dynamx.common.DynamXMain", new ModCandidate(file, file, ContainerType.DIR), map);
                            container.bindMetadata(MetadataCollection.from(null, ""));
                            FMLClientHandler.instance().addModAsResource(container);
                            packCount++;
                        } catch (Exception e) {
                            DynamXMain.log.error("Failed to load textures and models of DynamX pack : " + file.getName());
                            DynamXMain.log.throwing(e);
                            DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.INIT, file.getName(), "Failed to register as resource pack", e, ErrorTrackingService.TrackedErrorLevel.FATAL);
                        }
                    }
                    //Custom ModProtectionSystem repositories
                    protectedResources.put(file.getName(), modProtectionContainer.getParent().loadCustomRepository(modProtectionContainer, file));
                    //And load protected files -> Now directly handled by mps
                    for (File f : file.listFiles()) //Keep handle the PackFiles.jar, but deprecated
                    {
                        if (f.isFile() && f.getName().endsWith(".jar")) {
                            try {
                                //Protected resources (.part files), only add to classpath
                                //System.out.println("Adding "+f.toURI().toURL());
                                ((LaunchClassLoader) Thread.currentThread().getContextClassLoader()).addURL(f.toURI().toURL());
                            } catch (Exception e) {
                                DynamXMain.log.error("Failed to load mps resources jar : " + f.getName());
                                DynamXMain.log.throwing(e);
                                DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.INIT, f.getName(), "Failed to add to classpath", e, ErrorTrackingService.TrackedErrorLevel.FATAL);
                            }
                            DynamXMain.log.info("Loaded mps pack : " + file.getName());
                        }
                    }
                    DynamXMain.log.info("Loaded content pack : " + file.getName());
                } else if (file.isFile() && (file.getName().endsWith(".zip") || file.getName().endsWith(PACK_FILE_EXTENSION))) {
                    try {
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("modid", DynamXConstants.ID);
                        map.put("name", "DynamX assets : " + file.getName());
                        map.put("version", "1.0");
                        FMLModContainer container = new FMLModContainer("fr.dynamx.common.DynamXMain", new ModCandidate(file, file, ContainerType.JAR), map);
                        container.bindMetadata(MetadataCollection.from(null, ""));
                        FMLClientHandler.instance().addModAsResource(container);
                        packCount++;
                    } catch (Exception e) {
                        DynamXMain.log.error("Failed to load textures and models of DynamX pack : " + file.getName());
                        DynamXMain.log.throwing(e);
                        DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.INIT, file.getName(), "Failed to register as resource pack", e, ErrorTrackingService.TrackedErrorLevel.FATAL);
                    }
                    //Custom ModProtectionSystem repositories
                    protectedResources.put(file.getName(), modProtectionContainer.getParent().loadCustomRepository(modProtectionContainer, file));
                    //And load protected files -> Now directly handled by mps
                    DynamXMain.log.info("Loaded content pack : " + file.getName());
                }
            }
            DynamXMain.log.info("Loaded " + packCount + " DynamX resource packs");

            //Add built-in style, before customs by addons
            ACsGuiApi.registerStyleSheetToPreload(new ResourceLocation(DynamXConstants.ID, "css/block_custom.css"));
            ACsGuiApi.registerStyleSheetToPreload(new ResourceLocation(DynamXConstants.ID, "css/dnx_debug.css"));
            ACsGuiApi.registerStyleSheetToPreload(CarController.STYLE);
            //CssGuiManager.registerStyleSheetToPreload(new ResourceLocation(DynamXMain.ID, "css/main_menu.css"));
            ACsGuiApi.registerStyleSheetToPreload(new ResourceLocation(DynamXConstants.ID, "css/slope_generator.css"));
        } else {
            for (File file : myDir.listFiles()) {
                if(file.isDirectory() || file.getName().endsWith(".zip") || file.getName().endsWith(PACK_FILE_EXTENSION)) {
                    //Add custom ModProtectionSystem repositories
                    protectedResources.put(file.getName(), modProtectionContainer.getParent().loadCustomRepository(modProtectionContainer, file));
                }
                if (file.isDirectory()) {
                    //And load protected files
                    for (File f : file.listFiles()) {
                        if (f.isFile() && f.getName().endsWith(".jar")) {
                            try {
                                //Protected resources (.part files), only add to classpath
                                ((LaunchClassLoader) Thread.currentThread().getContextClassLoader()).addURL(f.toURI().toURL());
                            } catch (Exception e) {
                                DynamXMain.log.error("Failed to load mps resources jar : " + f.getName());
                                DynamXMain.log.throwing(e);
                                DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.INIT, f.getName(), "Failed to add to classpath", e, ErrorTrackingService.TrackedErrorLevel.FATAL);
                            }
                            DynamXMain.log.info("Loaded mps pack : " + file.getName());
                        }
                    }
                }
            }
            DynamXMain.log.info("Loaded server-side DynamX protected resource packs");
        }
        //FMLCommonHandler.instance().handleExit(0);
        return myDir; //the used path, used when reloading config
    }

    /**
     * @return Protected resources containers for each protected pack
     */
    public static Map<String, ModProtectionContainer> getProtectedResources() {
        return protectedResources;
    }

    /**
     * All builtin addon objects ({@link fr.dynamx.api.contentpack.object.subinfo.ISubInfoType}s, blocks, items...) should have been registered before packs loading <br> <br>
     * Use the addons init callback to avoid problems
     *
     * @return True if the loading of packs has started
     */
    public static boolean isPackLoadingStarted() {
        return initialized;
    }

    /**
     * Reloads all packs <br>
     * <strong>DON'T CALL THIS, USE {@link DynamXLoadingTasks} !</strong>
     *
     * @param resDir            The packs folder
     * @param loadBlocksConfigs If should load blocks.dynx and slopes.dynx
     */
    public static void reload(File resDir, boolean loadBlocksConfigs) {
        isHotReloading = initialized;
        if (!isHotReloading)
            initialized = true;
        for (InfoLoader<?, ?> loader : DynamXObjectLoaders.LOADERS)
            loader.clear(isHotReloading);
        DynamXContext.getErrorTracker().clear(DynamXLoadingTasks.PACK);
        try {
            ProgressManager.ProgressBar bar = ProgressManager.push("Loading content pack system", 1 + DynamXObjectLoaders.LOADERS.size());
            bar.step("Discover assets");

            MinecraftForge.EVENT_BUS.post(new ContentPackSystemEvent.ContentPackLoadEvent(PhysicsEntityEvent.Phase.PRE));
            //List<ModularVehicleInfoBuilder> vehiclesToLoad = new ArrayList<>();
            int packCount = 0;
            int errorCount = 0;
            String suffix = ".dynx";
            for (File contentPack : resDir.listFiles()) {
                if (contentPack.getName().equals("slopes.dynx") && loadBlocksConfigs) {
                    registerSlopes(new BufferedReader(new InputStreamReader(new FileInputStream(contentPack))));
                } else if (contentPack.getName().equals("blocks.dynx") && loadBlocksConfigs) {
                    registerBlockGrip(new BufferedReader(new InputStreamReader(new FileInputStream(contentPack))));
                } else if (contentPack.isDirectory()) {
                    // Loading pack, useful for debugging errors
                    String loadingPack = contentPack.getName();
                    try {
                        Stream<Path> configs = Files.walk(Paths.get(contentPack.getPath()));

                        //Seach for real pack name in the pack info
                        Optional<Path> info = configs.filter(path -> path.getFileName().toString().equals("pack_info.dynx")).findFirst();
                        if (info.isPresent()) {
                            try {
                                loadFile(isHotReloading, loadingPack, suffix, new FileInputStream(info.get().toFile()), info.get().getFileName().toString());
                            } catch (FileNotFoundException e) {
                                throw new RuntimeException("Failed to find file " + info.get(), e);
                            }
                            loadingPack = DynamXObjectLoaders.PACKS.findInfo(loadingPack + ".pack_info").getFixedPackName();
                        } else {
                            log.warn("Content pack " + loadingPack + " is missing a pack_info.dynx file !");
                            DynamXObjectLoaders.PACKS.addInfo(loadingPack + ".pack_info.dynx", new PackInfo(loadingPack).setPathName(contentPack.getName()).setPackVersion("dummy info"));
                            //DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.PACK, loadingPack, "Content Pack " + loadingPack + " is missing a pack_info.dynx file !", "Please add a pack_info.dynx file to this pack", ErrorTrackingService.TrackedErrorLevel.FATAL);
                        }
                        DynamXMain.log.info("Loading " + loadingPack + "(in " + contentPack.getName() + ")");
                        String finalLoadingPack = loadingPack;
                        configs = Files.walk(Paths.get(contentPack.getPath())); //FIXME THIS IS BAD
                        configs.forEach(path -> {
                            if (path.toString().endsWith(suffix) && !path.toString().endsWith("pack_info.dynx")) {
                                try {
                                    loadFile(isHotReloading, finalLoadingPack, suffix, new FileInputStream(path.toFile()), path.getFileName().toString());
                                } catch (FileNotFoundException e) {
                                    throw new RuntimeException("Failed to find file " + path, e);
                                }
                            }
                        });
                        packCount++;
                    } catch (Exception e) {
                        log.error("Content Pack " + loadingPack + " cannot be loaded : ", e);
                        DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.PACK, loadingPack, "Content Pack " + loadingPack + " cannot be loaded", e, ErrorTrackingService.TrackedErrorLevel.FATAL);
                        errorCount++;
                    }
                } else if (contentPack.isFile() && (contentPack.getName().endsWith(".zip") || contentPack.getName().endsWith(PACK_FILE_EXTENSION))) {
                    DynamXMain.log.info("Loading " + contentPack.getName());
                    // Loading pack, useful for debugging errors
                    String loadingPack = contentPack.getName().replace(".zip", "").replace(PACK_FILE_EXTENSION, "");
                    try {
                        ZipFile zip = new ZipFile(contentPack);

                        //Seach for real pack name in the pack info
                        Enumeration<? extends ZipEntry> configs = zip.entries();
                        Optional<ZipEntry> info = Optional.empty();
                        while (configs.hasMoreElements()) {
                            ZipEntry config = configs.nextElement();
                            if (config.getName().equals("pack_info.dynx")) {
                                info = Optional.of(config);
                                break;
                            }
                        }
                        if (info.isPresent()) {
                            try {
                                loadFile(isHotReloading, loadingPack, suffix, zip.getInputStream(info.get()), info.get().getName().substring(info.get().getName().lastIndexOf("/") + 1));
                            } catch (FileNotFoundException e) {
                                throw new RuntimeException("Failed to find file " + info.get(), e);
                            }
                            PackInfo ifo = DynamXObjectLoaders.PACKS.findInfo(loadingPack + ".pack_info");
                            loadingPack = ifo.getFixedPackName();
                            ifo.setPathName(contentPack.getName());
                        } else {
                            log.warn("Zip content pack " + loadingPack + " is missing a pack_info.dynx file !");
                            DynamXObjectLoaders.PACKS.addInfo(loadingPack + ".pack_info", new PackInfo(loadingPack).setPathName(contentPack.getName()).setPackVersion("dummy info"));
                            //DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.PACK, loadingPack, "Zip content Pack " + loadingPack + " is missing a pack_info.dynx file !", "Please add a pack_info.dynx file to this pack", ErrorTrackingService.TrackedErrorLevel.FATAL);
                        }
                        DynamXMain.log.info("Loading " + loadingPack + "(in " + contentPack.getName() + ")");
                        configs = zip.entries(); //FIXME THIS IS BAD
                        while (configs.hasMoreElements()) {
                            ZipEntry config = configs.nextElement();
                            if (config.getName().endsWith(suffix) && !config.getName().endsWith("pack_info.dynx")) {
                                loadFile(isHotReloading, loadingPack, suffix, zip.getInputStream(config), config.getName().substring(config.getName().lastIndexOf("/") + 1));
                            }
                        }
                        packCount++;
                    } catch (Exception e) {
                        log.error("Zip content Pack " + loadingPack + " cannot be loaded : ", e);
                        DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.PACK, loadingPack, "Zip content Pack " + loadingPack + " cannot be loaded", e, ErrorTrackingService.TrackedErrorLevel.FATAL);
                        errorCount++;
                    }
                }
            }
            //Load shapes
            for (InfoLoader<?, ?> loader : DynamXObjectLoaders.LOADERS) {
                bar.step("Post load : " + loader.getPrefix());
                loader.postLoad(isHotReloading);
            }
            ProgressManager.pop(bar);
            MinecraftForge.EVENT_BUS.post(new ContentPackSystemEvent.ContentPackLoadEvent(PhysicsEntityEvent.Phase.POST));
            log.info("Loaded " + packCount + " content packs");
            if (errorCount > 0)
                log.warn("Ignored " + errorCount + " errored packs");
        } catch (Exception e) {
            log.error("Fatal error while loading DynamX packs, we can't continue !", e);
            throw new RuntimeException(e);
        }
        if (FMLCommonHandler.instance().getSide().isClient()) {
            //Reload languages added by packs
            scheduleLanguageRefresh();
        }
        PackSyncHandler.computeAll();
        DynamXLoadingTasks.endTask(DynamXLoadingTasks.PACK);
        System.out.println("LOAD END");
    }

    private static void loadFile(boolean hot, String loadingPack, String suffix, InputStream inputFile, String configFileName) {
        BufferedReader inputStream = null;
        try {
            inputStream = new BufferedReader(new InputStreamReader(inputFile));
            String configName = configFileName.substring(0, configFileName.length() - suffix.length()).toLowerCase();
            boolean loaded = false;
            for (InfoLoader<?, ?> loader : DynamXObjectLoaders.LOADERS) {
                if (loader.load(loadingPack, configName, inputStream, hot)) {
                    loaded = true;
                    break;
                }
            }
            if (!loaded)
                throw new IllegalArgumentException("Invalid " + suffix + " file name : " + configFileName);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error("Content pack file " + configFileName + " of " + loadingPack + " cannot be loaded : ", e);
            DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.PACK, loadingPack, "Content pack file " + configFileName + " cannot be loaded", e, ErrorTrackingService.TrackedErrorLevel.FATAL);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private static void scheduleLanguageRefresh() {
        Minecraft.getMinecraft().addScheduledTask(() -> Minecraft.getMinecraft().getLanguageManager().onResourceManagerReload(Minecraft.getMinecraft().getResourceManager()));
    }

    private static void registerSlopes(BufferedReader reader) {
        String[] array = reader.lines().toArray(String[]::new);
        for (int i = 0; i < array.length; i++) {
            //Configuring length of slopes
            Pattern p = Pattern.compile("length\\s*:\\s*(\\d+)");
            Matcher m = p.matcher(array[i]);
            if (m.find()) {
                SLOPES_LENGTH = Integer.parseInt(m.group(1));
                continue;
            }

            //Configuring auto-placing of slopes
            p = Pattern.compile("auto place\\s*:\\s*(\\w+)");
            m = p.matcher(array[i]);
            if (m.find()) {
                PLACE_SLOPES = Boolean.parseBoolean(m.group(1));
                continue;
            }


            Block block = Block.getBlockFromName(array[i]);
            if (block != null) {
                slopes.add(block);
            } else {
                log.error("Block " + array[i] + " doesn't exist");
            }
        }
    }

    private static void registerBlockGrip(BufferedReader reader) {
        reader.lines().forEach(s -> {
            if (!s.trim().startsWith("//") && s.contains(":")) {
                String[] blockString = s.split(": ");
                Block block = Block.getBlockFromName(blockString[0]);
                if (block != null) {
                    String[] values = blockString[1].split(" ");
                    if (values.length > 1) {
                        BLOCKS_GRIP.put(block, new float[]{
                                Float.parseFloat(values[0]), Float.parseFloat(values[1])});
                    } else {
                        BLOCKS_GRIP.put(block, new float[]{
                                Float.parseFloat(values[0]), Float.parseFloat(values[0])});
                    }
                } else {
                    log.error("Block " + blockString[0] + " doesn't exist");
                }
            }
        });
    }

    public static float[] getBlockFriction(Block of) {
        return BLOCKS_GRIP.getOrDefault(of, DEFAULT_GRIP);
    }

    public static Map<Block, float[]> getBlocksGrip() {
        return BLOCKS_GRIP;
    }
}