package fr.hermes.api.mc.utils;

import net.minecraft.util.EnumFacing;

public interface HmBlockPos extends HmVec3i {
    HmBlockPos hm$add(int x, int y, int z);

    default HmBlockPos hm$up() {
        return this.hm$up(1);
    }

    default HmBlockPos hm$up(int n) {
        return this.hm$offset(EnumFacing.UP, n);
    }

    default HmBlockPos hm$down() {
        return this.hm$down(1);
    }

    default HmBlockPos hm$down(int n) {
        return this.hm$offset(EnumFacing.DOWN, n);
    }

    default HmBlockPos hm$north() {
        return this.hm$north(1);
    }

    default HmBlockPos hm$north(int n) {
        return this.hm$offset(EnumFacing.NORTH, n);
    }

    default HmBlockPos hm$south() {
        return this.hm$south(1);
    }

    default HmBlockPos hm$south(int n) {
        return this.hm$offset(EnumFacing.SOUTH, n);
    }

    default HmBlockPos hm$west() {
        return this.hm$west(1);
    }

    default HmBlockPos hm$west(int n) {
        return this.hm$offset(EnumFacing.WEST, n);
    }

    default HmBlockPos hm$east() {
        return this.hm$east(1);
    }

    default HmBlockPos hm$east(int n) {
        return this.hm$offset(EnumFacing.EAST, n);
    }

    default HmBlockPos hm$offset(EnumFacing facing) {
        return this.hm$offset(facing, 1);
    }

    HmBlockPos hm$offset(EnumFacing facing, int n);

    interface HmMutableBlockPos extends HmBlockPos {
        HmMutableBlockPos hm$setPos(int xIn, int yIn, int zIn);

        HmMutableBlockPos hm$setPos(double xIn, double yIn, double zIn);

        default HmBlockPos hm$setPos(HmBlockPos pos) {
            return hm$setPos(pos.hm$getX(), pos.hm$getY(), pos.hm$getZ());
        }
    }
}
