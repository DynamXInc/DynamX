package fr.dynamx.api.contentpack.object.render;

import com.jme3.math.Vector3f;
import fr.dynamx.api.obj.IModelTextureSupplier;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

public interface IObjPackObject extends IModelTextureSupplier {
    @SideOnly(Side.CLIENT)
    ResourceLocation getModel();

    default boolean isModelValid() {
        return getModel() != null && !getModel().getPath().toLowerCase().contains("disable_rendering");
    }

    default boolean shouldRegisterModel() {
        return isModelValid();
    }

    @SideOnly(Side.CLIENT)
    default float getItemScale() {
        return 1;
    }

    @SideOnly(Side.CLIENT)
    default Vector3f getItemRotate() {
        return Vector3fPool.get();
    }

    @SideOnly(Side.CLIENT)
    default Vector3f getItemTranslate() {
        return Vector3fPool.get();
    }

    @SideOnly(Side.CLIENT)
    default Enum3DRenderLocation get3DItemRenderLocation() {
        return Enum3DRenderLocation.ALL;
    }

    @SideOnly(Side.CLIENT)
    default void renderItem3D(ItemStack item, ItemCameraTransforms.TransformType renderType) {
        DynamXContext.getObjModelRegistry().getModel(getModel()).renderModel((byte) item.getMetadata());
    }

    @Nullable
    @SideOnly(Side.CLIENT)
    default String getItemIcon() {
        return null;
    }
}
