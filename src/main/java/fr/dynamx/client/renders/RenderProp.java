package fr.dynamx.client.renders;

import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.PropsEntity;
import fr.dynamx.utils.debug.renderer.DebugRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.common.MinecraftForge;

public class RenderProp<T extends PropsEntity<?>> extends RenderPhysicsEntity<T> {
    public RenderProp(RenderManager manager) {
        super(manager);
        addDebugRenderers(new DebugRenderer.HullDebug());
        MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.InitPhysicEntityRenderEvent<>(PropsEntity.class, this));
    }

    @Override
    public void renderMain(T entity, float partialsTicks) {
        //GlStateManager.pushMatrix();
        // Translate to block render pos and add the config translate value
        //GlStateManager.translate( entity.getPackInfo().translate[0],  entity.getPackInfo().translate[1], entity.getPackInfo().translate[2]);
        // Scale to the config scale value
        GlStateManager.scale(entity.getPackInfo().getScaleModifier().x, entity.getPackInfo().getScaleModifier().y, entity.getPackInfo().getScaleModifier().z);
        //Render the model
        renderModel(DynamXContext.getObjModelRegistry().getModel(entity.getPackInfo().getModel()), entity, (byte) entity.getMetadata());
        //GlStateManager.popMatrix();
    }

    @Override
    public void renderParts(T entity, float partialTicks) {
    }
}
