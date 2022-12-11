package fr.dynamx.client.renders.model.texture;

import fr.aym.acslib.impl.services.thrload.ThreadedTexture;
import fr.dynamx.common.objloader.data.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
//import net.optifine.shaders.MultiTexID;

public class OptifineTextureMat extends ThreadedTexture {
    private final Material material;
    private final String textureVariantName;

    public OptifineTextureMat(Material material, ResourceLocation textureResourceLocation, String textureVariantName) {
        super(textureResourceLocation);
        this.material = material;
        this.textureVariantName = textureVariantName;
    }

    //Do not rename, optifine needs to call this function
    /*public MultiTexID getMultiTexID() {
        MultiTexID multiTexID = material.multiTexID;
        multiTexID.base = material.diffuseTexture.get(textureVariantName).getGlTextureId();
        MaterialTexture normal = material.normalTexture.get(textureVariantName);
        if (normal != null) {
            multiTexID.norm = normal.getGlTextureId();
        }
        MaterialTexture specular = material.specularTexture.get(textureVariantName);
        if (specular != null) {
            multiTexID.spec = specular.getGlTextureId();
        }
        return multiTexID;
    }*/

    @Override
    public int getGlTextureId() {
        TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
        if (imageData != null)
            material.diffuseTexture.get(textureVariantName).uploadTexture(textureManager);
        return super.getGlTextureId();
    }
}
