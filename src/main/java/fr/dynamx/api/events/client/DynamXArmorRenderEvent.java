package fr.dynamx.api.events.client;

import fr.dynamx.api.events.EventPhase;
import fr.dynamx.core.client.renders.scene.BaseRenderContext;
import fr.dynamx.core.client.renders.scene.node.SceneNode;
import lombok.Getter;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Getter
@Cancelable
public class DynamXArmorRenderEvent extends Event {
    private final BaseRenderContext.ArmorRenderContext context;
    private final SceneNode<?, ?> sceneGraph;
    private final EventPhase eventPhase;

    public DynamXArmorRenderEvent(BaseRenderContext.ArmorRenderContext renderContext, SceneNode<?, ?> sceneGraph, EventPhase eventPhase) {
        this.context = renderContext;
        this.sceneGraph = sceneGraph;
        this.eventPhase = eventPhase;
    }
}
