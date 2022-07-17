package fr.dynamx.api.physics;

import com.jme3.bullet.PhysicsSoftSpace;

/**
 * The physics simulation mode <br>
 * It changes the precision of the physics in favor (or not) of the game performance (to reduce the lag)
 */
public interface IPhysicsSimulationMode {
    /**
     * Updates the bullet's physics world
     *
     * @param dynamicsWorld The bullet's dynamic world
     */
    void updatePhysicsWorld(PhysicsSoftSpace dynamicsWorld);

    /**
     * @return The interval of time simulated in each call of dynamicsWorld.stepSimulation
     */
    float getTimeStep();

    /**
     * @return The simulation mode name
     */
    String getName();
}
