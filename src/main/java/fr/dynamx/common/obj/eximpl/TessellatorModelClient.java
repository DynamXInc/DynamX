package fr.dynamx.common.obj.eximpl;

import fr.aym.acslib.api.services.ErrorManagerService;
import fr.dynamx.api.events.DynamXModelRenderEvent;
import fr.dynamx.api.events.EventStage;
import fr.dynamx.api.obj.IModelTextureSupplier;
import fr.dynamx.api.obj.IObjObject;
import fr.dynamx.client.renders.model.ObjModelClient;
import fr.dynamx.common.obj.texture.TextureData;
import fr.dynamx.utils.errors.DynamXErrorManager;
import fr.dynamx.utils.DynamXUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author jglrxavpok, modified by Yanis and Aym for DynamX
 */
public class TessellatorModelClient extends ObjModelClient {
    private final IModelTextureSupplier customTextures;

    private TessellatorModelClient(ResourceLocation location, List<IObjObject> objObjects, @Nullable IModelTextureSupplier customTextures) {
        super(location, objObjects);
        this.customTextures = customTextures;
    }

    public static TessellatorModelClient loadObjModel(ResourceLocation location, @Nullable IModelTextureSupplier customTextures) {
        try {
            IResource res = Minecraft.getMinecraft().getResourceManager().getResource(location);
            String content = new String(DynamXUtils.readInputStream(res.getInputStream()), StandardCharsets.UTF_8);
            String startPath = location.getPath().substring(0, location.getPath().lastIndexOf("/") + 1);
            List<IObjObject> objObjects = new ArrayList<>();
            new OBJLoader(objObjects).loadModelClient(QuickObjObject::new, startPath, content);
            return new TessellatorModelClient(location, objObjects, customTextures);
        } catch (Exception e) {
            //log.error(" Model " + location + " cannot be loaded !", e);
            DynamXErrorManager.addError(customTextures != null ? customTextures.getPackName() : "Non-pack model", "obj_error", ErrorManagerService.ErrorLevel.HIGH, location.toString(), "", e);
        }
        return null;
    }

    @Nullable
    @Override
    public IModelTextureSupplier getCustomTextures() {
        return customTextures;
    }

    /**
     * Creates display list for each {@link IObjObject}
     */
    @Override
    public void setupModel() {
        if (objObjects.isEmpty()) return; // Error while loading the model
        hasNoneMaterials = false;
        IObjObject step = null;
        try {
            for (IObjObject object : objObjects) {
                step = object;
                object.clearDisplayLists();
                if (object.getMesh().materials.length == 0) continue;
                if (getCustomTextures() != null) {
                    Map<Byte, TextureData> customTextures = this.getCustomTextures().getTexturesFor(object);
                    if (customTextures != null) {
                        TextureData defaultTexture = customTextures.get((byte) 0);
                        boolean log = object.getName().equalsIgnoreCase("chassis");
                        customTextures.values().forEach(data -> object.createList(defaultTexture, data, this, log));
                        continue;
                    }
                }
                object.createDefaultList(this);
            }
        } catch (Exception e) {
            //log.error("Cannot setup model " + getLocation() + " ! Step: " + (step == null ? null : step.getName()), e);
            DynamXErrorManager.addError(customTextures != null ? customTextures.getPackName() : "Non-pack model", "obj_error", ErrorManagerService.ErrorLevel.HIGH, getLocation().toString(), (step == null ? null : step.getName()), e);
        }
    }

    @Override
    public void renderModel(byte textureDataId) {
        objObjects.sort((a, b) -> {
            Vec3d v = Minecraft.getMinecraft().getRenderViewEntity() != null ? Minecraft.getMinecraft().getRenderViewEntity().getPositionVector() : new Vec3d(0, 0, 0);
            double aDist = v.distanceTo(new Vec3d(a.getCenter().x, a.getCenter().y, a.getCenter().z));
            double bDist = v.distanceTo(new Vec3d(b.getCenter().x, b.getCenter().y, b.getCenter().z));
            return Double.compare(aDist, bDist);
        });
        if (!MinecraftForge.EVENT_BUS.post(new DynamXModelRenderEvent.RenderFullModel(EventStage.PRE, this, getCustomTextures(), textureDataId))) {
            objObjects.forEach(object -> renderGroup(object, textureDataId));
            MinecraftForge.EVENT_BUS.post(new DynamXModelRenderEvent.RenderFullModel(EventStage.POST, this, getCustomTextures(), textureDataId));
        }
    }

    @Override
    public boolean renderGroups(String group, byte textureDataId) {
        boolean drawn = false;
        for (IObjObject object : objObjects) {
            if (object.getName().equalsIgnoreCase(group)) {
                renderGroup(object, textureDataId);
                drawn = true;
            }
        }
        return drawn;
    }

    @Override
    public boolean renderMainParts(byte textureDataId) {
        if (getCustomTextures() == null)
            throw new IllegalStateException("Cannot determine the parts to render !");
        if (!MinecraftForge.EVENT_BUS.post(new DynamXModelRenderEvent.RenderMainParts(EventStage.PRE, this, getCustomTextures(), textureDataId))) {
            boolean drawn = false;
            for (IObjObject object : objObjects) {
                if (getCustomTextures().canRenderPart(object.getName())) {
                    renderGroup(object, textureDataId);
                    drawn = true;
                }
            }
            MinecraftForge.EVENT_BUS.post(new DynamXModelRenderEvent.RenderMainParts(EventStage.POST, this, getCustomTextures(), textureDataId));
            return drawn;
        }
        return true;
    }

    @Override
    public void renderGroup(IObjObject obj, byte textureDataId) {
        if (!MinecraftForge.EVENT_BUS.post(new DynamXModelRenderEvent.RenderPart(EventStage.PRE, this, getCustomTextures(), textureDataId, obj)) && !obj.getName().equals("main")) {
            obj.render(this, textureDataId);
            MinecraftForge.EVENT_BUS.post(new DynamXModelRenderEvent.RenderPart(EventStage.POST, this, getCustomTextures(), textureDataId, obj));
        }
    }

    @Override
    public IObjObject getObjObject(String groupName) {
        return objObjects.stream().filter(o -> o.getName().equalsIgnoreCase(groupName)).findFirst().orElse(null);
    }
}
