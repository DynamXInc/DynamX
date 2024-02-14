package fr.dynamx.client.renders.model.renderer;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.dxmodel.DxModelPath;
import fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier;
import fr.dynamx.api.events.DynamXModelRenderEvent;
import fr.dynamx.api.events.EventStage;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.objloader.data.Material;
import fr.dynamx.common.objloader.data.ObjModelData;
import fr.dynamx.common.objloader.data.ObjObjectData;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.errors.DynamXErrorManager;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A render-able obj model
 *
 * @see ObjModelData
 */
public class ObjModelRenderer extends DxModelRenderer {

    @Getter
    private final List<ObjObjectRenderer> objObjects;
    @Getter
    private final Map<String, Material> materials;
    /**
     * Used for error logging, see {@link ObjObjectData}
     */
    public boolean hasNoneMaterials;

    protected ObjModelRenderer(DxModelPath location, List<ObjObjectRenderer> objObjects, Map<String, Material> materials, @Nullable IModelTextureVariantsSupplier textureVariants) {
        super(location, textureVariants);
        this.objObjects = objObjects;
        this.materials = materials;

        // Load variants
        hasNoneMaterials = false;
        ObjObjectRenderer loadingObject = null;
        try {
            for (ObjObjectRenderer object : objObjects) {
                loadingObject = object;
                object.clearVAO();
                if (object.getObjObjectData().getMaterials().isEmpty() || getTextureVariants() == null)
                    continue;
                IModelTextureVariantsSupplier.IModelTextureVariants variants = this.getTextureVariants().getTextureVariantsFor(object);
                if (variants != null)
                    object.setTextureVariants(this, variants);
            }
        } catch (Exception e) {
            DynamXErrorManager.addError(textureVariants != null ? textureVariants.getPackName() : "Non-pack model", DynamXErrorManager.MODEL_ERRORS, "obj_error", ErrorLevel.HIGH, getLocation().getModelPath().toString(), (loadingObject == null ? null : loadingObject.getObjObjectData().getName()), e);
        }
    }

    public static ObjModelRenderer loadObjModel(DxModelPath objModelPath, @Nullable IModelTextureVariantsSupplier textureVariants) {
        try {
            List<ObjObjectRenderer> objObjects = new ArrayList<>();
            ObjModelData objModelData = (ObjModelData) DynamXContext.getDxModelDataFromCache(objModelPath);
            objModelData.getObjObjects().forEach(ObjObjectData -> {
                objObjects.add(new ObjObjectRenderer(ObjObjectData));
            });
            return new ObjModelRenderer(objModelPath, objObjects, objModelData.getMaterials(), textureVariants);
        } catch (Exception e) {
            DynamXErrorManager.addError(textureVariants != null ? textureVariants.getPackName() : "Non-pack model", DynamXErrorManager.MODEL_ERRORS, "obj_error", ErrorLevel.HIGH, objModelPath.getModelPath().toString(), "", e);
        }
        return null;
    }

    @Override
    public void uploadVAOs() {
        objObjects.forEach(ObjObjectRenderer::uploadVAO);
    }

    @Override
    public void clearVAOs() {
        objObjects.forEach(ObjObjectRenderer::clearVAO);
    }

    /**
     * Called to render this group with displayList <br>
     * Will draw nothing if the model is not correctly loaded
     */
    public void renderGroup(ObjObjectRenderer obj, byte textureDataId) {
        DynamXRenderUtils.popGlAllAttribBits();
        if (!MinecraftForge.EVENT_BUS.post(new DynamXModelRenderEvent.RenderPart(EventStage.PRE, this, getTextureVariants(), textureDataId, obj)) && !obj.getObjObjectData().getName().equals("main")) {
            obj.render(this, textureDataId);
            MinecraftForge.EVENT_BUS.post(new DynamXModelRenderEvent.RenderPart(EventStage.POST, this, getTextureVariants(), textureDataId, obj));
        }
    }

    @Override
    public boolean renderGroup(String group, byte textureDataId, boolean forceVanillaRender) {
        ObjObjectRenderer objObjectRenderer = getObjObjectRenderer(group);
        if (objObjectRenderer == null)
            return false;
        renderGroup(objObjectRenderer, textureDataId);
        return true;
    }

    public boolean renderDefaultParts(byte textureDataId, boolean forceVanillaRender) {
        if (getTextureVariants() == null)
            throw new IllegalStateException("Cannot determine the parts to render !");
        if (!MinecraftForge.EVENT_BUS.post(new DynamXModelRenderEvent.RenderMainParts(EventStage.PRE, this, getTextureVariants(), textureDataId))) {
            boolean drawn = false;
            for (ObjObjectRenderer object : objObjects) {
                if (textureVariants.canRenderPart(object.getObjObjectData().getName())) {
                    renderGroup(object, textureDataId);
                    drawn = true;
                }
            }
            MinecraftForge.EVENT_BUS.post(new DynamXModelRenderEvent.RenderMainParts(EventStage.POST, this, textureVariants, textureDataId));
            return drawn;
        }
        return true;
    }

    public void renderModel(byte textureDataId, boolean forceVanillaRender) {
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

    public ObjObjectRenderer getObjObjectRenderer(String groupName) {
        return objObjects.stream().filter(o -> o.getObjObjectData().getName().equalsIgnoreCase(groupName)).findFirst().orElse(null);
    }

    @Override
    public boolean containsObjectOrNode(String name) {
        return objObjects.stream().anyMatch(o -> o.getObjObjectData().getName().equalsIgnoreCase(name));
    }

    @Override
    public boolean isEmpty() {
        return objObjects.isEmpty();
    }
}
