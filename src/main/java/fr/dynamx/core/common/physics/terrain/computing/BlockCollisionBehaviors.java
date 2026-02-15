package fr.dynamx.core.common.physics.terrain.computing;

import fr.dynamx.api.physics.terrain.IBlockCollisionBehavior;
import fr.dynamx.core.common.blocks.DynamXBlock;
import fr.dynamx.core.common.blocks.TEDynamXBlock;
import fr.dynamx.core.common.physics.terrain.element.DynamXBlockTerrainElement;
import fr.dynamx.core.utils.optimization.MutableBoundingBox;
import fr.dynamx.forge.DynamXConfig;
import fr.hermes.api.mc.blocks.HmBlockState;
import fr.hermes.api.mc.blocks.HmTileEntity;
import fr.hermes.api.mc.utils.HmAxis;
import fr.hermes.api.mc.world.HmWorld;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

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
        private final List<MutableBoundingBox> boxes = new ArrayList<>();

        @Override
        public boolean applies(HmWorld world, BlockPos pos, HmBlockState toBlock) {
            return false;
        }

        @Override
        public boolean stacks(HmWorld world, BlockPos pos, HmAxis axis, HmBlockState onBlock, HmBlockState stackingBlock, HmBlockState lastStacked) {
            return false;
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, HmWorld world, BlockPos at, HmBlockState ofBlock, HmAxis axis) {
            if (ofBlock.hm$blocksMovement()) //Si le block a une collision spéciale, on l'ajoute
            {
                ofBlock.hm$addCollisionBoxes(world, at, terrainBoxConstructor.getSearchZone(), boxes);
                if (boxes.size() <= DynamXConfig.maxComplexBlockBoxes) {
                    terrainBoxConstructor.injectBlockCollisions(at, ofBlock, boxes);
                    boxes.clear();
                } else {
                    boxes.clear();
                    MutableBoundingBox box = ofBlock.hm$getBoundingBox(world, at);
                    if (box != null) {
                        terrainBoxConstructor.addMutable(box.offset(at));
                    }
                }
            }
        }

        @Override
        public boolean isStackableBlock(HmWorld world, BlockPos pos, HmBlockState blockState) {
            return false;
        }
    }

    static class Leaves implements IBlockCollisionBehavior {
        @Override
        public boolean applies(HmWorld world, BlockPos pos, HmBlockState toBlock) {
            return toBlock.hm$getBlock() instanceof BlockLeaves;
        }

        @Override
        public boolean stacks(HmWorld world, BlockPos pos, HmAxis axis, HmBlockState onBlock, HmBlockState stackingBlock, HmBlockState lastStacked) {
            return false;
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, HmWorld world, BlockPos at, HmBlockState ofBlock, HmAxis axis) {
        }

        @Override
        public boolean isStackableBlock(HmWorld world, BlockPos pos, HmBlockState blockState) {
            return false;
        }
    }

    static class FullCube implements IBlockCollisionBehavior {
        @Override
        public boolean applies(HmWorld world, BlockPos pos, HmBlockState toBlock) {
            return toBlock.hm$isFullCube() && !(toBlock instanceof BlockLeaves);
        }

        @Override
        public boolean stacks(HmWorld world, BlockPos pos, HmAxis axis, HmBlockState onBlock, HmBlockState stackingBlock, HmBlockState lastStacked) {
            if (axis == HmAxis.Y && lastStacked != null && !lastStacked.hm$isFullCube()) {
                return false;
            }
            IBlockState onBlockMc = (IBlockState) onBlock;
            return onBlock.hm$isFullCube() || (axis == HmAxis.Y && onBlock.hm$getBlock() instanceof BlockSlab && onBlockMc.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.TOP);
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, HmWorld world, BlockPos at, HmBlockState ofBlock, HmAxis axis) {
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

    static class Slab implements IBlockCollisionBehavior {
        @Override
        public boolean applies(HmWorld world, BlockPos pos, HmBlockState toBlock) {
            return toBlock.hm$getBlock() instanceof BlockSlab;
        }

        @Override
        public boolean stacks(HmWorld world, BlockPos pos, HmAxis axis, HmBlockState onBlock, HmBlockState stackingBlock, HmBlockState lastStacked) {
            IBlockState onBlockMc = (IBlockState) onBlock;
            IBlockState stackingBlockMc = (IBlockState) stackingBlock;
            if (axis == HmAxis.Y) {
                if (lastStacked != null && lastStacked.hm$isFullCube()) {
                    return false; //cannot continue the plane
                }
                return (onBlock.hm$isFullCube() || (onBlock.hm$getBlock() instanceof BlockSlab && onBlockMc.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.TOP)) && stackingBlockMc.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.BOTTOM;
            }
            return onBlock.hm$getBlock() instanceof BlockSlab && !((BlockSlab) onBlock.hm$getBlock()).isDouble() && onBlockMc.getValue(BlockSlab.HALF) == stackingBlockMc.getValue(BlockSlab.HALF);
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, HmWorld world, BlockPos at, HmBlockState ofBlock, HmAxis axis) {
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
        public boolean applies(HmWorld world, BlockPos pos, HmBlockState toBlock) {
            return toBlock.hm$getBlock() instanceof DynamXBlock;
        }

        @Override
        public boolean stacks(HmWorld world, BlockPos pos, HmAxis axis, HmBlockState onBlock, HmBlockState stackingBlock, HmBlockState lastStacked) {
            return false;
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, HmWorld world, BlockPos at, HmBlockState ofBlock, HmAxis axis) {
            HmTileEntity te = world.hm$getTileEntity(at);
            if (te instanceof TEDynamXBlock) {
                terrainBoxConstructor.addCustomShapedElement(new DynamXBlockTerrainElement(cursor.dx, cursor.dy, cursor.dz, at));
            }
        }

        @Override
        public boolean isStackableBlock(HmWorld world, BlockPos pos, HmBlockState blockState) {
            return false;
        }
    }

    static class Stairs implements IBlockCollisionBehavior {
        @Override
        public boolean applies(HmWorld world, BlockPos pos, HmBlockState toBlock) {
            IBlockState toBlockMc = (IBlockState) toBlock;
            return toBlock.hm$getBlock() instanceof BlockStairs && toBlockMc.getValue(BlockStairs.SHAPE) == BlockStairs.EnumShape.STRAIGHT;
        }

        @Override
        public boolean stacks(HmWorld world, BlockPos pos, HmAxis axis, HmBlockState onBlock, HmBlockState stackingBlock, HmBlockState lastStacked) {
            IBlockState onBlockMc = (IBlockState) onBlock;
            IBlockState stackingBlockMc = (IBlockState) stackingBlock;
            if (axis == HmAxis.Y) {
                return false;
            } else if (onBlock.hm$getBlock() instanceof BlockStairs && (stackingBlockMc.getValue(BlockStairs.HALF) == onBlockMc.getValue(BlockStairs.HALF))) {
                EnumFacing facing = stackingBlockMc.getValue(BlockStairs.FACING);
                if (facing.getAxis().ordinal() != axis.ordinal()) { // TODO Might be miss-convertied oo
                    return facing == onBlockMc.getValue(BlockStairs.FACING);
                }
            }
            return false;
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, HmWorld world, BlockPos at, HmBlockState ofBlock, HmAxis axis) {
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
        public TerrainBoxBuilder initBoxBuilder(TerrainBoxConstructor terrainBoxConstructor, HmWorld world, BlockPos mutable, HmBlockState boxStart, double ox, double oy, double oz) {
            MutableBoundingBox box = boxStart.hm$getBoundingBox(world, mutable);
            IBlockState boxStartMc = (IBlockState) boxStart;
            EnumFacing facing = ((BlockStairs) boxStart.hm$getBlock()).getActualState(boxStartMc, (IBlockAccess) world, mutable).getValue(BlockStairs.FACING);
            if (boxStartMc.getValue(BlockStairs.HALF) == BlockStairs.EnumHalf.BOTTOM) {
                return new TerrainBoxBuilder.StairsTerrainBoxBuilder(ox + box.minX, oy + box.minY, oz + box.minZ, 0.5f, facing, false);
            }
            return new TerrainBoxBuilder.StairsTerrainBoxBuilder(ox + box.minX, oy + box.minY, oz + box.minZ, 0.5f, facing, true);
        }

        @Override
        public boolean isStackableBlock(HmWorld world, BlockPos pos, HmBlockState blockState) {
            return true;
        }
    }

    public static class Panes implements IBlockCollisionBehavior {
        private final List<MutableBoundingBox> waitingXBoxes = new ArrayList<>();
        private final List<MutableBoundingBox> waitingZBoxes = new ArrayList<>();
        private boolean hasXZLegs;

        @Override
        public boolean applies(HmWorld world, BlockPos pos, HmBlockState toBlock) {
            return toBlock.hm$getBlock() instanceof BlockPane; // TODO CONVERT
        }

        @Override
        public boolean stacks(HmWorld world, BlockPos pos, HmAxis axis, HmBlockState onBlock, HmBlockState stackingBlock, HmBlockState lastStacked) {
            if (!(onBlock.hm$getBlock() instanceof BlockPane))
                return false;
            return onBlock.equals(stackingBlock);
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, HmWorld world, BlockPos at, HmBlockState ofBlock, HmAxis axis) {
            if (axis == null) {
                terrainBoxConstructor.addBlockCollisions(world, at, ofBlock);
                return;
            }
            if (axis == HmAxis.Y) {
                boxBuilder.expandY(1);
                return;
            }
            IBlockState ofBlockMc = (IBlockState) ofBlock;
            IBlockState realStacking = ((BlockPane) ofBlock.hm$getBlock()).getActualState(ofBlockMc, (IBlockAccess) world, at);
            // if (onBlock.getValue(BlockPane.SOUTH) || onBlock.getValue(BlockPane.NORTH))
            // return !onBlock.getValue(BlockPane.EAST) && !onBlock.getValue(BlockPane.WEST);
            switch (axis) {
                case X:
                    boxBuilder.expandX((realStacking.getValue(BlockPane.EAST) ? 1 : 0.5625f));
                    if (realStacking.getValue(BlockPane.SOUTH) && !hasXZLegs)
                        waitingZBoxes.add(new MutableBoundingBox(0.4375D, 0.0D, 0.4375D, 0.5625D, 1.0D, 1.0D).offset(at));
                    break;
                case Z:
                    boxBuilder.expandZ((realStacking.getValue(BlockPane.SOUTH) ? 1 : 0.5625f));
                    if (realStacking.getValue(BlockPane.EAST) && !hasXZLegs)
                        waitingXBoxes.add(new MutableBoundingBox(0.4375D, 0.0D, 0.4375D, 1.0D, 1.0D, 0.5625D).offset(at));
                    if (realStacking.getValue(BlockPane.WEST) && !hasXZLegs)
                        waitingXBoxes.add(new MutableBoundingBox(0, 0.0D, 0.4375D, 0.5625f, 1.0D, 0.5625D).offset(at));
                    break;
            }
        }

        @Override
        public TerrainBoxBuilder initBoxBuilder(TerrainBoxConstructor terrainBoxConstructor, HmWorld world, BlockPos mutable, HmBlockState boxStart, double ox, double oy, double oz) {
            hasXZLegs = false;
            MutableBoundingBox box = boxStart.hm$getBoundingBox(world, mutable);
            double maxZ = box.maxZ;
            if (maxZ == 1 && box.maxX == 1) { //if x-stacking, but z+ leg
                maxZ = 0.5625f;
                hasXZLegs = true;
                waitingZBoxes.add(new MutableBoundingBox(0.4375D, 0.0D, 0.4375D, 0.5625D, 1.0D, 1.0D).offset(mutable));
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
        public boolean applies(HmWorld world, BlockPos pos, HmBlockState toBlock) {
            // TODO CONVERT
            return toBlock.hm$getBlock() == Blocks.GRASS_PATH || toBlock.hm$getBlock() == Blocks.FARMLAND;
        }

        @Override
        public boolean stacks(HmWorld world, BlockPos pos, HmAxis axis, HmBlockState onBlock, HmBlockState stackingBlock, HmBlockState lastStacked) {
            //System.out.println("Stacks ? "+onBlock+" "+stackingBlock+" "+lastStacked);
            //System.out.println("RESULT "+(axis != HmAxis.Y && applies(world, pos, onBlock)));
            return axis != HmAxis.Y && applies(world, pos, onBlock);
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, HmWorld world, BlockPos at, HmBlockState ofBlock, HmAxis axis) {
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
        public boolean applies(HmWorld world, BlockPos pos, HmBlockState toBlock) {
            return toBlock.hm$getBlock() instanceof BlockFence; // TODO CONVERT
        }

        @Override
        public boolean stacks(HmWorld world, BlockPos pos, HmAxis axis, HmBlockState onBlock, HmBlockState stackingBlock, HmBlockState lastStacked) {
            if (!(onBlock.hm$getBlock() instanceof BlockFence)) {
                return false;
            }
            IBlockState stackingBlockMc = (IBlockState) stackingBlock;
            IBlockState realStacking;
            switch (axis) {
                case Y:
                    return onBlock.equals(stackingBlock);
                case X:
                    realStacking = ((BlockFence) stackingBlock.hm$getBlock()).getActualState(stackingBlockMc, (IBlockAccess) world, pos);
                    return onBlock.equals(stackingBlock) && (realStacking.getValue(BlockFence.EAST) || realStacking.getValue(BlockFence.WEST));
                case Z:
                    realStacking = ((BlockFence) stackingBlock.hm$getBlock()).getActualState(stackingBlockMc, (IBlockAccess) world, pos);
                    return onBlock.equals(stackingBlock) && (realStacking.getValue(BlockFence.NORTH) || realStacking.getValue(BlockFence.SOUTH));
            }
            return false;
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, HmWorld world, BlockPos at, HmBlockState ofBlock, HmAxis axis) {
            if (axis == null) {
                terrainBoxConstructor.addBlockCollisions(world, at, ofBlock);
                return;
            }
            if (axis == HmAxis.Y) {
                boxBuilder.expandY(1);
                return;
            }
            IBlockState ofBlockMc = (IBlockState) ofBlock;
            IBlockState realStacking = ((BlockFence) ofBlock.hm$getBlock()).getActualState(ofBlockMc, (IBlockAccess) world, at);
            // if (onBlock.getValue(BlockPane.SOUTH) || onBlock.getValue(BlockPane.NORTH))
            // return !onBlock.getValue(BlockPane.EAST) && !onBlock.getValue(BlockPane.WEST);
            switch (axis) {
                case X:
                    boxBuilder.expandX((realStacking.getValue(BlockFence.EAST) ? 1 : 0.625f));
                    if (realStacking.getValue(BlockFence.SOUTH) && !hasXZLegs)
                        waitingZBoxes.add(new MutableBoundingBox(0.375D, 0.0D, 0.625D, 0.625D, 1D, 1.0D).offset(at));
                    break;
                case Z:
                    boxBuilder.expandZ((realStacking.getValue(BlockFence.SOUTH) ? 1 : 0.625f));
                    if (realStacking.getValue(BlockFence.EAST) && !hasXZLegs)
                        waitingXBoxes.add(new MutableBoundingBox(0.625D, 0.0D, 0.375D, 1.0D, 1D, 0.625D).offset(at));
                    if (realStacking.getValue(BlockFence.WEST) && !hasXZLegs)
                        waitingXBoxes.add(new MutableBoundingBox(0.0D, 0.0D, 0.375D, 0.375D, 1D, 0.625D).offset(at));
                    break;
            }
        }

        @Override
        public TerrainBoxBuilder initBoxBuilder(TerrainBoxConstructor terrainBoxConstructor, HmWorld world, BlockPos mutable, HmBlockState boxStart, double ox, double oy, double oz) {
            hasXZLegs = false;
            MutableBoundingBox box = boxStart.hm$getBoundingBox(world, mutable);
            double maxZ = box.maxZ;
            if (maxZ == 1 && box.maxX == 1) { //if x-stacking, but z+ leg
                maxZ = 0.625D;
                hasXZLegs = true;
                waitingZBoxes.add(new MutableBoundingBox(0.375D, 0.0D, 0.625D, 0.625D, 1D, 1.0D).offset(mutable));
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
        public boolean applies(HmWorld world, BlockPos pos, HmBlockState toBlock) {
            return toBlock.hm$getBlock() instanceof BlockWall;
        }

        @Override
        public boolean stacks(HmWorld world, BlockPos pos, HmAxis axis, HmBlockState onBlock, HmBlockState stackingBlock, HmBlockState lastStacked) {
            if (!(onBlock.hm$getBlock() instanceof BlockWall)) {
                return false;
            }
            IBlockState stackingBlockMc = (IBlockState) stackingBlock;
            IBlockState realStacking;
            switch (axis) {
                case Y:
                    return onBlock.equals(stackingBlock);
                case X:
                    realStacking = ((BlockWall) stackingBlock.hm$getBlock()).getActualState(stackingBlockMc, (IBlockAccess) world, pos);
                    return onBlock.equals(stackingBlock) && (realStacking.getValue(BlockWall.WEST) || realStacking.getValue(BlockWall.EAST));
                case Z:
                    realStacking = ((BlockWall) stackingBlock.hm$getBlock()).getActualState(stackingBlockMc, (IBlockAccess) world, pos);
                    return onBlock.equals(stackingBlock) && (realStacking.getValue(BlockWall.NORTH) || realStacking.getValue(BlockWall.SOUTH));
            }
            return false;
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, HmWorld world, BlockPos at, HmBlockState ofBlock, HmAxis axis) {
            if (axis == null) {
                terrainBoxConstructor.addBlockCollisions(world, at, ofBlock);
                return;
            }
            if (axis == HmAxis.Y) {
                boxBuilder.expandY(1);
                return;
            }
            IBlockState ofBlockMc = (IBlockState) ofBlock;
            IBlockState realStacking = ((BlockWall) ofBlock.hm$getBlock()).getActualState(ofBlockMc, (IBlockAccess) world, at);
            // if (onBlock.getValue(BlockPane.SOUTH) || onBlock.getValue(BlockPane.NORTH))
            // return !onBlock.getValue(BlockPane.EAST) && !onBlock.getValue(BlockPane.WEST);
            switch (axis) {
                case X:
                    boxBuilder.expandX((realStacking.getValue(BlockWall.EAST) ? 1 : 0.75f));
                    if (realStacking.getValue(BlockWall.SOUTH) && !hasXZLegs)
                        waitingZBoxes.add(new MutableBoundingBox(0.25D, 0.0D, 0.25D, 0.75D, 1.0D, 1.0D).offset(at));
                    break;
                case Z:
                    boxBuilder.expandZ((realStacking.getValue(BlockWall.SOUTH) ? 1 : 0.75f));
                    if (realStacking.getValue(BlockWall.EAST) && !hasXZLegs)
                        waitingXBoxes.add(new MutableBoundingBox(0.25D, 0.0D, 0.25D, 1.0D, 1.0D, 0.75D).offset(at));
                    if (realStacking.getValue(BlockWall.WEST) && !hasXZLegs)
                        waitingXBoxes.add(new MutableBoundingBox(0.0D, 0.0D, 0.0D, 0.75D, 1.0D, 0.75D).offset(at));
                    break;
            }
        }

        @Override
        public TerrainBoxBuilder initBoxBuilder(TerrainBoxConstructor terrainBoxConstructor, HmWorld world, BlockPos mutable, HmBlockState boxStart, double ox, double oy, double oz) {
            hasXZLegs = false;
            MutableBoundingBox box = boxStart.hm$getBoundingBox(world, mutable);
            double maxZ = box.maxZ;
            if (maxZ == 1 && box.maxX == 1) { //if x-stacking, but z+ leg
                maxZ = 0.75f;
                hasXZLegs = true;
                waitingZBoxes.add(new MutableBoundingBox(0.25D, 0.0D, 0.25D, 0.75D, 1.0D, 1.0D).offset(mutable));
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
