package fr.dynamx.api.physics.player;

import fr.hermes.api.mc.entities.HmPlayerEntity;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Interface for PhysicsWorld Blacklist
 */
public interface IPhysicsWorldBlacklist {
    /**
     * @param player - The Player that will be added in Physics World
     * @return isBlacklisted from PhysicsWorld
     */
    boolean isBlacklisted(HmPlayerEntity player);
}
