package fr.dynamx.common.items.vehicle;

import com.jme3.bullet.NativePhysicsObject;
import com.jme3.bullet.objects.PhysicsSoftBody;
import com.jme3.bullet.objects.infos.Sbcp;
import com.jme3.bullet.objects.infos.SoftBodyConfig;
import com.jme3.bullet.util.NativeSoftBodyUtil;
import com.jme3.math.Vector3f;
import fr.aym.acsguis.api.ACsGuiApi;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.client.gui.GuiBlockCustomization;
import fr.dynamx.client.gui.GuiSoftbodyConfig;
import fr.dynamx.client.renders.mesh.shapes.FacesMesh;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.type.objects.AbstractItemObject;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.SoftbodyEntity;
import fr.dynamx.common.entities.vehicles.CarEntity;
import fr.dynamx.common.items.DynamXItem;
import fr.dynamx.common.items.ItemModularEntity;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nonnull;

public class ItemSoftbody <T extends AbstractItemObject<T, ?>> extends DynamXItem<T> {

    PhysicsSoftBody softBody;

    public ItemSoftbody() {
        super(DynamXConstants.ID, "softbody", new ResourceLocation(DynamXConstants.ID, "softbody"));
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
       /* if(hand.equals(EnumHand.OFF_HAND)) return EnumActionResult.FAIL;
        if(player.isSneaking()) {
            softBody = new PhysicsSoftBody();
            NativeSoftBodyUtil.appendFromTriMesh(DynamXRenderUtils.icosphereMesh, softBody);
            FacesMesh facesMesh = new FacesMesh(softBody);
            ACsGuiApi.asyncLoadThenShowGui("Block Customization", () -> new GuiSoftbodyConfig(facesMesh, softBody));
        }*/

        return super.onItemUse(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
    }

    @Override
    @Nonnull
    public ActionResult<ItemStack> onItemRightClick(@Nonnull World worldIn, EntityPlayer playerIn, @Nonnull EnumHand hand) {
        ItemStack itemstack = playerIn.getHeldItem(hand);
        if(hand.equals(EnumHand.OFF_HAND)) return new ActionResult<>(EnumActionResult.FAIL, itemstack);
        if(playerIn.isSneaking()) {
            softBody = new PhysicsSoftBody();
            softBody.setUserObject(new BulletShapeType<>(EnumBulletShapeType.BULLET_ENTITY, null, softBody.getCollisionShape()));
            NativeSoftBodyUtil.appendFromTriMesh(DynamXRenderUtils.icosphereMesh, softBody);
            FacesMesh facesMesh = new FacesMesh(softBody);

            softBody.setPose(false, true);
            SoftBodyConfig config = softBody.getSoftConfig();
            config.set(Sbcp.PoseMatching, 0.05f);
            softBody.setPhysicsLocation(new Vector3f(0,0,0));
            //softBody.applyScale(new Vector3f(10,10,10));
            softBody.setCcdSweptSphereRadius(0.7f);
            softBody.setCcdMotionThreshold(0.7f);
            softBody.setMargin(0.1f);
            for (int i = 0; i < softBody.countNodes(); i++) {
                softBody.setNodeMass(i, 0);
            }
            DynamXContext.getPhysicsWorld(worldIn).addCollisionObject(softBody);
            ACsGuiApi.asyncLoadThenShowGui("Block Customization", () -> new GuiSoftbodyConfig(facesMesh, softBody));
        }
        /*if (hand == EnumHand.MAIN_HAND) {
            RayTraceResult raytraceresult = DynamXUtils.rayTraceEntitySpawn(worldIn, playerIn, hand);
            if (raytraceresult == null) {
                return new ActionResult<>(EnumActionResult.PASS, itemstack);
            }
            if (raytraceresult.typeOfHit == RayTraceResult.Type.BLOCK || raytraceresult.typeOfHit == RayTraceResult.Type.ENTITY) {
                BlockPos blockpos = raytraceresult.getBlockPos();
                if (raytraceresult.typeOfHit == RayTraceResult.Type.ENTITY) {
                    blockpos = raytraceresult.entityHit.getPosition();
                }

                if (worldIn.getBlockState(blockpos).getBlock() == Blocks.SNOW_LAYER) {
                    blockpos = blockpos.down();
                }

                if (!spawnEntity(itemstack, worldIn, playerIn, raytraceresult.hitVec)) {
                    return new ActionResult<>(EnumActionResult.FAIL, itemstack);
                }

                if (!playerIn.capabilities.isCreativeMode) {
                    itemstack.grow(-1);
                }

                playerIn.addStat(StatList.getObjectUseStats(this));
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, itemstack);
        }*/
        return new ActionResult<>(EnumActionResult.FAIL, itemstack);
    }

    public boolean spawnEntity(ItemStack itemStackIn, World worldIn, EntityPlayer playerIn, Vec3d blockPos) {
        if (!worldIn.isRemote) {
            SoftbodyEntity entity = new SoftbodyEntity(worldIn, Vector3fPool.get((float) blockPos.x, (float) blockPos.y + 1F, (float) blockPos.z), playerIn.rotationYaw % 360.0F);
            //if (!MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.Spawn(worldIn, entity, playerIn, this, blockPos)))
            worldIn.spawnEntity(entity);
        }
        return true;
    }

}