package fr.dynamx.client.renders.model;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.client.renders.model.renderer.ArmorRenderer;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.contentpack.type.objects.ArmorObject;
import fr.dynamx.utils.errors.DynamXErrorManager;
import lombok.Getter;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import org.joml.Matrix4f;

import javax.annotation.Nullable;

public class ModelObjArmor extends ModelBiped {
    private static final Matrix4f tempTransform = new Matrix4f();
    private final ArmorObject<?> armorObject;
    private final DxModelRenderer model;
    private final BaseRenderContext.ArmorRenderContext renderContext = new BaseRenderContext.ArmorRenderContext(this);
    private ArmorRenderer head;
    private ArmorRenderer body;
    private ArmorRenderer[] arms;
    private ArmorRenderer[] legs;
    private ArmorRenderer[] foot;

    /**
     * -- GETTER --
     *
     * @return the currently drawn part
     */
    @Getter
    private EntityEquipmentSlot activePart;
    @Getter
    private byte activeTextureId;

    public ModelObjArmor(ArmorObject<?> armorObject, DxModelRenderer model) {
        this.armorObject = armorObject;
        this.model = model;
        if (armorObject.getArmorHead() != null) {
            head = new ArmorRenderer(model, this, armorObject.getArmorHead());
            setBodyPart(head, bipedHead);
        }
        if (armorObject.getArmorBody() != null) {
            body = new ArmorRenderer(model, this, armorObject.getArmorBody());
            setBodyPart(body, bipedBody);
        }
        if (armorObject.getArmorArms() != null) {
            if (armorObject.getArmorArms().length == 2) {
                arms = new ArmorRenderer[]{new ArmorRenderer(model, this, armorObject.getArmorArms()[0]), new ArmorRenderer(model, this, armorObject.getArmorArms()[1])};
                arms[0].mirror = true;
                setBodyPart(arms[0], bipedLeftArm);
                setBodyPart(arms[1], bipedRightArm);
            } else
                DynamXErrorManager.addError(armorObject.getPackName(), DynamXErrorManager.MODEL_ERRORS, "armor_error", ErrorLevel.HIGH, armorObject.getName(), "The armor has only one arm part, it should be two !");
        }
        if (armorObject.getArmorLegs() != null) {
            if (armorObject.getArmorLegs().length == 2) {
                legs = new ArmorRenderer[]{new ArmorRenderer(model, this, armorObject.getArmorLegs()[0]), new ArmorRenderer(model, this, armorObject.getArmorLegs()[1])};
                legs[0].mirror = true;
                setBodyPart(legs[0], bipedLeftLeg);
                setBodyPart(legs[1], bipedRightLeg);
            } else
                DynamXErrorManager.addError(armorObject.getPackName(), DynamXErrorManager.MODEL_ERRORS, "armor_error", ErrorLevel.HIGH, armorObject.getName(), "The armor has only one leg part, it should be two !");
        }
        if (armorObject.getArmorFoot() != null) {
            if (armorObject.getArmorFoot().length == 2) {
                foot = new ArmorRenderer[]{new ArmorRenderer(model, this, armorObject.getArmorFoot()[0]), new ArmorRenderer(model, this, armorObject.getArmorFoot()[1])};
                foot[0].mirror = true;
                setBodyPart(foot[0], bipedLeftLeg);
                setBodyPart(foot[1], bipedRightLeg);
            } else
                DynamXErrorManager.addError(armorObject.getPackName(), DynamXErrorManager.MODEL_ERRORS, "armor_error", ErrorLevel.HIGH, armorObject.getName(), "The armor has only one feet part, it should be two !");
        }
    }

    /**
     * Sets the currently drawn part
     */
    public void setActivePart(EntityEquipmentSlot activePart, byte textureId) {
        this.activePart = activePart;
        this.activeTextureId = textureId;
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
        ItemStack itemstack = entity == null ? ItemStack.EMPTY : ((EntityLivingBase) entity).getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
        if (!itemstack.isEmpty()) {
            EnumAction enumaction = itemstack.getItemUseAction();
            if (enumaction == EnumAction.BLOCK) {
                rightArmPose = ModelBiped.ArmPose.BLOCK;
            } else if (enumaction == EnumAction.BOW) {
                rightArmPose = ModelBiped.ArmPose.BOW_AND_ARROW;
            } else {
                rightArmPose = ModelBiped.ArmPose.ITEM;
            }
        } else {
            rightArmPose = ModelBiped.ArmPose.EMPTY;
        }
        renderContext.setModelParams((EntityLivingBase) entity, getActivePart(), model, (byte) (armorObject.getMaxTextureMetadata() > 1 ? itemstack.getMetadata() : 0));
        ((SceneNode<BaseRenderContext.ArmorRenderContext, ArmorObject<?>>) armorObject.getSceneGraph()).render(renderContext, armorObject);
    }

    public void renderPart(Matrix4f transform, EntityEquipmentSlot part) {
        switch (part) {
            case HEAD: {
                if (head != null) {
                    renderPart(transform, head);
                }
                break;
            }
            case CHEST: {
                if (body != null) {
                    renderPart(transform, body);
                }
                if (arms != null) {
                    renderPart(transform, arms[0]);
                    renderPart(transform, arms[1]);
                }
                break;
            }
            case LEGS: {
                if (legs != null) {
                    renderPart(transform, legs[0]);
                    renderPart(transform, legs[1]);
                }
                break;
            }
            case FEET: {
                if (foot != null) {
                    renderPart(transform, foot[0]);
                    renderPart(transform, foot[1]);
                }
                break;
            }
        }
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
            renderPart(arms[0], scale);
        }
    }

    public void renderRightArm(float scale) {
        if (arms != null) {
            setModelAttributes(this); //Reset rotations
            renderPart(arms[1], scale);
        }
    }

    public void renderLeftLeg(float scale) {
        if (legs != null) {
            setModelAttributes(this); //Reset rotations
            renderPart(legs[0], scale);
        }
    }

    public void renderRightLeg(float scale) {
        if (legs != null) {
            setModelAttributes(this); //Reset rotations
            renderPart(legs[1], scale);
        }
    }

    protected void renderPart(ArmorRenderer armor, float scale) {
        armor.render(scale);
    }

    protected void renderPart(Matrix4f transform, ArmorRenderer armor) {
        tempTransform.set(transform); // armor.render modifies the transform matrix
        armor.render(tempTransform);
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
