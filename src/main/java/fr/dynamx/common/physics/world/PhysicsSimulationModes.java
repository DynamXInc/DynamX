package fr.dynamx.common.physics.world;

import com.jme3.bullet.PhysicsSoftSpace;
import fr.dynamx.api.physics.IPhysicsSimulationMode;

/**
 * DynamX {@link IPhysicsSimulationMode}s
 */
public class PhysicsSimulationModes
{
    /**
     * Full physics : 2 calls of bullet physics update per dynamx physics world update, with 0.025 time step
     */
    public static class FullPhysics implements IPhysicsSimulationMode {
        @Override
        public void updatePhysicsWorld(PhysicsSoftSpace dynamicsWorld) {
            dynamicsWorld.update(getTimeStep(), 0, false, true, false);
            dynamicsWorld.update(getTimeStep(), 0, false, true, false);
        }

        @Override
        public float getTimeStep() {
            return 0.025f;
        }

        @Override
        public String getName() {
            return "full";
        }
    }

    /**
     * Light physics : 1 call of bullet physics update per dynamx physics world update, with 0.050 time step
     */
    public static class LightPhysics implements IPhysicsSimulationMode {

        @Override
        public void updatePhysicsWorld(PhysicsSoftSpace dynamicsWorld) {
            dynamicsWorld.update(getTimeStep(), 0, false, true, false);
        }

        @Override
        public float getTimeStep() {
            return 0.05f;
        }

        @Override
        public String getName() {
            return "light";
        }
    }
}
