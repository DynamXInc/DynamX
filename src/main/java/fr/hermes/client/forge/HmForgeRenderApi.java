package fr.hermes.client.forge;

import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.items.HmItemStack;
import fr.hermes.client.api.HmRenderApi;
import fr.hermes.forge.abstracted.MixinMinecraftClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.culling.ICamera;

public class HmForgeRenderApi implements HmRenderApi {
    private final Minecraft mc;

    public HmForgeRenderApi(Minecraft mc) {
        this.mc = mc;
    }

    // FIXME IMPLEMENT

    @Override
    public boolean shouldRender(HmEntity e, ICamera icamera, double w, double y, double z) {
        return false;
    }

    @Override
    public void enableLightmap() {

    }

    @Override
    public void disableLightmap() {

    }

    @Override
    public void renderEntity(HmEntity e, float partialTicks) {

    }

    @Override
    public void renderItem(HmItemStack stack, IBakedModel guiBaked) {

    }

    @Override
    public void setOverlayMessage(String text, boolean animateColor) {

    }
}
