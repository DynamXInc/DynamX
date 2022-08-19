package fr.dynamx.api.physics.player;

import net.minecraft.entity.player.EntityPlayer;

public class DynamXPhysicsWorldBlacklistApi {

    private static IPhysicsWorldBlacklist blackListCondition;

    public static void setPhysicsWorldBlackListCondition(IPhysicsWorldBlacklist blackList) {
        DynamXPhysicsWorldBlacklistApi.blackListCondition = blackList;
    }

    public static boolean isBlacklisted(EntityPlayer player) {
        if(DynamXPhysicsWorldBlacklistApi.blackListCondition == null) return false;
        return DynamXPhysicsWorldBlacklistApi.blackListCondition.isBlacklisted(player);
    }

}
