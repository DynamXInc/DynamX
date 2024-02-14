package fr.dynamx.client.renders.scene;

import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.ItemDxModel;
import fr.dynamx.client.renders.model.ModelObjArmor;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.entities.ModularPhysicsEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Base class for the render context of a scene node
 *
 * @see IRenderContext
 * @see SceneNode
 */
@Getter
public abstract class BaseRenderContext implements IRenderContext {
    private DxModelRenderer model;
    private byte textureId;
    private float partialTicks;
    private boolean useVanillaRender;

    protected BaseRenderContext setModelParams(@Nonnull DxModelRenderer model, byte textureId) {
        this.model = model;
        this.textureId = textureId;
        return this;
    }

    protected BaseRenderContext setRenderParams(float partialTicks, boolean useVanillaRender) {
        this.partialTicks = partialTicks;
        this.useVanillaRender = useVanillaRender;
        return this;
    }

    /**
     * The context used when rendering a DynamX entity <br>
     * Reused to avoid creating a new object each time.
     *
     * @see fr.dynamx.client.renders.scene.node.EntityNode
     */
    @Getter
    @RequiredArgsConstructor
    public static class EntityRenderContext extends BaseRenderContext {
        private final RenderPhysicsEntity<?> render;
        private final Vector3f renderPosition = new Vector3f();
        /**
         * The entity that is rendered, can be null if we are rendering a static scene graph (like in the inventory)
         */
        @Nullable
        private ModularPhysicsEntity<?> entity;

        public EntityRenderContext setModelParams(@Nullable ModularPhysicsEntity<?> entity, @Nonnull DxModelRenderer model, byte textureId) {
            this.entity = entity;
            return (EntityRenderContext) super.setModelParams(model, textureId);
        }

        @Override
        public BaseRenderContext setModelParams(@Nonnull DxModelRenderer model, byte textureId) {
            return setModelParams(null, model, textureId);
        }

        public EntityRenderContext setRenderParams(double x, double y, double z, float partialTicks, boolean useVanillaRender) {
            renderPosition.set((float) x, (float) y, (float) z);
            return (EntityRenderContext) super.setRenderParams(partialTicks, useVanillaRender);
        }
    }

    /**
     * The context used when rendering a DynamX block <br>
     * Reused to avoid creating a new object each time.
     *
     * @see fr.dynamx.client.renders.scene.node.BlockNode
     */
    @Getter
    public static class BlockRenderContext extends BaseRenderContext {
        private final Vector3f renderPosition = new Vector3f();
        /**
         * The tile entity that is being rendered, can be null if we are rendering a static scene graph (like in the inventory)
         */
        @Nullable
        private TEDynamXBlock tileEntity;

        public BlockRenderContext setModelParams(@Nullable TEDynamXBlock tileEntity, @Nonnull DxModelRenderer model, byte textureId) {
            this.tileEntity = tileEntity;
            return (BlockRenderContext) super.setModelParams(model, textureId);
        }

        @Override
        public BaseRenderContext setModelParams(@Nonnull DxModelRenderer model, byte textureId) {
            return setModelParams(null, model, textureId);
        }

        public BlockRenderContext setRenderParams(double x, double y, double z, float partialTicks, boolean useVanillaRender) {
            renderPosition.set((float) x, (float) y, (float) z);
            return (BlockRenderContext) super.setRenderParams(partialTicks, useVanillaRender);
        }
    }

    /**
     * The context used when rendering an item <br>
     * Reused to avoid creating a new object each time.
     *
     * @see fr.dynamx.client.renders.scene.node.ItemNode
     */
    @Getter
    @RequiredArgsConstructor
    public static class ItemRenderContext extends BaseRenderContext {
        private ItemDxModel itemModel;
        private ItemCameraTransforms.TransformType renderType;
        /**
         * The item that is being rendered, not null
         */
        private ItemStack stack;

        public ItemRenderContext setModelParams(@Nonnull ItemDxModel itemModel, @Nonnull ItemStack stack, @Nonnull DxModelRenderer model, byte textureId) {
            this.itemModel = itemModel;
            this.stack = stack;
            return (ItemRenderContext) super.setModelParams(model, textureId);
        }

        public ItemRenderContext setRenderParams(@Nonnull ItemCameraTransforms.TransformType renderType, float partialTicks, boolean useVanillaRender) {
            this.renderType = renderType;
            return (ItemRenderContext) super.setRenderParams(partialTicks, useVanillaRender);
        }
    }

    /**
     * The context used when rendering an armor <br>
     * Reused to avoid creating a new object each time.
     *
     * @see fr.dynamx.client.renders.scene.node.ArmorNode
     */
    @Getter
    @RequiredArgsConstructor
    public static class ArmorRenderContext extends BaseRenderContext {
        private final ModelObjArmor armorModel;
        /**
         * The entity wearing the armor, can be null if we are rendering a static scene graph (like in the inventory)
         */
        @Nullable
        private EntityLivingBase entity;
        private EntityEquipmentSlot equipmentSlot;

        public ArmorRenderContext setModelParams(@Nullable EntityLivingBase entity, @Nonnull EntityEquipmentSlot equipmentSlot, @Nonnull DxModelRenderer model, byte textureId) {
            this.entity = entity;
            this.equipmentSlot = equipmentSlot;
            setRenderParams(1, true);
            return (ArmorRenderContext) super.setModelParams(model, textureId);
        }
    }
}
