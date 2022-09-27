package fr.dynamx.client.renders;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.client.renders.model.ModelObjArmor;
import fr.dynamx.common.entities.RagdollEntity;
import fr.dynamx.common.items.DynamXItemArmor;
import fr.dynamx.common.physics.entities.EnumRagdollBodyPart;
import fr.dynamx.utils.client.ClientDynamXUtils;
import fr.dynamx.utils.optimization.BoundingBoxPool;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

public class RenderRagdoll<T extends RagdollEntity> extends RenderPhysicsEntity<T> {
    private final ModelPlayer modelFat = new ModelPlayer(0, false);
    private final ModelPlayer modelLight = new ModelPlayer(0, true);

    public RenderRagdoll(RenderManager manager) {
        super(manager);
        MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.InitPhysicEntityRenderEvent<>(RagdollEntity.class, this));
    }

    @Override
    protected org.lwjgl.util.vector.Quaternion setupRenderTransform(T entity, double x, double y, double z, float entityYaw, float partialTicks) {
        GlStateManager.translate((float) x - (entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks),
                (float) y - (entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks),
                (float) z - (entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks));
        // GlStateManager.rotate(rotation);
        return GlQuaternionPool.get();
    }

    @Override
    public void renderMain(T entity, float partialsTicks) {
        if (entity.isInvisible())
            return;
        BoundingBoxPool.getPool().openSubPool();

        String useSkin = entity.getSkin();
        ResourceLocation texture;
        ModelPlayer model = modelFat;
        if (useSkin.contains(":")) {
            texture = new ResourceLocation(useSkin);
        } else {
            EntityPlayer p = Minecraft.getMinecraft().world.getPlayerEntityByName(useSkin);
            if (p == null)
                p = Minecraft.getMinecraft().player;
            String skinType = ((AbstractClientPlayer) p).getSkinType();
            model = "default".equals(skinType) ? modelFat : modelLight;
            texture = ((AbstractClientPlayer) p).getLocationSkin();
        }
        bindTexture(texture);

        GlStateManager.pushMatrix();
        // GlStateManager.disableLighting();

        //Chest
        renderBodyPart(entity, EnumRagdollBodyPart.CHEST, model.bipedBody, partialsTicks, texture);
        //Right arm
        renderBodyPart(entity, EnumRagdollBodyPart.RIGHT_ARM, model.bipedRightArm, partialsTicks, texture);
        //Left Arm
        renderBodyPart(entity, EnumRagdollBodyPart.LEFT_ARM, model.bipedLeftArm, partialsTicks, texture);
        //Head
        renderBodyPart(entity, EnumRagdollBodyPart.HEAD, model.bipedHead, partialsTicks, texture);
        //Right Leg
        renderBodyPart(entity, EnumRagdollBodyPart.RIGHT_LEG, model.bipedRightLeg, partialsTicks, texture);
        //Left Leg
        renderBodyPart(entity, EnumRagdollBodyPart.LEFT_LEG, model.bipedLeftLeg, partialsTicks, texture);

        // GlStateManager.enableLighting();
        GlStateManager.popMatrix();

        BoundingBoxPool.getPool().closeSubPool();
    }

    private void renderBodyPart(T ragdollPhysics, EnumRagdollBodyPart enumBodyPart, ModelRenderer model, float partialTicks, ResourceLocation texture) {
        GlStateManager.pushMatrix();

        Vector3f pos = ragdollPhysics.getTransforms().get((byte) enumBodyPart.ordinal()).getTransform().getPosition();
        Quaternion rot = ragdollPhysics.getTransforms().get((byte) enumBodyPart.ordinal()).getTransform().getRotation();
        Vector3f prevPos = ragdollPhysics.getTransforms().get((byte) enumBodyPart.ordinal()).getPrevTransform().getPosition();
        Quaternion prevRot = ragdollPhysics.getTransforms().get((byte) enumBodyPart.ordinal()).getPrevTransform().getRotation();

        GlStateManager.translate(prevPos.x + (pos.x - prevPos.x) * partialTicks, prevPos.y + (pos.y - prevPos.y) * partialTicks, prevPos.z + (pos.z - prevPos.z) * partialTicks);

        GlStateManager.rotate(ClientDynamXUtils.computeInterpolatedGlQuaternion(prevRot, rot, partialTicks));

        GlStateManager.rotate(180, 1, 0, 0);
        switch (enumBodyPart) {
            case HEAD:
                GlStateManager.translate(0, 0.25f, 0);

                ModelObjArmor armor = ragdollPhysics.getItemStackFromSlot(EntityEquipmentSlot.HEAD).getItem() instanceof DynamXItemArmor ?
                        ((DynamXItemArmor<?>) ragdollPhysics.getItemStackFromSlot(EntityEquipmentSlot.HEAD).getItem()).getInfo().getObjArmor() : null;
                if (armor != null) {
                    armor.renderHead(0.625f);
                }
                break;
            case CHEST:
                GlStateManager.translate(0, -0.375f, 0);

                armor = ragdollPhysics.getItemStackFromSlot(EntityEquipmentSlot.CHEST).getItem() instanceof DynamXItemArmor ?
                        ((DynamXItemArmor<?>) ragdollPhysics.getItemStackFromSlot(EntityEquipmentSlot.CHEST).getItem()).getInfo().getObjArmor() : null;
                if (armor != null) {
                    armor.renderChest(0.0625f);
                }
                break;
            case RIGHT_ARM:
                armor = ragdollPhysics.getItemStackFromSlot(EntityEquipmentSlot.CHEST).getItem() instanceof DynamXItemArmor ?
                        ((DynamXItemArmor<?>) ragdollPhysics.getItemStackFromSlot(EntityEquipmentSlot.CHEST).getItem()).getInfo().getObjArmor() : null;
                if (armor != null) {
                    //GlStateManager.translate(0.09, -0.225, 0);
                    GlStateManager.translate(0.369, -0.370, 0);
                    armor.renderRightArm(0.0625f);
                    GlStateManager.translate(0, -0.01, 0);
                    //GlStateManager.translate(0.369 - 0.09, -0.375 + 0.225, 0);
                } else {
                    GlStateManager.translate(0.369, -0.375, 0);
                }
                break;
            case LEFT_ARM:
                armor = ragdollPhysics.getItemStackFromSlot(EntityEquipmentSlot.CHEST).getItem() instanceof DynamXItemArmor ?
                        ((DynamXItemArmor<?>) ragdollPhysics.getItemStackFromSlot(EntityEquipmentSlot.CHEST).getItem()).getInfo().getObjArmor() : null;
                if (armor != null) {
                    //GlStateManager.translate(-0.09, -0.225, 0);
                    GlStateManager.translate(-0.369, -0.370, 0);
                    armor.renderLeftArm(0.0625f);
                    GlStateManager.translate(0, -0.01, 0);
                    //GlStateManager.translate(-0.369 + 0.09, -0.375 + 0.225, 0);
                } else {
                    GlStateManager.translate(-0.369, -0.375, 0);
                }
                // DynamXObjectLoaders.ARMORS.owners.get(0).getInfo().getObjArmor().setActivePart(EntityEquipmentSlot.CHEST, (byte) 0);
                break;
            case RIGHT_LEG:
                armor = ragdollPhysics.getItemStackFromSlot(EntityEquipmentSlot.LEGS).getItem() instanceof DynamXItemArmor ?
                        ((DynamXItemArmor<?>) ragdollPhysics.getItemStackFromSlot(EntityEquipmentSlot.LEGS).getItem()).getInfo().getObjArmor() : null;
                if (armor != null) {
                    //GlStateManager.translate(0.01, -0.376, 0);
                    GlStateManager.translate(0.125, -1.125, 0);
                    armor.renderRightLeg(0.0625f);
                    //GlStateManager.translate(0.125 - 0.01, -1.125 + 0.376, 0);
                } else {
                    GlStateManager.translate(0.125, -1.125, 0);
                }
                break;
            case LEFT_LEG:
                armor = ragdollPhysics.getItemStackFromSlot(EntityEquipmentSlot.LEGS).getItem() instanceof DynamXItemArmor ?
                        ((DynamXItemArmor<?>) ragdollPhysics.getItemStackFromSlot(EntityEquipmentSlot.LEGS).getItem()).getInfo().getObjArmor() : null;
                if (armor != null) {
                    //GlStateManager.translate(0.025, -0.376, 0);
                    GlStateManager.translate(-0.1, -1.125, 0);
                    armor.renderLeftLeg(0.0625f);
                    //GlStateManager.translate(-0.1 - 0.025, -1.125 + 0.376, 0);
                } else {
                    GlStateManager.translate(-0.1, -1.125, 0);
                }
                break;
        }
        bindTexture(texture);
        model.render(0.0625f);

        GlStateManager.popMatrix();
    }

    @Override
    public void renderParts(T entity, float partialTicks) {
    }

    @Override
    public void spawnParticles(T entity, float partialTicks) {
    }
}
