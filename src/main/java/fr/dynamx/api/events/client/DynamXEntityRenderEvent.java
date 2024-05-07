package fr.dynamx.api.events.client;

import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.common.entities.PhysicsEntity;
import lombok.Getter;
import net.minecraftforge.fml.common.eventhandler.Event;

import javax.annotation.Nullable;

/**
 * Fired when rendering a {@link PhysicsEntity} <br>
 * This event can be used to override the rendering of an entity, or to add custom rendering <br>
 * This event has different phases described by {@link DynamXEntityRenderEvent#renderType}
 */
@Getter
public class DynamXEntityRenderEvent extends Event {
    /**
     * The entity being rendered
     */
    @Nullable
    private final PhysicsEntity<?> entity;

    /**
     * The render context
     */
    private final BaseRenderContext.EntityRenderContext context;

    /**
     * The render type
     *
     * @see Type
     */
    private final Type renderType;

    /**
     * The render pass <br>
     * 0 for solid objects <br>
     * 1 for translucent objects <br>
     * Some types of render event are fired for both
     */
    private final int renderPass;

    public DynamXEntityRenderEvent(PhysicsEntity<?> entity, BaseRenderContext.EntityRenderContext context, Type renderType, int renderPass) {
        this.entity = entity;
        this.context = context;
        this.renderType = renderType;
        this.renderPass = renderPass;
    }

    /**
     * The render type
     *
     * @see Type#ENTITY
     * @see Type#PARTICLES
     * @see Type#DEBUG
     * @see Type#POST
     */
    public enum Type {
        /**
         * Fired before rendering the entity. Cancellable.
         */
        ENTITY,
        /**
         * Fired before spawning particles. Cancellable.
         */
        PARTICLES,
        /**
         * Fired before rendering debug. Cancellable.
         */
        DEBUG,
        /**
         * Fired after rendering the entity. Not cancellable.
         */
        POST
    }
}
