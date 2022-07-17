package fr.dynamx.common.physics.terrain.computing;

import com.jme3.math.Vector3f;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.physics.terrain.element.SlopeFace;
import fr.dynamx.common.physics.terrain.element.SlopeTerrainElement;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * @deprecated Not used. WIP.
 */
@Deprecated
public class SlopesCalculatron {
    /*


        //on commence à placer nos pentes
        //variables qu'on déclare maintenant qui nous serviront plus tard pour les pentes
        if (!ContentPackLoader.PLACE_SLOPES || ContentPackLoader.slopes.isEmpty()) return;
        profiler.start(Profiler.Profiles.SLOPE_CALCULUS);
        byte tx = 0, ty = 0;

        for (MutableBoundingBox bb : outListMutable) {
            if (bb.maxX - bb.minX == 1 && bb.maxZ - bb.minZ == 1)
                continue;//on s'assure qu'on check une plateforme suffisament large
            int y = (int) (Math.ceil(bb.maxY) - 1); //on va forcément tout en haut de notre plateforme
            long poolId = Vector3fPool.findFreePool();
            SlopeTerrainElement bu = new SlopeTerrainElement(poolId, null);
            for (int x = (int) bb.minX; x < bb.maxX; x++) {
                l:
                for (int z = (int) bb.minZ; z < bb.maxZ; z++) {
                    if (x < bb.maxX - 1 && x > bb.minX + 1 && z < bb.maxZ - 1 && z > bb.minZ + 1)
                        continue;// si a l'intérieur de la box on next
                    IBlockState iblockstate1 = world.getBlockState(mutable.setPos(x, y, z));// On passe le mutable en position x y z
                    if (!ContentPackLoader.slopes.contains(iblockstate1.getBlock()))
                        continue;
                    mutable.setPos(x, y + 1, z);

                    if (world.getBlockState(mutable).getBlock() == Blocks.AIR) // SI BLOC AU DESSUS EST UN BLOC
                        // D'AIR
                        if (iblockstate1.getBlock() != Blocks.AIR) {// SI PAS UN BLOC D'AIR

                            byte X = 0;
                            byte Y = 0;
                            if (x == bb.minX) X = -1;
                            if (x == bb.maxX - 1) X = 1;
                            if (z == bb.minZ) Y = -1;
                            if (z == bb.maxZ - 1) Y = 1;

                            byte kz = (byte) (bb.maxZ - bb.minZ == 1 ? 2 : 1);
                            byte kx = (byte) (bb.maxX - bb.minX == 1 ? 2 : 1);

                            for (int d = 0; d < kx; d++) {
                                for (int f = 0; f < kz; f++) {

                                    if (d == 1) X = (byte) -X;
                                    if (f == 1) Y = (byte) -Y;

                                    boolean simple = X + Y == 1 || X + Y == -1;// pente simple ?

                                    byte[] a = getMaxLength(x, y, z, X, Y, world, mutable, iblockstate1);
                                    boolean pushSlopeUp = a[1] == 1;
                                    boolean place = true;
                                    if (a[0] > 0) {
                                        if (!simple) {
                                            if (false)
                                                if (!(world.getBlockState(mutableSlopes1.setPos(x, y, z + Y)).isFullBlock()) && !(world.getBlockState(mutableSlopes1.setPos(x + X, y, z)).isFullBlock())) {
                                                    //La configuration permet le placement de la pente
                                                    byte u = a[2];
                                                    byte v = a[3];
                                                    mutableSlopes1.setPos(x + X, y, z + Y);
                                                    addSlope(mutableSlopes1, X, Y, (byte) (simple ? 1 : 2), u, v,
                                                            iblockstate1.isFullBlock() && !pushSlopeUp, pushSlopeUp, bu);
                                                }
                                        } else {
                                            mutableSlopes1.setPos(x + X, y, z + Y);
                                            addSlope(mutableSlopes1, X, Y, (byte) (simple ? 1 : 2), (byte) a[0],
                                                    iblockstate1.isFullBlock() && !pushSlopeUp, pushSlopeUp, bu);
                                        }
                                    }

                                    if (!simple) {
                                        //on va dans les deux direction orthogonales au coin
                                        byte X2 = (byte) (-1 * X);
                                        byte Y2 = (byte) (-1 * Y);


                                        //1 : +X2
                                        a = getMaxLength(x, y, z, (byte) 0, Y, world, mutable, iblockstate1);
                                        if (a[0] > 0) {

                                            pushSlopeUp = a[1] == 1;
                                            place = true;
                                            mutableSlopes1.setPos(x, y, z + Y);
                                            addSlope(mutableSlopes1, (byte) 0, Y, (byte) 1, (byte) a[0],
                                                    iblockstate1.isFullBlock() && !pushSlopeUp, pushSlopeUp, bu);
                                        }
                                        //2 : +Y2
                                        a = getMaxLength(x, y, z, (byte) X, (byte) (Y + Y2), world, mutable, iblockstate1);
                                        if (a[0] > 0) {
                                            pushSlopeUp = a[1] == 1;
                                            place = true;
                                            mutableSlopes1.setPos(x + X, y, z + (Y + Y2));
                                            addSlope(mutableSlopes1, X, (byte) (Y + Y2), (byte) (1), (byte) a[0],
                                                    iblockstate1.isFullBlock() && !pushSlopeUp, pushSlopeUp, bu);
                                        }

                                    }
                                }

                            }


                        }
                }
            }
            if (!bu.empty()) {
                slopes.add(bu);
            } else
                bu.clear();
        }

        mutable.release();
        mutableSlopes1.release();
        mutableSlopes2.release();
        mutableSlopes3.release();
        mutableSlopes4.release();
        profiler.end(Profiler.Profiles.SLOPE_CALCULUS);
     */

    //si y'a un truc à optimiser c'est ça
    private static byte[] getMaxLength(int x, int y, int z, byte X, byte Y, World world, BlockPos.MutableBlockPos mutable,
                                       IBlockState iblockstate1) {
        final boolean simple = X + Y == 1 || X + Y == -1;// pente simple ?
        byte l = 0, u = 0, v = 0;
        final byte max_length = (byte) ContentPackLoader.SLOPES_LENGTH;//la valeur permet de définir la longueur max des pentes
        boolean pushSlopeUp = false;
        if (simple) {
            m:
            while (l <= max_length) {
                for (byte i = 0; i <= 2; i++) {
                    mutable.setPos(x + X * (l + 1), y - 1 + i, z + Y * (l + 1));
                    IBlockState bs = world.getBlockState(mutable);
                    switch (i) {
                        case 0: // on est en dessous : si c'est pas solide on break
                            if (!bs.isFullBlock()) {

                                break m;
                            }
                            break;
                        case 1: // pas factorisé mais question d'opti
                            // on est au mm niveau
                            if (bs.isFullBlock()) // si voie bloqué pas de pentes dans tous les cas
                            {
                                l = 0;
                                break m;
                            } else if (iblockstate1.isFullBlock()) {// si bloc de base est plein
                                if (bs.getProperties().containsKey(BlockSlab.HALF) && bs
                                        .getProperties()
                                        .get(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.BOTTOM) {
                                    pushSlopeUp = true;
                                } else if (pushSlopeUp) {
                                    // sinon si bloc d'air et avant une slab avant c'est pas bon on
                                    // stop;
                                    break m;
                                } // sinon ca veut dire que c que des blocs d'air c'est good on
                                // continue

                            } else {// si bloc de base est une slab on se comporte différemment
                                if (bs.getBlock() != Blocks.AIR)
                                    break m;// si slab a coté de slab on zappe
                            }
                            break;

                        case 2: // on est au dessus : si voie bloqué pas de pentes
                            if (bs.getBlock() != Blocks.AIR) {

                                l = 0;
                                break m;
                            }
                            break;
                    }

                }
                l++;
            }
        } else {
            m:
            while (l <= max_length) {
                for (u = 0; u < l; u++) {
                    for (v = 0; v < l; v++) {
                        for (byte i = 0; i <= 2; i++) {
                            mutable.setPos(x + X * (u + 1), y - 1 + i, z + Y * (v + 1));
                            IBlockState bs = world.getBlockState(mutable);
                            switch (i) {
                                case 0: // on est en dessous : si c'est pas solide on break
                                    if (!bs.isFullBlock()) {
                                        break m;
                                    }
                                    break;
                                case 1: // pas factorisé mais question d'opti
                                    // on est au mm niveau
                                    if (bs.isFullBlock()) // si voie bloqué pas de pentes dans tous les cas
                                    {
                                        break m;
                                    } else if (iblockstate1.isFullBlock()) {// si bloc de base est plein

                                        if (bs.getProperties().containsKey(BlockSlab.HALF) && bs
                                                .getProperties()
                                                .get(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.BOTTOM) {
                                            pushSlopeUp = true;
                                        } else if (pushSlopeUp) {
                                            // sinon si bloc d'air et avant une slab avant c'est pas bon on
                                            // stop;
                                            break m;
                                        } // sinon ca veut dire que c que des blocs d'air c'est good on
                                        // continue

                                    } else {// si bloc de base est une slab on se comporte différemment
                                        if (bs.getBlock() != Blocks.AIR)
                                            break m;// si slab a coté de slab on zappe
                                    }
                                    break;

                                case 2: // on est au dessus : si voie bloqué pas de pentes
                                    if (bs.getBlock() != Blocks.AIR) {
                                        l = 0;
                                        break m;
                                    }
                                    break;
                            }
                        }
                    }


                }
                l++;
            }
            l--;
        }
        return new byte[]{l, (byte) (pushSlopeUp ? 1 : 0), u, v};
    }

    // ajout d'une slope à une certaine position, selon une directino, et pour une
    // certaine longueur
    // 0 south, 1 west,2north 3 east
    private static void addSlope(BlockPos.PooledMutableBlockPos blockpos, byte X, byte Y, byte type, byte u, byte v,
                                 boolean full, boolean up, SlopeTerrainElement builder) {
        if (X == 0 && Y == 0) return;
        Vector3f pos = Vector3fPool.get(blockpos.getX(), blockpos.getY() + (up ? 0.5F : 0F), blockpos.getZ());
        SlopeFace sh = new SlopeFace().buildShape(pos, X, Y, type, u, v, full, builder.getPoolId());
        builder.addSlope(sh);
    }

    // ajout d'une slope à une certaine position, selon une directino, et pour une
    // certaine longueur
    // 0 south, 1 west,2north 3 east
    private static void addSlope(BlockPos.PooledMutableBlockPos blockpos, byte X, byte Y, byte type, byte length,
                                 boolean full, boolean up, SlopeTerrainElement builder) {
        addSlope(blockpos, X, Y, type, length, length, full, up, builder);
    }
}
