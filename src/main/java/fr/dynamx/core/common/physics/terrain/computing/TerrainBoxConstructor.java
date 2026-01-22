package fr.dynamx.core.common.physics.terrain.computing;

import fr.dynamx.api.physics.terrain.ITerrainElement;
import fr.dynamx.core.common.physics.terrain.chunk.ChunkCollisions;
import fr.dynamx.core.common.physics.terrain.element.CompoundBoxTerrainElement;
import fr.dynamx.core.common.physics.terrain.element.EmptyTerrainElement;
import fr.dynamx.core.utils.optimization.MutableBoundingBox;
import fr.hermes.api.mc.blocks.HmBlockState;
import fr.hermes.api.mc.world.HmWorld;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Holds all boxes during the construction of one {@link ChunkCollisions} by the {@link TerrainCollisionsCalculator}
 *
 * @see TerrainBoxBuilder
 */
public class TerrainBoxConstructor {
    /**
     * The maximum amount of axis aligned collision boxes in one terrain collision element
     */
    private static final int MAX_BOXES_PER_MESH = 60;

    private final MutableBoundingBox searchZone;
    private final List<ITerrainElement> otherTerrainElements = new ArrayList<>();
    //Optimized, grouped collisions of many blocks (full cubes, slabs and snow)
    private final List<MutableBoundingBox> outListMutable = new ArrayList<>();
    //Collisions of special blocks like flower pots with no optimization
    private final List<MutableBoundingBox> outListVanilla = new ArrayList<>();
    private final int x, y, z;
    private final boolean debug;

    public TerrainBoxConstructor(MutableBoundingBox searchZone, int x, int y, int z, boolean debug) {
        this.searchZone = searchZone;
        this.x = x;
        this.y = y;
        this.z = z;
        this.debug = debug;
    }

    /**
     * Constructor with explicit bounds of the search zone.
     */
    public TerrainBoxConstructor(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean debug) {
        this(new MutableBoundingBox(minX, minY, minZ, maxX, maxY, maxZ), minX, minY, minZ, debug);
    }

    public MutableBoundingBox getSearchZone() {
        return searchZone;
    }

    public int getSearchMinX() { return (int) searchZone.minX; }

    public int getSearchMaxX() { return (int) searchZone.maxX; }

    public int getSearchMinY() { return (int) searchZone.minY; }

    public int getSearchMaxY() { return (int) searchZone.maxY; }

    public int getSearchMinZ() { return (int) searchZone.minZ; }

    public int getSearchMaxZ() { return (int) searchZone.maxZ; }

    public void addMutable(MutableBoundingBox boundingBox) {
        if (boundingBox == null)
            throw new NullPointerException("You can't add a null boundingBox.... " + x + " " + y + " " + z);
        outListMutable.add(boundingBox);
    }

    public void addBlockCollisions(HmWorld world, Vector3i at, HmBlockState ofBlock) {
        if (isDebug()) {
            List<MutableBoundingBox> boxes = new ArrayList<>();
            ofBlock.hm$addCollisionBoxes(world, at, getSearchZone(), boxes);
            injectBlockCollisions(at, ofBlock, boxes);
        } else {
            ofBlock.hm$addCollisionBoxes(world, at, getSearchZone(), getOutListVanilla());
        }
    }

    public void injectBlockCollisions(Vector3i at, HmBlockState ofBlock, List<MutableBoundingBox> boxes) {
        if (isDebug())
            System.out.println("Injecting " + boxes.size() + " boxes at " + at + " for " + ofBlock);
        outListVanilla.addAll(boxes);
    }

    public List<MutableBoundingBox> getOutListVanilla() {
        return outListVanilla;
    }

    public void addCustomShapedElement(ITerrainElement element) {
        otherTerrainElements.add(element);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public boolean isDebug() {
        return debug;
    }

    /**
     * Builds all collected terrain elements in the search zone, and returns them
     */
    public List<ITerrainElement> getTerrainElements() {
        //The result
        List<ITerrainElement> result = new ArrayList<>(otherTerrainElements);

        //Create complex collisions meshes, keeping maximum MAX_BOXES_PER_MESH in each
        List<MutableBoundingBox> vanillaBoxes = new ArrayList<>();
        //Sort them by y pos
        outListVanilla.sort(Comparator.comparingDouble(a -> {
            if (a != null) {
                return a.minY;
            }
            return 0;
        }));
        int count = 0;
        for (MutableBoundingBox box : outListVanilla) {
            if (box != null) {
                vanillaBoxes.add(new MutableBoundingBox(box));
                count++;
                if (count >= MAX_BOXES_PER_MESH) {
                    result.add(new CompoundBoxTerrainElement(-x, -y, -z, vanillaBoxes));
                    vanillaBoxes = new ArrayList<>();
                    count = 0;
                }
            }
        }
        if (!vanillaBoxes.isEmpty()) {
            result.add(new CompoundBoxTerrainElement(-x, -y, -z, vanillaBoxes));
        }
        //And add all standard TerrainElements
        if (!outListMutable.isEmpty()) {
            result.add(new CompoundBoxTerrainElement(-x, -y, -z, outListMutable));
        }
        if (result.isEmpty()) {
            // Helps to know that chunk collisions had been successfully loaded, but for an empty chunk
            result.add(new EmptyTerrainElement());
        }
        return result;
    }
}
