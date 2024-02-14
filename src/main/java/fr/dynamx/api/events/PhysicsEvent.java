package fr.dynamx.api.events;

import com.jme3.bullet.collision.PhysicsCollisionEvent;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.physics.CollisionsHandler;
import lombok.Getter;
import net.minecraftforge.fml.common.eventhandler.Event;

public class PhysicsEvent extends Event {

    @Getter
    private final IPhysicsWorld physicsWorld;

    public PhysicsEvent(IPhysicsWorld physicsWorld) {
        this.physicsWorld = physicsWorld;
    }

    /**
     * Fired when an entity is added to the physics world
     */
    public static class PhysicsEntityAdded extends PhysicsEvent {

        @Getter
        private final PhysicsEntity<?> physicsEntity;

        /**
         * @param physicsEntity the physics entity which was added
         * @param physicsWorld  the physics world where the physics entity was added
         */
        public PhysicsEntityAdded(PhysicsEntity<?> physicsEntity, IPhysicsWorld physicsWorld) {
            super(physicsWorld);
            this.physicsEntity = physicsEntity;
        }
    }

    /**
     * Fired when an entity is removed from the physics world
     */
    public static class PhysicsEntityRemoved extends PhysicsEvent {

        @Getter
        private final PhysicsEntity<?> physicsEntity;

        /**
         * @param physicsEntity the physics entity which was removed
         * @param physicsWorld  the physics world where the physics entity was removed
         */
        public PhysicsEntityRemoved(PhysicsEntity<?> physicsEntity, IPhysicsWorld physicsWorld) {
            super(physicsWorld);
            this.physicsEntity = physicsEntity;
        }
    }

    /**
     * Called on physics world update
     */
    public static class StepSimulation extends PhysicsEvent {
        @Getter
        private final float deltaTime;

        /**
         * @param physicsWorld The physics world where the simulation took place
         * @param deltaTime    The time that was simulated
         */
        public StepSimulation(IPhysicsWorld physicsWorld, float deltaTime) {
            super(physicsWorld);
            this.deltaTime = deltaTime;
        }
    }

    /**
     * Called after bullet's world initialization
     */
    public static class PhysicsWorldLoad extends PhysicsEvent {
        public PhysicsWorldLoad(IPhysicsWorld physicsWorld) {
            super(physicsWorld);
        }
    }

    /**
     * Called when a collision occurs in the physics engine
     */
    public static class PhysicsCollision extends PhysicsEvent {
        @Getter
        private final BulletShapeType<?> object1, object2;

        @Getter
        private final CollisionsHandler.CollisionInfo collisionInfo;

        /**
         * @param world   The physics world owning this chunk
         * @param object1 First collision object colliding with the second
         * @param object2 Second collision object colliding with the first
         */
        public PhysicsCollision(IPhysicsWorld world, BulletShapeType<?> object1, BulletShapeType<?> object2, CollisionsHandler.CollisionInfo collisionInfo) {
            super(world);
            this.object1 = object1;
            this.object2 = object2;
            this.collisionInfo = collisionInfo;
        }


        public static class Pre extends PhysicsCollision {

            /**
             * @param world   The physics world owning this chunk
             * @param object1 First collision object colliding with the second
             * @param object2 Second collision object colliding with the first
             */
            public Pre(IPhysicsWorld world, BulletShapeType<?> object1, BulletShapeType<?> object2, CollisionsHandler.CollisionInfo collisionInfo) {
                super(world, object1, object2, collisionInfo);
            }
        }

    }
}
