package fr.dynamx.common.contentpack;

import fr.aym.acsguis.api.ACsGuiApi;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.aym.acslib.api.services.mps.ModProtectionContainer;
import fr.dynamx.api.contentpack.ContentPackType;
import fr.dynamx.api.events.ContentPackSystemEvent;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.network.sync.SynchronizedEntityVariableRegistry;
import fr.dynamx.client.gui.GuiBlockCustomization;
import fr.dynamx.client.gui.GuiDnxDebug;
import fr.dynamx.client.gui.GuiLoadingErrors;
import fr.dynamx.client.handlers.hud.CarController;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.loader.InfoLoader;
import fr.dynamx.common.contentpack.loader.SubInfoTypesRegistry;
import fr.dynamx.common.contentpack.sync.PackSyncHandler;
import fr.dynamx.common.slopes.GuiSlopesConfig;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXLoadingTasks;
import fr.dynamx.utils.errors.DynamXErrorManager;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLModContainer;
import net.minecraftforge.fml.common.MetadataCollection;
import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.fml.common.discovery.ContainerType;
import net.minecraftforge.fml.common.discovery.ModCandidate;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
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
        SubInfoTypesRegistry.discoverSubInfoTypes(event);
        SynchronizedEntityVariableRegistry.discoverSyncVars(event);
        //Discover resources
        int packCount = 0;
        for (File file : myDir.listFiles()) {
            if (file.isDirectory() || file.getName().endsWith(".zip") || file.getName().endsWith(PACK_FILE_EXTENSION)) {
                DynamXMain.log.debug("Loading resource pack: " + file.getName());
                //Add assets
                if (side.isClient() && loadPackResources(file, file.isDirectory() ? ContainerType.DIR : ContainerType.JAR))
                    packCount++;
                else if (side.isServer())
                    packCount++;
                //Add custom ModProtectionSystem repositories
                protectedResources.put(file.getName(), modProtectionContainer.getParent().loadCustomRepository(modProtectionContainer, file));
            }
            if (file.isDirectory()) {
                //And load protected files -> Now directly handled by mps
                for (File f : file.listFiles()) {  //Keep handle the PackFiles.jar, but deprecated
                    if (f.isFile() && f.getName().endsWith(".jar")) {
                        try {
                            //Protected resources (.part files), only add to classpath
                            ((LaunchClassLoader) Thread.currentThread().getContextClassLoader()).addURL(f.toURI().toURL());
                        } catch (Throwable e) {
                            DynamXMain.log.error("Failed to load mps resources jar : " + f.getName());
                            DynamXMain.log.throwing(e);
                            if (!(e instanceof Exception)) //todo clean
                                e = new RuntimeException("encapsulated error", e);
                            DynamXErrorManager.addError(file.getName(), DynamXErrorManager.INIT_ERRORS, "res_pack_load_fail", ErrorLevel.FATAL, "assets", "Failed to add to classpath", (Exception) e, 700);
                        }
                        DynamXMain.log.info("Loaded mps pack file : " + f.getName());
                    }
                }
            }
        }
        DynamXMain.log.info("Loaded " + packCount + " DynamX resource packs");
        if (side.isClient()) {
            //Add built-in style, before customs by addons
            ACsGuiApi.registerStyleSheetToPreload(GuiDnxDebug.STYLE);
            ACsGuiApi.registerStyleSheetToPreload(GuiLoadingErrors.STYLE);
            ACsGuiApi.registerStyleSheetToPreload(CarController.STYLE);
            ACsGuiApi.registerStyleSheetToPreload(GuiBlockCustomization.STYLE);
            ACsGuiApi.registerStyleSheetToPreload(GuiSlopesConfig.STYLE);
        }
        return myDir; //return the used path, used when reloading config
    }

    private static boolean loadPackResources(File file, ContainerType type) {
        try {
            HashMap<String, Object> map = new HashMap<>();
            map.put("modid", DynamXConstants.ID);
            map.put("name", "DynamX assets : " + file.getName());
            map.put("version", "1.0");
            FMLModContainer container = new FMLModContainer("fr.dynamx.common.DynamXMain", new ModCandidate(file, file, type), map);
            container.bindMetadata(MetadataCollection.from(null, ""));
            FMLClientHandler.instance().addModAsResource(container);
            return true;
        } catch (Throwable e) {
            DynamXMain.log.error("Failed to load textures and models of DynamX pack : " + file.getName());
            DynamXMain.log.throwing(e);
            if (!(e instanceof Exception)) //todo clean
                e = new RuntimeException("encapsulated error", e);
            DynamXErrorManager.addError(file.getName(), DynamXErrorManager.INIT_ERRORS, "res_pack_load_fail", ErrorLevel.FATAL, "assets", "Failed to register as resource pack", (Exception) e, 700);
            return false;
        }
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
        for (InfoLoader<?> loader : DynamXObjectLoaders.LOADERS)
            loader.clear(isHotReloading);
        DynamXErrorManager.getErrorManager().clear(DynamXErrorManager.PACKS__ERRORS);
        DynamXContext.getObjModelDataCache().clear();
        try {
            ProgressManager.ProgressBar bar = ProgressManager.push("Loading content pack system", 1 + DynamXObjectLoaders.LOADERS.size());
            bar.step("Discover assets");

            MinecraftForge.EVENT_BUS.post(new ContentPackSystemEvent.Load(PhysicsEntityEvent.Phase.PRE));
            //List<ModularVehicleInfoBuilder> vehiclesToLoad = new ArrayList<>();
            int packCount = 0;
            int errorCount = 0;
            String suffix = ".dynx";
            for (File contentPack : resDir.listFiles()) {
                if (contentPack.getName().equals("slopes.dynx")) {
                    if (loadBlocksConfigs)
                        registerSlopes(new BufferedReader(new InputStreamReader(new FileInputStream(contentPack))));
                } else if (contentPack.getName().equals("blocks.dynx")) {
                    if (loadBlocksConfigs)
                        registerBlockGrip(new BufferedReader(new InputStreamReader(new FileInputStream(contentPack))));
                } else if (contentPack.isDirectory()) {
                    // Loading pack, useful for debugging errors
                    String loadingPack = contentPack.getName();
                    try {
                        AtomicReference<PackFile> packInfo = new AtomicReference<>();
                        List<PackFile> packFiles = new ArrayList<>();
                        Stream<Path> configs = Files.walk(Paths.get(contentPack.getPath()));
                        configs.forEach(path -> {
                            if (path.toString().endsWith(suffix)) {
                                try {
                                    PackFile packFile = new PackFile(path.getFileName().toString(), new FileInputStream(path.toFile()));
                                    if (packFile.getName().endsWith("pack_info.dynx"))
                                        packInfo.set(packFile);
                                    else
                                        packFiles.add(packFile);
                                } catch (FileNotFoundException e) {
                                    throw new RuntimeException("Failed to find file " + path, e);
                                }
                            }
                        });
                        loadPack(loadingPack, contentPack, ContentPackType.FOLDER, suffix, packInfo.get(), packFiles);
                        packCount++;
                    } catch (Throwable e) {
                        //log.error("Content Pack " + loadingPack + " cannot be loaded : ", e);
                        if (!(e instanceof Exception)) //todo clean
                            e = new RuntimeException("encapsulated error", e);
                        DynamXErrorManager.addError(loadingPack, DynamXErrorManager.PACKS__ERRORS, "pack_load_fail", ErrorLevel.FATAL, "loading folder pack", loadingPack, (Exception) e, 800);
                        errorCount++;
                    }
                } else if (contentPack.isFile() && (contentPack.getName().endsWith(".zip") || contentPack.getName().endsWith(PACK_FILE_EXTENSION))) {
                    // Loading pack, useful for debugging errors
                    String loadingPack = contentPack.getName().replace(".zip", "").replace(PACK_FILE_EXTENSION, "");
                    try {
                        ZipFile zip = new ZipFile(contentPack);
                        PackFile packInfo = null;
                        List<PackFile> packFiles = new ArrayList<>();
                        Enumeration<? extends ZipEntry> configs = zip.entries();
                        while (configs.hasMoreElements()) {
                            ZipEntry config = configs.nextElement();
                            if (config.getName().endsWith(suffix)) {
                                PackFile packFile = new PackFile(config.getName().substring(config.getName().lastIndexOf("/") + 1), zip.getInputStream(config));
                                if (config.getName().endsWith("pack_info.dynx"))
                                    packInfo = packFile;
                                else
                                    packFiles.add(packFile);
                            }
                        }
                        loadPack(loadingPack, contentPack, contentPack.getName().endsWith(".zip") ? ContentPackType.ZIP : ContentPackType.DNXPACK, suffix, packInfo, packFiles);
                        packCount++;
                    } catch (Throwable e) {
                        //log.error("Compressed content Pack " + loadingPack + " cannot be loaded : ", e);
                        if (!(e instanceof Exception)) //todo clean
                            e = new RuntimeException("encapsulated error", e);
                        DynamXErrorManager.addError(loadingPack, DynamXErrorManager.PACKS__ERRORS, "pack_load_fail", ErrorLevel.FATAL, "loading compressed pack", loadingPack, (Exception) e, 800);
                        errorCount++;
                    }
                } else if (!contentPack.getName().endsWith(".dll") && !contentPack.getName().endsWith(".so") && !contentPack.getName().endsWith(".dylib")) { //Bullet library files
                    log.warn("File " + contentPack.getName() + " isn't a valid DynamX content pack file");
                }
            }
            //Load shapes
            for (InfoLoader<?> loader : DynamXObjectLoaders.LOADERS) {
                bar.step("Post load : " + loader.getPrefix().substring(0, loader.getPrefix().length()-1));
                loader.postLoad(isHotReloading);
            }
            ProgressManager.pop(bar);
            MinecraftForge.EVENT_BUS.post(new ContentPackSystemEvent.Load(PhysicsEntityEvent.Phase.POST));
            log.info("Loaded " + packCount + " content packs");
            if (errorCount > 0)
                log.warn("Ignored " + errorCount + " errored packs");
        } catch (Throwable e) {
            log.error("Fatal error while loading DynamX packs, we can't continue !", e);
            throw new RuntimeException(e);
        }
        if (FMLCommonHandler.instance().getSide().isClient()) {
            //Reload languages added by packs
            scheduleLanguageRefresh();
        }
        PackSyncHandler.computeAll();
        DynamXLoadingTasks.endTask(DynamXLoadingTasks.PACK);
    }

    private static void loadPack(String loadingPack, File contentPack, ContentPackType packType, String suffix, PackFile packInfo, List<PackFile> packFiles) {
        //Search for real pack name in the pack info
        String packVersion = "<missing pack info>";
        PackInfo loadedInfo = packInfo != null ? loadPackInfoFile(loadingPack, suffix, packInfo, contentPack.getName(), packType) : null;
        if (loadedInfo != null) {
            loadingPack = loadedInfo.getFixedPackName();
            packVersion = loadedInfo.getPackVersion();
        } else {
            loadedInfo = new PackInfo(loadingPack, packType).setPathName(contentPack.getName()).setPackVersion("dummy_info");
            DynamXErrorManager.addError(loadingPack, DynamXErrorManager.PACKS__ERRORS, "missing_pack_info", ErrorLevel.HIGH, loadedInfo.getName(), "Add a pack_info.dynx file in the pack !", null, 600);
            DynamXObjectLoaders.PACKS.loadItems(loadedInfo, isHotReloading);
        }
        DynamXMain.log.info("Loading " + loadingPack + " version " + packVersion + " (in " + contentPack.getName() + ")");
        for (PackFile packFile : packFiles) {
            loadFile(loadingPack, suffix, packFile);
        }
    }

    private static PackInfo loadPackInfoFile(String loadingPack, String suffix, PackFile file, String pathName, ContentPackType packType) {
        BufferedReader inputStream = null;
        try {
            inputStream = new BufferedReader(new InputStreamReader(file.getInputStream()));
            String configName = file.getName().substring(0, file.getName().length() - suffix.length()).toLowerCase();
            return DynamXObjectLoaders.PACKS.load(loadingPack, configName, inputStream, isHotReloading, pathName, packType);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (Throwable e) {
            if (!(e instanceof Exception)) //todo clean
                e = new RuntimeException("encapsulated error", e);
            DynamXErrorManager.addError(loadingPack, DynamXErrorManager.PACKS__ERRORS, "pack_file_load_error", ErrorLevel.FATAL, file.getName().replace(suffix, ""), null, (Exception) e, 100);
            return null;
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

    private static void loadFile(String loadingPack, String suffix, PackFile file) {
        BufferedReader inputStream = null;
        try {
            inputStream = new BufferedReader(new InputStreamReader(file.getInputStream()));
            String configName = file.getName().substring(0, file.getName().length() - suffix.length()).toLowerCase();
            boolean loaded = false;
            for (InfoLoader<?> loader : DynamXObjectLoaders.LOADERS) {
                if (loader.load(loadingPack, configName, inputStream, isHotReloading)) {
                    loaded = true;
                    break;
                }
            }
            if (!loaded)
                throw new IllegalArgumentException("Invalid " + suffix + " file name : " + file.getName());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (Throwable e) {
            if (!(e instanceof Exception)) //todo clean
                e = new RuntimeException("encapsulated error", e);
            DynamXErrorManager.addError(loadingPack, DynamXErrorManager.PACKS__ERRORS, "pack_file_load_error", ErrorLevel.FATAL, file.getName().replace(suffix, ""), null, (Exception) e, 100);
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
                    log.error("Bad block grip config: block " + blockString[0] + " doesn't exist");
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

    private static class PackFile {
        private final String name;
        private final InputStream inputStream;

        private PackFile(String name, InputStream inputStream) {
            this.name = name;
            this.inputStream = inputStream;
        }

        public String getName() {
            return name;
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        @Override
        public String toString() {
            return "PackFile{" + name + '}';
        }
    }
}