package fr.dynamx.common.contentpack.parts;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.client.handlers.ClientEventHandler;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.IRenderContext;
import fr.dynamx.client.renders.scene.SceneBuilder;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.client.renders.scene.node.SimpleNode;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.common.entities.modules.SeatsModule;
import fr.dynamx.common.entities.vehicles.CarEntity;
import fr.dynamx.common.entities.vehicles.HelicopterEntity;
import fr.dynamx.utils.EnumSeatPlayerPosition;
import fr.dynamx.utils.client.ClientDynamXUtils;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.errors.DynamXErrorManager;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.MinecraftForgeClient;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A seat that can be used on vehicles
 */
@Setter
@RegisteredSubInfoType(name = "seat", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER}, strictName = false)
public class PartEntitySeat extends BasePartSeat<BaseVehicleEntity<?>, ModularVehicleInfo> implements IDrawablePart<IPhysicsPackInfo> {
    @PackFileProperty(configNames = "Driver")
    protected boolean isDriver;

    @Getter
    @Nullable
    @PackFileProperty(configNames = "LinkedDoorPart", required = false)
    protected String linkedDoor;

    @PackFileProperty(configNames = "DependsOnNode", required = false, description = "PartEntitySeat.DependsOnNode")
    protected String nodeDependingOnName;

    /**
     * The position used to get the hitbox of this seat <br>
     * Useful when this seat is attached to another part (with DependsOnNode): the Position is the relative position to this other part,
     * and the InteractPosition is relative to the 3D model (as when the seat is attached to nothing)
     */
    @PackFileProperty(configNames = "InteractPosition", required = false, defaultValue = "Position", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y)
    protected Vector3f interactPosition;

    public PartEntitySeat(ModularVehicleInfo owner, String partName) {
        super(owner, partName);
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        super.appendTo(owner);
        if (interactPosition != null) {
            interactPosition.multLocal(getScaleModifier(this.owner));
        }
    }

    @Override
    public boolean interact(BaseVehicleEntity<?> vehicleEntity, EntityPlayer player) {
        if (!(vehicleEntity instanceof IModuleContainer.ISeatsContainer))
            throw new IllegalStateException("The entity " + vehicleEntity + " has PartSeats, but does not implement IHaveSeats !");
        SeatsModule seats = ((IModuleContainer.ISeatsContainer) vehicleEntity).getSeats();
        Entity seatRider = seats.getSeatToPassengerMap().get(this);
        if (seatRider != null) {
            if (seatRider != player) {
                player.sendMessage(new TextComponentString("The seat is already taken"));
                return false;
            }
        }
        if (!hasDoor()) {
            return mountEntity(vehicleEntity, seats, player);
        }
        if (!(vehicleEntity instanceof CarEntity)) {
            return false;
        }
        PartDoor door = getLinkedPartDoor();
        if (door == null) {
            DynamXMain.log.error("Cannot mount : part door not found : " + linkedDoor);
            return false;
        }
        IModuleContainer.IDoorContainer doorContainer = (IModuleContainer.IDoorContainer) vehicleEntity;
        if (door.isPlayerMounting() || doorContainer.getDoors() == null) {
            return false;
        }
        if (door.isEnabled() && !doorContainer.getDoors().isDoorAttached(door.getId())) {
            return false;
        }
        if (!door.isEnabled() || doorContainer.getDoors().isDoorOpened(door.getId())) {
            boolean didMount = mountEntity(vehicleEntity, seats, player);
            if (didMount) {
                vehicleEntity.getModuleByType(DoorsModule.class).setDoorState(door.getId(), DoorsModule.DoorState.CLOSING);
            }
            return didMount;
        } else {
            return door.interact(vehicleEntity, player);
        }
    }

    public boolean hasDoor() {
        return getLinkedDoor() != null;
    }

    @Nullable
    @Override
    public PartDoor getLinkedPartDoor() {
        return getLinkedDoor() == null ? null : getOwner().getPartsByType(PartDoor.class).stream().filter(partDoor -> partDoor.getPartName().equalsIgnoreCase(getLinkedDoor())).findFirst().orElse(null);
    }

    @Override
    public void postLoad(ModularVehicleInfo owner, boolean hot) {
        super.postLoad(owner, hot);
        if (hasDoor() && getLinkedPartDoor() == null) {
            DynamXErrorManager.addPackError(getPackName(), "seat_door_not_found", ErrorLevel.HIGH, getName(), "Door " + getLinkedDoor() + " not found in " + owner.getFullName());
        }
    }

    @Override
    public void addModules(PackPhysicsEntity<?, ?> entity, ModuleListBuilder modules) {
        if (!(entity instanceof IModuleContainer.ISeatsContainer))
            throw new IllegalStateException("The entity " + entity + " has PartSeats, but does not implement IHaveSeats !");
        if (entity instanceof HelicopterEntity) return; //Helicopters have their own SeatsModule
        if (!modules.hasModuleOfClass(SeatsModule.class)) modules.add(new SeatsModule(entity));
    }

    @Override
    public void addToSceneGraph(IPhysicsPackInfo packInfo, SceneBuilder<IRenderContext, IPhysicsPackInfo> sceneBuilder) {
        if (nodeDependingOnName != null) {
            sceneBuilder.addNode(packInfo, this, nodeDependingOnName);
        } else {
            sceneBuilder.addNode(packInfo, this);
        }
    }

    @Override
    public SceneNode<IRenderContext, IPhysicsPackInfo> createSceneGraph(Vector3f modelScale, List<SceneNode<IRenderContext, IPhysicsPackInfo>> childGraph) {
        return new PartEntitySeatNode<>(this, modelScale, (List) childGraph);
    }

    @Override
    public String getNodeName() {
        return getPartName();
    }

    @Override
    public String getObjectName() {
        return null;
    }

    @Override
    public Vector3f getPosition() {
        return interactPosition != null ? interactPosition : super.getPosition();
    }

    public Vector3f getRelativeRenderPosition() {
        return super.getPosition();
    }

    @Override
    public boolean isDriver() {
        return isDriver;
    }

    class PartEntitySeatNode<A extends IPhysicsPackInfo> extends SimpleNode<BaseRenderContext.EntityRenderContext, A> {
        private final MutableBoundingBox debugBox = new MutableBoundingBox();

        public PartEntitySeatNode(PartEntitySeat seat, Vector3f scale, List<SceneNode<BaseRenderContext.EntityRenderContext, A>> linkedChilds) {
            super(seat.getRelativeRenderPosition(), GlQuaternionPool.newGlQuaternion(seat.getRotation()), scale, linkedChilds);
        }

        @Override
        public void render(BaseRenderContext.EntityRenderContext context, A packInfo, Matrix4f parentTransform) {
            if (MinecraftForgeClient.getRenderPass() != 0 || !(context.getEntity() instanceof IModuleContainer.ISeatsContainer)) {
                return;
            }

            SeatsModule seats = ((IModuleContainer.ISeatsContainer) context.getEntity()).getSeats();
            assert seats != null;
            Entity seatRider = seats.getSeatToPassengerMap().get(PartEntitySeat.this);
            if (seatRider == null ||
                    (seatRider == Minecraft.getMinecraft().player && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0)) {
                return;
            }

            ClientEventHandler.renderingEntity = seatRider.getUniqueID();
            DynamXRenderUtils.popGlAllAttribBits();

            float partialTicks = context.getPartialTicks();
            transformToRotationPoint(parentTransform);

            //Transform the player to match the seat rotation and size
            EnumSeatPlayerPosition position = getPlayerPosition();
            RenderPhysicsEntity.shouldRenderPlayerSitting = position == EnumSeatPlayerPosition.SITTING;
            if (getPlayerSize() != null) {
                transform.scale(getPlayerSize().x, getPlayerSize().y, getPlayerSize().z);
            }
            if (position == EnumSeatPlayerPosition.LYING) {
                transform.rotate(FastMath.PI / 2, 1, 0, 0);
            }

            GlStateManager.pushMatrix();
            GlStateManager.multMatrix(ClientDynamXUtils.getMatrixBuffer(transform));

            //The render the player, e.rotationYaw is the name plate rotation
            if (seatRider instanceof AbstractClientPlayer) {
                String skinType = ((AbstractClientPlayer) seatRider).getSkinType();
                RenderPlayer renderPlayer = context.getRender().getRenderManager().getSkinMap().get(skinType);
                if (renderPlayer != null) {
                    renderPlayer.doRender((AbstractClientPlayer) seatRider, 0, 0, 0, seatRider.rotationYaw, partialTicks);
                }
            } else {
                context.getRender().getRenderManager().renderEntity(seatRider, 0, 0, 0, seatRider.rotationYaw, partialTicks, false);
            }

            GlStateManager.popMatrix();
            ClientEventHandler.renderingEntity = null;
        }

        @Override
        public void renderDebug(BaseRenderContext.EntityRenderContext context, A packInfo) {
            super.renderDebug(context, packInfo);
            if (!DynamXDebugOptions.SEATS_AND_STORAGE.isActive()) {
                return;
            }
            GlStateManager.pushMatrix();
            Vector3f translation = PartEntitySeat.this.getPosition();
            GlStateManager.translate(translation.x, translation.y, translation.z);
            if (rotation != null) {
                GlStateManager.rotate(rotation);
            }
            getBox(debugBox);
            RenderGlobal.drawBoundingBox(debugBox.minX, debugBox.minY, debugBox.minZ,
                    debugBox.maxX, debugBox.maxY, debugBox.maxZ,
                    isDriver() ? 0 : 1, isDriver() ? 1 : 0, 0, 1);
            GlStateManager.popMatrix();
        }
    }
}
