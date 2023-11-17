package fr.dynamx.client.renders;

import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.client.handlers.ClientDebugSystem;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.common.contentpack.type.ParticleEmitterInfo;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.utils.client.ClientDynamXUtils;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.debug.renderer.DebugRenderer;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.util.vector.Quaternion;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class RenderPhysicsEntity<T extends PhysicsEntity<?>> extends Render<T> {
    private final List<DebugRenderer<T>> debugRenderers = new ArrayList<>();
    public static boolean shouldRenderPlayerSitting;

    public RenderPhysicsEntity(RenderManager manager) {
        super(manager);
        addDebugRenderers(new DebugRenderer.ShapesDebug(), new DebugRenderer.CenterOfMassDebug());
    }

    /**
     * Setups render translation and rotation before rendering the entity
     */
    public Quaternion setupRenderTransform(T entity, double x, double y, double z, float partialTicks) {
        GlStateManager.translate((float) x, (float) y, (float) z);
        Quaternion q = ClientDynamXUtils.computeInterpolatedGlQuaternion(entity.prevRenderRotation, entity.renderRotation, partialTicks);
        GlStateManager.rotate(q);
        return q;
    }

    @Override
    public void doRender(T entity, double x, double y, double z, float entityYaw, float partialTicks) {
        entity.wasRendered = true;
        if (!canRender(entity))
            return;
        QuaternionPool.openPool();
        Vector3fPool.openPool();
        GlQuaternionPool.openPool();
        int renderPass = MinecraftForgeClient.getRenderPass();
        //Render vehicle
        if (!MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.Render(entity, this, PhysicsEntityEvent.Render.Type.ENTITY, x, y, z, partialTicks, renderPass))) {
            renderEntity(entity, x, y, z, partialTicks, false);
        }
        if (renderPass == 0) {
            spawnParticles(entity, partialTicks);
            //Render debug
            if (!MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.Render(entity, this, PhysicsEntityEvent.Render.Type.DEBUG, x, y, z, partialTicks, renderPass))) {
                renderDebug(entity, x, y, z, partialTicks);
            }
        }
        MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.Render(entity, this, PhysicsEntityEvent.Render.Type.POST, x, y, z, partialTicks, renderPass));
        Vector3fPool.closePool();
        QuaternionPool.closePool();
        GlQuaternionPool.closePool();
    }

    public void spawnParticles(T physicsEntity, float partialTicks) {
        if (physicsEntity instanceof PackPhysicsEntity) {
            PackPhysicsEntity<?, ?> packPhysicsEntity = (PackPhysicsEntity<?, ?>) physicsEntity;
            if (packPhysicsEntity.getPackInfo() instanceof ParticleEmitterInfo.IParticleEmitterContainer) {
                DynamXRenderUtils.spawnParticles(
                        (ParticleEmitterInfo.IParticleEmitterContainer) packPhysicsEntity.getPackInfo(),
                        physicsEntity.world,
                        physicsEntity.physicsPosition,
                        physicsEntity.physicsRotation);
            }
        }
    }

    /**
     * @return All debug renders for this entity renderer
     */
    public List<DebugRenderer<T>> getDebugRenderers() {
        return debugRenderers;
    }

    /**
     * Adds the debug renders to the list
     */
    @SuppressWarnings("unchecked")
    public void addDebugRenderers(DebugRenderer<?>... renderers) {
        for (DebugRenderer<?> renderer : renderers) {
            debugRenderers.add((DebugRenderer<T>) renderer);
        }
    }

    /**
     * Renders the entity
     */
    public abstract void renderEntity(T entity, double x, double y, double z, float partialTicks, boolean useVanillaRender);

    /**
     * Renders active {@link DebugRenderer}s <br>
     * Shouldn't be overridden : use {@link DebugRenderer}s to render your debug <br>
     * Can be cancelled via the dedicated event
     */
    public final void renderDebug(T entity, double x, double y, double z, float partialTicks) {
        if (ClientDebugSystem.enableDebugDrawing) {
            List<DebugRenderer<T>> validRotatedRenders = debugRenderers.stream().filter(r -> r.shouldRender(entity) && r.hasEntityRotation(entity)).collect(Collectors.toList());
            List<DebugRenderer<T>> validPureRenders = debugRenderers.stream().filter(r -> r.shouldRender(entity) && !r.hasEntityRotation(entity)).collect(Collectors.toList());
            if (!validRotatedRenders.isEmpty() || !validPureRenders.isEmpty()) {
                QuaternionPool.openPool();
                Vector3fPool.openPool();

                GlStateManager.pushMatrix();
                {
                    GlStateManager.disableLighting();
                    GlStateManager.disableDepth();
                    GlStateManager.disableTexture2D();

                    GlStateManager.translate((float) x, (float) y, (float) z);

                    GlStateManager.pushMatrix();
                    {
                        Quaternion rotQuat = ClientDynamXUtils.computeInterpolatedGlQuaternion(
                                entity.prevRenderRotation,
                                entity.renderRotation,
                                partialTicks);
                        GlStateManager.rotate(rotQuat);
                        validRotatedRenders.forEach(renderer -> renderer.render(entity, this, x, y, z, partialTicks));
                    }
                    GlStateManager.popMatrix();

                    validPureRenders.forEach(renderer -> renderer.render(entity, this, x, y, z, partialTicks));

                    GlStateManager.enableLighting();
                    GlStateManager.enableTexture2D();
                    GlStateManager.enableDepth();
                }
                GlStateManager.popMatrix();

                Vector3fPool.closePool();
                QuaternionPool.closePool();
            }
        }
    }

    /**
     * You can return null : textures are managed by obj renderer
     */
    @Override
    protected ResourceLocation getEntityTexture(T entity) {
        return null;
    }

    /**
     * Checks if the entity can be rendered, before any rendering and event
     */
    public boolean canRender(T entity) {
        return entity.initialized == PhysicsEntity.EnumEntityInitState.ALL;
    }

    /**
     * Called to render this part <br>
     * Will draw a white box over the all entity if model wasn't loaded (not found for example)
     */
    public void renderModel(DxModelRenderer model, @Nullable Entity entity, byte textureDataId, boolean forceVanillaRender) {
        if (!model.isEmpty())
            model.renderModel(textureDataId, forceVanillaRender);
        else if (entity != null) //Error while loading the model
            renderOffsetAABB(entity.getEntityBoundingBox(), -entity.lastTickPosX, -entity.lastTickPosY, -entity.lastTickPosZ);
    }

    /**
     * Called to render the main part of this model with the custom texture <br>
     * Will draw a white box over the all entity if model wasn't loaded (not found for example)
     */
    public void renderMainModel(DxModelRenderer model, @Nullable Entity entity, byte textureDataId, boolean forceVanillaRender) {
        boolean drawn = model.renderDefaultParts(textureDataId, forceVanillaRender);
        if (!drawn && entity != null) {
            renderOffsetAABB(entity.getEntityBoundingBox(), -entity.lastTickPosX, -entity.lastTickPosY, -entity.lastTickPosZ);
        }
    }

    /**
     * Called to render specific parts with the custom texture <br>
     * Will draw a white box over the all entity if model wasn't loaded (not found for example)
     */
    public void renderModelGroup(DxModelRenderer model, String group, @Nullable Entity entity, byte textureDataId, boolean forceVanillaRender) {
        boolean drawn = model.renderGroups(group, textureDataId, forceVanillaRender);
        if (!drawn && entity != null) {
            renderOffsetAABB(entity.getEntityBoundingBox(), -entity.lastTickPosX, -entity.lastTickPosY, -entity.lastTickPosZ);
        }
    }
}
