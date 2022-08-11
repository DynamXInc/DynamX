package fr.dynamx.client.renders.vehicle;

import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.events.VehicleEntityEvent.RenderVehicleEntityEvent;
import fr.dynamx.api.events.VehicleEntityEvent.RenderVehicleEntityEvent.Type;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.renderer.ObjModelRenderer;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.vehicles.BoatEntity;
import fr.dynamx.common.entities.vehicles.CarEntity;
import fr.dynamx.common.entities.vehicles.TrailerEntity;
import fr.dynamx.utils.debug.renderer.BoatDebugRenderer;
import fr.dynamx.utils.debug.renderer.DebugRenderer;
import fr.dynamx.utils.debug.renderer.VehicleDebugRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.common.MinecraftForge;

public class RenderBaseVehicle<T extends BaseVehicleEntity<?>> extends RenderPhysicsEntity<T> {
    public RenderBaseVehicle(RenderManager manager) {
        super(manager);
        addDebugRenderers(new DebugRenderer.HullDebug());
        MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.InitPhysicEntityRenderEvent<>(BaseVehicleEntity.class, this));
    }

    @Override
    public void renderMain(T carEntity, float partialTicks) {
        if (!MinecraftForge.EVENT_BUS.post(new RenderVehicleEntityEvent(Type.CHASSIS, this, carEntity, PhysicsEntityEvent.Phase.PRE, partialTicks)) && carEntity.getPackInfo().isModelValid()) {
            /* Rendering the chassis */
            ObjModelRenderer vehicleModel = DynamXContext.getObjModelRegistry().getModel(carEntity.getPackInfo().getModel());
            GlStateManager.scale(carEntity.getPackInfo().getScaleModifier().x, carEntity.getPackInfo().getScaleModifier().y, carEntity.getPackInfo().getScaleModifier().z);
            renderMainModel(vehicleModel, carEntity, carEntity.getEntityTextureID());
            GlStateManager.scale(1 / carEntity.getPackInfo().getScaleModifier().x, 1 / carEntity.getPackInfo().getScaleModifier().y, 1 / carEntity.getPackInfo().getScaleModifier().z);
        }
        MinecraftForge.EVENT_BUS.post(new RenderVehicleEntityEvent(Type.CHASSIS, this, carEntity, PhysicsEntityEvent.Phase.POST, partialTicks));
    }

    @Override
    public void renderParts(T carEntity, float partialTicks) {
        if (!MinecraftForge.EVENT_BUS.post(new RenderVehicleEntityEvent(Type.PARTS, this, carEntity, PhysicsEntityEvent.Phase.PRE, partialTicks))) {
            if (carEntity.getPackInfo().isModelValid()) {
                carEntity.getDrawableModules().forEach(d -> ((IPhysicsModule.IDrawableModule<T>) d).drawParts(this, partialTicks, carEntity));
            }
        }
        MinecraftForge.EVENT_BUS.post(new RenderVehicleEntityEvent(Type.PARTS, this, carEntity, PhysicsEntityEvent.Phase.POST, partialTicks));
    }

    @Override
    public void spawnParticles(T carEntity, float partialTicks) {
        super.spawnParticles(carEntity, partialTicks);
        if (!MinecraftForge.EVENT_BUS.post(new RenderVehicleEntityEvent(Type.PARTICLES, this, carEntity, PhysicsEntityEvent.Phase.PRE, partialTicks))) {
            if (carEntity instanceof IModuleContainer.IPropulsionContainer) {
                ((IModuleContainer.IPropulsionContainer<?>) carEntity).getPropulsion().spawnPropulsionParticles(this, partialTicks);
            }
            MinecraftForge.EVENT_BUS.post(new RenderVehicleEntityEvent(Type.PARTICLES, this, carEntity, PhysicsEntityEvent.Phase.POST, partialTicks));
        }
    }

    @Override
    public boolean canRender(T entity) {
        return super.canRender(entity) && entity.getEntityTextureID() != -1;
    }

    public static class RenderCar<T extends CarEntity<?>> extends RenderBaseVehicle<T> {
        public RenderCar(RenderManager manager) {
            super(manager);
            VehicleDebugRenderer.addAll(this, true);
        }
    }

    public static class RenderTrailer<T extends TrailerEntity<?>> extends RenderBaseVehicle<T> {
        public RenderTrailer(RenderManager manager) {
            super(manager);
            VehicleDebugRenderer.addAll(this, false);
        }
    }

    public static class RenderBoat<T extends BoatEntity<?>> extends RenderBaseVehicle<T> {
        public RenderBoat(RenderManager manager) {
            super(manager);
            BoatDebugRenderer.addAll(this);
        }
    }

    /*public static final ResourceLocation SHADOW_TEXTURES = new ResourceLocation(DynamXMain.ID,"textures/shadow.png");
    public void renderShadow(Entity p_renderShadow_1_, double p_renderShadow_2_, double p_renderShadow_4_, double p_renderShadow_6_, float p_renderShadow_8_, float p_renderShadow_9_) {

        shadowSize = 3;

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        this.renderManager.renderEngine.bindTexture(new ResourceLocation(DynamXMain.ID,"textures/shadow.png"));
        World lvt_10_1_ = this.getWorldFromRenderManager();
        GlStateManager.depthMask(false);
        float lvt_11_1_ = this.shadowSize;
        if (p_renderShadow_1_ instanceof EntityLiving) {
            EntityLiving lvt_12_1_ = (EntityLiving)p_renderShadow_1_;
            lvt_11_1_ *= lvt_12_1_.getRenderSizeModifier();
            if (lvt_12_1_.isChild()) {
                lvt_11_1_ *= 0.5F;
            }
        }

        double lvt_12_2_ = p_renderShadow_1_.lastTickPosX + (p_renderShadow_1_.posX - p_renderShadow_1_.lastTickPosX) * (double)p_renderShadow_9_;
        double lvt_14_1_ = p_renderShadow_1_.lastTickPosY + (p_renderShadow_1_.posY - p_renderShadow_1_.lastTickPosY) * (double)p_renderShadow_9_;
        double lvt_16_1_ = p_renderShadow_1_.lastTickPosZ + (p_renderShadow_1_.posZ - p_renderShadow_1_.lastTickPosZ) * (double)p_renderShadow_9_;
        int lvt_18_1_ = MathHelper.floor(lvt_12_2_ - (double)lvt_11_1_);
        int lvt_19_1_ = MathHelper.floor(lvt_12_2_ + (double)lvt_11_1_);
        int lvt_20_1_ = MathHelper.floor(lvt_14_1_ - (double)lvt_11_1_);
        int lvt_21_1_ = MathHelper.floor(lvt_14_1_);
        int lvt_22_1_ = MathHelper.floor(lvt_16_1_ - (double)lvt_11_1_);
        int lvt_23_1_ = MathHelper.floor(lvt_16_1_ + (double)lvt_11_1_);
        double lvt_24_1_ = p_renderShadow_2_ - lvt_12_2_;
        double lvt_26_1_ = p_renderShadow_4_ - lvt_14_1_;
        double lvt_28_1_ = p_renderShadow_6_ - lvt_16_1_;
        Tessellator lvt_30_1_ = Tessellator.getInstance();
        BufferBuilder lvt_31_1_ = lvt_30_1_.getBuffer();
        lvt_31_1_.begin(7, DefaultVertexFormats.POSITION_TEX_LMAP_COLOR);
        Iterator var32 = BlockPos.getAllInBoxMutable(new BlockPos(lvt_18_1_, lvt_20_1_, lvt_22_1_), new BlockPos(lvt_19_1_, lvt_21_1_, lvt_23_1_)).iterator();

        while(var32.hasNext()) {
            BlockPos lvt_33_1_ = (BlockPos)var32.next();
            IBlockState lvt_34_1_ = lvt_10_1_.getBlockState(lvt_33_1_.down());
            if (lvt_34_1_.getRenderType() != EnumBlockRenderType.INVISIBLE && lvt_10_1_.getLightFromNeighbors(lvt_33_1_) > 3) {
                this.renderShadowSingle(lvt_34_1_, p_renderShadow_2_, p_renderShadow_4_, p_renderShadow_6_, lvt_33_1_, p_renderShadow_8_, lvt_11_1_, lvt_24_1_, lvt_26_1_, lvt_28_1_);
            }
        }
        GlStateManager.color(1, 1, 0, 1);

        lvt_30_1_.draw();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
    }

    private World getWorldFromRenderManager() {
        return this.renderManager.world;
    }

    public void renderShadowSingle(IBlockState p_renderShadowSingle_1_, double p_renderShadowSingle_2_, double p_renderShadowSingle_4_, double p_renderShadowSingle_6_, BlockPos p_renderShadowSingle_8_, float p_renderShadowSingle_9_, float p_renderShadowSingle_10_, double p_renderShadowSingle_11_, double p_renderShadowSingle_13_, double p_renderShadowSingle_15_) {
        if (p_renderShadowSingle_1_.isFullCube()) {
            Tessellator lvt_17_1_ = Tessellator.getInstance();
            BufferBuilder lvt_18_1_ = lvt_17_1_.getBuffer();
            double lvt_19_1_ = 1d;//((double)p_renderShadowSingle_9_ - (p_renderShadowSingle_4_ - ((double)p_renderShadowSingle_8_.getY() + p_renderShadowSingle_13_)) / 2.0D) * 0.5D * (double)this.getWorldFromRenderManager().getLightBrightness(p_renderShadowSingle_8_);
            if (lvt_19_1_ >= 0.0D) {
                if (lvt_19_1_ > 1.0D) {
                    lvt_19_1_ = 1.0D;
                }

                AxisAlignedBB lvt_21_1_ = p_renderShadowSingle_1_.getBoundingBox(this.getWorldFromRenderManager(), p_renderShadowSingle_8_);
                double lvt_22_1_ = (double)p_renderShadowSingle_8_.getX() + lvt_21_1_.minX + p_renderShadowSingle_11_;
                double lvt_24_1_ = (double)p_renderShadowSingle_8_.getX() + lvt_21_1_.maxX + p_renderShadowSingle_11_;
                double lvt_26_1_ = (double)p_renderShadowSingle_8_.getY() + lvt_21_1_.minY + p_renderShadowSingle_13_ + 0.015625D;
                double lvt_28_1_ = (double)p_renderShadowSingle_8_.getZ() + lvt_21_1_.minZ + p_renderShadowSingle_15_;
                double lvt_30_1_ = (double)p_renderShadowSingle_8_.getZ() + lvt_21_1_.maxZ + p_renderShadowSingle_15_;
                float lvt_32_1_ = (float)((p_renderShadowSingle_2_ - lvt_22_1_) / 2.0D / (double)p_renderShadowSingle_10_ + 0.5D);
                float lvt_33_1_ = (float)((p_renderShadowSingle_2_ - lvt_24_1_) / 2.0D / (double)p_renderShadowSingle_10_ + 0.5D);
                float lvt_34_1_ = (float)((p_renderShadowSingle_6_ - lvt_28_1_) / 2.0D / (double)p_renderShadowSingle_10_ + 0.5D);
                float lvt_35_1_ = (float)((p_renderShadowSingle_6_ - lvt_30_1_) / 2.0D / (double)p_renderShadowSingle_10_ + 0.5D);
                GlStateManager.color(1, 1, 0, 1);
                int j = 0;
                int k = 255;
                lvt_18_1_.pos(lvt_22_1_, lvt_26_1_, lvt_28_1_).tex(lvt_32_1_, lvt_34_1_).lightmap(j, k).color(1, 1, 0, (float)lvt_19_1_).endVertex();
                lvt_18_1_.pos(lvt_22_1_, lvt_26_1_, lvt_30_1_).tex(lvt_32_1_, lvt_35_1_).lightmap(j, k).color(1, 1, 0, (float)lvt_19_1_).endVertex();
                lvt_18_1_.pos(lvt_24_1_, lvt_26_1_, lvt_30_1_).tex(lvt_33_1_, lvt_35_1_).lightmap(j, k).color(1, 1, 0, (float)lvt_19_1_).endVertex();
                lvt_18_1_.pos(lvt_24_1_, lvt_26_1_, lvt_28_1_).tex(lvt_33_1_, lvt_34_1_).lightmap(j, k).color(1, 1, 0, (float)lvt_19_1_).endVertex();
                //lvt_18_1_.putBrightness4(1, 1, 1, 1);
            }
        }
    }*/
}
