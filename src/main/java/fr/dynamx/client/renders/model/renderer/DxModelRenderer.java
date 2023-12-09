package fr.dynamx.client.renders.model.renderer;

import fr.dynamx.api.dxmodel.DxModelPath;
import fr.dynamx.api.dxmodel.EnumDxModelFormats;
import fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;

import javax.vecmath.Vector4f;

public abstract class DxModelRenderer {

    @Getter
    protected final DxModelPath location;

    @Getter
    protected final IModelTextureVariantsSupplier textureVariants;
    @Getter
    @Setter
    protected Vector4f modelColor = new Vector4f(1, 1, 1, 1);

    @Getter
    private EnumDxModelFormats format;

    public DxModelRenderer(DxModelPath location, IModelTextureVariantsSupplier textureVariants) {
        this.location = location;
        this.textureVariants = textureVariants;
        this.format = location.getFormat();
    }

    /**
     * Called to render this model with default texture <br>
     * Will draw nothing if the model is not correctly loaded
     */
    public void renderModel(boolean forceVanillaRender) {
        renderModel((byte) 0, forceVanillaRender);
    }

    /**
     * Called to render this model <br>
     * Will draw nothing if the model is not correctly loaded
     */
    public abstract void renderModel(byte textureDataId, boolean forceVanillaRender);

    public abstract boolean renderGroups(String group, byte textureDataId, boolean forceVanillaRender);

    public abstract void renderGroup(String group, byte textureDataId, boolean forceVanillaRender);

    public abstract boolean renderDefaultParts(byte textureDataId, boolean forceVanillaRender);

    public void uploadVAOs() {
    }

    public void clearVAOs() {
    }

    public void renderPreview(BlockObject<?> blockObjectInfo, EntityPlayer player, BlockPos blockPos, boolean canPlace, float orientation, float partialTicks, int textureNum) {
        double px = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double py = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
        GlStateManager.pushMatrix();
        GlStateManager.translate(-px + blockPos.getX() + 0.5, -py + blockPos.getY() + 1.5, -pz + blockPos.getZ() + 0.5);
        GlStateManager.rotate(GlQuaternionPool.get(DynamXGeometry.eulerToQuaternion((blockObjectInfo.getRotation().z),
                ((blockObjectInfo.getRotation().y + orientation * 22.5f) % 360),
                (blockObjectInfo.getRotation().x))));
        DynamXRenderUtils.glTranslate(blockObjectInfo.getTranslation());
        GlStateManager.scale(blockObjectInfo.getScaleModifier().x, blockObjectInfo.getScaleModifier().y, blockObjectInfo.getScaleModifier().z);
        GlStateManager.disableBlend();
        setModelColor(new Vector4f(canPlace ? 0 : 1, canPlace ? 1 : 0, 0, 0.7f));
        renderModel((byte) textureNum, true);
        GlStateManager.enableBlend();
        GlStateManager.popMatrix();
    }

    public abstract boolean containsObjectOrNode(String name);

    public abstract boolean isEmpty();
}
