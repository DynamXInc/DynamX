package fr.dynamx.client.renders.model;

import fr.aym.acslib.api.services.ErrorTrackingService;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.type.objects.ArmorObject;
import fr.dynamx.utils.DynamXLoadingTasks;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

public class ModelObjArmor extends ModelBiped {
    private final ArmorObject<?> armorObject;
    private ArmorRenderer head;
    private ArmorRenderer body;
    private ArmorRenderer[] arms;
    private ArmorRenderer[] legs;
    private ArmorRenderer[] foot;

    private EntityEquipmentSlot activePart;
    private byte activeTextureId;

    public ModelObjArmor(ArmorObject<?> armorObject) {
        this.armorObject = armorObject;
    }

    public void init(ObjModelRenderer model) {
        if (armorObject.getArmorHead() != null) {
            head = new ArmorRenderer(model, this, armorObject.getArmorHead());
            setBodyPart(head, bipedHead);
        }
        if (armorObject.getArmorBody() != null) {
            body = new ArmorRenderer(model, this, armorObject.getArmorBody());
            setBodyPart(body, bipedBody);
        }
        if (armorObject.getArmorArms() != null) {
            if (armorObject.getArmorArms().length != 2) {
                DynamXMain.log.warn("Armor " + armorObject.getFullName() + " has one arm in the config, but there should be 2 arms !");
                DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.PACK, armorObject.getFullName(), "Missing arm", "The armor has only one arm part, it should be two !", ErrorTrackingService.TrackedErrorLevel.HIGH);
            } else {
                arms = new ArmorRenderer[]{new ArmorRenderer(model, this, armorObject.getArmorArms()[0]), new ArmorRenderer(model, this, armorObject.getArmorArms()[1])};
                arms[0].mirror = true;
                setBodyPart(arms[0], bipedLeftArm);
                setBodyPart(arms[1], bipedRightArm);
            }
        }
        if (armorObject.getArmorLegs() != null) {
            if (armorObject.getArmorLegs().length != 2) {
                DynamXMain.log.warn("Armor " + armorObject.getFullName() + " has one leg in the config, but there should be 2 legs !");
                DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.PACK, armorObject.getFullName(), "Missing leg", "The armor has only one leg part, it should be two !", ErrorTrackingService.TrackedErrorLevel.HIGH);
            } else {
                legs = new ArmorRenderer[]{new ArmorRenderer(model, this, armorObject.getArmorLegs()[0]), new ArmorRenderer(model, this, armorObject.getArmorLegs()[1])};
                legs[0].mirror = true;
                setBodyPart(legs[0], bipedLeftLeg);
                setBodyPart(legs[1], bipedRightLeg);
            }
        }
        if (armorObject.getArmorFoot() != null) {
            if (armorObject.getArmorFoot().length != 2) {
                DynamXMain.log.warn("Armor " + armorObject.getFullName() + " has one feet in the config, but there should be 2 foot !");
                DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.PACK, armorObject.getFullName(), "Missing feet", "The armor has only one feet part, it should be two !", ErrorTrackingService.TrackedErrorLevel.HIGH);
            } else {
                foot = new ArmorRenderer[]{new ArmorRenderer(model, this, armorObject.getArmorFoot()[0]), new ArmorRenderer(model, this, armorObject.getArmorFoot()[1])};
                foot[0].mirror = true;
                setBodyPart(foot[0], bipedLeftLeg);
                setBodyPart(foot[1], bipedRightLeg);
            }
        }
    }

    /**
     * Sets the currently drawn part
     */
    public void setActivePart(EntityEquipmentSlot activePart, byte textureId) {
        this.activePart = activePart;
        this.activeTextureId = textureId;
    }

    /**
     * @return the currently drawn part
     */
    public EntityEquipmentSlot getActivePart() {
        return activePart;
    }

    public byte getActiveTextureId() {
        return activeTextureId;
    }

    @Override
    public void setModelAttributes(ModelBase model) {
        super.setModelAttributes(model);
        ModelBiped base = (ModelBiped) model;
        if (head != null)
            copyModelAnglesForArmor(base.bipedHead, head);
        if (body != null)
            copyModelAnglesForArmor(base.bipedBody, body);
        if (arms != null) {
            copyModelAnglesForArmor(base.bipedLeftArm, arms[0]);
            copyModelAnglesForArmor(base.bipedRightArm, arms[1]);
        }
        if (legs != null) {
            copyModelAnglesForArmor(base.bipedLeftLeg, legs[0]);
            copyModelAnglesForArmor(base.bipedRightLeg, legs[1]);
        }
        if (foot != null) {
            copyModelAnglesForArmor(base.bipedLeftLeg, foot[0]);
            copyModelAnglesForArmor(base.bipedRightLeg, foot[1]);
        }
    }

    @Override
    public void render(@Nullable Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        GlStateManager.pushMatrix();
        //GlStateManager.scale(armorInfo.scale[0],armorInfo.scale[1],armorInfo.scale[2]);
        isSneak = entity != null && entity.isSneaking();
        ItemStack itemstack = entity == null ? ItemStack.EMPTY : ((EntityLivingBase) entity).getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
        rightArmPose = itemstack.isEmpty() ? ArmPose.EMPTY : ArmPose.ITEM;

        if (!itemstack.isEmpty()) {
            EnumAction enumaction = itemstack.getItemUseAction();
            if (enumaction == EnumAction.BLOCK) {
                rightArmPose = ArmPose.BLOCK;
            } else if (enumaction == EnumAction.BOW) {
                rightArmPose = ArmPose.BOW_AND_ARROW;
            }
        }
        if (isSneak) {
            GlStateManager.translate(0.0F, 0.2F, 0.0F);
        }

        switch (getActivePart()) {
            case HEAD: {
                if (head != null) {
                    render(head, scale);
                }
                break;
            }
            case CHEST: {
                if (body != null) {
                    render(body, scale);
                }
                if (arms != null) {
                    render(arms[0], scale);
                    render(arms[1], scale);
                }
                break;
            }
            case LEGS: {
                if (legs != null) {
                    render(legs[0], scale);
                    render(legs[1], scale);
                }
                break;
            }
            case FEET: {
                if (foot != null) {
                    render(foot[0], scale);
                    render(foot[1], scale);
                }
                break;
            }
        }
        GlStateManager.popMatrix();
    }

    public void renderHead(float scale) {
        if (head != null) {
            setModelAttributes(this); //Reset rotations
            head.render(scale);
        }
    }

    public void renderChest(float scale) {
        if (body != null) {
            setModelAttributes(this); //Reset rotations
            body.render(scale);
        }
    }

    public void renderLeftArm(float scale) {
        if (arms != null) {
            setModelAttributes(this); //Reset rotations
            render(arms[0], scale);
        }
    }

    public void renderRightArm(float scale) {
        if (arms != null) {
            setModelAttributes(this); //Reset rotations
            render(arms[1], scale);
        }
    }

    public void renderLeftLeg(float scale) {
        if (legs != null) {
            setModelAttributes(this); //Reset rotations
            render(legs[0], scale);
        }
    }

    public void renderRightLeg(float scale) {
        if (legs != null) {
            setModelAttributes(this); //Reset rotations
            render(legs[1], scale);
        }
    }

    protected void render(ArmorRenderer armor, float scale) {
        armor.render(scale);
    }

    private static void copyModelAnglesForArmor(ModelRenderer bodyPart, ModelRenderer armor) {
        armor.rotationPointX = bodyPart.rotationPointX / 16f;
        armor.rotationPointY = bodyPart.rotationPointY / 16f;
        armor.rotationPointZ = bodyPart.rotationPointZ / 16f;

        armor.rotateAngleX = bodyPart.rotateAngleX;
        armor.rotateAngleY = -bodyPart.rotateAngleY;
        armor.rotateAngleZ = -bodyPart.rotateAngleZ;
    }

    private void setBodyPart(ArmorRenderer armor, ModelRenderer bodyPart) {
        armor.offsetX = bodyPart.rotationPointX / 16f;
        armor.offsetY = bodyPart.rotationPointY / 16f;
        armor.offsetZ = bodyPart.rotationPointZ / 16f;
    }
}
