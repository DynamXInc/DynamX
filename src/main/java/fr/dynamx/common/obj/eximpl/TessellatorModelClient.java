package fr.dynamx.common.obj.eximpl;

import fr.aym.acslib.api.services.ErrorTrackingService;
import fr.dynamx.api.events.DynamXBlockEvent;
import fr.dynamx.api.events.DynamXRenderEvent;
import fr.dynamx.api.events.EventStage;
import fr.dynamx.api.obj.IModelTextureSupplier;
import fr.dynamx.api.obj.IObjObject;
import fr.dynamx.client.renders.model.ObjModelClient;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.obj.texture.TextureData;
import fr.dynamx.utils.DynamXLoadingTasks;
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

import static fr.dynamx.common.DynamXMain.log;

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
        String content;
        String startPath;

        try {
            IResource res = Minecraft.getMinecraft().getResourceManager().getResource(location);
            content = new String(DynamXUtils.readInputStream(res.getInputStream()), StandardCharsets.UTF_8);
            startPath = location.getPath().substring(0, location.getPath().lastIndexOf("/") + 1);
            List<IObjObject> objObjects = new ArrayList<>();
            new OBJLoader(objObjects).loadModelClient(QuickObjObject::new, startPath, content);
            return new TessellatorModelClient(location, objObjects, customTextures);
        } catch (Exception e) {
            log.error(" Model " + location + " cannot be loaded !", e);
            DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.MODEL,
                    customTextures != null ? customTextures.getPackName() : "Non-pack model",
                    "Model " + location + " cannot be loaded !", e, ErrorTrackingService.TrackedErrorLevel.HIGH);
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
        if (!objObjects.isEmpty()) { //No error while loading the model
            hasNoneMaterials = false;
            //System.out.println("Custom texs of " + getLocation() + " are " + customTextures);
            IObjObject step = null;
            try {
                for (IObjObject object : objObjects) {
                    step = object;
                    object.clearDisplayLists();
                    if (object.getMesh().materials.length > 0) {
                        if (getCustomTextures() != null) {
                            Map<Byte, TextureData> customTextures = this.getCustomTextures().getTexturesFor(object);
                            if (customTextures != null) {
                                TextureData defaultTexture = customTextures.get((byte) 0);
                                boolean log = object.getName().equalsIgnoreCase("chassis");
                                for (TextureData data : customTextures.values()) {
                                    object.createList(defaultTexture, data, this, log);
                                }
                            } else {
                                object.createDefaultList(this);
                            }
                        } else {
                            object.createDefaultList(this);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Cannot setup model " + getLocation() + " ! Step: "+(step == null ? null : step.getName()), e);
                DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.MODEL, customTextures != null ? customTextures.getPackName() : "Non-pack model", "Cannot setup model " + getLocation() + " ! Step: "+(step == null ? null : step.getName()), e, ErrorTrackingService.TrackedErrorLevel.HIGH);
            }
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
        if(!MinecraftForge.EVENT_BUS.post(new DynamXRenderEvent.RenderModel(EventStage.PRE, getLocation(), "main")))
        {
            for (IObjObject object : objObjects) {
                //System.out.println("DO render "+object.getName()+" "+textureDataId+" "+object.modelDisplayList+" "+location);
                renderGroup(object, textureDataId);
            }
        }
        MinecraftForge.EVENT_BUS.post(new DynamXRenderEvent.RenderModel(EventStage.POST, getLocation(), "main"));
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
        if(getCustomTextures() == null) {
            throw new IllegalStateException("Cannot determine the parts to render !");
        }
        boolean drawn = false;
        for (IObjObject object : objObjects) {
            if (getCustomTextures().canRenderPart(object.getName())) {
                renderGroup(object, textureDataId);
                drawn = true;
            }
        }
        return drawn;
    }

    @Override
    public void renderGroup(IObjObject obj, byte textureDataId) {
        if(!MinecraftForge.EVENT_BUS.post(new DynamXRenderEvent.RenderModel(EventStage.PRE, getLocation(), obj.getName())))
        {
            if (!obj.getName().equals("main")) {
                obj.render(this, textureDataId);
            }
        }
        MinecraftForge.EVENT_BUS.post(new DynamXRenderEvent.RenderModel(EventStage.POST, getLocation(), obj.getName()));
    }

    @Override
    public IObjObject getObjObject(String groupName) {
        for (IObjObject o : objObjects) {
            if (o.getName().equalsIgnoreCase(groupName))
                return o;
        }
        return null;
    }
}
