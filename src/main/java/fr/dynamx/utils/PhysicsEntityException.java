package fr.dynamx.utils;

import fr.dynamx.common.entities.PhysicsEntity;

public class PhysicsEntityException extends RuntimeException
{
    public PhysicsEntityException(PhysicsEntity<?> entity, String message) {
        super("Exception simulating " + getEntityDetails(entity) + " : " + message);
    }

    public PhysicsEntityException(PhysicsEntity<?> entity, String message, Throwable cause) {
        super("Exception simulating " + getEntityDetails(entity) + " : " + message, cause);
    }

    private static String getEntityDetails(PhysicsEntity<?> entity) {
        return entity + " ( " + entity.getClass() + ")[is_reg=" + entity.isRegistered+", phyWorld="+entity.usesPhysicsWorld()+", phyHand="+entity.physicsHandler+", existed="+entity.ticksExisted+", phyPos="+entity.physicsPosition+"]";
    }
}
