package fr.dynamx.core.common.items.tools;

import fr.dynamx.core.common.entities.PhysicsEntity;
import fr.dynamx.core.common.items.DynamXItemRegistry;
import fr.dynamx.core.server.command.CmdShockWave;
import fr.dynamx.core.utils.DynamXConstants;
import fr.dynamx.core.utils.RegistryNameSetter;
import fr.dynamx.core.utils.optimization.Vector3fPool;
import fr.dynamx.core.utils.physics.DynamXPhysicsHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

import java.util.List;

public class ItemShockWave extends Item {
    public ItemShockWave() {
        super();
        RegistryNameSetter.setRegistryName(this, DynamXConstants.ID, "shockwave");
        setTranslationKey(DynamXConstants.ID + "." + "shockwave");
        setCreativeTab(DynamXItemRegistry.vehicleTab);
        DynamXItemRegistry.add(this);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        List<PhysicsEntity> entities = worldIn.getEntitiesWithinAABB(PhysicsEntity.class, playerIn.getEntityBoundingBox().grow(20));
        entities.forEach(physicsEntity -> DynamXPhysicsHelper.createExplosion(physicsEntity, Vector3fPool.get((float) playerIn.posX, (float) playerIn.posY, (float) playerIn.posZ), CmdShockWave.explosionForce));
        return new ActionResult<ItemStack>(EnumActionResult.PASS, playerIn.getHeldItem(handIn));
    }
}
