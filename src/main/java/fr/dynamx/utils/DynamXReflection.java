package fr.dynamx.utils;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static fr.dynamx.common.DynamXMain.log;

public class DynamXReflection {
    public static Method updateFallState, dealFireDamage, canTriggerWalking, doBlockCollisions, playStepSound, makeFlySound, playFlySound;
    public static Method worldIsChunkLoaded;

    public static void initReflection() {
        log.debug("---- Start Reflection ----");

        updateFallState = findMethod(Entity.class, "func_184231_a", void.class,
                double.class, boolean.class, IBlockState.class, BlockPos.class);
        dealFireDamage = findMethod(Entity.class, "func_70081_e", void.class,
                int.class);
        canTriggerWalking = findMethod(Entity.class, "func_70041_e_", boolean.class);
        doBlockCollisions = findMethod(Entity.class, "func_145775_I", void.class);
        playStepSound = findMethod(Entity.class, "func_180429_a", void.class,
                BlockPos.class, Block.class);
        playFlySound = findMethod(Entity.class, "func_191954_d", float.class,
                float.class);
        makeFlySound = findMethod(Entity.class, "func_191957_ae", boolean.class);
        worldIsChunkLoaded = findMethod(World.class, "func_175680_a", boolean.class, int.class, int.class, boolean.class);

        log.debug("---- End Reflection ----");

        log.info("Successfully applied reflection");
    }

    public static Method findMethod(Class<?> clazz, String srgName, Class<?> returnType, Class<?>... parameterTypes) {
        log.debug(srgName);
        return ObfuscationReflectionHelper.findMethod(clazz, srgName, returnType, parameterTypes);
    }

    public static Object invokeMethod(Method method, Object obj, Object... args) {
        try {
            return args.length > 0 ? method.invoke(obj, args) : method.invoke(obj);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return obj;
        }
    }

    public static String getCreativeTabName(CreativeTabs tab) {
        //System.out.println("Name of "+tab+" is "+ObfuscationReflectionHelper.getPrivateValue(CreativeTabs.class, tab, 15));
        return ObfuscationReflectionHelper.getPrivateValue(CreativeTabs.class, tab, 15);
    }
}
