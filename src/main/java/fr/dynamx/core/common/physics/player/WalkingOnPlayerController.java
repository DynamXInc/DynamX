package fr.dynamx.core.common.physics.player;

import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.IRotatedCollisionHandler;
import fr.dynamx.core.common.DynamXContext;
import fr.dynamx.core.common.entities.PhysicsEntity;
import fr.dynamx.core.utils.maths.DynamXGeometry;
import fr.hermes.forge.JmeVector3fPool;
import fr.hermes.api.mc.HmOrientation;
import fr.hermes.api.mc.HmPlayerEntity;

/**
 * Responsible to update a walking player <br>
 * A walking player is a player standing on the top of a {@link PhysicsEntity} <br>
 * This controller teleport the player each tick at his standing position, relative to the entity, computed when he landed on the entity <br>
 * WalkingOnPlayerControllers are added by the {@link IRotatedCollisionHandler}, and removed when the player moves
 */
public class WalkingOnPlayerController {
    public static WalkingOnPlayerController controller;

    public HmPlayerEntity player;
    public PhysicsEntity<?> entity;
    public HmOrientation face;
    public Vector3f offset;

    public WalkingOnPlayerController(HmPlayerEntity player, PhysicsEntity<?> entity, HmOrientation face, Vector3f offset) {
        this.player = player;
        this.entity = entity;
        this.face = face;
        this.offset = offset;
        if (DynamXContext.getPlayerToCollision().containsKey(player)) {
            DynamXContext.getPlayerToCollision().get(player).removeFromWorld(false, player.getHmWorld());
        }
    }

    /**
     * Teleport the player to the right pos and disables arms animation
     */
    public void applyOffset() {
        Vector3f newPos = JmeVector3fPool.get((float) entity.getPosX(), (float) entity.getPosY(), (float) entity.getPosZ());
        newPos.addLocal(DynamXGeometry.rotateVectorByQuaternion(offset, entity.physicsRotation));
        player.prevPosX = player.posX;
        player.prevPosY = player.posY;
        player.prevPosZ = player.posZ;
        player.setPosition(newPos.x, newPos.y, newPos.z);
        player.limbSwingAmount = player.limbSwing = player.prevLimbSwingAmount = 0;
    }

    /**
     * Should be called on the client of the player holding this controller <br>
     * Syncs the state of the controller, and restores player rigid body
     */
    public void disable() {
        controller = null;
        entity.walkingOnPlayers.remove(player);
        DynamXContext.getWalkingPlayers().remove(player);
        entity.getSynchronizer().onWalkingPlayerChange(player.getEntityId(), offset, (byte) -1);
        if (!player.isRiding() && DynamXContext.getPlayerToCollision().containsKey(player)) {
            DynamXContext.getPlayerToCollision().get(player).addToWorld();
        }
    }
}
