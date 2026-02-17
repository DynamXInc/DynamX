package fr.hermes.forge.abstracted.entities;

import fr.dynamx.core.utils.DynamXConstants;
import fr.hermes.api.mc.entities.HmLivingEntity;
import fr.hermes.api.mc.items.HmItemStack;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

// TODO CHANGE REMAP TARGET
@Mixin(value = EntityLivingBase.class, remap = DynamXConstants.REMAP)
public abstract class MixinLivingEntity implements HmLivingEntity {
    @Shadow
    public float limbSwing;

    @Shadow
    public float limbSwingAmount;

    @Shadow
    public float prevLimbSwingAmount;

    @Shadow
    public abstract ItemStack getHeldItemMainhand();

    @Shadow
    public abstract ItemStack getHeldItem(EnumHand hand);

    @Override
    public float hm$getLimbSwing() {
        return limbSwing;
    }

    @Override
    public void hm$setLimbSwing(float limbSwing) {
        this.limbSwing = limbSwing;
    }

    @Override
    public float hm$getLimbSwingAmount() {
        return limbSwingAmount;
    }

    @Override
    public void hm$setLimbSwingAmount(float amount) {
        this.limbSwingAmount = amount;
    }

    @Override
    public float hm$getPrevLimbSwingAmount() {
        return prevLimbSwingAmount;
    }

    @Override
    public void hm$setPrevLimbSwingAmount(float amount) {
        this.prevLimbSwingAmount = amount;
    }

    @Override
    public HmItemStack hm$getHeldItemMainHand() {
        return (HmItemStack) (Object) getHeldItemMainhand();
    }

    @Override
    public HmItemStack hm$getHeldItem(EnumHand hand) {
        return (HmItemStack) (Object) getHeldItem(hand);
    }
}
