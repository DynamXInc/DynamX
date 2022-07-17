package fr.dynamx.api.events;

import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.physics.terrain.chunk.ChunkCollisions;
import fr.dynamx.common.physics.terrain.chunk.EnumChunkCollisionsState;
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
    public static class PhysicsEntityAddedEvent extends PhysicsEvent {

        @Getter
        private final PhysicsEntity<?> physicsEntity;

        /**
         * @param physicsEntity the physics entity which was added
         * @param physicsWorld  the physics world where the physics entity was added
         */
        public PhysicsEntityAddedEvent(PhysicsEntity<?> physicsEntity, IPhysicsWorld physicsWorld) {
            super(physicsWorld);
            this.physicsEntity = physicsEntity;
        }
    }

    /**
     * Fired when an entity is removed from the physics world
     */
    public static class PhysicsEntityRemovedEvent extends PhysicsEvent {

        @Getter
        private final PhysicsEntity<?> physicsEntity;

        /**
         * @param physicsEntity the physics entity which was removed
         * @param physicsWorld  the physics world where the physics entity was removed
         */
        public PhysicsEntityRemovedEvent(PhysicsEntity<?> physicsEntity, IPhysicsWorld physicsWorld) {
            super(physicsWorld);
            this.physicsEntity = physicsEntity;
        }
    }

    /**
     * Called on physics world update
     */
    public static class StepSimulationEvent extends PhysicsEvent {
        @Getter
        private final float deltaTime;

        /**
         * @param physicsWorld The physics world where the simulation took place
         * @param deltaTime    The time that was simulated
         */
        public StepSimulationEvent(IPhysicsWorld physicsWorld, float deltaTime) {
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
     * Called when the state of a ChunkCollision has changed, see {@link EnumChunkCollisionsState}
     */
    public static class ChunkCollisionsStateEvent extends PhysicsEvent {
        @Getter
        private final ChunkCollisions collisions;
        @Getter
        private final EnumChunkCollisionsState oldState;

        /**
         * Called when the state of a ChunkCollision has changed, see {@link EnumChunkCollisionsState}
         *
         * @param physicsWorld The physics world owning this chunk
         * @param collisions   The chunk (16*16*16 area)
         * @param oldState     The old state of the chunk
         */
        public ChunkCollisionsStateEvent(IPhysicsWorld physicsWorld, ChunkCollisions collisions, EnumChunkCollisionsState oldState) {
            super(physicsWorld);
            this.collisions = collisions;
            this.oldState = oldState;
        }
    }

   /*
    public static class PhysicsCollisionEvent extends PhysicsEvent
    {
        @Getter private final BulletShapeType<?> object1, object2;

        public PhysicsCollisionEvent(IPhysicsWorld world, BulletShapeType<?> o1, BulletShapeType<?> o2){
            super(world);
            object1 = o1;
            object2 = o2;
            System.out.println("Cogne "+o1+" "+o2);
        }
    }*/
}
