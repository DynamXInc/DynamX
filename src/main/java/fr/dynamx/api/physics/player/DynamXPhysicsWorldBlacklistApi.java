package fr.dynamx.api.physics.player;

import net.minecraft.entity.player.EntityPlayer;

/**
 * An api class to exclude players from physics world
 *
 * @see IPhysicsWorldBlacklist
 */
public class DynamXPhysicsWorldBlacklistApi {

    /**
     * Condition
     */
    private static IPhysicsWorldBlacklist blackListCondition = player -> false;

    /**
     * @param blackList - Blacklist that will be applied to DynamX
     */
    public static void setPhysicsWorldBlackListCondition(IPhysicsWorldBlacklist blackList) {
        DynamXPhysicsWorldBlacklistApi.blackListCondition = blackList;
    }

    /**
     * @return - Active PhysicsWorldBlacklist Condition
     */
    public static IPhysicsWorldBlacklist getActiveCondition() {
       return DynamXPhysicsWorldBlacklistApi.blackListCondition;
    }

    /**
     * @param player - The player that will be added in PhysicsWorld
     * @return - Player is blacklisted from world
     */
    public static boolean isBlacklisted(EntityPlayer player) {
        return DynamXPhysicsWorldBlacklistApi.blackListCondition.isBlacklisted(player);
    }

}
