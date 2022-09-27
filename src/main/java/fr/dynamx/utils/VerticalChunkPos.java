package fr.dynamx.utils;

import java.util.Objects;

/**
 * Like {@link net.minecraft.util.math.ChunkPos}, but with an y coordinate
 */
public class VerticalChunkPos {
    /**
     * Mutable {@link VerticalChunkPos} used to find VerticalChunkPos into maps or lists
     */
    public static class Mutable {
        public int x;
        public int z;
        public int y;

        public void setPos(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof VerticalChunkPos) {
                VerticalChunkPos that = (VerticalChunkPos) o;
                return x == that.x &&
                        z == that.z &&
                        y == that.y;
            }
            if (o == null || getClass() != o.getClass()) return false;
            Mutable that = (Mutable) o;
            return x == that.x &&
                    z == that.z &&
                    y == that.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }

        @Override
        public String toString() {
            return "MutableVCP[" + this.x + ", " + y + ", " + this.z + "]";
        }

        public VerticalChunkPos toImmutable() {
            return new VerticalChunkPos(x, y, z);
        }
    }

    /**
     * The X position of this Chunk Coordinate Pair
     */
    public final int x;
    /**
     * The Z position of this Chunk Coordinate Pair
     */
    public final int z;
    /**
     * The y position (world y position divided by 16)
     */
    public final int y;
    /**
     * The hash code, to save cpu time
     */
    private final int hashCode;

    public VerticalChunkPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.hashCode = Objects.hash(x, y, z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof VerticalChunkPosContainer) {
            VerticalChunkPos that = ((VerticalChunkPosContainer) o).getPos();
            return x == that.x &&
                    z == that.z &&
                    y == that.y;
        } else if (o.getClass() == Mutable.class) {
            Mutable that = (Mutable) o;
            return x == that.x &&
                    z == that.z &&
                    y == that.y;
        } else if (o.getClass() == VerticalChunkPos.class) {
            VerticalChunkPos that = (VerticalChunkPos) o;
            return x == that.x &&
                    z == that.z &&
                    y == that.y;
        } else
            return false;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "VCP[" + this.x + ", " + y + ", " + this.z + "]";
    }

    /**
     * The {@link VerticalChunkPos} container, used for easy comparison and usage in lists/hash maps.
     */
    public interface VerticalChunkPosContainer {
        VerticalChunkPos getPos();

        default boolean posEquals(Object o) {
            if (this == o) return true;
            if (o.getClass() == VerticalChunkPos.class)
                return VerticalChunkPos.equals(getPos(), (VerticalChunkPos) o);
            else if (o instanceof VerticalChunkPos.VerticalChunkPosContainer)
                return VerticalChunkPos.equals(getPos(), ((VerticalChunkPos.VerticalChunkPosContainer) o).getPos());
            return false;
        }
    }

    /**
     * @return True if each coordinate of the pos1 is equal to the other
     */
    public static boolean equals(VerticalChunkPos pos1, VerticalChunkPos pos2) {
        return pos1.x == pos2.x && pos1.y == pos2.y && pos1.z == pos2.z;
    }
}