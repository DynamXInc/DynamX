package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.common.contentpack.loader.ModularVehicleInfoBuilder;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import javax.vecmath.Vector2f;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PartDoor extends InteractivePart<BaseVehicleEntity<?>, ModularVehicleInfoBuilder> implements IPhysicsPackInfo {
    @Getter
    @PackFileProperty(configNames = "LocalCarAttachPoint", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y, required = false)
    private final Vector3f carAttachPoint = new Vector3f();
    @Getter
    @PackFileProperty(configNames = "LocalDoorAttachPoint", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y, required = false)
    private final Vector3f doorAttachPoint = new Vector3f();
    @Getter
    @PackFileProperty(configNames = "AttachStrength", required = false)
    private final int attachStrength = 400;

    @Getter
    @PackFileProperty(configNames = "OpenedDoorAngleLimit", required = false)
    private final Vector2f openLimit = new Vector2f();
    @Getter
    @PackFileProperty(configNames = "ClosedDoorAngleLimit", required = false)
    private final Vector2f closeLimit = new Vector2f();
    @Getter
    @PackFileProperty(configNames = "DoorOpenForce", required = false)
    private final Vector2f openMotor = new Vector2f(1, 200);
    @Getter
    @PackFileProperty(configNames = "DoorCloseForce", required = false)
    private final Vector2f closeMotor = new Vector2f(-1.5f, 300);

    @Getter
    @PackFileProperty(configNames = "AutoMountDelay", required = false)
    private final byte mountDelay = (byte) 40;
    @Getter
    @PackFileProperty(configNames = "DoorCloseTime", required = false)
    private final byte doorCloseTime = (byte) 25;

    @Getter
    @PackFileProperty(configNames = "Enabled", required = false, defaultValue = "true")
    private final boolean enabled = true;

    public boolean isPlayerMounting;

    public PartDoor(ModularVehicleInfoBuilder owner, String partName) {
        super(owner, partName, 0, 0);
    }

    public void setPlayerMounting(boolean playerMounting) {
        isPlayerMounting = playerMounting;
    }

    /**
     * @return True if the mounting animation is playing, use to prevent other interactions in the same time
     */
    public boolean isPlayerMounting() {
        return isPlayerMounting;
    }

    @Override
    public boolean interact(BaseVehicleEntity<?> entity, EntityPlayer player) {
        DoorsModule doors = ((IModuleContainer.IDoorContainer) entity).getDoors();
        if (isEnabled() && !doors.isDoorAttached(getId())) {
            if (!entity.world.isRemote) {
                doors.spawnDoor(this);
            }
        }
        return true;
    }

    public void mount(BaseVehicleEntity<?> vehicleEntity, PartSeat seat, EntityPlayer context) {
        Vector3fPool.openPool();
        if (!MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.VehicleInteractEntityEvent(context, vehicleEntity, seat))) {
            seat.interact(vehicleEntity, context);
        }
        Vector3fPool.closePool();
    }

    @Nullable
    public PartSeat getLinkedSeat(BaseVehicleEntity<?> vehicleEntity) {
        return vehicleEntity.getPackInfo().getPartsByType(PartSeat.class).stream().filter(seat -> seat.getLinkedDoor() != null && seat.getLinkedDoor().equalsIgnoreCase(getPartName())).findFirst().orElse(null);
    }

    @Override
    public void appendTo(ModularVehicleInfoBuilder owner) {
        super.appendTo(owner);
        owner.arrangeDoorID(this);
        owner.addRenderedParts(getPartName());
    }

    @Override
    public void addModules(BaseVehicleEntity<?> entity, ModuleListBuilder modules) {
        if (!modules.hasModuleOfClass(DoorsModule.class)) {
            modules.add(new DoorsModule(entity));
        }
    }

    @Override
    public Vector3f getCenterOfMass() {
        return new Vector3f();
    }

    /**
     * @return All collision shapes of the object
     */
    @Override
    public Collection<? extends IShapeInfo> getShapes() {
        return Collections.singletonList(new IShapeInfo() {

            @Override
            public Vector3f getPosition() {
                return PartDoor.this.getPosition();
            }

            @Override
            public Vector3f getSize() {
                return getScale();
            }
        });
    }

    @Override
    public List<Vector3f> getCollisionShapeDebugBuffer() {
        return new ArrayList<>();
    }

    @Override
    public <T extends InteractivePart<?, ModularVehicleInfoBuilder>> List<T> getInteractiveParts() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public ResourceLocation getHudCursorTexture() {
        return new ResourceLocation(DynamXConstants.ID, "textures/door.png");
    }

    /**
     * @return The name of the object
     */
    @Override
    public String getName() {
        return "PartDoor named " + getPartName() + " in " + getOwner().getName();
    }

    @Override
    public void getBox(MutableBoundingBox out) {
        out.setTo(new MutableBoundingBox(getScale()));
    }
}
