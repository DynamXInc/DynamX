package fr.hermes.api.mod;

import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.items.HmItemStack;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.culling.ICamera;

public interface HmRenderApi {
    boolean shouldRender(HmEntity e, ICamera icamera, double w, double y, double z);

    void enableLightmap();

    void disableLightmap();

    void renderEntity(HmEntity e, float partialTicks);

    void renderItem(HmItemStack stack, IBakedModel guiBaked);
}
