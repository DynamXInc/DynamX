package fr.dynamx.client.renders.model.texture;

import fr.aym.acslib.impl.services.thrload.ThreadedTexture;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.objloader.data.Material;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

@RequiredArgsConstructor
@ToString
public class MaterialTexture {
    @Getter
    private final ResourceLocation path;
    @Getter
    private final String textureVariantName;
    @Getter
    private int glTextureId;

    public void loadTexture(Material material, TextureManager man) {
        ITextureObject obj = man.getTexture(path);
        if (obj == null) {
            //obj = DynamXContext.isOptifineLoaded() ? new OptifineTextureMat(material, path, textureVariantName) : new ThreadedTexture(path);
            obj = new ThreadedTexture(path);
            man.loadTexture(path, obj);
        }
    }

    public void uploadTexture(TextureManager man) {
        ITextureObject obj = man.getTexture(path);
        if (obj instanceof ThreadedTexture)
            ((ThreadedTexture) obj).uploadTexture(man);
        glTextureId = obj.getGlTextureId();
    }
}
