package fr.dynamx.client.renders.model;

import fr.dynamx.api.obj.IModelTextureSupplier;
import fr.dynamx.api.obj.IObjObject;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.common.obj.eximpl.TessellatorModelClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A render-able obj model
 *
 * @see fr.dynamx.common.obj.ObjModelServer
 */
public abstract class ObjModelClient {
    private final ResourceLocation location;
    public final List<IObjObject> objObjects;
    /**
     * Used for error logging, see {@link IObjObject}
     */
    public boolean hasNoneMaterials;

    public ObjModelClient(ResourceLocation location, List<IObjObject> objObjects) {
        this.location = location;
        this.objObjects = objObjects;
    }

    public ResourceLocation getLocation() {
        return location;
    }

    public static ObjModelClient createObjModel(ResourceLocation location, @Nullable IModelTextureSupplier customTextures) {
        return TessellatorModelClient.loadObjModel(location, customTextures);
    }

    @Nullable
    public abstract IModelTextureSupplier getCustomTextures();

    public abstract void setupModel();

    /**
     * Called to render this group with displayList <br>
     * Will draw nothing if the model is not correctly loaded
     */
    public abstract void renderGroup(IObjObject group, byte textureDataId);

    public void renderGroup(IObjObject obj) {
        renderGroup(obj, (byte) 0);
    }

    public abstract IObjObject getObjObject(String groupName);

    /**
     * Called to render this part <br>
     * Will draw nothing if the model is not correctly loaded
     *
     * @return True if successfully drawn something
     */
    public abstract boolean renderGroups(String groupsName, byte textureDataId);

    public abstract boolean renderMainParts(byte textureDataId);

    /**
     * Called to render this model <br>
     * Will draw nothing if the model is not correctly loaded
     */
    public abstract void renderModel(byte textureDataId);

    /**
     * Called to render this model with default texture <br>
     * Will draw nothing if the model is not correctly loaded
     */
    public void renderModel() {
        renderModel((byte) 0);
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
}
