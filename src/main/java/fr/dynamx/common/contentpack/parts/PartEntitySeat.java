package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.client.handlers.ClientEventHandler;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.scene.EntityRenderContext;
import fr.dynamx.client.renders.scene.SceneGraph;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.common.entities.modules.SeatsModule;
import fr.dynamx.common.entities.vehicles.CarEntity;
import fr.dynamx.common.entities.vehicles.HelicopterEntity;
import fr.dynamx.utils.EnumSeatPlayerPosition;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.errors.DynamXErrorManager;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.MinecraftForgeClient;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A seat that can be used on vehicles
 */
@Getter
@Setter
@RegisteredSubInfoType(name = "seat", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER}, strictName = false)
public class PartEntitySeat extends BasePartSeat<BaseVehicleEntity<?>, ModularVehicleInfo> implements IDrawablePart<PackPhysicsEntity<?, ?>, IPhysicsPackInfo> {
    @PackFileProperty(configNames = "Driver")
    protected boolean isDriver;

    @Nullable
    @PackFileProperty(configNames = "LinkedDoorPart", required = false)
    protected String linkedDoor;

    public PartEntitySeat(ModularVehicleInfo owner, String partName) {
        super(owner, partName);
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
        if (hasDoor()) {
            if (vehicleEntity instanceof CarEntity) {
                PartDoor door = getLinkedPartDoor();
                if (door != null) {
                    if (!door.isPlayerMounting()) {
                        IModuleContainer.IDoorContainer doorContainer = (IModuleContainer.IDoorContainer) vehicleEntity;
                        if (doorContainer.getDoors() == null) return false;
                        if (!door.isEnabled() || doorContainer.getDoors().isDoorAttached(door.getId())) {
                            if (!door.isEnabled() || doorContainer.getDoors().isDoorOpened(door.getId())) {
                                boolean didMount = mount(vehicleEntity, seats, player);
                                if (didMount) {
                                    vehicleEntity.getModuleByType(DoorsModule.class).setDoorState(door.getId(), DoorsModule.DoorState.CLOSING);
                                }
                                return didMount;
                            } else {
                                return door.interact(vehicleEntity, player);
                            }
                        }
                    } //else
                    //DynamXMain.log.error("Cannot mount : player mounting : " + linkedDoor);
                } else DynamXMain.log.error("Cannot mount : part door not found : " + linkedDoor);
            }
        } else {
            return mount(vehicleEntity, seats, player);
        }
        return false;
    }

    public boolean hasDoor() {
        return getLinkedDoor() != null;
    }

    @Nullable
    @Override
    public PartDoor getLinkedPartDoor() {
        System.out.println("Press f " + getLinkedDoor() +" // " + getOwner().getPartsByType(PartDoor.class).stream().map(PartDoor::getPartName).reduce((s, s2) -> s + ", " + s2).orElse("null"));
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
    public SceneGraph<PackPhysicsEntity<?, ?>, IPhysicsPackInfo> createSceneGraph(Vector3f modelScale, List<SceneGraph<PackPhysicsEntity<?, ?>, IPhysicsPackInfo>> childGraph) {
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

    class PartEntitySeatNode<T extends BaseVehicleEntity<?>, A extends IPhysicsPackInfo> extends SceneGraph.Node<T, A> {
        public PartEntitySeatNode(BasePartSeat seat, Vector3f scale, List<SceneGraph<T, A>> linkedChilds) {
            super(seat.getPosition(), GlQuaternionPool.newGlQuaternion(seat.getRotation()), scale, linkedChilds);
        }

        @Override
        public void render(@Nullable T entity, EntityRenderContext context, A packInfo) {
            if (MinecraftForgeClient.getRenderPass() != 0 || !(entity instanceof IModuleContainer.ISeatsContainer))
                return;
            SeatsModule seats = ((IModuleContainer.ISeatsContainer) entity).getSeats();
            assert seats != null;
            Entity seatRider = seats.getSeatToPassengerMap().get(PartEntitySeat.this);
            if (seatRider == null) return;
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
                if (position == EnumSeatPlayerPosition.LYING) GlStateManager.rotate(90, -1, 0, 0);

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
                RenderGlobal.drawBoundingBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, isDriver() ? 0 : 1, isDriver() ? 1 : 0, 0, 1);
                GlStateManager.popMatrix();
            }
            super.renderDebug(entity, context, packInfo);
        }
    }
}
