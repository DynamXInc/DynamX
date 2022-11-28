package fr.dynamx.common.obj.eximpl;

import fr.dynamx.api.obj.ObjModelPath;
import fr.dynamx.common.contentpack.PackInfo;
import fr.dynamx.common.obj.ObjModelServer;
import fr.dynamx.common.obj.SimpleObjObject;
import fr.dynamx.utils.DynamXUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class TessellatorModelServer extends ObjModelServer {
    public TessellatorModelServer(ObjModelPath path) {
        super(path);
        try {
            String content = new String(DynamXUtils.readInputStream(FMLCommonHandler.instance().getSide().isClient() ? client(path) : server(path)), StandardCharsets.UTF_8);
            new OBJLoader(objObjects).loadModelServer(SimpleObjObject::new, content);
        } catch (Exception e) {
            //Don't remove the throw - Aym
            throw new RuntimeException(path + " cannot be loaded !", e);
        }
    }

    @SideOnly(Side.CLIENT)
    private InputStream client(ObjModelPath path) throws IOException {
        IResource res = Minecraft.getMinecraft().getResourceManager().getResource(path.getModelPath());
        return res.getInputStream();
    }

    private InputStream server(ObjModelPath path) throws IOException {
        InputStream result = null;
        for (PackInfo packInfo : path.getPackLocations()) {
            result = packInfo.readFile(path.getModelPath());
            if (result != null)
                break;
        }
        if (result == null)
            throw new FileNotFoundException("Model not found : " + path + ". Pack : " + path.getPackName());
        return result;
    }
}
