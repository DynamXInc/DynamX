package fr.dynamx.api.events;

import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.entities.modules.IEngineModule;
import fr.dynamx.api.entities.modules.ISeatsModule;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.client.gui.VehicleHud;
import fr.dynamx.client.handlers.hud.CarController;
import fr.dynamx.client.renders.vehicle.RenderBaseVehicle;
import fr.dynamx.common.contentpack.parts.PartSeat;
import fr.dynamx.common.contentpack.type.vehicle.PartWheelInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.WheelsModule;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;
import java.util.List;

public class VehicleEntityEvent extends Event {
    @Getter
    private final Side side;

    @Getter()
    private final BaseVehicleEntity<?> entity;

    public VehicleEntityEvent(Side side, BaseVehicleEntity<?> vehicleEntity) {
        this.entity = vehicleEntity;
        this.side = side;
    }

    /**
     * Called on server side when a player entity interacts with a vehicle
     */
    @Cancelable
    public static class PlayerInteract extends VehicleEntityEvent {
        /**
         * The player who interacted with the vehicle
         */
        @Getter
        private final EntityPlayer player;
        /**
         * The part that the player interacted with (null if the player interacted with the vehicle itself)
         */
        @Nullable
        @Getter
        private final InteractivePart<?, ?> part;
        @Getter
        private final InteractionType interactionType;

        /**
         * @param player        the player who interacted with the vehicle
         * @param vehicleEntity the vehicle that the player interacted with
         * @param part          the part that the player interacted with (null if the player interacted with the vehicle itself)
         */
        public PlayerInteract(EntityPlayer player, BaseVehicleEntity<?> vehicleEntity, @Nullable InteractivePart<?, ?> part) {
            super(Side.SERVER, vehicleEntity);
            this.player = player;
            this.part = part;
            this.interactionType = part == null ? InteractionType.VEHICLE : InteractionType.PART;
        }

        /**
         * @return If the part is not null
         */
        public boolean withPart() {
            return interactionType == InteractionType.PART;
        }

        /**
         * @return If the part is null, cancelling the event will do nothing
         */
        public boolean withVehicle() {
            return interactionType == InteractionType.VEHICLE;
        }

        public enum InteractionType {
            VEHICLE, PART
        }
    }

    /**
     * Called on client and server sides when a entity has mounted on an entity <br>
     * On client side, also called when the vehicle entity starts to be tracked by the local player
     */
    public static class PlayerMount extends VehicleEntityEvent {
        @Getter
        private final EntityPlayer player;
        @Getter
        private final ISeatsModule module;
        @Getter
        private final PartSeat seat;

        /**
         * @param player        the player who mounted the vehicle
         * @param vehicleEntity the vehicle that the player mounted
         * @param module        the seats module, calling this event
         * @param seat          the seat that the player mounted
         */
        public PlayerMount(Side side, EntityPlayer player, BaseVehicleEntity<?> vehicleEntity, ISeatsModule module, PartSeat seat) {
            super(side, vehicleEntity);
            this.player = player;
            this.module = module;
            this.seat = seat;
        }
    }

    /**
     * Called on client and server sides when a entity has dismounted an entity
     */
    public static class PlayerDismount extends VehicleEntityEvent {
        @Getter
        private final EntityPlayer player;
        @Getter
        private final ISeatsModule module;
        @Getter
        private final PartSeat seat;

        /**
         * @param player        the player who dismounted the vehicle
         * @param vehicleEntity the vehicle that the player dismounted
         * @param module        the seats module, calling this event
         * @param seat          the seat that the player dismounted
         */
        public PlayerDismount(Side side, EntityPlayer player, BaseVehicleEntity<?> vehicleEntity, ISeatsModule module, PartSeat seat) {
            super(side, vehicleEntity);
            this.player = player;
            this.module = module;
            this.seat = seat;
        }
    }

    /**
     * Fired when loading a vehicle from NBT
     */
    public static class LoadFromNBT extends VehicleEntityEvent {
        @Getter
        private final NBTTagCompound nbtTagCompound;

        public LoadFromNBT(NBTTagCompound nbtTagCompound, BaseVehicleEntity<?> vehicleEntity) {
            super(vehicleEntity.world.isRemote ? Side.CLIENT : Side.SERVER, vehicleEntity);
            this.nbtTagCompound = nbtTagCompound;
        }
    }

    /**
     * Fired when saving a vehicle to NBT
     */
    public static class SaveToNBT extends VehicleEntityEvent {
        @Getter
        private final NBTTagCompound nbtTagCompound;

        public SaveToNBT(NBTTagCompound nbtTagCompound, BaseVehicleEntity<?> vehicleEntity) {
            super(vehicleEntity.world.isRemote ? Side.CLIENT : Side.SERVER, vehicleEntity);
            this.nbtTagCompound = nbtTagCompound;
        }
    }

    /**
     * Fired for each car entity render step, before and after the step, with pos and rotations transformations applied <br>
     * Pre phase is cancellable <br>
     * The debug render has not Pre phase
     */
    @Cancelable
    public static class Render extends VehicleEntityEvent {
        @Getter
        private final RenderBaseVehicle<?> renderBaseVehicle;
        @Getter
        private final BaseVehicleEntity<?> carEntity;
        @Getter
        private final Type type;
        @Getter
        private final PhysicsEntityEvent.Phase eventPase;
        @Getter
        private final float partialTicks;

        /**
         * @param type              the type of the render
         * @param renderBaseVehicle the class the render of the car
         * @param carEntity         the rendered car
         * @param phase             the phase of the render (Post or Pre)
         * @param partialTicks      the partial render ticks
         */
        public Render(Type type, RenderBaseVehicle<?> renderBaseVehicle, BaseVehicleEntity<?> carEntity, PhysicsEntityEvent.Phase phase, float partialTicks) {
            super(Side.CLIENT, carEntity);
            this.type = type;
            this.renderBaseVehicle = renderBaseVehicle;
            this.carEntity = carEntity;
            this.eventPase = phase;
            this.partialTicks = partialTicks;
        }

        public enum Type {
            CHASSIS, PROPULSION, PARTS, PARTICLES, LIGHTS, STEERING_WHEEL
        }
    }

    /**
     * Fired when creating a vehicle HUD (it's a gui displayed as an HUD) <br>
     * Cancelling the event will remove DynamX components from the HUD
     */
    @Cancelable
    public static class CreateHud extends VehicleEntityEvent {
        /**
         * The vehicle's HUD gui
         */
        @Getter
        private final VehicleHud vehicleHud;
        /**
         * The list of HUD css style sheets, modifiable
         */
        @Getter
        private final List<ResourceLocation> styleSheets;
        /**
         * True if the local player is driving this vehicle
         */
        @Getter
        private final boolean isPlayerDriving;
        /**
         * The vehicle controllers displayed on the HUD, modifiable
         */
        @Getter
        private final List<IVehicleController> controllers;

        /**
         * @param vehicleHUD      the vehicle's HUD
         * @param styleSheets     the list of HUD css style sheets, modifiable
         * @param isPlayerDriving true if the local player is driving this vehicle
         * @param vehicleEntity   the vehicle
         * @param controllers     the vehicle controllers displayed on the HUD
         */
        public CreateHud(VehicleHud vehicleHUD, List<ResourceLocation> styleSheets, boolean isPlayerDriving, BaseVehicleEntity<?> vehicleEntity, List<IVehicleController> controllers) {
            super(Side.CLIENT, vehicleEntity);
            this.vehicleHud = vehicleHUD;
            this.styleSheets = styleSheets;
            this.isPlayerDriving = isPlayerDriving;
            this.controllers = controllers;
        }
    }

    /**
     * Called on client side when the engine sounds of the entity are updated
     */
    @Cancelable
    public static class UpdateSounds extends VehicleEntityEvent {
        @Getter
        private final PhysicsEntityEvent.Phase eventPhase;
        /**
         * The {@link fr.dynamx.api.entities.modules.IEngineModule} of the entity, responsible for sounds update
         */
        @Getter
        private final IEngineModule module;

        public UpdateSounds(BaseVehicleEntity<?> vehicleEntity, IEngineModule module, PhysicsEntityEvent.Phase phase) {
            super(Side.CLIENT, vehicleEntity);
            this.eventPhase = phase;
            this.module = module;
        }
    }

    /**
     * Called when a vehicle's wheel is changed, useful to update addons information <br>
     * This event is fired on simulation sides <br>
     * This event is cancellable, and you can change the newWheel to be set
     */
    @Cancelable
    public static class ChangeWheel extends VehicleEntityEvent {
        /**
         * The part id of the changed wheel
         */
        @Getter
        private final byte wheelPartId;
        /**
         * The previous wheel
         */
        @Getter
        private final PartWheelInfo oldWheel;
        /**
         * The new wheel, can be changed with another wheel
         */
        @Getter
        @Setter
        private PartWheelInfo newWheel;
        /**
         * The wheels module responsible for this wheel
         */
        @Getter
        private final WheelsModule wheelsModule;

        public ChangeWheel(Side side, BaseVehicleEntity<?> vehicleEntity, WheelsModule wheelsModule, PartWheelInfo oldWheel, PartWheelInfo newWheel, byte wheelPartId) {
            super(side, vehicleEntity);
            this.wheelsModule = wheelsModule;
            this.wheelPartId = wheelPartId;
            this.oldWheel = oldWheel;
            this.newWheel = newWheel;
        }
    }

    /**
     * Called on {@link CarController} post update, so you can override controls
     * <br> This event is not cancellable, and fired on client side
     */
    public static class ControllerUpdate<T extends IVehicleController> extends VehicleEntityEvent {
        @Getter
        private final T controller;

        public ControllerUpdate(BaseVehicleEntity<?> vehicleEntity, T controller) {
            super(Side.CLIENT, vehicleEntity);
            this.controller = controller;
        }
    }
}
