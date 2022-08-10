package fr.dynamx.common.blocks;

import fr.dynamx.api.contentpack.object.IInfoOwner;
import fr.dynamx.api.contentpack.object.render.Enum3DRenderLocation;
import fr.dynamx.api.contentpack.object.render.IObjPackObject;
import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.api.events.DynamXBlockEvent;
import fr.dynamx.common.capability.DynamXChunkData;
import fr.dynamx.common.capability.DynamXChunkDataProvider;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.common.items.DynamXItemRegistry;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.RegistryNameSetter;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class DynamXBlock<T extends BlockObject<?>> extends Block implements IInfoOwner<T>, IResourcesOwner {

    public static final PropertyInteger METADATA = PropertyInteger.create("metadata", 0, 15);

    /**
     * Cache
     */
    public T blockObjectInfo;

    public final int textureNum;

    private final boolean isObj;

    /**
     * Use the other constructor to create custom blocks and easily set BlockObject's properties
     */
    public DynamXBlock(T blockObjectInfo) {
        super(Material.ROCK);

        this.blockObjectInfo = blockObjectInfo;
        setDefaultState(this.blockState.getBaseState().withProperty(METADATA, 0));
        RegistryNameSetter.setRegistryName(this, DynamXConstants.ID, blockObjectInfo.getFullName().toLowerCase());
        setTranslationKey(DynamXConstants.ID + "." + blockObjectInfo.getFullName().toLowerCase());
        setCreativeTab(blockObjectInfo.getCreativeTab(DynamXItemRegistry.objectTab));
        textureNum = blockObjectInfo.getMaxTextureMetadata();
        isObj = blockObjectInfo.isObj();
        setLightLevel(blockObjectInfo.getLightLevel());

        DynamXItemRegistry.registerItemBlock(this);
    }

    /**
     * Use this constructor to create a custom block having the same functionalities as pack blocks <br>
     * You can customise block properties using this.blockObjectInfo <br> <br>
     * NOTE : Registry name and translation key are automatically set and the block is automatically registered into Forge by DynamX,
     * but don't forget to set a creative tab ! <br><br>
     *
     * <strong>NOTE : Should be called during addons initialization</strong>
     *
     * @param material  The block material
     * @param modid     The mod owning this block, used to register the block
     * @param blockName The name of the block
     * @param model     The obj model of the block, must be under "dynamxmod:models/<model>"
     */
    public DynamXBlock(Material material, String modid, String blockName, String model) {
        super(material);
        if (modid.contains("builtin_mod_")) { //Backward-compatibility
            blockObjectInfo = (T) DynamXObjectLoaders.BLOCKS.addBuiltinObject(this, modid, blockName);
            modid = modid.replace("builtin_mod_", "");
        } else {
            blockObjectInfo = (T) DynamXObjectLoaders.BLOCKS.addBuiltinObject(this, "dynx." + modid, blockName);
        }
        blockObjectInfo.setModel(model);
        blockObjectInfo.setDescription("Builtin " + modid + "'s block");
        textureNum = 1;
        isObj = blockObjectInfo.isObj();

        RegistryNameSetter.setRegistryName(this, modid, blockObjectInfo.getFullName().toLowerCase());
        setTranslationKey(blockObjectInfo.getFullName().toLowerCase());
        setDefaultState(this.blockState.getBaseState());

        DynamXItemRegistry.registerItemBlock(this);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(METADATA, meta);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(METADATA);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, METADATA);
    }

    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add("Description: " + getInfo().getDescription());
        tooltip.add("Pack: " + getInfo().getPackName());
        if (stack.getMetadata() > 0 && textureNum > 1) {
            tooltip.add("Texture: " + getInfo().getTexturesFor(null).get((byte) stack.getMetadata()).getName());
        }
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote && isObj) {
            if (playerIn.isSneaking() && playerIn.capabilities.isCreativeMode) {
                TileEntity te = worldIn.getTileEntity(pos);
                if (te != null)
                    ((TEDynamXBlock) te).openConfigGui();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return isObj;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        if (isObj) {
            DynamXBlockEvent.CreateTileEntity event = new DynamXBlockEvent.CreateTileEntity(world.isRemote ? Side.CLIENT : Side.SERVER, this, world, new TEDynamXBlock(blockObjectInfo));
            MinecraftForge.EVENT_BUS.post(event);
            return event.getTileEntity();
        }
        return null;
    }

    @Override //Handled by the RotatedCollisionHandler
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean isActualState) {
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return getComputedBB(source, pos);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
        return FULL_BLOCK_AABB;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        super.breakBlock(worldIn, pos, state);
        DynamXChunkData data = worldIn.getChunk(pos).getCapability(DynamXChunkDataProvider.DYNAM_X_CHUNK_DATA_CAPABILITY, null);
        System.out.println("Collisions are " + data.getBlocksAABB());
        System.out.println("Remove" + data.getBlocksAABB().get(pos) + " at " + pos);
        data.getBlocksAABB().remove(pos);
    }

    @Nullable
    @Override
    public RayTraceResult collisionRayTrace(IBlockState blockState, World worldIn, BlockPos pos, Vec3d start, Vec3d end) {
        return this.rayTrace(pos, start, end, getComputedBB(worldIn, pos));
    }

    @Nullable
    protected RayTraceResult rayTrace(BlockPos pos, Vec3d start, Vec3d end, AxisAlignedBB boundingBox) {
        Vec3d vec3d = start.subtract(pos.getX(), pos.getY(), pos.getZ());
        Vec3d vec3d1 = end.subtract(pos.getX(), pos.getY(), pos.getZ());
        return !intersects(boundingBox, vec3d, vec3d1) ? null : new RayTraceResult(new Vec3d(0, 0, 0), EnumFacing.DOWN, pos);
    }

    public static boolean intersects(AxisAlignedBB boundingBox, Vec3d min, Vec3d max) {
        //Because vanilla method is side only client...
        return boundingBox.intersects(Math.min(min.x, max.x), Math.min(min.y, max.y), Math.min(min.z, max.z), Math.max(min.x, max.x), Math.max(min.y, max.y), Math.max(min.z, max.z));
    }

    public AxisAlignedBB getComputedBB(IBlockAccess world, BlockPos pos) {
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof TEDynamXBlock) {
            return ((TEDynamXBlock) tileEntity).computeBoundingBox();
        } //FIXME DO FOR NO-OBJ BLOCKS
        return FULL_BLOCK_AABB;
    }

    @Override
    public T getInfo() {
        return blockObjectInfo;
    }

    @Override
    public void setInfo(T info) {
        blockObjectInfo = info;
    }

    @Override
    public boolean createJson() {
        return IResourcesOwner.super.createJson() || blockObjectInfo.get3DItemRenderLocation() != Enum3DRenderLocation.ALL;
    }

    @Override
    public String getJsonName(int meta) {
        return getInfo().getName().toLowerCase();
    }

    @Override
    public IObjPackObject getObjModel() {
        return isObj ? getInfo() : null;
    }

    @Override
    public int getMaxMeta() {
        return textureNum;
    }

    @Override
    @Nonnull
    public EnumBlockRenderType getRenderType(@Nonnull final IBlockState state) {
        return isObj ? EnumBlockRenderType.INVISIBLE : EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean isBlockNormalCube(IBlockState blockState) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isNormalCube(IBlockState state) {
        return false;
    }

    public boolean isObj() {
        return isObj;
    }
}
