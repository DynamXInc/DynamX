package fr.dynamx.utils.client;

import com.google.common.collect.Maps;
import fr.dynamx.api.contentpack.object.IInfoOwner;
import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.contentpack.type.ObjectInfo;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.IStateMapper;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static fr.dynamx.common.DynamXMain.log;
import static fr.dynamx.utils.DynamXConstants.ID;

@SideOnly(Side.CLIENT)
public class ContentPackUtils {
    private static final Map<String, String> packToLangFile = new HashMap<>();

    /**
     * Creates the json model files for this object, in the corresponding pack, if not already present
     */
    public static void addMissingJSONs(IResourcesOwner item, ObjectInfo<?> objectInfo, File dynxDir, byte metadata) {
        if (dynxDir.isDirectory()) {
            File modelDir = new File(dynxDir, objectInfo.getPackName() + "/assets/" + ID + "/models/item");
            createItemJsonFile(modelDir, item.getJsonName(metadata), objectInfo.getIconFileName(metadata));
        }
    }

    private static void createItemJsonFile(File dir, String fileName, String iconName) {
        ResourceLocation location = new ResourceLocation(ID, "models/item/" + fileName + ".json");
        try {
            try {
                Minecraft.getMinecraft().getResourceManager().getResource(location);
            } catch (FileNotFoundException e) {
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                writeInFile(new File(dir, fileName + ".json"),
                        "{ \"parent\": \"builtin/generated\", \"textures\": { \"layer0\": \"" + ID + ":icons/" + iconName + "\" }, \"display\": { "
                                + "\"thirdperson_lefthand\": { \"rotation\": [ 0, 90, -35 ], \"translation\": [ 0, 1.25, -2.5 ], \"scale\": [ 0.85, 0.85, 0.85 ] }, "
                                + "\"thirdperson_righthand\": { \"rotation\": [ 0, 90, -35 ], \"translation\": [ 0, 1.25, -2.5 ], \"scale\": [ 0.85, 0.85, 0.85 ] }, "
                                + "\"firstperson_lefthand\": { \"rotation\": [ 0, -45, 25 ], \"translation\": [ 0, 4, 2 ], \"scale\": [ 0.85, 0.85, 0.85 ] }, "
                                + "\"firstperson_righthand\": { \"rotation\": [ 0, -45, 25 ], \"translation\": [ 0, 4, 2 ], \"scale\": [ 0.85, 0.85, 0.85 ] }"
                                + " } }");
            }
        } catch (IOException e) {
            log.error("Failed to create item json file " + fileName, e);
        }
    }

    /**
     * Creates the json model files for this object, in the corresponding pack, if not already present
     */
    public static void createBlockJson(IResourcesOwner item, ObjectInfo<?> objectInfo, File dynxDir) {
        if (dynxDir.isDirectory()) {
            File modelDir = new File(dynxDir, objectInfo.getPackName() + "/assets/" + ID + "/blockstates");
            createBlockstateJsonFile(modelDir, item.getJsonName(0));
        }
    }

    private static void createBlockstateJsonFile(File dir, String fileName) {
        try {
            writeInFile(new File(dir, fileName + ".json"),
                    "{ \"variants\": { \"metadata=0\": { \"model\": \"" + ID + ":" + fileName + "\"}," +
                            "\"metadata=1\": { \"model\": \"" + ID + ":" + fileName + "\", \"y\": 90}," +
                            "\"metadata=2\": { \"model\": \"" + ID + ":" + fileName + "\", \"y\": 180}," +
                            "\"metadata=3\": { \"model\": \"" + ID + ":" + fileName + "\", \"y\": 270}" + " } }");
        } catch (IOException e) {
            log.error("Failed to create item json file " + fileName, e);
        }
    }

    /**
     * Creates a file and write contents, if the file not already exists
     */
    private static void writeInFile(File file, String contents) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            out.write(contents);
            out.close();
            log.info(file.getName() + " not found so we created one");
        }
    }

    private static boolean existsCaseSensitive(File dir, String filename){
        String[] files = dir.list();
        for(String file : files)
            if(file.equals(filename))
                return true;
        return false;
    }

    public static File getPackLangFile(File dynxDir, String packName) throws IOException {
        File langPath = new File(dynxDir, packName + "/assets/" + ID + "/lang/");
        if (!langPath.exists()) {
            langPath.mkdirs();
        }
        if(packToLangFile.containsKey(packName)) {
            return new File(langPath, packToLangFile.get(packName));
        }
        File mcmetaFile = new File(dynxDir, packName + "/pack.mcmeta");
        File langUsFile = new File(langPath, "en_us.lang");
        File langUSFile = new File(langPath, "en_US.lang");
        if(mcmetaFile.exists()) {
            packToLangFile.put(packName, "en_us.lang");
            if(existsCaseSensitive(langPath, "en_US.lang")) {
                log.info("[AUTO-LANG] Renaming " + langUSFile.getPath() + " to " + langUsFile.getPath());
                if(!langUSFile.renameTo(langUsFile))
                    log.warn("[AUTO-LANG] Failed to rename");
                return langUSFile;
            } else {
                langUsFile.createNewFile();
                return langUsFile;
            }
        } else {
            packToLangFile.put(packName, "en_US.lang");
            if(existsCaseSensitive(langPath, "en_us.lang")) {
                log.info("[AUTO-LANG] Renaming " + langUsFile.getPath() + " to " + langUSFile.getPath());
                if(!langUsFile.renameTo(langUSFile))
                    log.warn("[AUTO-LANG] Failed to rename");
                return langUsFile;
            } else {
                langUSFile.createNewFile();
                return langUSFile;
            }
        }
    }

    /**
     * Writes the translation of this object in the pack lang file, if not already present in the translation file
     */
    @SuppressWarnings("unchecked")
    public static void addMissingLangFile(File dynxDir, String packName, String translationKey, String translationValue) {
        if (!I18n.hasKey(translationKey)) {
            try {
                writeInLangFile(getPackLangFile(dynxDir, packName), translationKey+"="+translationValue);
            } catch (IOException e) {
                log.error("Failed to add missing translation for " + packName + " : " +  translationKey, e);
            }
        }
    }

    /**
     * Writes the translation of this object in the given lang file, if not already present in the file
     */
    public static void writeInLangFile(File langFile, String translation) throws IOException {
        BufferedReader inputStream = new BufferedReader(new InputStreamReader(new FileInputStream(langFile)));
        if (inputStream.lines().noneMatch(s -> s.contains(translation.substring(0, translation.lastIndexOf("="))))) {
            BufferedWriter out = new BufferedWriter(new FileWriter(langFile, true));
            out.write(translation + "\n");
            out.close();
            //log.info("Translation not found so we added one");
        }
        inputStream.close();
    }

    /**
     * Registers an block with a {@link IStateMapper}, it permits to ignore some blockstate properties for the render <br>
     * See {@link BlockModelShapes} to see how Minecraft uses this
     *
     * @param block       The block
     * @param stateMapper The {@link IStateMapper}
     */
    public static void registerBlockWithStateMapper(Block block, IStateMapper stateMapper) {
        ModelLoader.setCustomStateMapper(block, stateMapper);
    }

    public static void registerDynamXBlockStateMapper(IInfoOwner<BlockObject<?>> block) {
        registerBlockWithStateMapper((Block) block, new StateMapperBase() {
            protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
                Map<IProperty<?>, Comparable<?>> map = Maps.newLinkedHashMap(state.getProperties());
                map.put(DynamXBlock.METADATA, ((state.getValue(DynamXBlock.METADATA) + 1) / 4) % 4);
                return new ModelResourceLocation(DynamXConstants.ID + ":" + ((DynamXBlock<?>) block).getJsonName(state.getValue(DynamXBlock.METADATA)), this.getPropertyString(map));
            }
        });
    }

    /**
     * Forge will not cry if this block has no model
     */
    public static void registerBlockWithNoModel(Block block) {
        registerBlockWithStateMapper(block, NO_MODEL);
    }

    private static final IStateMapper NO_MODEL = blockIn -> {
        return Maps.newHashMap();
    };
}
