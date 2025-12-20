package fr.dynamx.core.utils.client;

import fr.dynamx.core.common.DynamXContext;
import fr.dynamx.core.common.network.packets.MessageAttachTrailer;
import fr.dynamx.core.utils.DynamXUtils;
import fr.dynamx.core.utils.maths.DynamXGeometry;
import fr.dynamx.core.utils.maths.DynamXMath;
import fr.dynamx.core.utils.optimization.GlQuaternionPool;
import fr.dynamx.core.utils.optimization.QuaternionPool;
import fr.hermes.api.mc.world.HmWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3i;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Quaternion;

import java.nio.FloatBuffer;

/**
 * Some client interpolation utils
 *
 * @see DynamXRenderUtils
 */
public class ClientDynamXUtils {

    private static final FloatBuffer EMPTY_FLOAT_BUFFER = BufferUtils.createFloatBuffer(16);

    /**
     * Interpolation (slerp) between prevRotation and rotation jme quaternions
     *
     * @param step Step, between 0 and 1
     * @return The interpolated quaternion, in lwjgl class
     */
    public static Quaternionf computeInterpolatedJomlQuaternion(com.jme3.math.Quaternion prevRotation, com.jme3.math.Quaternion rotation, float step) {
        return DynamXUtils.toQuaternion(computeInterpolatedGlQuaternion(prevRotation, rotation, step, false));
    }

    public static Quaternionf computeInterpolatedJomlQuaternion(com.jme3.math.Quaternion prevRotation, com.jme3.math.Quaternion rotation, float step, boolean inverse) {
        return DynamXUtils.toQuaternion(computeInterpolatedGlQuaternion(prevRotation, rotation, step, inverse));
    }


    /**
     * Interpolation (slerp) between prevRotation and rotation jme quaternions
     *
     * @param step Step, between 0 and 1
     * @return The interpolated quaternion, in lwjgl class
     */
    public static Quaternion computeInterpolatedGlQuaternion(com.jme3.math.Quaternion prevRotation, com.jme3.math.Quaternion rotation, float step) {
        return computeInterpolatedGlQuaternion(prevRotation, rotation, step, false);
    }

    /**
     * Interpolation (slerp) between prevRotation and rotation jme quaternions <br>
     * The result is returned and stored into glQuatCache <br>
     * <strong>You should open a GlQuaternionPool before calling this, and the returned Quaternion belongs to this pool</strong>
     *
     * @param step    Step, between 0 and 1
     * @param inverse If true the returned quaternion will be inverted (opposite rotation)
     * @return The interpolated quaternion, in lwjgl class, inverted if required
     */
    public static Quaternion computeInterpolatedGlQuaternion(com.jme3.math.Quaternion prevRotation, com.jme3.math.Quaternion rotation, float step, boolean inverse) {
        com.jme3.math.Quaternion cache = QuaternionPool.get();
        DynamXMath.slerp(step, prevRotation, rotation, cache);
        if (inverse)
            DynamXGeometry.inverseQuaternion(cache, cache);
        return GlQuaternionPool.get(cache);
    }

    @SideOnly(Side.CLIENT)
    public static int getLightNear(HmWorld world, Vector3i pos, int horizontalRadius, int maxHeight) {
        if (!world.hm$getBlockState(pos).isOpaqueCube()) {
            return world.getCombinedLight(pos, 0);
        }
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos(pos);
        for (int y = 0; y <= maxHeight; y++) {
            for (int x = -horizontalRadius; x <= horizontalRadius; x++) {
                for (int z = -horizontalRadius; z <= horizontalRadius; z++) {
                    blockpos$mutableblockpos.setPos(x + pos.getX(), y + pos.getY(), z + pos.getZ());
                    if (!world.hm$getBlockState(blockpos$mutableblockpos).isOpaqueCube()) {
                        return world.getCombinedLight(blockpos$mutableblockpos, 0);
                    }
                }
            }
        }
        return 0;
    }

    public static void attachTrailer(){
        DynamXContext.getNetwork().sendToServer(new MessageAttachTrailer());
    }

    public static Quaternion inverseGlQuaternion(Quaternion quat, Quaternion result) {
        float norm = quat.length();
        if (norm > 0.0) {
            float invNorm = 1.0f / norm;
            result.set(-quat.getX() * invNorm, -quat.getY() * invNorm, -quat.getZ() * invNorm, quat.getW() * invNorm);
        }
        return result;
    }

    public static FloatBuffer getMatrixBuffer(Matrix4f matrix) {
        EMPTY_FLOAT_BUFFER.clear();
        return matrix.get(EMPTY_FLOAT_BUFFER);
    }
}