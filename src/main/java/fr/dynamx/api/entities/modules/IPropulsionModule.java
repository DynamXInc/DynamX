package fr.dynamx.api.entities.modules;

import fr.dynamx.api.physics.entities.IPropulsionHandler;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Base interface for propulsion modules, can be wheels, wings... <br>
 * The propulsion is responsible for the movement of the entity (but the propulsion is controlled by the {@link IEngineModule}) <br>
 * Needs an {@link IPropulsionHandler}
 */
public interface IPropulsionModule<P extends AbstractEntityPhysicsHandler<?, ?>> extends IPhysicsModule<P> {
    IPropulsionHandler getPhysicsHandler();

    /**
     * Used by the render and by the sync code <br>
     * Currently hard-coded
     */
    float[] getPropulsionProperties();

    @SideOnly(Side.CLIENT)
    void spawnPropulsionParticles(RenderPhysicsEntity<?> render, float partialTicks);
}
