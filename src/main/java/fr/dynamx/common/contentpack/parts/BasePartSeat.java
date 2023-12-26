package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.client.handlers.ClientEventHandler;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.scene.EntityRenderContext;
import fr.dynamx.client.renders.scene.SceneGraph;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.SeatsModule;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.EnumSeatPlayerPosition;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.debug.DynamXDebugOption;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.client.MinecraftForgeClient;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A part that can be used as a seat for a player. <br>
 * Handles player rendering and mounting via the {@link SeatsModule}.
 *
 * @param <A> The vehicle entity type
 * @param <T> The owner of this part. Should implement ISubInfoTypeOwner<?>.
 * @see PartEntitySeat for a seat that can be used on vehicles
 * @see PartBlockSeat for a seat that can be used on block and props
 */
@Getter
@Setter
public abstract class BasePartSeat<A extends Entity, T extends ISubInfoTypeOwner<T>> extends InteractivePart<A, T> implements IDrawablePart<PackPhysicsEntity<?, ?>, IPhysicsPackInfo> {
    @Accessors(fluent = true)
    @PackFileProperty(configNames = "ShouldLimitFieldOfView", required = false, defaultValue = "true")
    protected boolean shouldLimitFieldOfView = true;

    @PackFileProperty(configNames = "MaxYaw", required = false, defaultValue = "-105")
    protected float maxYaw = -105.0f;

    @PackFileProperty(configNames = "MinYaw", required = false, defaultValue = "105")
    protected float minYaw = 105.0f;

    @PackFileProperty(configNames = "MaxPitch", required = false, defaultValue = "-105")
    protected float maxPitch = -105.0f;

    @PackFileProperty(configNames = "MinPitch", required = false, defaultValue = "105")
    protected float minPitch = 105.0f;

    @PackFileProperty(configNames = "Rotation", required = false, defaultValue = "1 0 0 0")
    protected Quaternion rotation;

    @PackFileProperty(configNames = "PlayerPosition", required = false, defaultValue = "SITTING")
    protected EnumSeatPlayerPosition playerPosition = EnumSeatPlayerPosition.SITTING;

    @PackFileProperty(configNames = "CameraRotation", required = false, defaultValue = "0")
    protected float rotationYaw;

    @PackFileProperty(configNames = "CameraPositionY", required = false, defaultValue = "0")
    protected float cameraPositionY;

    @PackFileProperty(configNames = "PlayerSize", required = false, defaultValue = "1 1 1")
    protected Vector3f playerSize;

    public BasePartSeat(T owner, String partName) {
        super(owner, partName, 0.4f, 1.8f);
    }

    @Override
    public DynamXDebugOption getDebugOption() {
        return DynamXDebugOptions.SEATS_AND_STORAGE;
    }

    public boolean mount(A vehicleEntity, SeatsModule seats, Entity entity) {
        if (seats.getSeatToPassengerMap().containsValue(entity)) {
            return false; //Player on another seat
        }
        seats.getSeatToPassengerMap().put(this, entity);
        if (!entity.startRiding(vehicleEntity, false)) //something went wrong : dismount
        {
            seats.getSeatToPassengerMap().remove(this);
            return false;
        }
        return true;
    }

    @Override
    public ResourceLocation getHudCursorTexture() {
        return new ResourceLocation(DynamXConstants.ID, "textures/seat.png");
    }

    @Override
    public String getName() {
        return "PartSeat named " + getPartName() + " in " + getOwner().getName();
    }

    public boolean hasDoor() {
        return false;
    }

    @Nullable
    public PartDoor getLinkedPartDoor(BaseVehicleEntity<?> vehicleEntity) {
        return null;
    }

    public boolean isDriver() {
        return false;
    }

    @Override
    public SceneGraph<PackPhysicsEntity<?, ?>, IPhysicsPackInfo> createSceneGraph(Vector3f modelScale, List<SceneGraph<PackPhysicsEntity<?, ?>, IPhysicsPackInfo>> childGraph) {
        return new PartSeatNode<>(this, modelScale, (List) childGraph);
    }

    @Override
    public String getNodeName() {
        return getPartName();
    }

    @Override
    public String getObjectName() {
        return null;
    }

    class PartSeatNode<T extends BaseVehicleEntity<?>, A extends IPhysicsPackInfo> extends SceneGraph.Node<T, A> {
        public PartSeatNode(BasePartSeat seat, Vector3f scale, List<SceneGraph<T, A>> linkedChilds) {
            super(seat.getPosition(), GlQuaternionPool.newGlQuaternion(seat.getRotation()), scale, linkedChilds);
        }

        @Override
        public void render(@Nullable T entity, EntityRenderContext context, A packInfo) {
            if (MinecraftForgeClient.getRenderPass() != 0 || !(entity instanceof IModuleContainer.ISeatsContainer))
                return;
            SeatsModule seats = ((IModuleContainer.ISeatsContainer) entity).getSeats();
            assert seats != null;
            Entity seatRider = seats.getSeatToPassengerMap().get(BasePartSeat.this);
            if (seatRider == null)
                return;
            ClientEventHandler.renderingEntity = seatRider.getUniqueID();
            if ((seatRider != Minecraft.getMinecraft().player || Minecraft.getMinecraft().gameSettings.thirdPersonView != 0)) {
                DynamXRenderUtils.popGlAllAttribBits();
                float partialTicks = context.getPartialTicks();
                GlStateManager.pushMatrix();
                transformToRotationPoint();

                //Transform the player to match the seat rotation and size
                EnumSeatPlayerPosition position = getPlayerPosition();
                RenderPhysicsEntity.shouldRenderPlayerSitting = position == EnumSeatPlayerPosition.SITTING;
                if (getPlayerSize() != null)
                    GlStateManager.scale(getPlayerSize().x, getPlayerSize().y, getPlayerSize().z);
                if (position == EnumSeatPlayerPosition.LYING)
                    GlStateManager.rotate(90, -1, 0, 0);

                //The render the player, e.rotationYaw is the name plate rotation
                if (seatRider instanceof AbstractClientPlayer) {
                    if (ClientEventHandler.renderPlayer != null) {
                        //Remove player's yaw offset rotation, to avoid stiff neck
                        if (shouldLimitFieldOfView()) {
                            ((AbstractClientPlayer) seatRider).renderYawOffset = ((AbstractClientPlayer) seatRider).prevRenderYawOffset = 0;
                        }
                        ClientEventHandler.renderPlayer.doRender((AbstractClientPlayer) seatRider, 0, 0, 0, seatRider.rotationYaw, partialTicks);
                    }
                } else {
                    Minecraft.getMinecraft().getRenderManager().renderEntity(seatRider, 0, 0, 0, seatRider.rotationYaw, partialTicks, false);
                }
                GlStateManager.popMatrix();
            }
            ClientEventHandler.renderingEntity = null;
        }


        @Override
        public void renderDebug(@Nullable T entity, EntityRenderContext context, A packInfo) {
            if (DynamXDebugOptions.SEATS_AND_STORAGE.isActive()) {
                GlStateManager.pushMatrix();
                transformForDebug();
                AxisAlignedBB box = getBox();
                RenderGlobal.drawBoundingBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                        isDriver() ? 0 : 1, isDriver() ? 1 : 0, 0, 1);
                GlStateManager.popMatrix();
            }
            super.renderDebug(entity, context, packInfo);
        }
    }

    @Override
    public Class<?> getIdClass() {
        return BasePartSeat.class;
    }
}
