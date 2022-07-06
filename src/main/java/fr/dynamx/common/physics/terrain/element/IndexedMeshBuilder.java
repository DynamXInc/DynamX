package fr.dynamx.common.physics.terrain.element;

import com.jme3.bullet.collision.shapes.infos.IndexedMesh;
import com.jme3.math.Vector3f;
import fr.dynamx.common.physics.utils.StairsBox;
import fr.dynamx.utils.debug.TerrainDebugData;
import fr.dynamx.utils.debug.TerrainDebugRenderer;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Provides methods to convert boxes and faces into triangles (arrays of points) that you can used with {@link IndexedMesh}
 */
public class IndexedMeshBuilder
{
    private final int x, y, z;
    private final List<IndexedMesh> meshes = new ArrayList<>();
    private final Map<Integer, TerrainDebugData> debugData;
    /** Temporary vector helping to build the faces */
    private final Vector3f min = Vector3fPool.get();
    /** Temporary vector helping to build the faces */
    private final Vector3f max = Vector3fPool.get();

    /**
     * Creates a new IndexedMeshBuilder capable to convert lists of boxes into IndexedMeshes, applying the given offset to all boxes
     * @param debugData A map where all boxes are added with no offset, useful to display them, nullable
     */
    public IndexedMeshBuilder(int offsetX, int offsetY, int offsetZ, @Nullable Map<Integer, TerrainDebugData> debugData)
    {
        this.x = offsetX;
        this.y = offsetY;
        this.z = offsetZ;
        this.debugData = debugData;
    }

    /**
     * Adds all boxes (split into faces) to the out list, one IndexedMesh per box
     */
    public void addBoxes(List<MutableBoundingBox> boxes)
    {
        for (MutableBoundingBox boxAABB : boxes) {
            min.set((float) (x + boxAABB.minX), (float) boxAABB.minY +y, (float) (z + boxAABB.minZ));
            max.set((float) (x + boxAABB.maxX), (float) boxAABB.maxY +y, (float) (z + boxAABB.maxZ));
            Vector3f[] triangles = new Vector3f[4*6];
            int[] indices = new int[6*6];

            appendOrientedFaceToMesh((byte) 0, min.y, max.y, min.z, max.z, min.x, triangles, indices, 0);
            appendOrientedFaceToMesh((byte) 1, min.x, max.x, min.z, max.z, min.y, triangles, indices, 1);
            appendOrientedFaceToMesh((byte) 2, min.x, max.x, min.y, max.y, min.z, triangles, indices, 2);
            appendOrientedFaceToMesh((byte) 0, min.y, max.y, min.z, max.z, max.x, triangles, indices, 3);
            appendOrientedFaceToMesh((byte) 1, min.x, max.x, min.z, max.z, max.y, triangles, indices, 4);
            appendOrientedFaceToMesh((byte) 2, min.x, max.x, min.y, max.y, max.z, triangles, indices, 5);

            meshes.add(new IndexedMesh(triangles, indices));

            if(debugData != null) {
                min.subtractLocal(x, y, z);
                max.subtractLocal(x, y, z);
                float margin = 0.01f;
                //BlockPos p = new BlockPos((min.x + max.x) / 2, (min.y + max.y) / 2, (min.z + max.z) / 2);
                /*if(debugData.containsKey(p))
                {
                    //DynamXMain.log.warn("Found duplicated collision boxes ! Please report this to Aym' "+min+" "+max+" "+boxAABB+" "+boxes+" "+p);
                    debugData.put(p.add(0, 8, 0), new float[]{min.x-margin, min.y-margin, min.z-margin, max.x+margin, max.y+margin, max.z+margin, 1, 1, 0});
                }
                else*/
                TerrainDebugData tdebugData = new TerrainDebugData(TerrainDebugRenderer.BLOCKS, new float[]{min.x-margin, min.y-margin, min.z-margin, max.x+margin, max.y+margin, max.z+margin});
                debugData.put(tdebugData.getUuid(), tdebugData);
            }
        }
    }

    /**
     * Adds all boxes (split into faces) to the out list, one IndexedMesh per box
     */
    public void addStairBoxes(List<StairsBox> boxes)
    {
        for (StairsBox boxAABB : boxes) {
            EnumFacing.Axis axis = boxAABB.getFacing().getAxis();
            min.set(x + (axis == EnumFacing.Axis.Z ? boxAABB.getMin() : boxAABB.getMinOtherCoord()), y + boxAABB.getMinY(), z + (axis == EnumFacing.Axis.X ? boxAABB.getMin() : boxAABB.getMinOtherCoord()));
            max.set(x + (axis == EnumFacing.Axis.Z ? boxAABB.getMax() : boxAABB.getMinOtherCoord()+1), y + boxAABB.getMinY() + 1, z + (axis == EnumFacing.Axis.X ? boxAABB.getMax() : boxAABB.getMinOtherCoord() + 1));
            //System.out.println("Min and max "+min+" "+max);
            Vector3f[] triangles = new Vector3f[4*6];
            int[] indices = new int[6*6];

            //appendOrientedFaceToMesh((byte) 0, min.y, max.y, min.z, max.z, min.x, triangles, indices, 0);
            appendOrientedFaceToMesh((byte) 1, min.x, max.x, min.z, max.z, boxAABB.isInverted() ? max.y : min.y, triangles, indices, 1);
            //appendOrientedFaceToMesh((byte) 2, min.x, max.x, min.y, max.y, min.z, triangles, indices, 2);
            //appendOrientedFaceToMesh((byte) 0, min.y, max.y, min.z, max.z, max.x, triangles, indices, 3);
            //appendOrientedFaceToMesh((byte) 1, min.x, max.x, min.z, max.z, max.y, triangles, indices, 4);
            //appendOrientedFaceToMesh((byte) 2, min.x, max.x, min.y, max.y, max.z, triangles, indices, 5);

            switch (boxAABB.getFacing()) {
                case EAST:
                    appendOrientedFaceToMesh((byte) 0, boxAABB.isInverted() ? min.y+0.5f : min.y, boxAABB.isInverted() ? max.y : min.y+0.5f, min.z, max.z, min.x, triangles, indices, 0);
                    appendSlopeBorderToMesh((byte) 2, min.x, max.x, boxAABB.isInverted() ? max.y : min.y, min.y+0.5f, boxAABB.isInverted() ? min.y : max.y, min.z, triangles, indices, 2);
                    appendOrientedFaceToMesh((byte) 0, min.y, max.y, min.z, max.z, max.x, triangles, indices, 3);
                    appendSlopeFaceToMesh((byte) 2, min.x, max.x, min.y + 0.5f, boxAABB.isInverted() ? min.y : max.y,
                            min.z, max.z, triangles, indices, 4, boxAABB.isInverted()); //main slope
                    appendSlopeBorderToMesh((byte) 2, min.x, max.x, boxAABB.isInverted() ? max.y : min.y, min.y+0.5f, boxAABB.isInverted() ? min.y : max.y, max.z, triangles, indices, 5);
                    break;
                case SOUTH:
                    appendSlopeBorderToMesh((byte) 0, boxAABB.isInverted() ? max.y : min.y, min.y+0.5f, min.z, max.z, boxAABB.isInverted() ? min.y : max.y, min.x, triangles, indices, 0);
                    appendOrientedFaceToMesh((byte) 2, min.x, max.x, boxAABB.isInverted() ? min.y+0.5f : min.y, boxAABB.isInverted() ? max.y : min.y+0.5f, min.z, triangles, indices, 2);
                    appendSlopeBorderToMesh((byte) 0, boxAABB.isInverted() ? max.y : min.y, min.y+0.5f, min.z, max.z, boxAABB.isInverted() ? min.y : max.y, max.x, triangles, indices, 3);
                    appendSlopeFaceToMesh((byte) 0, min.z, max.z, min.y + 0.5f, boxAABB.isInverted() ? min.y : max.y,
                            min.x, max.x, triangles, indices, 4, boxAABB.isInverted()); //main slope
                    appendOrientedFaceToMesh((byte) 2, min.x, max.x, min.y, max.y, max.z, triangles, indices, 5);
                    break;
                case WEST:
                    appendOrientedFaceToMesh((byte) 0, min.y, max.y, min.z, max.z, min.x, triangles, indices, 0);
                    appendSlopeBorderToMesh((byte) 2, min.x, max.x, boxAABB.isInverted() ? max.y : min.y, boxAABB.isInverted() ? min.y : max.y, min.y+0.5f, min.z, triangles, indices, 2);
                    appendOrientedFaceToMesh((byte) 0, boxAABB.isInverted() ? min.y+0.5f : min.y, boxAABB.isInverted() ? max.y : min.y+0.5f, min.z, max.z, max.x, triangles, indices, 3);
                    appendSlopeFaceToMesh((byte) 2, max.x, min.x, min.y + 0.5f, boxAABB.isInverted() ? min.y : max.y,
                            max.z, min.z, triangles, indices, 4, boxAABB.isInverted()); //main slope
                    appendSlopeBorderToMesh((byte) 2, min.x, max.x, boxAABB.isInverted() ? max.y : min.y, boxAABB.isInverted() ? min.y : max.y, min.y+0.5f, max.z, triangles, indices, 5);
                    break;
                case NORTH:
                    appendSlopeBorderToMesh((byte) 0, boxAABB.isInverted() ? max.y : min.y, boxAABB.isInverted() ? min.y : max.y, min.z, max.z, min.y+0.5f, min.x, triangles, indices, 0);
                    appendOrientedFaceToMesh((byte) 2, min.x, max.x, min.y, max.y, min.z, triangles, indices, 2);
                    appendSlopeBorderToMesh((byte) 0, boxAABB.isInverted() ? max.y : min.y, boxAABB.isInverted() ? min.y : max.y, min.z, max.z, min.y+0.5f, max.x, triangles, indices, 3);
                    appendSlopeFaceToMesh((byte) 0, max.z, min.z, min.y + 0.5f, boxAABB.isInverted() ? min.y : max.y,
                            min.x, max.x, triangles, indices, 4, boxAABB.isInverted()); //main slope
                    appendOrientedFaceToMesh((byte) 2, min.x, max.x, boxAABB.isInverted() ? min.y+0.5f : min.y, boxAABB.isInverted() ? max.y : min.y+0.5f, max.z, triangles, indices, 5);
                    break;
            }

            meshes.add(new IndexedMesh(triangles, indices));

            if(debugData != null) {
                /*min.subtractLocal(x, y, z);
                max.subtractLocal(x, y, z);
                float margin = 0.01f;
                //BlockPos p = new BlockPos((min.x + max.x) / 2, (min.y + max.y) / 2, (min.z + max.z) / 2);
                /*if(debugData.containsKey(p))
                {
                    //DynamXMain.log.warn("Found duplicated collision boxes ! Please report this to Aym' "+min+" "+max+" "+boxAABB+" "+boxes+" "+p);
                    debugData.put(p.add(0, 8, 0), new float[]{min.x-margin, min.y-margin, min.z-margin, max.x+margin, max.y+margin, max.z+margin, 1, 1, 0});
                }
                else*/
                //TerrainDebugData tdebugData = new TerrainDebugData(TerrainDebugRenderer.STARIS, new float[]{min.x-margin, min.y-margin, min.z-margin, max.x+margin, max.y+margin, max.z+margin});
                TerrainDebugData tdebugData = new TerrainDebugData(TerrainDebugRenderer.STAIRS, computeDebug(Vector3fPool.get(-x, -y, -z), Arrays.asList(triangles), indices));
                debugData.put(tdebugData.getUuid(), tdebugData);
            }
        }
    }

    public static void appendSlopeBorderToMesh(byte orientation, float xMin, float xMax, float yMin, float yMax1, float yMax2, float z, Vector3f[] triangles, int[] indices, int offest)
    {
        assert xMin < xMax : "bad x sign";
        assert yMin < yMax1 : "bad y1 sign";
        assert yMin < yMax2 : "bad y2 sign";
        indices[0+offest*6] = 0+offest*4;
        indices[1+offest*6] = 1+offest*4;
        indices[2+offest*6] = 2+offest*4;
        indices[3+offest*6] = 0+offest*4;
        indices[4+offest*6] = 3+offest*4;
        indices[5+offest*6] = 2+offest*4;
        //Split this face into 2 min for bullet
        switch (orientation) {
            case 0:
                triangles[0 + offest * 4] = new Vector3f(z, xMin, yMin);
                triangles[1 + offest * 4] = new Vector3f(z, xMax, yMin);
                triangles[2 + offest * 4] = new Vector3f(z, yMax2, yMax1);
                triangles[3 + offest * 4] = new Vector3f(z, xMin, yMax1);
                break;
            case 2:
                triangles[0 + offest * 4] = new Vector3f(xMin, yMin, z);
                triangles[1 + offest * 4] = new Vector3f(xMin, yMax1, z);
                triangles[2 + offest * 4] = new Vector3f(xMax, yMax2, z);
                triangles[3 + offest * 4] = new Vector3f(xMax, yMin, z);
                break;
        }
    }

    public static void appendSlopeFaceToMesh(byte orientation, float xMin, float xMax, float yMin, float yMax, float zMin, float zMax, Vector3f[] triangles, int[] indices, int offest, boolean upper)
    {
        assert yMin < yMax : "bad y sign";
        indices[0+offest*6] = 0+offest*4;
        indices[1+offest*6] = 1+offest*4;
        indices[2+offest*6] = 2+offest*4;
        indices[3+offest*6] = 0+offest*4;
        indices[4+offest*6] = 3+offest*4;
        indices[5+offest*6] = 2+offest*4;
        //Split this face into 2 min for bullet
        switch (orientation) {
            case 0:
                triangles[0 + offest * 4] = new Vector3f(zMin, yMin, xMin);
                triangles[1 + offest * 4] = new Vector3f(zMax, yMin, xMin);
                triangles[2 + offest * 4] = new Vector3f(zMax, yMax, xMax);
                triangles[3 + offest * 4] = new Vector3f(zMin, yMax, xMax);
                break;
            case 2:
                triangles[0 + offest * 4] = new Vector3f(xMin, yMin, zMin);
                triangles[1 + offest * 4] = new Vector3f(xMax, yMax, zMin);
                triangles[2 + offest * 4] = new Vector3f(xMax, yMax, zMax);
                triangles[3 + offest * 4] = new Vector3f(xMin, yMin, zMax);
                break;
        }
    }

    /**
     * @return The list of meshes got from calls of addBoxes and addMutableBoxes
     */
    public List<IndexedMesh> getMeshes()
    {
        return meshes;
    }

    /**
     * Used for simple cubic boxes, adds a face to the arrays used to create an {@link IndexedMesh}
     *
     * @param orientation This face only accepts 3 orientations, normals are on the axis : <br>
     * 0 : x axis <br>
     * 1 : y axis <br>
     * 2 : z axis <br>
     * @param xMin xMax - yMin - yMax - z : The position of min and max points of this face, "xMin" isn't the real x pos, depends on the orientation
     * @param triangles The triangles of the IndexedMesh to create
     * @param indices The indices of the IndexedMesh to create
     * @param offest The number of faces already added into the arrays, ie the number of calls of this function with the same arrays
     */
    public static void appendOrientedFaceToMesh(byte orientation, float xMin, float xMax, float yMin, float yMax, float z, Vector3f[] triangles, int[] indices, int offest)
    {
        assert xMin < xMax : "bad x sign";
        assert yMin < yMax : "bad y sign";
        indices[0+offest*6] = 0+offest*4;
        indices[1+offest*6] = 1+offest*4;
        indices[2+offest*6] = 2+offest*4;
        indices[3+offest*6] = 0+offest*4;
        indices[4+offest*6] = 3+offest*4;
        indices[5+offest*6] = 2+offest*4;
        //Split this face into 2 min for bullet
        switch (orientation) {
            case 0:
                triangles[0 + offest * 4] = new Vector3f(z, xMin, yMin);
                triangles[1 + offest * 4] = new Vector3f(z, xMax, yMin);
                triangles[2 + offest * 4] = new Vector3f(z, xMax, yMax);
                triangles[3 + offest * 4] = new Vector3f(z, xMin, yMax);
                break;
            case 1:
                triangles[0 + offest * 4] = new Vector3f(xMin, z, yMin);
                triangles[1 + offest * 4] = new Vector3f(xMax, z, yMin);
                triangles[2 + offest * 4] = new Vector3f(xMax, z, yMax);
                triangles[3 + offest * 4] = new Vector3f(xMin, z, yMax);
                break;
            case 2:
                triangles[0 + offest * 4] = new Vector3f(xMin, yMin, z);
                triangles[1 + offest * 4] = new Vector3f(xMin, yMax, z);
                triangles[2 + offest * 4] = new Vector3f(xMax, yMax, z);
                triangles[3 + offest * 4] = new Vector3f(xMax, yMin, z);
                break;
        }
    }

    /**
     * Used for slopes, adds the points of the upper and bottom face of the slope to the arrays used to create an {@link IndexedMesh} <br>
     *     If there os duplicated points, they are only added once into triangles
     *
     * @param points The points of the slope, don't care of their order
     * @param triangles The list of triangles of the IndexedMesh to create
     * @param indices The indices of the IndexedMesh to create
     * @param offest The number of slopes already added into the arrays, ie the number of calls of this function with the same arrays
     */
    public static void appendSlopePointsToMesh(Vector3f[] points, List<Vector3f> triangles, int[] indices, int offest)
    {
        //Create a map pointing to the index of the corresponding triangle in triangles list, avoiding any duplicate
        //Add the other points into the triangles list
        int[] pointMap = new int[points.length];
        for(int i=0;i<points.length;i++)
        {
            pointMap[i] = triangles.indexOf(points[i]);
            if(pointMap[i] == -1)
            {
                pointMap[i] = triangles.size();
                triangles.add(points[i]);
            }
        }
        //Upper face
        indices[0+offest*12] = pointMap[0];
        indices[1+offest*12] = pointMap[1];
        indices[2+offest*12] = pointMap[2];
        indices[3+offest*12] = pointMap[0];
        indices[4+offest*12] = pointMap[3];
        indices[5+offest*12] = pointMap[2];

        if(pointMap.length<=4)
            return;
        //Bottom face
        indices[6+offest*12] = pointMap[4];
        indices[7+offest*12] = pointMap[5];
        indices[8+offest*12] = pointMap[6];
        indices[9+offest*12] = pointMap[4];
        indices[10+offest*12] = pointMap[7];
        indices[11+offest*12] = pointMap[6];
    }

    /**
     * Computes render debug for the given mesh
     *
     * @param offsetPos The in-world pos of the mesh
     * @param triangles The list of triangles of the IndexedMesh
     * @param indices The indices of the IndexedMesh
     * @return The debug data
     */
    public static float[] computeDebug(Vector3f offsetPos, List<Vector3f> triangles, int[] indices)
    {
        float[] debugData = new float[indices.length * 3];
        for (int i = 0; i < indices.length; i++) {
            Vector3f pos1 = triangles.get(indices[i]);
            debugData[i * 3] = pos1.x + offsetPos.x;
            debugData[i * 3 + 1] = pos1.y + offsetPos.y;
            debugData[i * 3 + 2] = pos1.z + offsetPos.z;
        }
        return debugData;
    }
}
