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
     * Will draw nothing if the model is not correctly loaded <br>
     * <strong>For GLTF models, this method pushed the GL11.GL_ALL_ATTRIB_BITS that must be popped with {@link DynamXRenderUtils#popGlAllAttribBits()}</strong>
     *
     * @param forceVanillaRender Should be false to render the model in the game world, true otherwise
     */
    public void renderModel(boolean forceVanillaRender) {
        renderModel((byte) 0, forceVanillaRender);
    }

    /**
     * Renders this model <br>
     * Will draw nothing if the model is not correctly loaded <br>
     * <strong>For GLTF models, this method pushed the GL11.GL_ALL_ATTRIB_BITS that must be popped with {@link DynamXRenderUtils#popGlAllAttribBits()}</strong>
     *
     * @param textureDataId      The texture id to use
     * @param forceVanillaRender Should be false to render the model in the game world, true otherwise
     */
    public abstract void renderModel(byte textureDataId, boolean forceVanillaRender);

    /**
     * Renders the given object of this model <br>
     * Will draw nothing if the model is not correctly loaded or the object isn't found <br>
     * <strong>For GLTF models, this method pushed the GL11.GL_ALL_ATTRIB_BITS that must be popped with {@link DynamXRenderUtils#popGlAllAttribBits()}</strong>
     *
     * @param group              The name of the object to render
     * @param textureDataId      The texture id to use
     * @param forceVanillaRender Should be false to render the model in the game world, true otherwise
     * @return true if something has been drawn, false otherwise
     */
    public abstract boolean renderGroup(String group, byte textureDataId, boolean forceVanillaRender);

    /**
     * Renders the default parts of this model <br>
     * The default parts are the obj objects that are not drawn by an {@link fr.dynamx.api.contentpack.object.part.IDrawablePart} (like the chassis of a vehicle) <br>
     * <strong>For GLTF models, this method pushed the GL11.GL_ALL_ATTRIB_BITS that must be popped with {@link DynamXRenderUtils#popGlAllAttribBits()}</strong>
     *
     * @param textureDataId      The texture id to use
     * @param forceVanillaRender Should be false to render the model in the game world, true otherwise
     * @return true if something has been drawn, false otherwise
     */
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
        DynamXRenderUtils.popGlAllAttribBits();
        GlStateManager.popMatrix();
    }

    public abstract boolean containsObjectOrNode(String name);

    public abstract boolean isEmpty();
}
