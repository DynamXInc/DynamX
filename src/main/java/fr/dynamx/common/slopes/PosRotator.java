package fr.dynamx.common.slopes;

import com.jme3.math.Vector3f;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class PosRotator
{
    private final EnumFacing direction;

    public PosRotator(EnumFacing direction) {
        this.direction = direction;
    }

    public BlockPos.MutableBlockPos mute(int i, BlockPos.MutableBlockPos from) {
        switch (direction)
        {
            case NORTH:
                from.setPos(from.getX(), from.getY(), from.getZ()-i);
                break;
            case SOUTH:
                from.setPos(from.getX(), from.getY(), from.getZ()+i);
                break;
            case EAST:
                from.setPos(from.getX()+i, from.getY(), from.getZ());
                break;
            case WEST:
                from.setPos(from.getX()-i, from.getY(), from.getZ());
                break;
        }
        return from;
    }

    public BlockPos mute(int i, int x, int y, int z) {
        switch (direction)
        {
            case NORTH:
                return new BlockPos(x, y, z-i);
            case SOUTH:
                return new BlockPos(x, y, z+i);
            case EAST:
                return new BlockPos(x+i, y, z);
            case WEST:
                return new BlockPos(x-i, y, z);
        }
        return new BlockPos(x, y, z);
    }
    public Vector3f mute(int i, int x, float y, int z) {
        switch (direction)
        {
            case NORTH:
                return Vector3fPool.get(x, y, z-i);
            case SOUTH:
                return Vector3fPool.get(x, y, z+i);
            case EAST:
                return Vector3fPool.get(x+i, y, z);
            case WEST:
                return Vector3fPool.get(x-i, y, z);
        }
        return Vector3fPool.get(x, y, z);
    }

    public int getLittleX(int xstart, int xend) {
        return direction.getAxis() == EnumFacing.Axis.X ? (direction.getAxisDirection() == EnumFacing.AxisDirection.NEGATIVE ? Math.max(xstart, xend) : Math.min(xstart, xend)) :  Math.min(xstart, xend);
    }

    public int getLittleZ(int zstart, int zend) {
        return direction.getAxis() == EnumFacing.Axis.Z ? (direction.getAxisDirection() == EnumFacing.AxisDirection.NEGATIVE ? Math.max(zstart, zend) : Math.min(zstart, zend)) : Math.min(zstart, zend);
    }

    public int getBigX(int xstart, int xend) {
        return direction.getAxis() == EnumFacing.Axis.X ? (direction.getAxisDirection() == EnumFacing.AxisDirection.NEGATIVE ? Math.min(xstart, xend) : Math.max(xstart, xend)) : Math.max(xstart, xend);
    }

    public int getBigZ(int zstart, int zend) {
        return direction.getAxis() == EnumFacing.Axis.Z ? (direction.getAxisDirection() == EnumFacing.AxisDirection.NEGATIVE ? Math.min(zstart, zend) : Math.max(zstart, zend)) : Math.max(zstart, zend);
    }

    /*public boolean lessOrEqualsX(int x1, int x2, int z1, int z2) {
        switch (direction)
        {
            case NORTH:
                return z1 >= z2;
            case SOUTH:
                return z1 <= z2;
            case WEST:
                return x1 >= x2;
            case EAST:
                return x1 <= x2;
        }
        return false;
    }

    public boolean lessOrEqualsZ(int x1, int x2, int z1, int z2) {
        return lessOrEqualsX(z1, z2, x1, x2);
    }*/

    public int getTheDir(BlockPos from) {
        switch (direction)
        {
            case NORTH:
            case SOUTH:
                return from.getZ();
        }
        return from.getX();
    }
    public int getTheDir(Vector3f from) {
        switch (direction)
        {
            case NORTH:
            case SOUTH:
                return (int) from.z;
        }
        return (int) from.x;
    }

    public int getTheOtherDir(BlockPos from) {
        switch (direction)
        {
            case NORTH:
            case SOUTH:
                return from.getX();
        }
        return from.getZ();
    }
    public float getTheOtherDir(Vector3f from) {
        switch (direction)
        {
            case NORTH:
            case SOUTH:
                return from.x;
        }
        return from.z;
    }
    public BlockPos setTheOtherDir(BlockPos in, int value) {
        switch (direction)
        {
            case NORTH:
            case SOUTH:
                return new BlockPos(value, in.getY(), in.getZ());
        }
        return new BlockPos(in.getX(), in.getY(), value);
    }

    /*public int getZ(BlockPos from) {
        switch (direction)
        {
            case NORTH:
            case SOUTH:
                return from.getX();
        }
        return from.getZ();
    }*/

    /*public int followDir(int from) {
        if(direction.getAxisDirection() == EnumFacing.AxisDirection.NEGATIVE)
            return from-1;
        return from+1;
    }*/

    public int fixBorderX(int x) {
        return direction == EnumFacing.WEST ? x+1 : x;
    }
    public int fixBorderZ(int z) {
        return direction == EnumFacing.NORTH ? z+1 : z;
    }

    public int counterFixBorderX(int x) {
        return direction == EnumFacing.WEST ? x-1 : x;
    }
    public int counterFixBorderZ(int z) {
        return direction == EnumFacing.NORTH ? z-1 : z;
    }
}
