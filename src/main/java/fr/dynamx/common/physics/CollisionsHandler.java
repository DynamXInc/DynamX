package fr.dynamx.common.physics;

import com.jme3.bullet.collision.PhysicsCollisionEvent;
import fr.dynamx.api.events.PhysicsEvent;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.physics.player.PlayerPhysicsHandler;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CollisionsHandler {

    @Getter
    private static final List<CollisionInfo> CACHED_COLLISIONS = new ArrayList<>();

    @Getter
    @Setter
    private static int EXPIRATION_TIME = 3 * 20;

    public static void tick() {
        CACHED_COLLISIONS.removeIf(CollisionInfo::tick);
    }

    /**
     * Handles collision between two bodies <br>
     */
    public static void handleCollision(PhysicsCollisionEvent collisionEvent, BulletShapeType<?> bodyA, BulletShapeType<?> bodyB) {
        if (bodyA.getType().isEntity() && bodyB.getType().isEntity() || bodyA.getType().isEntity() && bodyB.getType().isTerrain() || bodyA.getType().isTerrain() && bodyB.getType().isEntity()) {
            CollisionInfo info = new CollisionInfo(bodyA, bodyB, EXPIRATION_TIME, collisionEvent);
            if (!CACHED_COLLISIONS.contains(info)) {
                CACHED_COLLISIONS.add(info);
                info.handleCollision();
            }
        }
    }

    public static class CollisionInfo {
        @Getter
        private final BulletShapeType<?> entityA, entityB;
        @Getter
        private final PhysicsCollisionEvent collisionEvent;
        @Getter
        private int time;

        public CollisionInfo(BulletShapeType<?> entityA, BulletShapeType<?> entityB, int time, PhysicsCollisionEvent collisionEvent) {
            this.entityA = entityA;
            this.entityB = entityB;
            this.time = time;
            this.collisionEvent = collisionEvent;
        }

        public boolean tick() {
            return time-- <= 0;
        }

        public void handleCollision() {
            MinecraftForge.EVENT_BUS.post(new PhysicsEvent.PhysicsCollisionEvent(DynamXContext.getPhysicsWorld(), entityA, entityB, this));
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
