package fr.dynamx.common.blocks;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.object.render.Enum3DRenderLocation;
import fr.dynamx.api.contentpack.object.render.IObjPackObject;
import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.events.DynamXBlockEvent;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.capability.DynamXChunkData;
import fr.dynamx.common.capability.DynamXChunkDataProvider;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.parts.PartBlockSeat;
import fr.dynamx.common.contentpack.parts.PartStorage;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.common.contentpack.type.objects.PropObject;
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
import net.minecraft.util.ResourceLocation;
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

public class DynamXBlock<T extends BlockObject<?>> extends Block implements IDynamXItem<T>, IResourcesOwner {

    public static final PropertyInteger METADATA = PropertyInteger.create("metadata", 0, 15);

    /**
     * Cache
     */
    public T blockObjectInfo;

    public final int textureNum;

    private final boolean isObj;

    /**
     * Internally used by DynamX, don't use this constructor <br>
     * Creates a {@link Block} from a loaded {@link BlockObject}
     *
     * @param blockObjectInfo a BlockObject loaded by the content pack system
     */
    public DynamXBlock(T blockObjectInfo) {
        super(blockObjectInfo.getMaterial());
        this.blockObjectInfo = blockObjectInfo;
        setCreativeTab(blockObjectInfo.getCreativeTab(DynamXItemRegistry.objectTab));
        textureNum = Math.min(16, blockObjectInfo.getMaxTextureMetadata());
        isObj = blockObjectInfo.isObj();
        setLightLevel(blockObjectInfo.getLightLevel());

        initBlock(DynamXConstants.ID);
        setTranslationKey(DynamXConstants.ID + "." + blockObjectInfo.getFullName().toLowerCase());
    }

    /**
     * Use this constructor to create a custom block having the same functionalities as pack blocks. A second constructor allows you to also add a prop with this blocks. <br>
     * You can customise block properties using this.blockObjectInfo. <br> <br>
     * NOTE : Registry name and translation key are automatically set and the block is automatically registered into Forge by DynamX,
     * but don't forget to set a creative tab ! <br><br>
     *
     * <strong>NOTE : Should be called during addons initialization</strong>
     *
     * @param material  The block material
     * @param modid     The mod owning this block, used to register the block
     * @param blockName The name of the block
     * @param model     The obj model of the block
     */
    public DynamXBlock(Material material, String modid, String blockName, ResourceLocation model) {
        this(material, modid, blockName, model, null);
    }

    /**
     * Use this constructor to create a custom block having the same functionalities as pack blocks. This constructor also adds a prop to the block, if the "propsName" parameter isn't null. <br>
     * You can customise block properties using this.blockObjectInfo. <br> <br>
     * NOTE : Registry name and translation key are automatically set and the block and prop are automatically registered into Forge by DynamX,
     * but don't forget to set a creative tab ! <br><br>
     *
     * <strong>NOTE : Should be called during addons initialization</strong>
     *
     * @param material  The block material
     * @param modid     The mod owning this block, used to register the block
     * @param blockName The name of the block
     * @param model     The obj model of the block
     * @param propsName The name of the props to create, can be null
     */
    public DynamXBlock(Material material, String modid, String blockName, ResourceLocation model, String propsName) {
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

        initBlock(modid);
        setTranslationKey(blockObjectInfo.getFullName().toLowerCase());

        if (propsName != null) {
            PropObject<?> prop = new PropObject<>((ISubInfoTypeOwner<BlockObject<?>>) getInfo(), propsName);
            prop.setEmptyMass(10);
            prop.setCenterOfMass(new Vector3f(0, 0, 0));
            DynamXObjectLoaders.PROPS.addBuiltinObject("dynx." + modid, prop);
            getInfo().setPropObject(prop);
            System.out.println("PROPS ADDED ?");
        }
    }

    protected void initBlock(String modid) {
        setDefaultState(this.blockState.getBaseState().withProperty(METADATA, 0));
        RegistryNameSetter.setRegistryName(this, modid, blockObjectInfo.getFullName().toLowerCase());
        DynamXItemRegistry.registerItemBlock(this);
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        ItemStack pickBlock = super.getPickBlock(state, target, world, pos, player);
        pickBlock.setItemDamage(state.getValue(METADATA));
        return pickBlock;
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
            tooltip.add("Texture: " + getInfo().getMainObjectVariantName((byte) stack.getMetadata()));
        }
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (playerIn.isSneaking() && playerIn.capabilities.isCreativeMode) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te != null && worldIn.isRemote)
                ((TEDynamXBlock) te).openConfigGui();
            return true;
        }
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TEDynamXBlock) {
                //If we clicked a part, try to interact with it.
                InteractivePart hitPart = ((TEDynamXBlock) te).getHitPart(playerIn);
                if (hitPart instanceof PartStorage) {
                    playerIn.openGui(DynamXMain.instance, hitPart.getId()+2, worldIn, pos.getX(), pos.getY(), pos.getZ());
                    return true;
                }
                if (hitPart instanceof PartBlockSeat && hitPart.canInteract(((TEDynamXBlock) te).getSeatEntities().get(0), playerIn)) {
                    // TODO if (!MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.PlayerInteract(context, (BaseVehicleEntity<?>) vehicleEntity, hitPart)))
                    byte idx = hitPart.getId();
                    if (idx >= ((TEDynamXBlock) te).getSeatEntities().size())
                        idx = 0;
                    hitPart.interact(((TEDynamXBlock) te).getSeatEntities().get(idx), playerIn);
                    return true;
                }
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
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TEDynamXBlock && ((TEDynamXBlock) te).getSeatEntities() != null)
            ((TEDynamXBlock) te).getSeatEntities().forEach(Entity::setDead);
        super.breakBlock(worldIn, pos, state);
        DynamXChunkData data = worldIn.getChunk(pos).getCapability(DynamXChunkDataProvider.DYNAM_X_CHUNK_DATA_CAPABILITY, null);
        data.getBlocksAABB().remove(pos);
    }

    @Nullable
    @Override
    public RayTraceResult collisionRayTrace(IBlockState blockState, World worldIn, BlockPos pos, Vec3d start, Vec3d end) {
        return this.rayTrace(pos, start, end, getComputedBB(worldIn, pos));
    }

    @Nullable
    protected RayTraceResult rayTrace(BlockPos pos, Vec3d start, Vec3d end, AxisAlignedBB boundingBox) {
        /*Vec3d vec3d = start.subtract(pos.getX(), pos.getY(), pos.getZ());
        Vec3d vec3d1 = end.subtract(pos.getX(), pos.getY(), pos.getZ());
        return !intersects(boundingBox, vec3d, vec3d1) ? null : new RayTraceResult(new Vec3d(0, 0, 0), EnumFacing.DOWN, pos);*/
        return super.rayTrace(pos, start, end, boundingBox);
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
