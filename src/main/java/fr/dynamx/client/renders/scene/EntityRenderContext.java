package fr.dynamx.client.renders.scene;

import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.common.entities.PhysicsEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The context of a render call of {@link SceneGraph#render(PhysicsEntity, EntityRenderContext, IPhysicsPackInfo)}
 */
@Getter
@RequiredArgsConstructor
public class EntityRenderContext {
    //TODO CONTEXT FOR ON-GUI AND IN-HAND RENDER
    private final RenderPhysicsEntity<?> render;
    private final DxModelRenderer model;
    private final byte textureId;
    private final double x;
    private final double y;
    private final double z;
    private final float partialTicks;
    private final boolean useVanillaRender;
}
