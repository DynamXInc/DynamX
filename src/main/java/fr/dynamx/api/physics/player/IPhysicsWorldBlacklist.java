package fr.dynamx.api.physics.player;

import net.minecraft.entity.player.EntityPlayer;

public interface IPhysicsWorldBlacklist {

    boolean isBlacklisted(EntityPlayer player);

}
