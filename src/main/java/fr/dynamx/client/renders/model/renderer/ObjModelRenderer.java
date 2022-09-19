package fr.dynamx.client.renders.model.renderer;

import fr.aym.acslib.api.services.ErrorTrackingService;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.events.DynamXModelRenderEvent;
import fr.dynamx.api.events.EventStage;
import fr.dynamx.api.obj.IModelTextureVariantsSupplier;
import fr.dynamx.api.obj.ObjModelPath;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.common.objloader.data.ObjModelData;
import fr.dynamx.common.objloader.data.ObjObjectData;
import fr.dynamx.client.renders.model.texture.TextureVariantData;
import fr.dynamx.utils.DynamXLoadingTasks;
import fr.dynamx.utils.errors.DynamXErrorManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import javax.vecmath.Vector4f;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static fr.dynamx.common.DynamXMain.log;

/**
 * A render-able obj model
 *
 * @see ObjModelData
 */
public class ObjModelRenderer {
    @Getter
    private final ResourceLocation location;
    @Getter
    private final List<ObjObjectRenderer> objObjects;
    /**
     * Used for error logging, see {@link ObjObjectData}
     */
    public boolean hasNoneMaterials;
    @Getter
    private final IModelTextureVariantsSupplier textureVariants;
    @Getter
    @Setter
    private Vector4f modelColor = new Vector4f(1,1,1,1);

    public ObjModelRenderer(ResourceLocation location, List<ObjObjectRenderer> objObjects, @Nullable IModelTextureVariantsSupplier textureVariants) {
        this.location = location;
        this.objObjects = objObjects;
        this.textureVariants = textureVariants;
    }

    public static ObjModelRenderer loadObjModel(ObjModelPath objModelPath, @Nullable IModelTextureVariantsSupplier textureVariants) {
        try {
            List<ObjObjectRenderer> objObjects = new ArrayList<>();

            ObjModelData objModelData = DynamXContext.getObjModelDataFromCache(objModelPath);
            objModelData.getObjObjects().forEach(ObjObjectData -> {
                objObjects.add(new ObjObjectRenderer(ObjObjectData));
            });
            return new ObjModelRenderer(objModelPath.getModelPath(), objObjects, textureVariants);
        } catch (Exception e) {
            DynamXErrorManager.addError(textureVariants != null ? textureVariants.getPackName() : "Non-pack model", DynamXErrorManager.MODEL_ERRORS, "obj_error", ErrorLevel.HIGH, objModelPath.toString(), "", e);
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
                if (object.getObjObjectData().getMesh().materialForEachVertex.length == 0) continue;
                if (getTextureVariants() != null) {
                    Map<Byte, TextureVariantData> textureVariants = this.getTextureVariants().getTextureVariantsFor(object);
                    if (textureVariants != null) {
                        TextureVariantData defaultTexture = textureVariants.get((byte) 0);
                        boolean log = object.getObjObjectData().getName().equalsIgnoreCase("chassis");
                        textureVariants.values().forEach(data -> object.createList(defaultTexture, data, this, log));
                        continue;
                    }
                }
                object.createDefaultList(this);
            }
        } catch (Exception e) {
            DynamXErrorManager.addError(textureVariants != null ? textureVariants.getPackName() : "Non-pack model", DynamXErrorManager.MODEL_ERRORS, "obj_error", ErrorLevel.HIGH, getLocation().toString(), (step == null ? null : step.getObjObjectData().getName()), e);
        }
    }

    /**
     * Called to render this group with displayList <br>
     * Will draw nothing if the model is not correctly loaded
     */
    public void renderGroup(ObjObjectRenderer obj, byte textureDataId) {
        if (!MinecraftForge.EVENT_BUS.post(new DynamXModelRenderEvent.RenderPart(EventStage.PRE, this, getTextureVariants(), textureDataId, obj)) && !obj.getObjObjectData().getName().equals("main")) {
            obj.render(this, textureDataId);
            MinecraftForge.EVENT_BUS.post(new DynamXModelRenderEvent.RenderPart(EventStage.POST, this, getTextureVariants(), textureDataId, obj));
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
        if (getTextureVariants() == null)
            throw new IllegalStateException("Cannot determine the parts to render !");
        if (!MinecraftForge.EVENT_BUS.post(new DynamXModelRenderEvent.RenderMainParts(EventStage.PRE, this, getTextureVariants(), textureDataId))) {
            boolean drawn = false;
            for (ObjObjectRenderer object : objObjects) {
                if (getTextureVariants().canRenderPart(object.getObjObjectData().getName())) {
                    renderGroup(object, textureDataId);
                    drawn = true;
                }
            }
            MinecraftForge.EVENT_BUS.post(new DynamXModelRenderEvent.RenderMainParts(EventStage.POST, this, getTextureVariants(), textureDataId));
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
        if (!MinecraftForge.EVENT_BUS.post(new DynamXModelRenderEvent.RenderFullModel(EventStage.PRE, this, getTextureVariants(), textureDataId))) {
            objObjects.forEach(object -> {
                object.setObjectColor(modelColor);
                renderGroup(object, textureDataId);
            });
            MinecraftForge.EVENT_BUS.post(new DynamXModelRenderEvent.RenderFullModel(EventStage.POST, this, getTextureVariants(), textureDataId));
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

        setModelColor(new Vector4f(canPlace ? 0 : 1, canPlace ? 1 : 0, 0, 0.7f));
        renderModel();
        GlStateManager.enableBlend();
        GlStateManager.popMatrix();
    }

    public ObjObjectRenderer getObjObjectRenderer(String groupName) {
        return objObjects.stream().filter(o -> o.getObjObjectData().getName().equalsIgnoreCase(groupName)).findFirst().orElse(null);
    }
}
