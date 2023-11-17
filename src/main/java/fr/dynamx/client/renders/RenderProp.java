package fr.dynamx.client.renders;

import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.scene.EntityRenderContext;
import fr.dynamx.client.renders.scene.SceneGraph;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.type.objects.PropObject;
import fr.dynamx.common.entities.PropsEntity;
import fr.dynamx.utils.debug.renderer.BoatDebugRenderer;
import fr.dynamx.utils.debug.renderer.DebugRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.common.MinecraftForge;

public class RenderProp<T extends PropsEntity<?>> extends RenderPhysicsEntity<T> {
    public RenderProp(RenderManager manager) {
        super(manager);
        addDebugRenderers(new BoatDebugRenderer.FloatsDebug(), new DebugRenderer.SeatDebug());
        MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.InitRenderer<>(PropsEntity.class, this));
    }

    @Override
    public void renderEntity(T entity, double x, double y, double z, float partialTicks, boolean useVanillaRender) {
        if (entity.getPackInfo() == null) {
            renderOffsetAABB(entity.getEntityBoundingBox(), x - entity.lastTickPosX, y - entity.lastTickPosY, z - entity.lastTickPosZ);
            return;
        }
        DxModelRenderer modelRenderer = DynamXContext.getDxModelRegistry().getModel(entity.getPackInfo().getModel());
        if (modelRenderer == null) {
            renderOffsetAABB(entity.getEntityBoundingBox(), x - entity.lastTickPosX, y - entity.lastTickPosY, z - entity.lastTickPosZ);
            return;
        }
        EntityRenderContext context = new EntityRenderContext(this, modelRenderer, entity.getEntityTextureID(), x, y, z, partialTicks, useVanillaRender);
        ((SceneGraph<T, PropObject<?>>) entity.getPackInfo().getSceneGraph()).render(entity, context, entity.getPackInfo());
    }
}
