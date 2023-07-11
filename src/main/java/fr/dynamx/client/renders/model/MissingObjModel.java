package fr.dynamx.client.renders.model;

import fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier;
import fr.dynamx.api.dxmodel.DxModelPath;
import fr.dynamx.client.renders.model.renderer.ObjModelRenderer;
import fr.dynamx.client.renders.model.renderer.ObjObjectRenderer;
import fr.dynamx.common.contentpack.PackInfo;
import fr.dynamx.common.objloader.data.ObjObjectData;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;

import javax.vecmath.Vector3f;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Missing obj model indicating errors
 */
public class MissingObjModel extends ObjModelRenderer {
    public static final PackInfo DYNAMX_PACKINFO = PackInfo.forAddon(DynamXConstants.ID);

    private static final ObjObjectData emptyPart = new ObjObjectData("empty") {
        @Override
        public String getName() {
            return "empty";
        }
    };

    private static ObjObjectRenderer emptyPartRenderer;

    public MissingObjModel() {
        super(new DxModelPath(DYNAMX_PACKINFO, new ResourceLocation(DynamXConstants.ID, "obj/missing.obj")), new ArrayList<>(), new HashMap<>(), null);
        emptyPart.setCenter(new Vector3f());
        ObjObjectRenderer objObjectRenderer = new ObjObjectRenderer(emptyPart) {
            @Override
            public void render(ObjModelRenderer model, byte textureVariantID) {
                MissingObjModel.this.renderModel(textureVariantID, false); //take care, MissingObjModel.this != model
            }
        };
        getObjObjects().add(objObjectRenderer);
        emptyPartRenderer = objObjectRenderer;
    }

    public static ObjObjectRenderer getEmptyPart() {
        return emptyPartRenderer;
    }

    @Override
    public IModelTextureVariantsSupplier getTextureVariants() {
        return null;
    }

    @Override
    public void renderGroup(ObjObjectRenderer group, byte textureDataId) {
        renderModel(textureDataId, false);
    }

    @Override
    public ObjObjectRenderer getObjObjectRenderer(String groupName) {
        return emptyPartRenderer;
    }

    @Override
    public boolean renderGroups(String groupsName, byte textureDataId, boolean forceVanillaRender) {
        renderModel(textureDataId, false);
        return true;
    }

    @Override
    public boolean renderDefaultParts(byte textureDataId, boolean forceVanillaRender) {
        renderModel(textureDataId, false);
        return true;
    }

    @Override
    public void renderModel(byte textureDataId, boolean forceVanillaRender) {
        GlStateManager.color(1, 0, 0, 1);
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.065f, 0.065f, 0.065f);
        GlStateManager.rotate(180, 0, 0, 0);
        Minecraft.getMinecraft().fontRenderer.drawString("Error", -16, -16, 0xFFFF0000);
        GlStateManager.popMatrix();
        Render.renderOffsetAABB(new AxisAlignedBB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5), 0, 0, 0);
        GlStateManager.color(1, 1, 1, 1);
    }
}
