package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.common.entities.modules.SeatsModule;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.EnumSeatPlayerPosition;
import fr.dynamx.utils.debug.DynamXDebugOption;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

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
public abstract class BasePartSeat<A extends Entity, T extends ISubInfoTypeOwner<T>> extends InteractivePart<A, T> {
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
    public PartDoor getLinkedPartDoor() {
        return null;
    }

    public boolean isDriver() {
        return false;
    }

    @Override
    public Class<?> getIdClass() {
        return BasePartSeat.class;
    }
}
