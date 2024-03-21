package fr.dynamx.api.events;

import fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier;
import fr.dynamx.client.renders.model.renderer.ObjModelRenderer;
import fr.dynamx.client.renders.model.renderer.ObjObjectRenderer;
import lombok.Getter;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Getter
public class DynamXModelRenderEvent extends Event {
    private final EventPhase stage;
    private final ObjModelRenderer model;
    private final IModelTextureVariantsSupplier textureSupplier;
    private final byte textureId;

    public DynamXModelRenderEvent(EventPhase stage, ObjModelRenderer model, IModelTextureVariantsSupplier textureSupplier, byte textureId) {
        this.stage = stage;
        this.model = model;
        this.textureSupplier = textureSupplier;
        this.textureId = textureId;
    }

    @Cancelable
    public static class RenderFullModel extends DynamXModelRenderEvent {
        public RenderFullModel(EventPhase stage, ObjModelRenderer model, IModelTextureVariantsSupplier textureSupplier, byte textureId) {
            super(stage, model, textureSupplier, textureId);
        }
    }

    @Cancelable
    public static class RenderMainParts extends DynamXModelRenderEvent {
        public RenderMainParts(EventPhase stage, ObjModelRenderer model, IModelTextureVariantsSupplier textureSupplier, byte textureId) {
            super(stage, model, textureSupplier, textureId);
        }
    }

    @Cancelable
    public static class RenderPart extends DynamXModelRenderEvent {
        @Getter
        private final ObjObjectRenderer objObjectRenderer;

        public RenderPart(EventPhase stage, ObjModelRenderer model, IModelTextureVariantsSupplier textureSupplier, byte textureId, ObjObjectRenderer objObjectRenderer) {
            super(stage, model, textureSupplier, textureId);
            this.objObjectRenderer = objObjectRenderer;
        }
    }
}
