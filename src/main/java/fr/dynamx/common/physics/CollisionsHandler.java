package fr.dynamx.common.physics;

import com.jme3.bullet.collision.PhysicsCollisionEvent;
import fr.dynamx.api.events.PhysicsEvent;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.common.entities.PhysicsEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.common.MinecraftForge;

import java.util.HashSet;
import java.util.Objects;

public class CollisionsHandler {

    @Getter
    private static final HashSet<CollisionInfo> CACHED_COLLISIONS = new HashSet<>();

    @Getter
    @Setter
    private static int EXPIRATION_TIME = 3 * 20;

    public static void tick() {
        CACHED_COLLISIONS.removeIf(CollisionInfo::tick);
    }

    /**
     * Handles collision between two bodies <br>
     */
    public static void handleCollision(IPhysicsWorld physicsWorld, PhysicsCollisionEvent collisionEvent, BulletShapeType<?> bodyA, BulletShapeType<?> bodyB, float impulse) {
        if ((bodyA.getType().isEntity() && bodyB.getType().isEntity()) || (bodyA.getType().isEntity() && bodyB.getType().isTerrain()) || (bodyA.getType().isTerrain() && bodyB.getType().isEntity())) {
            CollisionInfo info = new CollisionInfo(physicsWorld, bodyA, bodyB, EXPIRATION_TIME, collisionEvent);
            if (CACHED_COLLISIONS.add(info)) {
                info.handleCollision();
            }
        }
    }

    @AllArgsConstructor
    public static class CollisionInfo {
        @Getter
        private IPhysicsWorld physicsWorld;
        @Getter
        private final BulletShapeType<?> entityA, entityB;
        @Getter
        private int time;
        @Getter
        private final PhysicsCollisionEvent collisionEvent;

        public boolean tick() {
            return time-- <= 0;
        }

        public void handleCollision() {
            if (entityA.getType().isPlayer() && entityB.getType().isTerrain())
                return;
            MinecraftForge.EVENT_BUS.post(new PhysicsEvent.PhysicsCollision(physicsWorld, entityA, entityB, this));
            if (entityA.getObjectIn() instanceof PhysicsEntity && entityB.getObjectIn() instanceof PhysicsEntity) {
                if (entityA.getType().isBulletEntity()) {
                    ((PhysicsEntity<?>) entityA.getObjectIn()).onCollisionEnter(collisionEvent, entityA, entityB);
                } else if (entityB.getType().isBulletEntity()) {
                    ((PhysicsEntity<?>) entityB.getObjectIn()).onCollisionEnter(collisionEvent, entityA, entityB);
                }
            }
           /* if (entityA.getType().isPlayer() && entityB.getType().isBulletEntity()) {
                //((PlayerPhysicsHandler) entity1.getObjectIn()).handleCollision(event, entity2);
            } else if (entityB.getType().isPlayer() && entityA.getType().isBulletEntity())
                ((PlayerPhysicsHandler) entityB.getObjectIn()).handleCollision(collisionEvent, entityA);*/
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CollisionInfo that = (CollisionInfo) o;
            return (entityA.equals(that.entityA) &&
                    entityB.equals(that.entityB)) || (entityA.equals(that.entityB) &&
                    entityB.equals(that.entityA));
        }

        @Override
        public int hashCode() {
            return Objects.hash(entityA, entityB);
        }
    }
}
