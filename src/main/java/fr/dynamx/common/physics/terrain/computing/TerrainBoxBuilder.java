package fr.dynamx.common.physics.terrain.computing;

import fr.dynamx.common.physics.terrain.element.CompoundStairsTerrainElement;
import fr.dynamx.common.physics.utils.StairsBox;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import net.minecraft.util.EnumFacing;

import java.util.Arrays;

/**
 * An optimized box builder for the physic terrain
 *
 * @see TerrainCollisionsCalculator
 * @see fr.dynamx.api.physics.terrain.IBlockCollisionBehavior
 */
public interface TerrainBoxBuilder
{
    void expandX(float by);

    void expandY(float by);

    void expandZ(float by);

    int getXSize();

    int getYSize();

    int getZSize();

    /**
     * Builds this boxes, and adds it to the 'terrainBuilder'
     *
     * @param terrainBuilder The terrain builder
     * @param cursor The current cursor, updated here
     * @param minX The working zone minX
     * @param minY The working zone minY
     * @param minZ The working zone minZ
     * @param maxX The working zone maxX
     * @param maxY The working zone maxY
     * @param maxZ The working zone maxZ
     */
    void build(TerrainBoxConstructor terrainBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, int minX, int minY, int minZ, int maxX, int maxY, int maxZ);

    class StairsTerrainBoxBuilder implements TerrainBoxBuilder
    {
        protected final double minX, minY, minZ;
        protected final double startYSize;
        private int cursorX, cursorZ;
        private final EnumFacing facing;
        private final boolean upper;

        public StairsTerrainBoxBuilder(double minX, double minY, double minZ, double startYSize, EnumFacing facing, boolean upper) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.startYSize = startYSize;
            this.facing = facing;
            this.upper = upper;
            cursorX = 0;
            cursorZ = 0;
        }

        @Override
        public void expandX(float by) {
            cursorX += 1;
        }

        @Override
        public void expandY(float by) {
            throw new IllegalStateException("Stairs are not Y-stackable !");
        }

        @Override
        public void expandZ(float by) {
            cursorZ += 1;
        }

        @Override
        public int getXSize() {
            return cursorX;
        }

        @Override
        public int getYSize() {
            return 0;
        }

        @Override
        public int getZSize() {
            return cursorZ;
        }

        protected StairsBox compile() {
            return facing.getAxis() == EnumFacing.Axis.X ? new StairsBox((float) minZ, (float) (minZ + cursorZ + 1), (float) minY, (float) minX, upper, facing) :
                    new StairsBox((float) minX, (float) (minX + cursorX + 1), (float) minY, (float) minZ, upper, facing);
        }

        @Override
        public void build(TerrainBoxConstructor terrainBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            StairsBox builtBox = compile();
            //On marque ce qui a été visité
            for (byte i = (byte) (this.minX - minX); i < Math.ceil(this.minX + cursorX + 1 - minX); i++) {
                for (byte h = (byte) (this.minY - minY); h < Math.ceil(this.minY + 0.5f - minY); h++) {
                    for (byte j = (byte) (this.minZ - minZ); j < Math.ceil(this.minZ + cursorZ + 1 - minZ); j++) {
                        //System.out.println("Set used at "+i+" "+h+" "+j);
                        if(i >= cursor.map.length)
                            throw new ArrayIndexOutOfBoundsException("Passed x size limit "+i+" "+builtBox+" "+terrainBuilder.getSearchZone()+" "+minX+" "+cursor.lx+" "+maxX+" "+cursor.map.length);
                        if(h >= cursor.map[i].length)
                            throw new ArrayIndexOutOfBoundsException("Passed y size limit "+h+" "+builtBox+" "+terrainBuilder.getSearchZone()+" "+minY+" "+cursor.ly+" "+maxY+" "+cursor.map[i].length);
                        if(j >= cursor.map[i][h].length)
                            throw new ArrayIndexOutOfBoundsException("Passed z size limit "+j+" "+builtBox+" "+terrainBuilder.getSearchZone()+" "+minZ+" "+cursor.lz+" "+maxZ+" "+cursor.map[i][h].length);
                        cursor.map[i][h][j] = 1;
                    }
                }
            }
            //System.out.println("DeltaY "+(currentBox.minY - minY)+" "+Math.ceil(currentBox.maxY - minY));
            // System.out.println("DeltaZ "+(currentBox.minZ - minZ)+" "+(currentBox.maxZ - minZ));

            // on a fait le max qu'on a pu à partir du bloc, on ajoute la aaabb et on repart
            // en "bas a droite" de la aabb pour continuer la construction
            cursor.dx = (byte) DynamXMath.preciseRound(this.minX + (cursorX+1) - minX);
            cursor.dy = (byte) DynamXMath.preciseRound(this.minY - minY);
            cursor.dz = (byte) DynamXMath.preciseRound(this.minZ - minZ);

            //System.out.println("Got "+ cursor +" "+this.minX+" "+this.minY+" "+this.minZ+" "+cursorX+" "+cursorZ+" "+startYSize+" "+facing+" "+upper+" "+builtBox.getMin()+" "+builtBox.getMax()+" "+builtBox.getMinOtherCoord()+" "+builtBox.getMinY());

            //System.out.println("Finish and add at "+dx+" "+dy+" "+dz+" "+currentBox);
            terrainBuilder.addCustomShapedElement(new CompoundStairsTerrainElement(-terrainBuilder.getX(), -terrainBuilder.getY(), -terrainBuilder.getZ(), Arrays.asList(builtBox)));
        }
    }

    class MutableTerrainBoxBuilder implements TerrainBoxBuilder
    {
        protected final double minX, minY, minZ;
        protected int cursorX, cursorY, cursorZ;

        protected double startXSize, startYSize, startZSize;
        protected double lastX, lastY, lastZ;

        public MutableTerrainBoxBuilder(double minX, double minY, double minZ, double lastX, double lastY, double lastZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.startXSize = lastX;
            this.startYSize = lastY;
            this.startZSize = lastZ;
        }

        @Override
        public void expandX(float by) {
            cursorX += 1;
            lastX = by;
        }

        @Override
        public void expandY(float by) {
            cursorY += 1;
            lastY = by;
        }

        @Override
        public void expandZ(float by) {
            cursorZ += 1;
            lastZ = by;
        }

        @Override
        public int getXSize() {
            return cursorX;
        }

        @Override
        public int getYSize() {
            return cursorY;
        }

        @Override
        public int getZSize() {
            return cursorZ;
        }

        protected MutableBoundingBox compile() {
            //if(cursorY +lastY-1+ startYSize == 0)
              //  lastY += 0.5f;

            int expandX = cursorX > 0 ? cursorX - 1 : 0;
            int expandY = cursorY > 0 ? cursorY - 1 : 0;
            int expandZ = cursorZ > 0 ? cursorZ - 1 : 0;
            //System.out.println("Compile "+this+" get "+new MutableBoundingBox(minX, minY, minZ, minX + expandX + startXSize + lastX,
              //      minY + expandY + startYSize + lastY, minZ + expandZ + startZSize + lastZ)+" // "+expandX+" "+expandY+" "+expandZ);
            return new MutableBoundingBox(minX, minY, minZ, minX + expandX + startXSize + lastX,
                    minY + expandY + startYSize + lastY, minZ + expandZ + startZSize + lastZ);
        }

        @Override
        public void build(TerrainBoxConstructor terrainBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            MutableBoundingBox builtBox = compile();
            //On marque ce qui a été visité
            for (byte i = (byte) (builtBox.minX - minX); i < Math.ceil(builtBox.maxX - minX); i++) {
                for (byte h = (byte) (builtBox.minY - minY); h < Math.ceil(builtBox.maxY - minY); h++) {
                    for (byte j = (byte) (builtBox.minZ - minZ); j < Math.ceil(builtBox.maxZ - minZ); j++) {
                        //System.out.println("Set used at "+i+" "+h+" "+j);
                        if(i >= cursor.map.length)
                            throw new ArrayIndexOutOfBoundsException("Passed x size limit "+i+" "+builtBox+" "+terrainBuilder.getSearchZone()+" "+minX+" "+cursor.lx+" "+maxX+" "+cursor.map.length);
                        if(h >= cursor.map[i].length)
                            throw new ArrayIndexOutOfBoundsException("Passed y size limit "+h+" "+builtBox+" "+terrainBuilder.getSearchZone()+" "+minY+" "+cursor.ly+" "+maxY+" "+cursor.map[i].length);
                        if(j >= cursor.map[i][h].length)
                            throw new ArrayIndexOutOfBoundsException("Passed z size limit "+j+" "+builtBox+" "+terrainBuilder.getSearchZone()+" "+minZ+" "+cursor.lz+" "+maxZ+" "+cursor.map[i][h].length);
                        cursor.map[i][h][j] = 1;
                    }
                }
            }
            //System.out.println("DeltaY "+(currentBox.minY - minY)+" "+Math.ceil(currentBox.maxY - minY));
            // System.out.println("DeltaZ "+(currentBox.minZ - minZ)+" "+(currentBox.maxZ - minZ));

            // on a fait le max qu'on a pu à partir du bloc, on ajoute la aaabb et on repart
            // en "bas a droite" de la aabb pour continuer la construction
            cursor.dx = (byte) DynamXMath.preciseRound(builtBox.maxX - minX);
            cursor.dy = (byte) DynamXMath.preciseRound(builtBox.minY - minY);
            cursor.dz = (byte) DynamXMath.preciseRound(builtBox.minZ - minZ);
            //System.out.println("Finish and add at "+dx+" "+dy+" "+dz+" "+currentBox);
            terrainBuilder.addMutable(builtBox);
        }

        /* Maybe recycle this, but not so important
        public void reset() {
           minX = minY = minZ = startXSize = startYSize = startZSize = lastX = lastY = lastZ = cursorX = cursorY = cursorZ = 0;
        }*/

        @Override
        public String toString() {
            return "MutableBoxBuilder{" +
                    "expandX=" + cursorX +
                    ", expandY=" + cursorY +
                    ", expandZ=" + cursorZ +
                    ", startMX=" + startXSize +
                    ", startMY=" + startYSize +
                    ", startMZ=" + startZSize +
                    ", startX=" + minX +
                    ", startY=" + minY +
                    ", startZ=" + minZ +
                    ", lastX=" + lastX +
                    ", lastY=" + lastY +
                    ", lastZ=" + lastZ +
                    '}';
        }
    }
}
