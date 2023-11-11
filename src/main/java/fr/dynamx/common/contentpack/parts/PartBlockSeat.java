package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Quaternion;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.common.entities.SeatEntity;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.debug.DynamXDebugOption;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

@RegisteredSubInfoType(name = "seat", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER}, strictName = false)
public class PartBlockSeat<T extends BlockObject<T>> extends InteractivePart<SeatEntity, T> {
    @PackFileProperty(configNames = "Rotation", required = false, defaultValue = "1 0 0 0")
    private Quaternion rotation;
    @PackFileProperty(configNames = "CameraRotation", required = false, defaultValue = "0")
    private float rotationYaw;

    public PartBlockSeat(T owner, String partName) {
        super(owner, partName, 0.4f, 1.8f);
    }

    @Override
    public DynamXDebugOption getDebugOption() {
        return DynamXDebugOptions.SEATS_AND_STORAGE;
    }

    @Override
    public void appendTo(T modulableVehicleInfo) {
        super.appendTo(modulableVehicleInfo);
        modulableVehicleInfo.arrangeSeatID(this);
    }

    public Quaternion getRotation() {
        return rotation;
    }

    public float getRotationYaw() {
        return rotationYaw;
    }

    @Override
    public ResourceLocation getHudCursorTexture() {
        return new ResourceLocation(DynamXConstants.ID, "textures/seat.png");
    }

    @Override
    public boolean interact(SeatEntity entity, EntityPlayer with) {
        return with.startRiding(entity);
    }

    @Override
    public String getName() {
        return "PartBlockSeat named " + getPartName() + " in " + getOwner().getName();
    }
}
