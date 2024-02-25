package fr.dynamx.api.events.client;

import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.node.AbstractItemNode;
import lombok.Getter;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Getter
@Cancelable
public class DynamXRenderItemEvent extends Event {
    private final BaseRenderContext.ItemRenderContext context;
    private final AbstractItemNode<?, ?> sceneGraph;
    private final EventStage stage;

    public DynamXRenderItemEvent(BaseRenderContext.ItemRenderContext context, AbstractItemNode<?, ?> sceneGraph, EventStage stage) {
        this.context = context;
        this.sceneGraph = sceneGraph;
        this.stage = stage;
    }

    public enum EventStage {
        PRE(),
        RENDER(),
        TRANSFORM(),
        POST()
    }
}
