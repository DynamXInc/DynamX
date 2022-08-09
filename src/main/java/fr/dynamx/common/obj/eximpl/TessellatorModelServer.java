package fr.dynamx.common.obj.eximpl;

import fr.aym.mps.IMpsClassLoader;
import fr.dynamx.api.contentpack.ContentPackType;
import fr.dynamx.api.obj.ObjModelPath;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.obj.ObjModelServer;
import fr.dynamx.common.obj.SimpleObjObject;
import fr.dynamx.utils.DynamXUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class TessellatorModelServer extends ObjModelServer {
    private final IMpsClassLoader mpsClassLoader;

    public TessellatorModelServer(IMpsClassLoader mpsClassLoader, ObjModelPath path) {
        super(path);
        this.mpsClassLoader = mpsClassLoader;
        try {
            String content = new String(DynamXUtils.readInputStream(FMLCommonHandler.instance().getSide().isClient() ? client(path) : server(path)), StandardCharsets.UTF_8);
            new OBJLoader(objObjects).loadModelServer(SimpleObjObject::new, content);
        } catch (Exception e) {
            //Don't remove the throw - Aym
            throw new RuntimeException("Model " + path + " cannot be loaded ! Has secure loader: " + (mpsClassLoader != null), e);
        }
    }

    @SideOnly(Side.CLIENT)
    private InputStream client(ObjModelPath path) throws IOException {
        IResource res = Minecraft.getMinecraft().getResourceManager().getResource(path.getModelPath());
        return res.getInputStream();
    }

    private InputStream server(ObjModelPath path) throws IOException {
        if (mpsClassLoader != null) {
            InputStream protectedIs = mpsClassLoader.getResourceAsStream("assets/" + path.getModelPath().getNamespace() + "/" + path.getModelPath().getPath());
            if (protectedIs != null)
                return protectedIs;
        }
        InputStream result = null;
        switch (path.getPackInfo().getPackType()) {
            case FOLDER:
                String fullPath = DynamXMain.resDir + File.separator + path.getPackName() + File.separator + "assets" +
                    File.separator + path.getModelPath().getNamespace() + File.separator + path.getModelPath().getPath().replace("/", File.separator);
                result = Files.newInputStream(Paths.get(fullPath));
                break;
            case DNXPACK:
            case ZIP:
                ZipFile root = new ZipFile(DynamXMain.resDir + File.separator + path.getPackName());
                String entry = "assets/" + path.getModelPath().getNamespace() + "/" + path.getModelPath().getPath();
                result = root.getInputStream(root.getEntry(entry));
                break;
            case BUILTIN:
                System.out.println("Builtin model " + path);
                entry = "/assets/" + path.getModelPath().getNamespace() + "/" + path.getModelPath().getPath();
                System.out.println("entry " + entry + " " + ContentPackType.class.getResourceAsStream(entry));
                result = ContentPackType.class.getResourceAsStream(entry);
                break;
        }
        if (result == null)
            throw new FileNotFoundException("Model not found : " + path + ". Pack : " + path.getPackInfo() + ". Has mps class loader : " + (mpsClassLoader != null));
        return result;
    }
}
