package fr.dynamx.common.objloader;

import fr.dynamx.client.renders.model.texture.MaterialTexture;
import fr.dynamx.common.objloader.data.Material;
import fr.dynamx.utils.RegistryNameSetter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;

import javax.vecmath.Vector3f;
import java.util.ArrayList;
import java.util.List;

public class MTLLoader {
    public static final String COMMENT = "#";
    public static final String NEW_MATERIAL = "newmtl";
    public static final String AMBIENT_COLOR = "Ka";
    public static final String DIFFUSE_COLOR = "Kd";
    public static final String SPECULAR_COLOR = "Ks";
    public static final String TRANSPARENCY_D = "d";
    public static final String TRANSPARENCY_TR = "Tr";
    public static final String ILLUMINATION = "illum";

    public static final String TEXTURE_AMBIENT = "map_Ka";
    public static final String TEXTURE_DIFFUSE = "map_Kd";
    public static final String TEXTURE_SPECULAR = "map_Ks";
    public static final String TEXTURE_TRANSPARENCY = "map_d";

    private final List<Material> materials = new ArrayList<>();

    public void parse(String startPath, String content) {
        String[] lines = content.split("\n");
        Material current = null;
        for (String s : lines) {
            String line = s.trim();
            String[] parts = line.split(" ");
            switch (parts[0]) {
                case COMMENT:
                    break;
                case NEW_MATERIAL:
                    Material material = new Material(parts[1]);
                    materials.add(material);
                    current = material;
                    break;
                case AMBIENT_COLOR:
                    current.ambientColor = new Vector3f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3]));
                    break;
                case DIFFUSE_COLOR:
                    current.diffuseColor = new Vector3f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3]));
                    break;
                case TEXTURE_DIFFUSE: {
                    String name = parts.length >= 3 ? parts[2] : "Default";
                    String textureName = parts[1].equalsIgnoreCase("white") ? "textures/white.png" : startPath + parts[1];
                    current.diffuseTexture.put(name,
                            new MaterialTexture(RegistryNameSetter.getResourceLocationWithDynamXDefault(textureName), name, -1));
                    break;
                }
                case TEXTURE_AMBIENT: {
                    String name = parts.length >= 3 ? parts[2] : "Default";
                    current.ambientTexture.put(name,
                            new MaterialTexture(RegistryNameSetter.getResourceLocationWithDynamXDefault(startPath + parts[1]), name, -1));
                    break;
                }
                case TRANSPARENCY_D:
                case TRANSPARENCY_TR:
                    current.transparency = (float) Double.parseDouble(parts[1]);
                    break;
            }
        }
    }

    public List<Material> getMaterials() {
        return materials;
    }

    /**
     * Loads all textures used by this material, avoiding any duplicated textures shared between different materials <br>
     * It only read images files, so it can be called in any thread
     */
    public void loadTextures() {
        TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
        for (Material material : materials) {
            material.ambientTexture.forEach((textureName, textures) -> textures.loadTexture(textureManager));
            material.diffuseTexture.forEach((textureName, textures) -> textures.loadTexture(textureManager));
        }
    }

    /**
     * Uploads all textures used by this material, avoiding any duplicated textures shared between different materials <br>
     * It creates all gl texture ids, so it should be called in gl thread
     */
    public void uploadTextures() {
        TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
        for (Material material : materials) {
            material.ambientTexture.forEach((textureName, textures) -> textures.uploadTexture(textureManager));
            material.diffuseTexture.forEach((textureName, textures) -> textures.uploadTexture(textureManager));
        }
    }
}
