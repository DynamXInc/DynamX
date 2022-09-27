package fr.dynamx.common.physics;

import com.jme3.bullet.collision.PhysicsCollisionEvent;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.physics.player.PlayerPhysicsHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CollisionsHandler {

    private static final List<CollisionInfo> cachedCollisions = new ArrayList<>();

    public static void tick() {
        cachedCollisions.removeIf(CollisionInfo::tick);
    }

    /**
     * Handles collision between two bodies <br>
     */
    public static void handleCollision(PhysicsCollisionEvent collisionEvent, BulletShapeType<?> bodyA, BulletShapeType<?> bodyB) {
        if (bodyA.getType().isEntity() && bodyB.getType().isEntity() || bodyA.getType().isEntity() && bodyB.getType().isTerrain() || bodyA.getType().isTerrain() && bodyB.getType().isEntity()) {
            CollisionInfo info = new CollisionInfo(bodyA, bodyB, 3 * 20, collisionEvent);
            if (!cachedCollisions.contains(info)) {
                cachedCollisions.add(info);
                info.handleCollision();
            }
        }
    }


    public static List<CollisionInfo> getCachedCollisions() {
        return cachedCollisions;
    }

    public static class CollisionInfo {
        private final BulletShapeType<?> entityA, entityB;
        private final PhysicsCollisionEvent collisionEvent;
        private int time;

        public CollisionInfo(BulletShapeType<?> entityA, BulletShapeType<?> entityB, int time, PhysicsCollisionEvent collisionEvent) {
            this.entityA = entityA;
            this.entityB = entityB;
            this.time = time;
            this.collisionEvent = collisionEvent;
        }

        public PhysicsCollisionEvent getCollisionEvent() {
            return collisionEvent;
        }

        public BulletShapeType<?> getEntityA() {
            return entityA;
        }

        public BulletShapeType<?> getEntityB() {
            return entityB;
        }

        public int getTime() {
            return time;
        }

        public boolean tick() {
            time--;
            return time <= 0;
        }

        public void handleCollision() {
            //if (getAppliedImpulse() != 0) {
            if (entityB.getObjectIn() instanceof PhysicsEntity) {
                if (entityA.getType().isBulletEntity()) {
                    ((PhysicsEntity<?>) entityA.getObjectIn()).onCollisionEnter(collisionEvent, entityA, entityB);
                } else if (entityB.getType().isBulletEntity()) {
                    ((PhysicsEntity<?>) entityB.getObjectIn()).onCollisionEnter(collisionEvent, entityA, entityB);
                }
            }
            // }
            if (entityA.getType().isPlayer() && entityB.getType().isBulletEntity()) {
                //((PlayerPhysicsHandler) entity1.getObjectIn()).handleCollision(event, entity2);
            } else if (entityB.getType().isPlayer() && entityA.getType().isBulletEntity())
                ((PlayerPhysicsHandler) entityB.getObjectIn()).handleCollision(collisionEvent, entityA);
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
