package fr.dynamx.api.events;

import fr.dynamx.api.obj.IModelTextureSupplier;
import fr.dynamx.client.renders.model.ObjModelClient;
import fr.dynamx.client.renders.model.ObjObjectRenderer;
import lombok.Getter;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

public class DynamXModelRenderEvent extends Event {
    @Getter
    private final EventStage stage;
    @Getter
    private final ObjModelClient model;
    @Getter
    private final IModelTextureSupplier textureSupplier;
    @Getter
    private final byte textureId;

    public DynamXModelRenderEvent(EventStage stage, ObjModelClient model, IModelTextureSupplier textureSupplier, byte textureId) {
        this.stage = stage;
        this.model = model;
        this.textureSupplier = textureSupplier;
        this.textureId = textureId;
    }

    @Cancelable
    public static class RenderFullModel extends DynamXModelRenderEvent {
        public RenderFullModel(EventStage stage, ObjModelClient model, IModelTextureSupplier textureSupplier, byte textureId) {
            super(stage, model, textureSupplier, textureId);
        }
    }

    @Cancelable
    public static class RenderMainParts extends DynamXModelRenderEvent {
        public RenderMainParts(EventStage stage, ObjModelClient model, IModelTextureSupplier textureSupplier, byte textureId) {
            super(stage, model, textureSupplier, textureId);
        }
    }

    @Cancelable
    public static class RenderPart extends DynamXModelRenderEvent {
        @Getter
        private final ObjObjectRenderer objObjectRenderer;

        public RenderPart(EventStage stage, ObjModelClient model, IModelTextureSupplier textureSupplier, byte textureId, ObjObjectRenderer objObjectRenderer) {
            super(stage, model, textureSupplier, textureId);
            this.objObjectRenderer = objObjectRenderer;
        }
    }
}
