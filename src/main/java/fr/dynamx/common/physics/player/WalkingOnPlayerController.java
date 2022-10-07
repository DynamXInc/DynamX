package fr.dynamx.common.physics.player;

import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.IRotatedCollisionHandler;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;

/**
 * Responsible to update a walking player <br>
 * A walking player is a player standing on the top of a {@link PhysicsEntity} <br>
 * This controller teleport the player each tick at his standing position, relative to the entity, computed when he landed on the entity <br>
 * WalkingOnPlayerControllers are added by the {@link IRotatedCollisionHandler}, and removed when the player moves
 */
public class WalkingOnPlayerController {
    public static WalkingOnPlayerController controller;

    public EntityPlayer player;
    public PhysicsEntity<?> entity;
    public EnumFacing face;
    public Vector3f offset;

    public WalkingOnPlayerController(EntityPlayer player, PhysicsEntity<?> entity, EnumFacing face, Vector3f offset) {
        this.player = player;
        this.entity = entity;
        this.face = face;
        this.offset = offset;
        if (DynamXContext.getPlayerToCollision().containsKey(player)) {
            DynamXContext.getPlayerToCollision().get(player).removeFromWorld(false);
        }
    }

    /**
     * Teleport the player to the right pos and disables arms animation
     */
    public void applyOffset() {
        Vector3f newPos = Vector3fPool.get((float) entity.posX, (float) entity.posY, (float) entity.posZ);
        newPos.addLocal(DynamXGeometry.rotateVectorByQuaternion(offset, entity.physicsRotation));//PhysicsHelper.getRotatedPoint(offset, -entity.rotationPitch, entity.rotationYaw, entity.rotationRoll));
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
