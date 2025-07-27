package fr.dynamx.common.physics.terrain.computing;

import com.jme3.bullet.collision.shapes.infos.IndexedMesh;
import com.jme3.math.Vector3f;
import com.jme3.util.BufferUtils;
import fr.dynamx.api.physics.terrain.ITerrainElement;
import fr.dynamx.common.physics.terrain.element.VehicleTerrainElement;
import fr.dynamx.utils.VerticalChunkPos;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class SmoothTerrainCalculator {

    private static final float RAMP_THRESHOLD_MIN = 0.25f;
    private static final float RAMP_THRESHOLD_MAX = 1.0f;

    public static ITerrainElement generateTerrainMesh(VerticalChunkPos myPos, World mcWorld) {
        int chunkX = myPos.x * 16;
        int chunkY = myPos.y * 16;
        int chunkZ = myPos.z * 16;

        float[][] heightmap = new float[18][18];
        for (int x = 0; x < 18; x++) {
            for (int z = 0; z < 18; z++) {
                heightmap[x][z] = sampleCornerHeight(mcWorld, chunkX + x - 1, chunkZ + z - 1, chunkY, chunkY + 16);
            }
        }

        applySmoothing(heightmap);

        Vector3f[][] vertices = new Vector3f[18][18];
        for (int x = 0; x < 18; x++) {
            for (int z = 0; z < 18; z++) {
                vertices[x][z] = new Vector3f(chunkX + x - 1, heightmap[x][z], chunkZ + z - 1);
            }
        }

        List<Vector3f> vertexList = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        for (int x = 0; x < 17; x++) {
            for (int z = 0; z < 17; z++) {
                Vector3f v00 = vertices[x][z];
                Vector3f v10 = vertices[x + 1][z];
                Vector3f v01 = vertices[x][z + 1];
                Vector3f v11 = vertices[x + 1][z + 1];
                addQuad(vertexList, indices, v00, v10, v11, v01);
            }
        }

        if (vertexList.isEmpty()) {
            return null;
        }

        FloatBuffer positions = BufferUtils.createFloatBuffer(vertexList.size() * 3);
        for (Vector3f v : vertexList) {
            positions.put(v.x).put(v.y).put(v.z);
        }
        positions.flip();

        IntBuffer idx = BufferUtils.createIntBuffer(indices.size());
        for (Integer i : indices) {
            idx.put(i);
        }
        idx.flip();

        IndexedMesh mesh = new IndexedMesh(positions, idx);


        return new VehicleTerrainElement(mesh);
    }

    static void applySmoothing(float[][] heightmap) {
        final int SIZE = heightmap.length;
        float[][] newHeights = new float[SIZE][SIZE];

        for (int x = 0; x < SIZE; x++) {
            System.arraycopy(heightmap[x], 0, newHeights[x], 0, SIZE);
        }

        // Horizontal edges  (x , z)  –>  (x+1 , z)
        for (int x = 0; x < SIZE - 1; x++) {
            for (int z = 0; z < SIZE; z++) {
                smoothPair(x, z, x + 1, z, heightmap, newHeights);
            }
        }
        // Vertical edges  (x , z)  –>  (x , z+1)
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE - 1; z++) {
                smoothPair(x, z, x, z + 1, heightmap, newHeights);
            }
        }

        for (int x = 0; x < SIZE; x++) {
            System.arraycopy(newHeights[x], 0, heightmap[x], 0, SIZE);
        }
    }

    private static void smoothPair(int xA, int zA, int xB, int zB,
                                   float[][] src, float[][] dst) {

        float hA = src[xA][zA];
        float hB = src[xB][zB];
        float diff = hA - hB;
        float abs = Math.abs(diff);

        if (abs <= RAMP_THRESHOLD_MIN) {
            float avg = 0.5f * (hA + hB);
            dst[xA][zA] = Math.max(dst[xA][zA], avg);
            dst[xB][zB] = Math.max(dst[xB][zB], avg);

        } else if (abs <= RAMP_THRESHOLD_MAX) {
            float mid = 0.5f * (hA + hB);
            if (hA < hB) {
                dst[xA][zA] = Math.max(dst[xA][zA], mid);
            } else {
                dst[xB][zB] = Math.max(dst[xB][zB], mid);
            }
        }
    }

    private static void addQuad(List<Vector3f> vertexList, List<Integer> indices, Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4) {
        int i1 = getIndex(vertexList, v1);
        int i2 = getIndex(vertexList, v2);
        int i3 = getIndex(vertexList, v3);
        int i4 = getIndex(vertexList, v4);

        indices.add(i1);indices.add(i2);indices.add(i3);
        indices.add(i1);indices.add(i3);indices.add(i4);
    }

    private static int getIndex(List<Vector3f> vertexList, Vector3f v) {
        int index = vertexList.indexOf(v);
        if (index == -1) {
            index = vertexList.size();
            vertexList.add(v);
        }
        return index;
    }

    private static float sampleCornerHeight(World world, int x, int z, int minY, int maxY) {
        float h1 = getTopSolidBlockY(world, x, z, minY, maxY);
        float h2 = getTopSolidBlockY(world, x - 1, z, minY, maxY);
        float h3 = getTopSolidBlockY(world, x, z - 1, minY, maxY);
        float h4 = getTopSolidBlockY(world, x - 1, z - 1, minY, maxY);
        return Math.max(Math.max(h1, h2), Math.max(h3, h4));
    }

    private static float getTopSolidBlockY(World world, int x, int z, int minY, int maxY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, maxY, z);

        if (world.isBlockLoaded(pos)) {
            for (int y = world.getChunk(pos).getTopFilledSegment() + 15; y >= minY; y--) {

                pos.setY(y);
                IBlockState state = world.getBlockState(pos);
                Block block = state.getBlock();

                if (block.getMaterial(state) != Material.AIR
                        && state.getMaterial().isSolid()
                        && !block.isFoliage(world, pos)) {

                    return y + (float) state.getBoundingBox(world, pos).maxY;
                }
            }
        }
        return minY;
    }
}


