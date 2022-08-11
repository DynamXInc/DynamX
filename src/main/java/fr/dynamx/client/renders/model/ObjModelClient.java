package fr.dynamx.client.renders.model;

import fr.aym.acslib.api.services.ErrorTrackingService;
import fr.dynamx.api.events.DynamXModelRenderEvent;
import fr.dynamx.api.events.EventStage;
import fr.dynamx.api.obj.IModelTextureSupplier;
import fr.dynamx.api.obj.ObjModelPath;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.common.objloader.ObjModelData;
import fr.dynamx.common.objloader.ObjObjectData;
import fr.dynamx.client.renders.model.texture.TextureData;
import fr.dynamx.utils.DynamXLoadingTasks;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static fr.dynamx.common.DynamXMain.log;

/**
 * A render-able obj model
 *
 * @see ObjModelData
 */
public class ObjModelClient {
    @Getter
    private final ResourceLocation location;
    @Getter
    private final List<ObjObjectRenderer> objObjects;
    /**
     * Used for error logging, see {@link ObjObjectData}
     */
    public boolean hasNoneMaterials;
    @Getter
    private final IModelTextureSupplier customTextures;

    public ObjModelClient(ResourceLocation location, List<ObjObjectRenderer> objObjects, @Nullable IModelTextureSupplier customTextures) {
        this.location = location;
        this.objObjects = objObjects;
        this.customTextures = customTextures;
    }

    public static ObjModelClient loadObjModel(ObjModelPath objModelPath, @Nullable IModelTextureSupplier customTextures) {
        try {
            List<ObjObjectRenderer> objObjects = new ArrayList<>();

            ObjModelData objModelData = DynamXContext.getObjModelDataFromCache(objModelPath);
            objModelData.getObjObjects().forEach(ObjObjectData -> {
                objObjects.add(new ObjObjectRenderer(ObjObjectData));
            });
            return new ObjModelClient(objModelPath.getModelPath(), objObjects, customTextures);
        } catch (Exception e) {
            log.error(" Model " + objModelPath.getModelPath() + " cannot be loaded !", e);
            DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.MODEL,
                    customTextures != null ? customTextures.getPackName() : "Non-pack model",
                    "Model " + objModelPath.getModelPath() + " cannot be loaded !", e, ErrorTrackingService.TrackedErrorLevel.HIGH);
        }
        return null;
    }

    /**
     * Creates display list for each {@link ObjObjectData}
     */
    public void setupModel() {
        if (objObjects.isEmpty()) return; // Error while loading the model
        hasNoneMaterials = false;
        ObjObjectRenderer step = null;
        try {
            for (ObjObjectRenderer object : objObjects) {
                step = object;
                object.clearDisplayLists();
                if (object.getObjObjectData().getMesh().materials.length == 0) continue;
                if (getCustomTextures() != null) {
                    Map<Byte, TextureData> customTextures = this.getCustomTextures().getTexturesFor(object);
                    if (customTextures != null) {
                        TextureData defaultTexture = customTextures.get((byte) 0);
                        boolean log = object.getObjObjectData().getName().equalsIgnoreCase("chassis");
                        customTextures.values().forEach(data -> object.createList(defaultTexture, data, this, log));
                        continue;
                    }
                }
                object.createDefaultList(this);
            }
        } catch (Exception e) {
            log.error("Cannot setup model " + getLocation() + " ! Step: " + (step == null ? null : step.getObjObjectData().getName()), e);
            DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.MODEL, customTextures != null ? customTextures.getPackName() : "Non-pack model", "Cannot setup model " + getLocation() + " ! Step: " + (step == null ? null : step.getObjObjectData().getName()), e, ErrorTrackingService.TrackedErrorLevel.HIGH);
        }
    }

    /**
     * Called to render this group with displayList <br>
     * Will draw nothing if the model is not correctly loaded
     */
    public void renderGroup(ObjObjectRenderer obj, byte textureDataId) {
        if (!MinecraftForge.EVENT_BUS.post(new DynamXModelRenderEvent.RenderPart(EventStage.PRE, this, getCustomTextures(), textureDataId, obj)) && !obj.getObjObjectData().getName().equals("main")) {
            obj.render(this, textureDataId);
            MinecraftForge.EVENT_BUS.post(new DynamXModelRenderEvent.RenderPart(EventStage.POST, this, getCustomTextures(), textureDataId, obj));
        }
    }

    public void renderGroup(ObjObjectRenderer obj) {
        renderGroup(obj, (byte) 0);
    }

    /**
     * Called to render this part <br>
     * Will draw nothing if the model is not correctly loaded
     *
     * @return True if successfully drawn something
     */
    public boolean renderGroups(String group, byte textureDataId) {
        boolean drawn = false;
        for (ObjObjectRenderer object : objObjects) {
            if (object.getObjObjectData().getName().equalsIgnoreCase(group)) {
                renderGroup(object, textureDataId);
                drawn = true;
            }
        }
        return drawn;
    }

    public boolean renderDefaultParts(byte textureDataId) {
        if (getCustomTextures() == null)
            throw new IllegalStateException("Cannot determine the parts to render !");
        if (!MinecraftForge.EVENT_BUS.post(new DynamXModelRenderEvent.RenderMainParts(EventStage.PRE, this, getCustomTextures(), textureDataId))) {
            boolean drawn = false;
            for (ObjObjectRenderer object : objObjects) {
                if (getCustomTextures().canRenderPart(object.getObjObjectData().getName())) {
                    renderGroup(object, textureDataId);
                    drawn = true;
                }
            }
            MinecraftForge.EVENT_BUS.post(new DynamXModelRenderEvent.RenderMainParts(EventStage.POST, this, getCustomTextures(), textureDataId));
            return drawn;
        }
        return true;
    }

    /**
     * Called to render this model with default texture <br>
     * Will draw nothing if the model is not correctly loaded
     */
    public void renderModel() {
        renderModel((byte) 0);
    }

    /**
     * Called to render this model <br>
     * Will draw nothing if the model is not correctly loaded
     */
    public void renderModel(byte textureDataId) {
        objObjects.sort((a, b) -> {
            Vec3d v = Minecraft.getMinecraft().getRenderViewEntity() != null ? Minecraft.getMinecraft().getRenderViewEntity().getPositionVector() : new Vec3d(0, 0, 0);
            double aDist = v.distanceTo(new Vec3d(a.getObjObjectData().getCenter().x, a.getObjObjectData().getCenter().y, a.getObjObjectData().getCenter().z));
            double bDist = v.distanceTo(new Vec3d(b.getObjObjectData().getCenter().x, b.getObjObjectData().getCenter().y, b.getObjObjectData().getCenter().z));
            return Double.compare(aDist, bDist);
        });
        if (!MinecraftForge.EVENT_BUS.post(new DynamXModelRenderEvent.RenderFullModel(EventStage.PRE, this, getCustomTextures(), textureDataId))) {
            objObjects.forEach(object -> renderGroup(object, textureDataId));
            MinecraftForge.EVENT_BUS.post(new DynamXModelRenderEvent.RenderFullModel(EventStage.POST, this, getCustomTextures(), textureDataId));
        }
    }


    public void renderPreview(BlockObject<?> blockObjectInfo, EntityPlayer player, BlockPos blockPos, boolean canPlace, float orientation, float partialTicks) {
        double px = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double py = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
        GlStateManager.pushMatrix();
        GlStateManager.translate(-px, -py, -pz);
        GlStateManager.translate(blockPos.getX() + 0.5 + blockObjectInfo.getTranslation().x,
                blockPos.getY() + 1.5 + blockObjectInfo.getTranslation().y,
                blockPos.getZ() + 0.5 + blockObjectInfo.getTranslation().z);
        //GlStateManager.translate(blockInfo.translate[0], blockInfo.translate[1], blockInfo.translate[2]);
        GlStateManager.scale(blockObjectInfo.getScaleModifier().x, blockObjectInfo.getScaleModifier().y, blockObjectInfo.getScaleModifier().z);
        GlStateManager.rotate(orientation * 22.5f, 0.0F, -1.0F, 0.0F);
        if (blockObjectInfo.getRotation().x != 0)
            GlStateManager.rotate(blockObjectInfo.getRotation().x, 1, 0, 0);
        if (blockObjectInfo.getRotation().y != 0)
            GlStateManager.rotate(blockObjectInfo.getRotation().y, 0, 1, 0);
        if (blockObjectInfo.getRotation().z != 0)
            GlStateManager.rotate(blockObjectInfo.getRotation().z, 0, 0, 1);

        GlStateManager.disableBlend();
        GlStateManager.color(canPlace ? 0 : 1, canPlace ? 1 : 0, 0, 1);
        renderModel();
        GlStateManager.enableBlend();
        GlStateManager.popMatrix();
    }

    public ObjObjectRenderer getObjObjectRenderer(String groupName) {
        return objObjects.stream().filter(o -> o.getObjObjectData().getName().equalsIgnoreCase(groupName)).findFirst().orElse(null);
    }
}
