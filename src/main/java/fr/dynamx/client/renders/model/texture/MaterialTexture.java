package fr.dynamx.client.renders.model.texture;

import fr.aym.acslib.impl.services.thrload.ThreadedTexture;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

@AllArgsConstructor
@ToString
public class MaterialTexture {
    @Getter
    private final ResourceLocation path;
    @Getter
    private final String name;
    @Getter
    private int glTextureId;

    public void loadTexture(TextureManager man) {
        ITextureObject obj = man.getTexture(path);
        if (obj == null) {
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
