package fr.dynamx.client.renders.model;

import fr.dynamx.api.obj.IModelTextureVariantsSupplier;
import fr.dynamx.api.obj.ObjModelPath;
import fr.dynamx.client.DynamXModelRegistry;
import fr.dynamx.client.renders.model.renderer.ObjModelRenderer;
import fr.dynamx.client.renders.model.renderer.ObjObjectRenderer;
import fr.dynamx.common.objloader.data.Mesh;
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
    private static final ObjObjectData emptyPart = new ObjObjectData("empty") {
        @Override
        public String getName() {
            return "empty";
        }
    };

    private static ObjObjectRenderer emptyPartRenderer;

    public MissingObjModel() {
        super(new ObjModelPath(DynamXModelRegistry.BASE_PACKINFO, new ResourceLocation(DynamXConstants.ID, "obj/missing.obj")), new ArrayList<>(), new HashMap<>(), null);
        emptyPart.setCenter(new Vector3f());
        ObjObjectRenderer objObjectRenderer = new ObjObjectRenderer(emptyPart) {
            @Override
            public void render(ObjModelRenderer model, byte textureVariantID) {
                MissingObjModel.this.renderModel(textureVariantID); //take care, MissingObjModel.this != model
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
    public void setupModel() {
    }

    @Override
    public void renderGroup(ObjObjectRenderer group, byte textureDataId) {
        renderModel(textureDataId);
    }

    @Override
    public ObjObjectRenderer getObjObjectRenderer(String groupName) {
        return emptyPartRenderer;
    }

    @Override
    public boolean renderGroups(String groupsName, byte textureDataId) {
        renderModel(textureDataId);
        return true;
    }

    @Override
    public boolean renderDefaultParts(byte textureDataId) {
        renderModel(textureDataId);
        return true;
    }

    @Override
    public void renderModel(byte textureDataId) {
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
