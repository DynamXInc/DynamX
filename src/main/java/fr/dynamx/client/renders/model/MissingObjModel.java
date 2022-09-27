package fr.dynamx.client.renders.model;

import fr.dynamx.api.obj.IModelTextureSupplier;
import fr.dynamx.api.obj.IObjObject;
import fr.dynamx.common.obj.Mesh;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;

import javax.annotation.Nullable;
import javax.vecmath.Vector3f;
import java.util.ArrayList;

/**
 * Missing obj model indicating errors
 */
public class MissingObjModel extends ObjModelClient {
    private static final AxisAlignedBB BOX = new AxisAlignedBB(-1, -1, -1, 1, 1, 1);
    private static final Vector3f zero = new Vector3f();
    private static final IObjObject emptyPart = new IObjObject() {
        @Override
        public String getName() {
            return "empty";
        }

        @Override
        public Vector3f getCenter() {
            return zero;
        }

        @Override
        public void setCenter(Vector3f center) {
        }

        @Override
        public Mesh getMesh() {
            return null;
        }
    };

    public MissingObjModel() {
        super(new ResourceLocation(DynamXConstants.ID, "obj/missing.obj"), new ArrayList<>());
        objObjects.add(emptyPart);
    }

    public static IObjObject getEmptyPart() {
        return emptyPart;
    }

    @Nullable
    @Override
    public IModelTextureSupplier getCustomTextures() {
        return null;
    }

    @Override
    public void setupModel() {
    }

    @Override
    public void renderGroup(IObjObject group, byte textureDataId) {
        renderModel(textureDataId);
    }

    @Override
    public IObjObject getObjObject(String groupName) {
        return emptyPart;
    }

    @Override
    public boolean renderGroups(String groupsName, byte textureDataId) {
        renderModel(textureDataId);
        return true;
    }

    @Override
    public boolean renderMainParts(byte textureDataId) {
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
