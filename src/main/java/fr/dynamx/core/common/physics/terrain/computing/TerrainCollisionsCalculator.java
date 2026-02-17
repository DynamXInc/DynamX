package fr.dynamx.core.common.physics.terrain.computing;

import fr.dynamx.api.physics.terrain.IBlockCollisionBehavior;
import fr.dynamx.api.physics.terrain.ITerrainElement;
import fr.dynamx.core.common.DynamXMain;
import fr.dynamx.core.utils.VerticalChunkPos;
import fr.dynamx.core.utils.debug.Profiler;
import fr.dynamx.core.utils.optimization.QuaternionPool;
import fr.dynamx.core.utils.optimization.SubClassPool;
import fr.hermes.api.mc.utils.HmAxis;
import fr.hermes.api.mc.blocks.HmBlockState;
import fr.hermes.api.mc.world.HmWorld;
import fr.dynamx.core.utils.optimization.JmeVector3fPool;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The calculator of all block terrain collisions
 *
 * @see fr.dynamx.api.physics.terrain.ITerrainManager
 * @see IBlockCollisionBehavior
 */
public class TerrainCollisionsCalculator {
    //A wildcard
    private static final IBlockCollisionBehavior NONE_BEHAVIOR = new BlockCollisionBehaviors.None();
    //Take a copy of behaviors in each thread, so there isn't local context conflicts (see panes)
    private static final ThreadLocal<IBlockCollisionBehavior[]> baseBehaviors = ThreadLocal.withInitial(() -> new IBlockCollisionBehavior[]{new BlockCollisionBehaviors.Leaves(), new BlockCollisionBehaviors.FullCube(), new BlockCollisionBehaviors.Slab(), new BlockCollisionBehaviors.DynamXBlockBehavior(),
            new BlockCollisionBehaviors.Stairs(), new BlockCollisionBehaviors.Panes(), new BlockCollisionBehaviors.PathBlock(), new BlockCollisionBehaviors.Fences(), new BlockCollisionBehaviors.Walls()});
    private static final List<IBlockCollisionBehavior> customBehaviors = new ArrayList<>();
    public static final ThreadLocal<Map<HmBlockState, IBlockCollisionBehavior>> behaviorLookup = ThreadLocal.withInitial(HashMap::new);

    //TODO OPTI COMMENT LINKED LINES
    public static final boolean printDebug = false;

    /**
     * Modders, use the method in {@link fr.dynamx.api.physics.terrain.DynamXTerrainApi}
     */
    public static void addCustomBlockBehavior(IBlockCollisionBehavior behavior) {
        customBehaviors.add(behavior);
    }

    /**
     * Computes all collisions (TerrainElements) of this chunk, which can take some time
     *
     * @param profiler The current profiler
     * @return A list of the TerrainElements in this chunk
     * @see ITerrainElement
     * @see IBlockCollisionBehavior
     */
    public static List<ITerrainElement> computeCollisionFaces(VerticalChunkPos myPos, HmWorld mcWorld, Profiler profiler, boolean debug) {
        profiler.start(Profiler.Profiles.CHUNK_BLOCK_COLLS_COMPUTE);
        int x = myPos.x * 16;
        int y = myPos.y * 16;
        int z = myPos.z * 16;

        JmeVector3fPool.openPool(SubClassPool.CHUNK_COLLISIONS_COMPUTE);
        QuaternionPool.openPool(SubClassPool.CHUNK_COLLISIONS_COMPUTE);

        //Root terrain
        TerrainBoxConstructor boxBuilder = new TerrainBoxConstructor(x, y, z, x + 16, y + 16, z + 16, debug);
        //Compute all boxes
        TerrainCollisionsCalculator.loadBlockCollisions(mcWorld, boxBuilder);

        QuaternionPool.closePool();
        JmeVector3fPool.closePool();

        //And return all TerrainElements
        List<ITerrainElement> result = boxBuilder.getTerrainElements();
        profiler.end(Profiler.Profiles.CHUNK_BLOCK_COLLS_COMPUTE);
        return result;
    }

    /**
     * Finds the collision behavior of the given block state
     *
     * @param lookup   The lookup cache for quicker access
     * @param world
     * @param pos
     * @param forState The block state
     * @return The behavior of this block state
     */
    public static IBlockCollisionBehavior findBehavior(Map<HmBlockState, IBlockCollisionBehavior> lookup, HmWorld world, BlockPos pos, HmBlockState forState) {
        IBlockCollisionBehavior behavior = lookup.get(forState);
        if (behavior == null) {
            behavior = NONE_BEHAVIOR;
            if (!customBehaviors.isEmpty()) {
                for (IBlockCollisionBehavior b : customBehaviors) {
                    if (b.applies(world, pos, forState)) {
                        behavior = b;
                        break;
                    }
                }
            }
            if (behavior == NONE_BEHAVIOR) {
                for (IBlockCollisionBehavior b : baseBehaviors.get()) {
                    if (b.applies(world, pos, forState)) {
                        behavior = b;
                        break;
                    }
                }
            }
            lookup.put(forState, behavior);
            return behavior;
        }
        return behavior;
    }

    /**
     * Checks if this block can be added on the current stack
     *
     * @param lookup      The behavior lookup
     * @param world       The mc world
     * @param pos         The looking pos
     * @param axis        The stacking axis
     * @param cur         The block being stacked (or not)
     * @param onto        The first block of the stack
     * @param lastStacked The last added block, on the same axis
     * @param cursor      The local position
     * @param ox          Offset x
     * @param oy          Offset y
     * @param oz          Offset z
     * @param result      The stack result, filled with the result of the method, and returned
     */
    private static void canContinueAdding(Map<HmBlockState, IBlockCollisionBehavior> lookup, HmWorld world, BlockPos pos, HmAxis axis, HmBlockState cur, HmBlockState onto, @Nullable HmBlockState lastStacked, TerrainCursor cursor, int ox, int oy, int oz, StackResult result) {
        if (printDebug)
            System.out.println("Test " + cur + " " + ox + " " + oy + " " + oz + " " + cursor + " st " + cursor.getAt(ox, oy, oz));
        if (world.hm$isAirBlock(pos) || cursor.getAt(ox, oy, oz) == 1) {
            result.set(StackIssue.REFUSED, null);
        } else {
            IBlockCollisionBehavior behavior = findBehavior(lookup, world, pos, cur);
            if (behavior == null)
                throw new IllegalStateException("But wtf, behavior is null, cur " + cur + " " + behaviorLookup + " " + pos + " " + cursor);
            if (behavior == NONE_BEHAVIOR) {
                result.set(StackIssue.REFUSED, null);
            } else if (behavior.stacks(world, pos, axis, onto, cur, lastStacked)) {
                result.set(StackIssue.STACKABLE, behavior);
            } else {
                result.set(StackIssue.NOT_STACKABLE, behavior);
            }
        }
    }

    /**
     * Checks if this block can be added on the current stack, to form a row on z axis (checking all along the x axis)
     *
     * @param lookup         The behavior lookup
     * @param world          The mc world
     * @param terrainBuilder The terrain builder
     * @param currentBox     The box being built
     * @param mutable        The looking pos
     * @param with           The first block of the stack
     * @param cursor         The local position
     * @param x              Offset x
     * @param y              Offset y
     * @param z              Offset z
     * @return The behavior of this block only if this behavior is stackable in the given context, or none
     */
    private static boolean addRow(StackResult behavior, Map<HmBlockState, IBlockCollisionBehavior> lookup, HmWorld world, TerrainBoxConstructor terrainBuilder, TerrainBoxBuilder currentBox, int x, int y, int z, TerrainCursor cursor, BlockPos.MutableBlockPos mutable, HmBlockState with) {
        HmBlockState cur = null, lastCur = null;
        int cz = currentBox.getZSize();
        for (int i = 0; i <= currentBox.getXSize(); i++) { //Along the x axis
            mutable.setPos(x + i, y, z);
            cur = world.hm$getBlockState(mutable);
            canContinueAdding(lookup, world, mutable, HmAxis.Z, cur, with, lastCur, cursor, i, 0, cz + 1, behavior);
            if (printDebug)
                System.out.println("Row : get behavior " + behavior.issue + " " + behavior.behavior + " " + cur + " " + mutable);
            if (behavior.issue == StackIssue.NOT_STACKABLE) { //not stackable
                //Bug-fix : do not add the collision here, but try to stack it after
                //behavior.behavior.addBlockCollision(terrainBuilder, null, cursor, world, mutable, cur, null);
                //cursor.setFilledAt(i, 0, cz + 1);
                return false;
            } else if (behavior.issue == StackIssue.REFUSED) { //no block, or already added
                return false;
            }
            lastCur = cur;
        }
        if (cur != null) {//If row can be added, add it !
            int oldz = currentBox.getZSize();
            behavior.behavior.addBlockCollision(terrainBuilder, currentBox, cursor, world, mutable, cur, HmAxis.Z);
            if (currentBox.getZSize() == oldz) { //Avoid infinite loops...
                DynamXMain.log.error("Detected z stuck element at " + cursor + " " + mutable + " " + cur + " " + behavior + " " + currentBox + " " + terrainBuilder + ". Incrementing z size anyway...");
                currentBox.expandZ(1);
            }
            return true;
        } else
            return false;
    }


    /**
     * Checks if this block can be added on the current stack, to form a plan on y axis (checking all along the x and z axis)
     *
     * @param lookup         The behavior lookup
     * @param world          The mc world
     * @param terrainBuilder The terrain builder
     * @param currentBox     The box being built
     * @param mutable        The looking pos
     * @param with           The first block of the stack
     * @param cursor         The local position
     * @param x              Offset x
     * @param y              Offset y
     * @param z              Offset z
     * @return The behavior of this block only if this behavior is stackable in the given context, or none
     */
    private static boolean addPlane(StackResult behavior, Map<HmBlockState, IBlockCollisionBehavior> lookup, HmWorld world, TerrainBoxConstructor terrainBuilder, TerrainBoxBuilder currentBox, int x, int y, int z, TerrainCursor cursor, BlockPos.MutableBlockPos mutable, HmBlockState with) {
        HmBlockState cur = null, lastCur = null;
        int cy = currentBox.getYSize();
        for (int i = 0; i <= currentBox.getXSize(); i++) { //Along the x axis
            for (int j = 0; j <= currentBox.getZSize(); j++) { //Along the z axis
                mutable.setPos(x + i, y, z + j);
                cur = world.hm$getBlockState(mutable);
                canContinueAdding(lookup, world, mutable, HmAxis.Y, cur, with, lastCur, cursor, i, cy + 1, j, behavior);
                if (printDebug)
                    System.out.println("Plane : get behavior " + behavior.issue + " " + behavior.behavior + " " + cur + " " + mutable);
                if (behavior.issue == StackIssue.NOT_STACKABLE) { //not stackable
                    //Bug-fix : do not add the collision here, but try to stack it after
                    //TODO DETECT NOT STACKABLE AT ALL BLOCKS HERE
                    //behavior.behavior.addBlockCollision(terrainBuilder, null, cursor, world, mutable, cur, null);
                    //cursor.setFilledAt(i, cy + 1, j);
                    return false;
                } else if (behavior.issue == StackIssue.REFUSED) { //no block, or already added
                    return false;
                }
                lastCur = cur;
            }
        }
        if (cur != null) { //If plane can be added, add it !
            int oldy = currentBox.getYSize();
            behavior.behavior.addBlockCollision(terrainBuilder, currentBox, cursor, world, mutable, cur, HmAxis.Y);
            if (currentBox.getYSize() == oldy) { //Avoid infinite loops...
                DynamXMain.log.error("Detected y stuck element at {} {} {} {} {} {}. Incrementing y size anyway...", cursor, mutable, cur, behavior, currentBox, terrainBuilder);
                currentBox.expandY(1);
            }
            return true;
        } else
            return false;
    }

    /**
     * Computes all terrain collision shapes that are into the input axisAlignedBB <br>
     * Heavy method, don't abuse !
     *
     * @param world          The minecraft world
     * @param terrainBuilder The terrain construction context
     */
    public static void loadBlockCollisions(HmWorld world, TerrainBoxConstructor terrainBuilder) {
        int minX = terrainBuilder.getSearchMinX();
        int maxX = terrainBuilder.getSearchMaxX();
        int minY = terrainBuilder.getSearchMinY();
        int maxY = terrainBuilder.getSearchMaxY();
        int minZ = terrainBuilder.getSearchMinZ();
        int maxZ = terrainBuilder.getSearchMaxZ();

        //Local pos to speed-up things
        BlockPos.PooledMutableBlockPos mutable = BlockPos.PooledMutableBlockPos.retain();
        //Local map to speed-up things
        Map<HmBlockState, IBlockCollisionBehavior> lookup = behaviorLookup.get();

        //longueur de la zone à construire (c'est des chunks donc 16 forcément, mais au cas où le code est adaptable pour n'importe quelle région)
        byte lx = (byte) (maxX - minX - 1);
        byte lz = (byte) (maxZ - minZ - 1);
        byte ly = (byte) (maxY - minY - 1);

        //variables permettant de naviguer relativement à l'origine du chunk
        TerrainCursor cursor = new TerrainCursor(new byte[lx + 2][ly + 2][lz + 2], lx, ly, lz);

        //If the last checked block is stackable
        boolean[] stackable = new boolean[1];
        //The block currently beeing checked
        HmBlockState cur;

        long[] times = new long[6];

        long t0 = System.currentTimeMillis();
        y:
        do {// tant qu'on n'est pas arrivé "en haut a droite" de notre box on continue : on
            // n'a pas parcouru tous les blocs

            // premièrement, on cherche un bloc de base sur lequel s'appuyer pour construire
            // un AABB
            long t1 = System.currentTimeMillis();
            mutable.setPos(minX + cursor.dx, minY + cursor.dy, minZ + cursor.dz);
            HmBlockState boxStart = world.hm$getBlockState(mutable);

            //System.out.println("Begin at "+dx+" "+dy+" "+dz);
            stackable[0] = true;
            IBlockCollisionBehavior behavior = findBehavior(lookup, world, mutable, boxStart);
            while (cursor.getHere() == 1 || !boxStart.hm$blocksMovement() || !(stackable[0] = behavior.isStackableBlock(world, mutable, boxStart))) { // on ne repasse par sur des blocs ayant déjà
                // été traités ni sur ceux ayant des collisions spéciales (ou aucune collisions) /!\ ordre des conditions important

                if (printDebug && !world.hm$isAirBlock(mutable))
                    System.out.println("Fail0 at " + cursor + " " + cursor.getHere());
                if (!stackable[0] && !world.hm$isAirBlock(mutable)) { //Si le block a une collision spéciale, on l'ajoute
                    if (printDebug)
                        System.out.println("ADDINGGGGG " + boxStart);
                    behavior.addBlockCollision(terrainBuilder, null, cursor, world, mutable, boxStart, null);
                }
                if (!cursor.incrXZY()) {
                    if (printDebug)
                        System.out.println("End has been reached");
                    break y;// on a parcouru toute le chunk et on n'a rien trouvé, on arrête et passe au chunk suivant
                }
                mutable.setPos(minX + cursor.dx, minY + cursor.dy, minZ + cursor.dz);
                boxStart = world.hm$getBlockState(mutable);
                behavior = findBehavior(lookup, world, mutable, boxStart);
            }
            long t2 = System.currentTimeMillis();

            // on a trouvé un bloc sur lequel s'appuyer : boxStart
            // on check si c'est un bloc complet ou une dalle (pour l'instant je ne gère que
            // ces deux types de blocs)
            // 09/09/20, Aym : ajout du support des autres types de blocks, voir "isStackable"
            // 22/04/21, Aym : généralisation et support des dalles supérieures pour l'optimisation
            //useless, done before behavior = findBehavior(boxStart);
            IBlockCollisionBehavior starter = behavior;
            //We found a valid block to begin stacking : create the box builder
            TerrainBoxBuilder currentBox = behavior.initBoxBuilder(terrainBuilder, world, mutable, boxStart, minX + cursor.dx, minY + cursor.dy, minZ + cursor.dz);
            //Last stack result
            StackResult stackResult = new StackResult();

            //If we can stack in x+ direction
            if (cursor.dx + currentBox.getXSize() < lx) {
                // on cherche maintenant à étendre notre AABB le plus possible
                mutable.setPos(minX + cursor.dx + (currentBox.getXSize() + 1), minY + cursor.dy, minZ + cursor.dz);
                cur = world.hm$getBlockState(mutable);
                if (printDebug) {
                    System.out.println("==============");
                    System.out.println("Got start point " + cursor + " " + behavior + " " + boxStart + " " + cur + " " + mutable);
                }

                // on essaie d'aller le plus loin possible en x en ne trouvant aucun bloc d'air,
                // on enregistre notre distance dans k
                do {
                    canContinueAdding(lookup, world, mutable, HmAxis.X, cur, boxStart, null, cursor, currentBox.getXSize() + 1, 0, 0, stackResult);
                    if (printDebug)
                        System.out.println("Line : get behavior " + behavior + " " + cur + " " + mutable);
                    if (stackResult.behavior != null) {//If block can be added, add it !
                        if (stackResult.issue == StackIssue.STACKABLE) {
                            cursor.setFilledAt(currentBox.getXSize() + 1, 0, 0); //Mark as checked and add it to the box builder
                            behavior.addBlockCollision(terrainBuilder, currentBox, cursor, world, mutable, cur, HmAxis.X);
                        }/* else { //not stackable with boxStart
                            //check it later, be cause it may be stackable with another block
                            //behavior.addCustomBox(terrainBuilder, null, world, mutable, cur, null);
                        }*/
                    }
                    behavior = stackResult.behavior;
                    mutable.setPos(minX + cursor.dx + (currentBox.getXSize() + 1), minY + cursor.dy, minZ + cursor.dz);
                    cur = world.hm$getBlockState(mutable);
                    //System.out.println("StackX at "+dx+" "+dy+" "+dz+" "+k);
                } //If behavior is null or NONE_BEHAVIOR, then we finished stacking in x+ direction
                while (cursor.dx + currentBox.getXSize() < lx && stackResult.issue == StackIssue.STACKABLE);
                if (printDebug)
                    System.out.println("LINE END " + cur + " " + behavior + " " + cursor + " " + currentBox.getXSize());
            } else if (printDebug) {
                System.out.println("IGNORED LINE : NO SPACE " + cursor + " " + currentBox.getXSize());
            }
            long t3 = System.currentTimeMillis();

            // de la meme maniere qu'avec x, on essaie d'aller le plus loin possible en z en
            // conservant la longueur xSize (on parcours des colonnes de longueur xSize du coup)
            while (cursor.dz + currentBox.getZSize() < lz && addRow(stackResult, lookup, world, terrainBuilder, currentBox, minX + cursor.dx, minY + cursor.dy, minZ + cursor.dz + (currentBox.getZSize() + 1), cursor, mutable, boxStart))
                ;

            long t4 = System.currentTimeMillis();

            // pareil, mais en hauteur. Donc on parcours des rectangles de k*expandZ blocs
            while (cursor.dy + currentBox.getYSize() < ly && addPlane(stackResult, lookup, world, terrainBuilder, currentBox, minX + cursor.dx, minY + cursor.dy + (currentBox.getYSize() + 1), minZ + cursor.dz, cursor, mutable, boxStart))
                ;

            long t5 = System.currentTimeMillis();

            //Finish the box building
            starter.onBoxBuildEnd(terrainBuilder, currentBox);
            byte oldx = cursor.dx;
            byte oldy = cursor.dy;
            byte oldz = cursor.dz;
            //Build the box (add it to the terrain builder)
            currentBox.build(terrainBuilder, cursor, minX, minY, minZ, maxX, maxY, maxZ);
            if (cursor.dx == oldx && cursor.dy == oldy && cursor.dz == oldz) { //Avoid infinite loops...
                DynamXMain.log.error("Detected stuck element at {} {} {} {} {} {}. Incrementing cursor anyway...", cursor, mutable, boxStart, behavior, currentBox, terrainBuilder);
                cursor.incrXZY();
            }

            long t6 = System.currentTimeMillis();
            times[0] += (t6 - t1);
            times[1] += (t2 - t1);
            times[2] += (t3 - t2);
            times[3] += (t4 - t3);
            times[4] += (t5 - t4);
            times[5] += (t6 - t5);
            if (printDebug)
                System.out.println(cursor + " were deltas");
        } while (cursor.incrZY());

        mutable.release();

        //System.out.println("Quand j'ai fini les vanilles sont "+outListVanilla);
        // no pooled mutable to release in Hermes path

        //long tt = (System.currentTimeMillis()-t0);
        //if(tt >= 6)
        //  System.out.println("Took "+tt+" ms to compute at "+terrainBuilder.getX()+" "+terrainBuilder.getY()+" "+terrainBuilder.getZ()+" times "+ Arrays.toString(times));
    }

    /**
     * An helper to move in the computing terrain zone, and know computed positions
     */
    public static class TerrainCursor {
        /**
         * x pos of the cursor
         */
        protected byte dx;
        /**
         * y pos of the cursor
         */
        protected byte dy;
        /**
         * z pos of the cursor
         */
        protected byte dz;
        /**
         * Marks the already-checked positions
         */
        protected final byte[][][] map;
        /**
         * x size of the area
         */
        protected final byte lx;
        /**
         * y size of the area
         */
        protected final byte ly;
        /**
         * z size of the area
         */
        protected final byte lz;

        protected TerrainCursor(byte[][][] map, byte lx, byte ly, byte lz) {
            this.map = map;
            this.lx = lx;
            this.ly = ly;
            this.lz = lz;
        }

        /**
         * Returns the state of the zone at the current dx, dy, dz pos
         *
         * @return 1 ? checked : not checked
         */
        protected byte getHere() {
            return map[dx][dy][dz];
        }

        /**
         * Returns the state of the zone at the given dx+ox, dy+oy, dz+oz pos
         *
         * @return 1 ? checked : not checked
         */
        protected byte getAt(int ox, int oy, int oz) {
            return map[dx + ox][dy + oy][dz + oz];
        }

        /**
         * Marks the given dx+ox, dy+oy, dz+oz pos as filled
         */
        protected void setFilledAt(int ox, int oy, int oz) {
            map[dx + ox][dy + oy][dz + oz] = 1;
        }

        /**
         * Increments dx, then increments dz and dy if needed
         *
         * @return false if we reached the end of the area
         */
        protected boolean incrXZY() {
            dx++;
            return incrZY();
        }

        /**
         * Increments dz, then increments dy if needed
         *
         * @return false if we reached the end of the area
         */
        protected boolean incrZY() {
            if (dx > lx) {
                dz++;
                dx = 0;
                if (dz > lz) {
                    dz = 0;
                    dy++;
                    return dy <= ly;// on a parcouru toute le chunk et on n'a rien trouvé, on arrête et passe au chunk suivant
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return "TerrainCursor{" +
                    "dx=" + dx +
                    ", dy=" + dy +
                    ", dz=" + dz +
                    ", lx=" + lx +
                    ", ly=" + ly +
                    ", lz=" + lz +
                    '}';
        }
    }

    /**
     * Result of a stack test (a tentative to add a block below another block)
     */
    public enum StackIssue {
        /**
         * Impossible (air, already added, ...)
         */
        REFUSED,
        /**
         * Not stackable on this block, but has a collision
         */
        NOT_STACKABLE,
        /**
         * Stackable on this block, so continue the box-building !
         */
        STACKABLE
    }

    /**
     * Wrapper for the result of a stack test (a tentative to add a block below another block)
     */
    public static class StackResult {
        /**
         * The issue of the stack test
         */
        protected StackIssue issue;
        /**
         * The behavior of the block being stacked (or not)
         */
        protected IBlockCollisionBehavior behavior;

        public void set(StackIssue issue, IBlockCollisionBehavior behavior) {
            this.issue = issue;
            this.behavior = behavior;
        }
    }
}
