package fr.dynamx.common.obj.eximpl;

import fr.dynamx.common.obj.Material;
import fr.dynamx.common.obj.texture.MaterialTexture;
import fr.dynamx.utils.RegistryNameSetter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class MtlMaterialLib
{
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

    private final ArrayList<Material> materials = new ArrayList<>();

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
                    //current.diffuseTex = parts[1].contains(File.separator+File.separator) ? startPath + parts[1].replace(File.separator+File.separator,
                    // File.separator) : startPath + parts[1];
                    String name = parts.length >= 3 ? parts[2] : "Default";
                    current.diffuseTexture.put(name,
                            new MaterialTexture(RegistryNameSetter.getResourceLocationWithDynamXDefault(startPath + parts[1]), name, -1));
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
     *     It only read images files, so it can be called in any thread
     */
    public void loadTextures()
    {
        TextureManager man = Minecraft.getMinecraft().getTextureManager();
        //System.out.println("LOAD man is "+man);
        for(Material mat : materials)
        {
            if(mat.ambientTexture != null) {
                mat.ambientTexture.forEach((textureName, textures) -> textures.loadTexture(man));
            }
            if(mat.diffuseTexture != null) {
                mat.diffuseTexture.forEach((textureName, textures) -> textures.loadTexture(man));
            }
        }
    }

    /**
     * Uploads all textures used by this material, avoiding any duplicated textures shared between different materials <br>
     *     It creates all gl texture ids, so it should be called in gl thread
     */
    public void uploadTextures()
    {
        TextureManager man = Minecraft.getMinecraft().getTextureManager();
        /*System.out.printf("UPLOAD man is "+man);
        if(man == null)
            throw new NullPointerException("Mc Texture Manager not loaded !");*/
        for(Material mat : materials)
        {
            if(mat.ambientTexture != null) {
                mat.ambientTexture.forEach((textureName, textures) -> textures.uploadTexture(man));
            }
            if(mat.diffuseTexture != null) {
                mat.diffuseTexture.forEach((textureName, textures) -> textures.uploadTexture(man));
            }
        }
    }
}
