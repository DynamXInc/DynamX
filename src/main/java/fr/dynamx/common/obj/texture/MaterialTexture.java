package fr.dynamx.common.obj.texture;

import fr.aym.acslib.impl.services.thrload.ThreadedTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

public class MaterialTexture
{
    private ResourceLocation path;
    private String name;
    private int id;

    public MaterialTexture(ResourceLocation path, String name, int id) {
        this.path = path;
        this.name = name;
        this.id = id;
    }

    public void loadTexture(TextureManager man){
        ITextureObject obj = man.getTexture(path);
        if (obj == null) {
            obj = new ThreadedTexture(path);
            man.loadTexture(path, obj);
        }
    }

    public void uploadTexture(TextureManager man){
        ITextureObject obj = man.getTexture(path);
        if(obj instanceof ThreadedTexture)
            ((ThreadedTexture) obj).uploadTexture(man);
        id = obj.getGlTextureId();
    }

    public ResourceLocation getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public int getGlTextureId() {
        return id;
    }

    @Override
    public String toString() {
        return "MaterialTexture{" +
                "path=" + path +
                ", name='" + name + '\'' +
                ", id=" + id +
                '}';
    }
}
