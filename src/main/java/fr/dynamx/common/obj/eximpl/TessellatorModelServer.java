package fr.dynamx.common.obj.eximpl;

import fr.aym.mps.IMpsClassLoader;
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
            InputStream protectedd = mpsClassLoader.getResourceAsStream("assets/" + path.getModelPath().getNamespace() + "/" + path.getModelPath().getPath());
            //System.out.println("Search " + "assets/" + path.getModelPath().getNamespace() + "/" + path.getModelPath().getPath() + " " + path + " : " + protectedd);
            if (protectedd != null) {
                return protectedd;
            }
        }
        if(path.isBuiltinModel()) {
            System.out.println("Builtin model " + path);
            String entry = "/assets/" + path.getModelPath().getNamespace() + "/" + path.getModelPath().getPath();
            System.out.println("entry "+entry+" "+getClass().getResourceAsStream(entry));
            return getClass().getResourceAsStream(entry);
        }
        if (path.getPackName().contains(".zip") || path.getPackName().contains(ContentPackLoader.PACK_FILE_EXTENSION)) {
            ZipFile root = new ZipFile(DynamXMain.resDir + File.separator + path.getPackName());
            //System.out.println("Root is "+root);
            String entry = "assets/" + path.getModelPath().getNamespace() + "/" + path.getModelPath().getPath();
            //System.out.println("Entry path is "+entry+" "+path);
            ZipEntry t = root.getEntry(entry);
            if (t == null) {
                throw new FileNotFoundException("Not found in zip : " + path + ". Has mps class loader : " + (mpsClassLoader != null));
            }
            return root.getInputStream(t);
        }
        String fullPath = DynamXMain.resDir + File.separator + path.getPackName() + File.separator + "assets" +
                File.separator + path.getModelPath().getNamespace() + File.separator + path.getModelPath().getPath().replace("/", File.separator);
        //System.out.println("Full path is "+fullPath+" "+path);
        return new FileInputStream(fullPath);
    }
}
