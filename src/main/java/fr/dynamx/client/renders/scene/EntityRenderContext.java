package fr.dynamx.client.renders.scene;

import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.common.entities.IDynamXObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The context of a render call of {@link SceneGraph#render(IDynamXObject, EntityRenderContext, fr.dynamx.api.contentpack.object.render.IModelPackObject)} <br>
 * Reused to avoid creating a new object each time.
 */
@Getter
@RequiredArgsConstructor
public class EntityRenderContext {
    private final RenderPhysicsEntity<?> render;
    private DxModelRenderer model;
    private byte textureId;
    private double x;
    private double y;
    private double z;
    private float partialTicks;
    private boolean useVanillaRender;

    public EntityRenderContext setEntityParams(DxModelRenderer model, byte textureId) {
        this.model = model;
        this.textureId = textureId;
        return this;
    }

    public EntityRenderContext setRenderParams(double x, double y, double z, float partialTicks, boolean useVanillaRender) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.partialTicks = partialTicks;
        this.useVanillaRender = useVanillaRender;
        return this;
    }
}
