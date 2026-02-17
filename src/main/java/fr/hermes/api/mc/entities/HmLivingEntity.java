package fr.hermes.api.mc.entities;

import fr.hermes.api.mc.items.HmItemStack;
import net.minecraft.util.EnumHand;

public interface HmLivingEntity extends HmEntity {
    float hm$getLimbSwing();
    void hm$setLimbSwing(float limbSwing);
    float hm$getLimbSwingAmount(); // TODO UNDERSTAND THIS AND FIND A BETTER NAME
    void hm$setLimbSwingAmount(float amount);
    float hm$getPrevLimbSwingAmount();
    void hm$setPrevLimbSwingAmount(float amount);

    HmItemStack hm$getHeldItemMainHand();

    HmItemStack hm$getHeldItem(EnumHand hand);
}
