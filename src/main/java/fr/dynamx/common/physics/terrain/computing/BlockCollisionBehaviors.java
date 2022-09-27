package fr.dynamx.common.physics.terrain.computing;

import fr.dynamx.api.physics.terrain.IBlockCollisionBehavior;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.physics.terrain.element.DynamXBlockTerrainElement;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * All {@link IBlockCollisionBehavior} add by DynamX <br>
 * The behaviors added in the {@link fr.dynamx.api.physics.terrain.ITerrainManager} have the priority over them
 */
public class BlockCollisionBehaviors {
    /**
     * Default fallback behavior
     */
    static class None implements IBlockCollisionBehavior {
        private final List<AxisAlignedBB> boxes = new ArrayList<>();

        @Override
        public boolean applies(IBlockAccess world, BlockPos pos, IBlockState toBlock) {
            return false;
        }

        @Override
        public boolean stacks(IBlockAccess world, BlockPos pos, EnumFacing.Axis axis, IBlockState onBlock, IBlockState stackingBlock, IBlockState lastStacked) {
            return false;
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, World world, BlockPos at, IBlockState ofBlock, EnumFacing.Axis axis) {
            if (ofBlock.getMaterial().blocksMovement()) //Si le block a une collision spéciale, on l'ajoute
            {
                ofBlock.addCollisionBoxToList(world, at, terrainBoxConstructor.getSearchZone(), boxes, null, false);
                if (boxes.size() <= DynamXConfig.maxComplexBlockBoxes) {
                    terrainBoxConstructor.injectBlockCollisions(at, ofBlock, boxes);
                    boxes.clear();
                } else {
                    boxes.clear();
                    AxisAlignedBB box = ofBlock.getBoundingBox(world, at);
                    terrainBoxConstructor.addMutable(new MutableBoundingBox(box).offset(at.getX(), at.getY(), at.getZ()));
                }
            }
        }

        @Override
        public boolean isStackableBlock(IBlockAccess world, BlockPos pos, IBlockState blockState) {
            return false;
        }
    }

    static class Leaves implements IBlockCollisionBehavior {
        @Override
        public boolean applies(IBlockAccess world, BlockPos pos, IBlockState toBlock) {
            return toBlock.getBlock() instanceof BlockLeaves;
        }

        @Override
        public boolean stacks(IBlockAccess world, BlockPos pos, EnumFacing.Axis axis, IBlockState onBlock, IBlockState stackingBlock, IBlockState lastStacked) {
            return false;
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, World world, BlockPos at, IBlockState ofBlock, EnumFacing.Axis axis) {
        }

        @Override
        public boolean isStackableBlock(IBlockAccess world, BlockPos pos, IBlockState blockState) {
            return false;
        }
    }

    static class FullCube implements IBlockCollisionBehavior {
        @Override
        public boolean applies(IBlockAccess world, BlockPos pos, IBlockState toBlock) {
            return toBlock.isFullCube() && !(toBlock instanceof BlockLeaves);
        }

        @Override
        public boolean stacks(IBlockAccess world, BlockPos pos, EnumFacing.Axis axis, IBlockState onBlock, IBlockState stackingBlock, IBlockState lastStacked) {
            if (axis == EnumFacing.Axis.Y && lastStacked != null && !lastStacked.isFullCube()) {
                return false;
            }
            return onBlock.isFullCube() || (axis == EnumFacing.Axis.Y && onBlock.getBlock() instanceof BlockSlab && onBlock.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.TOP);
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, World world, BlockPos at, IBlockState ofBlock, EnumFacing.Axis axis) {
            if (axis == null) {
                terrainBoxConstructor.addBlockCollisions(world, at, ofBlock);
            } else {
                switch (axis) {
                    case X:
                        boxBuilder.expandX(1);
                        break;
                    case Y:
                        boxBuilder.expandY(1);
                        break;
                    case Z:
                        boxBuilder.expandZ(1);
                        break;
                }
            }
        }
    }

  /*  static class MaybeFullBlock implements IBlockCollisionBehavior {
        @Override
        public boolean applies(IBlockAccess world, BlockPos pos, IBlockState toBlock) {
            AxisAlignedBB bb = toBlock.getBoundingBox(world, pos);
            return bb.maxY - bb.minY == 1 && bb.maxX - bb.minX == 1 && bb.maxZ - bb.minZ == 1;
        }

        @Override
        public boolean stacks(IBlockAccess world, BlockPos pos, EnumFacing.Axis axis, IBlockState onBlock, IBlockState stackingBlock, IBlockState lastStacked) {
            return applies(world, pos, onBlock) || (axis == EnumFacing.Axis.Y && onBlock.getBlock() instanceof BlockSlab && onBlock.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.TOP);
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, World world, BlockPos at, IBlockState ofBlock, EnumFacing.Axis axis) {
            if (axis == null) {
                ofBlock.addCollisionBoxToList(world, at, terrainBoxConstructor.getSearchZone(), terrainBoxConstructor.getOutListVanilla(), null, false);
            } else {
                switch (axis) {
                    case X:
                        boxBuilder.expandX(1);
                        break;
                    case Y:
                        boxBuilder.expandY(1);
                        break;
                    case Z:
                        boxBuilder.expandZ(1);
                        break;
                }
            }
        }
    }*/

    static class Slab implements IBlockCollisionBehavior {
        @Override
        public boolean applies(IBlockAccess world, BlockPos pos, IBlockState toBlock) {
            return toBlock.getBlock() instanceof BlockSlab;
        }

        @Override
        public boolean stacks(IBlockAccess world, BlockPos pos, EnumFacing.Axis axis, IBlockState onBlock, IBlockState stackingBlock, IBlockState lastStacked) {
            if (axis == EnumFacing.Axis.Y) {
                if (lastStacked != null && lastStacked.isFullCube())
                    return false; //cannot continue the plane
                return (onBlock.isFullCube() || (onBlock.getBlock() instanceof BlockSlab && onBlock.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.TOP)) && stackingBlock.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.BOTTOM;
            }
            return onBlock.getBlock() instanceof BlockSlab && !((BlockSlab) onBlock.getBlock()).isDouble() && onBlock.getValue(BlockSlab.HALF) == stackingBlock.getValue(BlockSlab.HALF);
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, World world, BlockPos at, IBlockState ofBlock, EnumFacing.Axis axis) {
            if (axis == null) {
                terrainBoxConstructor.addBlockCollisions(world, at, ofBlock);
            } else {
                switch (axis) {
                    case X:
                        boxBuilder.expandX(1);
                        break;
                    case Y:
                        boxBuilder.expandY(0.5f); //Can't be up
                        break;
                    case Z:
                        boxBuilder.expandZ(1);
                        break;
                }
            }
        }
    }

    static class DynamXBlockBehavior implements IBlockCollisionBehavior {
        @Override
        public boolean applies(IBlockAccess world, BlockPos pos, IBlockState toBlock) {
            return toBlock.getBlock() instanceof DynamXBlock;
        }

        @Override
        public boolean stacks(IBlockAccess world, BlockPos pos, EnumFacing.Axis axis, IBlockState onBlock, IBlockState stackingBlock, IBlockState lastStacked) {
            return false;
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, World world, BlockPos at, IBlockState ofBlock, EnumFacing.Axis axis) {
            TileEntity te = world.getTileEntity(at);
            if (te instanceof TEDynamXBlock)
                terrainBoxConstructor.addCustomShapedElement(new DynamXBlockTerrainElement(cursor.dx, cursor.dy, cursor.dz, at, ((TEDynamXBlock) te).getRelativeTranslation(), ((TEDynamXBlock) te).getCollidableRotation()));
        }

        @Override
        public boolean isStackableBlock(IBlockAccess world, BlockPos pos, IBlockState blockState) {
            return false;
        }
    }

    static class Stairs implements IBlockCollisionBehavior {
        @Override
        public boolean applies(IBlockAccess world, BlockPos pos, IBlockState toBlock) {
            return toBlock.getBlock() instanceof BlockStairs && toBlock.getValue(BlockStairs.SHAPE) == BlockStairs.EnumShape.STRAIGHT;
        }

        @Override
        public boolean stacks(IBlockAccess world, BlockPos pos, EnumFacing.Axis axis, IBlockState onBlock, IBlockState stackingBlock, IBlockState lastStacked) {
            if (axis == EnumFacing.Axis.Y) {
                return false;
            } else if (onBlock.getBlock() instanceof BlockStairs && (stackingBlock.getValue(BlockStairs.HALF) == onBlock.getValue(BlockStairs.HALF))) {
                EnumFacing facing = stackingBlock.getValue(BlockStairs.FACING);
                if (axis.negate().test(facing)) {
                    return facing == onBlock.getValue(BlockStairs.FACING);
                }
            }
            return false;
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, World world, BlockPos at, IBlockState ofBlock, EnumFacing.Axis axis) {
            if (axis == null) {
                terrainBoxConstructor.addBlockCollisions(world, at, ofBlock);
            } else {
                switch (axis) {
                    case X:
                        boxBuilder.expandX(1);
                        break;
                    case Y:
                        boxBuilder.expandY(0.5f); //Can't be up
                        break;
                    case Z:
                        boxBuilder.expandZ(1);
                        break;
                }
            }
        }

        @Override
        public TerrainBoxBuilder initBoxBuilder(TerrainBoxConstructor terrainBoxConstructor, World world, BlockPos mutable, IBlockState boxStart, double ox, double oy, double oz) {
            AxisAlignedBB box = boxStart.getBoundingBox(world, mutable);
            EnumFacing facing = ((BlockStairs) boxStart.getBlock()).getActualState(boxStart, world, mutable).getValue(BlockStairs.FACING);
            if (boxStart.getValue(BlockStairs.HALF) == BlockStairs.EnumHalf.BOTTOM) {
                return new TerrainBoxBuilder.StairsTerrainBoxBuilder(ox + box.minX, oy + box.minY, oz + box.minZ, 0.5f, facing, false);
            }
            return new TerrainBoxBuilder.StairsTerrainBoxBuilder(ox + box.minX, oy + box.minY, oz + box.minZ, 0.5f, facing, true);
        }

        @Override
        public boolean isStackableBlock(IBlockAccess world, BlockPos pos, IBlockState blockState) {
            return true;
        }
    }

    public static class Panes implements IBlockCollisionBehavior {
        private final List<MutableBoundingBox> waitingXBoxes = new ArrayList<>();
        private final List<MutableBoundingBox> waitingZBoxes = new ArrayList<>();
        private boolean hasXZLegs;

        @Override
        public boolean applies(IBlockAccess world, BlockPos pos, IBlockState toBlock) {
            return toBlock.getBlock() instanceof BlockPane;
        }

        @Override
        public boolean stacks(IBlockAccess world, BlockPos pos, EnumFacing.Axis axis, IBlockState onBlock, IBlockState stackingBlock, IBlockState lastStacked) {
            if (!(onBlock.getBlock() instanceof BlockPane))
                return false;
            return onBlock.equals(stackingBlock);
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, World world, BlockPos at, IBlockState ofBlock, EnumFacing.Axis axis) {
            if (axis == null) {
                terrainBoxConstructor.addBlockCollisions(world, at, ofBlock);
                return;
            }
            if (axis == EnumFacing.Axis.Y) {
                boxBuilder.expandY(1);
                return;
            }
            IBlockState realStacking = ((BlockPane) ofBlock.getBlock()).getActualState(ofBlock, world, at);
            // if (onBlock.getValue(BlockPane.SOUTH) || onBlock.getValue(BlockPane.NORTH))
            // return !onBlock.getValue(BlockPane.EAST) && !onBlock.getValue(BlockPane.WEST);
            switch (axis) {
                case X:
                    boxBuilder.expandX((realStacking.getValue(BlockPane.EAST) ? 1 : 0.5625f));
                    if (realStacking.getValue(BlockPane.SOUTH) && !hasXZLegs)
                        waitingZBoxes.add(new MutableBoundingBox(0.4375D, 0.0D, 0.4375D, 0.5625D, 1.0D, 1.0D).offset(at.getX(), at.getY(), at.getZ()));
                    break;
                case Z:
                    boxBuilder.expandZ((realStacking.getValue(BlockPane.SOUTH) ? 1 : 0.5625f));
                    if (realStacking.getValue(BlockPane.EAST) && !hasXZLegs)
                        waitingXBoxes.add(new MutableBoundingBox(0.4375D, 0.0D, 0.4375D, 1.0D, 1.0D, 0.5625D).offset(at.getX(), at.getY(), at.getZ()));
                    if (realStacking.getValue(BlockPane.WEST) && !hasXZLegs)
                        waitingXBoxes.add(new MutableBoundingBox(0, 0.0D, 0.4375D, 0.5625f, 1.0D, 0.5625D).offset(at.getX(), at.getY(), at.getZ()));
                    break;
            }
        }

        @Override
        public TerrainBoxBuilder initBoxBuilder(TerrainBoxConstructor terrainBoxConstructor, World world, BlockPos mutable, IBlockState boxStart, double ox, double oy, double oz) {
            hasXZLegs = false;
            AxisAlignedBB box = boxStart.getBoundingBox(world, mutable);
            double maxZ = box.maxZ;
            if (maxZ == 1 && box.maxX == 1) { //if x-stacking, but z+ leg
                maxZ = 0.5625f;
                hasXZLegs = true;
                waitingZBoxes.add(new MutableBoundingBox(0.4375D, 0.0D, 0.4375D, 0.5625D, 1.0D, 1.0D).offset(mutable.getX(), mutable.getY(), mutable.getZ()));
            }

            return new TerrainBoxBuilder.MutableTerrainBoxBuilder(ox + box.minX, oy + box.minY, oz + box.minZ, box.maxX - box.minX, box.maxY - box.minY, maxZ - box.minZ) {
                @Override
                public void expandZ(float by) {
                    super.expandZ(by);
                    if (hasXZLegs) {
                        startZSize = box.maxZ - box.minZ;
                    }
                }
            };
        }

        @Override
        public void onBoxBuildEnd(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder) {
            if (boxBuilder.getXSize() == 0) {
                waitingXBoxes.forEach(terrainBoxConstructor::addMutable);
            }
            waitingXBoxes.clear();
            if (boxBuilder.getZSize() == 0) {
                waitingZBoxes.forEach(terrainBoxConstructor::addMutable);
            }
            waitingZBoxes.clear();
        }
    }

    static class PathBlock implements IBlockCollisionBehavior {
        @Override
        public boolean applies(IBlockAccess world, BlockPos pos, IBlockState toBlock) {
            //   System.out.println("Test "+toBlock+" "+pos);
            return toBlock.getBlock() == Blocks.GRASS_PATH || toBlock.getBlock() == Blocks.FARMLAND;
        }

        @Override
        public boolean stacks(IBlockAccess world, BlockPos pos, EnumFacing.Axis axis, IBlockState onBlock, IBlockState stackingBlock, IBlockState lastStacked) {
            //System.out.println("Stacks ? "+onBlock+" "+stackingBlock+" "+lastStacked);
            //System.out.println("RESULT "+(axis != EnumFacing.Axis.Y && applies(world, pos, onBlock)));
            return axis != EnumFacing.Axis.Y && applies(world, pos, onBlock);
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, World world, BlockPos at, IBlockState ofBlock, EnumFacing.Axis axis) {
            //System.out.println("Add farm "+ofBlock+" "+at);
            if (axis == null) {
                terrainBoxConstructor.addBlockCollisions(world, at, ofBlock);
            } else {
                switch (axis) {
                    case X:
                        boxBuilder.expandX(1);
                        break;
                    case Z:
                        boxBuilder.expandZ(1);
                        break;
                }
            }
        }
    }

    static class Fences implements IBlockCollisionBehavior {
        private final List<MutableBoundingBox> waitingXBoxes = new ArrayList<>();
        private final List<MutableBoundingBox> waitingZBoxes = new ArrayList<>();
        private boolean hasXZLegs;

        @Override
        public boolean applies(IBlockAccess world, BlockPos pos, IBlockState toBlock) {
            return toBlock.getBlock() instanceof BlockFence;
        }

        @Override
        public boolean stacks(IBlockAccess world, BlockPos pos, EnumFacing.Axis axis, IBlockState onBlock, IBlockState stackingBlock, IBlockState lastStacked) {
            if (!(onBlock.getBlock() instanceof BlockFence))
                return false;
            IBlockState realStacking;
            switch (axis) {
                case Y:
                    return onBlock.equals(stackingBlock);
                case X:
                    realStacking = ((BlockFence) stackingBlock.getBlock()).getActualState(stackingBlock, world, pos);
                    return onBlock.equals(stackingBlock) && (realStacking.getValue(BlockFence.EAST) || realStacking.getValue(BlockFence.WEST));
                case Z:
                    realStacking = ((BlockFence) stackingBlock.getBlock()).getActualState(stackingBlock, world, pos);
                    return onBlock.equals(stackingBlock) && (realStacking.getValue(BlockFence.NORTH) || realStacking.getValue(BlockFence.SOUTH));
            }
            return false;
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, World world, BlockPos at, IBlockState ofBlock, EnumFacing.Axis axis) {
            if (axis == null) {
                terrainBoxConstructor.addBlockCollisions(world, at, ofBlock);
                return;
            }
            if (axis == EnumFacing.Axis.Y) {
                boxBuilder.expandY(1);
                return;
            }
            IBlockState realStacking = ((BlockFence) ofBlock.getBlock()).getActualState(ofBlock, world, at);
            // if (onBlock.getValue(BlockPane.SOUTH) || onBlock.getValue(BlockPane.NORTH))
            // return !onBlock.getValue(BlockPane.EAST) && !onBlock.getValue(BlockPane.WEST);
            switch (axis) {
                case X:
                    boxBuilder.expandX((realStacking.getValue(BlockFence.EAST) ? 1 : 0.625f));
                    if (realStacking.getValue(BlockFence.SOUTH) && !hasXZLegs)
                        waitingZBoxes.add(new MutableBoundingBox(0.375D, 0.0D, 0.625D, 0.625D, 1D, 1.0D).offset(at.getX(), at.getY(), at.getZ()));
                    break;
                case Z:
                    boxBuilder.expandZ((realStacking.getValue(BlockFence.SOUTH) ? 1 : 0.625f));
                    if (realStacking.getValue(BlockFence.EAST) && !hasXZLegs)
                        waitingXBoxes.add(new MutableBoundingBox(0.625D, 0.0D, 0.375D, 1.0D, 1D, 0.625D).offset(at.getX(), at.getY(), at.getZ()));
                    if (realStacking.getValue(BlockFence.WEST) && !hasXZLegs)
                        waitingXBoxes.add(new MutableBoundingBox(0.0D, 0.0D, 0.375D, 0.375D, 1D, 0.625D).offset(at.getX(), at.getY(), at.getZ()));
                    break;
            }
        }

        @Override
        public TerrainBoxBuilder initBoxBuilder(TerrainBoxConstructor terrainBoxConstructor, World world, BlockPos mutable, IBlockState boxStart, double ox, double oy, double oz) {
            hasXZLegs = false;
            AxisAlignedBB box = boxStart.getBoundingBox(world, mutable);
            double maxZ = box.maxZ;
            if (maxZ == 1 && box.maxX == 1) { //if x-stacking, but z+ leg
                maxZ = 0.625D;
                hasXZLegs = true;
                waitingZBoxes.add(new MutableBoundingBox(0.375D, 0.0D, 0.625D, 0.625D, 1D, 1.0D).offset(mutable.getX(), mutable.getY(), mutable.getZ()));
            }

            return new TerrainBoxBuilder.MutableTerrainBoxBuilder(ox + box.minX, oy + box.minY, oz + box.minZ, box.maxX - box.minX, box.maxY - box.minY, maxZ - box.minZ) {
                @Override
                public void expandZ(float by) {
                    super.expandZ(by);
                    if (hasXZLegs) {
                        startZSize = box.maxZ - box.minZ;
                    }
                }
            };
        }

        @Override
        public void onBoxBuildEnd(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder) {
            if (boxBuilder.getXSize() == 0) {
                waitingXBoxes.forEach(terrainBoxConstructor::addMutable);
            }
            waitingXBoxes.clear();
            if (boxBuilder.getZSize() == 0) {
                waitingZBoxes.forEach(terrainBoxConstructor::addMutable);
            }
            waitingZBoxes.clear();
        }
    }

    static class Walls implements IBlockCollisionBehavior {
        private final List<MutableBoundingBox> waitingXBoxes = new ArrayList<>();
        private final List<MutableBoundingBox> waitingZBoxes = new ArrayList<>();
        private boolean hasXZLegs;

        @Override
        public boolean applies(IBlockAccess world, BlockPos pos, IBlockState toBlock) {
            return toBlock.getBlock() instanceof BlockWall;
        }

        @Override
        public boolean stacks(IBlockAccess world, BlockPos pos, EnumFacing.Axis axis, IBlockState onBlock, IBlockState stackingBlock, IBlockState lastStacked) {
            if (!(onBlock.getBlock() instanceof BlockWall))
                return false;
            IBlockState realStacking;
            switch (axis) {
                case Y:
                    return onBlock.equals(stackingBlock);
                case X:
                    realStacking = ((BlockWall) stackingBlock.getBlock()).getActualState(stackingBlock, world, pos);
                    return onBlock.equals(stackingBlock) && (realStacking.getValue(BlockWall.WEST) || realStacking.getValue(BlockWall.EAST));
                case Z:
                    realStacking = ((BlockWall) stackingBlock.getBlock()).getActualState(stackingBlock, world, pos);
                    return onBlock.equals(stackingBlock) && (realStacking.getValue(BlockWall.NORTH) || realStacking.getValue(BlockWall.SOUTH));
            }
            return false;
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, World world, BlockPos at, IBlockState ofBlock, EnumFacing.Axis axis) {
            if (axis == null) {
                terrainBoxConstructor.addBlockCollisions(world, at, ofBlock);
                return;
            }
            if (axis == EnumFacing.Axis.Y) {
                boxBuilder.expandY(1);
                return;
            }
            IBlockState realStacking = ((BlockWall) ofBlock.getBlock()).getActualState(ofBlock, world, at);
            // if (onBlock.getValue(BlockPane.SOUTH) || onBlock.getValue(BlockPane.NORTH))
            // return !onBlock.getValue(BlockPane.EAST) && !onBlock.getValue(BlockPane.WEST);
            switch (axis) {
                case X:
                    boxBuilder.expandX((realStacking.getValue(BlockWall.EAST) ? 1 : 0.75f));
                    if (realStacking.getValue(BlockWall.SOUTH) && !hasXZLegs)
                        waitingZBoxes.add(new MutableBoundingBox(0.25D, 0.0D, 0.25D, 0.75D, 1.0D, 1.0D).offset(at.getX(), at.getY(), at.getZ()));
                    break;
                case Z:
                    boxBuilder.expandZ((realStacking.getValue(BlockWall.SOUTH) ? 1 : 0.75f));
                    if (realStacking.getValue(BlockWall.EAST) && !hasXZLegs)
                        waitingXBoxes.add(new MutableBoundingBox(0.25D, 0.0D, 0.25D, 1.0D, 1.0D, 0.75D).offset(at.getX(), at.getY(), at.getZ()));
                    if (realStacking.getValue(BlockWall.WEST) && !hasXZLegs)
                        waitingXBoxes.add(new MutableBoundingBox(0.0D, 0.0D, 0.0D, 0.75D, 1.0D, 0.75D).offset(at.getX(), at.getY(), at.getZ()));
                    break;
            }
        }

        @Override
        public TerrainBoxBuilder initBoxBuilder(TerrainBoxConstructor terrainBoxConstructor, World world, BlockPos mutable, IBlockState boxStart, double ox, double oy, double oz) {
            hasXZLegs = false;
            AxisAlignedBB box = boxStart.getBoundingBox(world, mutable);
            double maxZ = box.maxZ;
            if (maxZ == 1 && box.maxX == 1) { //if x-stacking, but z+ leg
                maxZ = 0.75f;
                hasXZLegs = true;
                waitingZBoxes.add(new MutableBoundingBox(0.25D, 0.0D, 0.25D, 0.75D, 1.0D, 1.0D).offset(mutable.getX(), mutable.getY(), mutable.getZ()));
            }

            return new TerrainBoxBuilder.MutableTerrainBoxBuilder(ox + box.minX, oy + box.minY, oz + box.minZ, box.maxX - box.minX, box.maxY - box.minY, maxZ - box.minZ) {
                @Override
                public void expandZ(float by) {
                    super.expandZ(by);
                    if (hasXZLegs) {
                        startZSize = box.maxZ - box.minZ;
                    }
                }
            };
        }

        @Override
        public void onBoxBuildEnd(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder) {
            if (boxBuilder.getXSize() == 0) {
                waitingXBoxes.forEach(terrainBoxConstructor::addMutable);
            }
            waitingXBoxes.clear();
            if (boxBuilder.getZSize() == 0) {
                waitingZBoxes.forEach(terrainBoxConstructor::addMutable);
            }
            waitingZBoxes.clear();
        }
    }
}
