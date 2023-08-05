package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.registry.*;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.api.obj.ObjModelPath;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.renderer.ObjModelRenderer;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.type.ObjectCollisionsHelper;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.common.handlers.TaskScheduler;
import fr.dynamx.common.physics.utils.RigidBodyTransform;
import fr.dynamx.common.physics.utils.SynchronizedRigidBodyTransform;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.client.ClientDynamXUtils;
import fr.dynamx.utils.debug.DynamXDebugOption;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import javax.vecmath.Vector2f;
import java.util.List;

@RegisteredSubInfoType(name = "door", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER}, strictName = false)
public class PartDoor extends InteractivePart<BaseVehicleEntity<?>, ModularVehicleInfo> implements IPhysicsPackInfo, IDrawablePart<BaseVehicleEntity<?>> {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = SubInfoTypeRegistries.WHEELED_VEHICLES)
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("CarAttachPoint".equals(key))
            return new IPackFilePropertyFixer.FixResult("LocalCarAttachPoint", true);
        if ("DoorAttachPoint".equals(key))
            return new IPackFilePropertyFixer.FixResult("LocalDoorAttachPoint", true);
        if ("OpenLimit".equals(key))
            return new IPackFilePropertyFixer.FixResult("OpenedDoorAngleLimit", true);
        if ("CloseLimit".equals(key))
            return new IPackFilePropertyFixer.FixResult("ClosedDoorAngleLimit", true);
        if ("OpenMotor".equals(key))
            return new IPackFilePropertyFixer.FixResult("DoorOpenForce", true);
        if ("CloseMotor".equals(key))
            return new IPackFilePropertyFixer.FixResult("DoorCloseForce", true);
        return null;
    };
    @Getter
    @PackFileProperty(configNames = "PartName")
    private String partName;

    @Getter
    @PackFileProperty(configNames = "LocalCarAttachPoint", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y, required = false)
    private Vector3f carAttachPoint = new Vector3f();
    @Getter
    @PackFileProperty(configNames = "LocalDoorAttachPoint", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y, required = false)
    private Vector3f doorAttachPoint = new Vector3f();
    @Getter
    @PackFileProperty(configNames = "AttachStrength", required = false)
    private int attachStrength = 400;

    @Getter
    @PackFileProperty(configNames = "Axis", required = false, defaultValue = "Y_ROT")
    private DynamXPhysicsHelper.EnumPhysicsAxis axisToUse = DynamXPhysicsHelper.EnumPhysicsAxis.Y_ROT;
    @Getter
    @PackFileProperty(configNames = "OpenedDoorAngleLimit", required = false)
    private Vector2f openLimit = new Vector2f();
    @Getter
    @PackFileProperty(configNames = "ClosedDoorAngleLimit", required = false)
    private Vector2f closeLimit = new Vector2f();
    @Getter
    @PackFileProperty(configNames = "DoorOpenForce", required = false)
    private Vector2f openMotor = new Vector2f(1, 200);
    @Getter
    @PackFileProperty(configNames = "DoorCloseForce", required = false)
    private Vector2f closeMotor = new Vector2f(-1.5f, 300);

    @Getter
    @PackFileProperty(configNames = "AutoMountDelay", required = false)
    private byte mountDelay = (byte) 40;
    @Getter
    @PackFileProperty(configNames = "DoorCloseTime", required = false)
    private byte doorCloseTime = (byte) 25;

    @Getter
    @PackFileProperty(configNames = "Enabled", required = false, defaultValue = "true")
    private boolean enabled = true;

    /**
     * True if the mounting animation is playing, use to prevent other interactions in the same time
     */
    @Getter
    @Setter
    public boolean isPlayerMounting;

    @Getter
    private ObjectCollisionsHelper collisionsHelper = new ObjectCollisionsHelper();

    private String oldPartName;

    public PartDoor(ModularVehicleInfo owner, String partName) {
        super(owner, partName, 0, 0);
        this.oldPartName = partName;
    }

    @Override
    public DynamXDebugOption getDebugOption() {
        return DynamXDebugOptions.DOOR_ATTACH_POINTS;
    }

    @Override
    public boolean interact(BaseVehicleEntity<?> entity, EntityPlayer player) {
        DoorsModule doors = ((IModuleContainer.IDoorContainer) entity).getDoors();
        if(doors == null)
            return false;
        if (isEnabled() && !doors.isDoorAttached(getId())) {
            if (!entity.world.isRemote) {
                doors.spawnDoor(this);
            }
        } else if(!isPlayerMounting()) {
            PartSeat seat = getLinkedSeat(entity);
            if (player.isSneaking() || seat == null) {
                doors.switchDoorState(getId());
            } else {
                if (isEnabled()) {
                    if (doors.isDoorOpened(getId())) {
                        mount(entity, seat, player);
                        doors.setDoorState(getId(), DoorsModule.DoorState.CLOSING);
                        return true;
                    }
                    isPlayerMounting = true;
                    doors.setDoorState(getId(), DoorsModule.DoorState.OPENING);
                    TaskScheduler.schedule(new TaskScheduler.ScheduledTask(getMountDelay()) {
                        @Override
                        public void run() {
                            isPlayerMounting = false;
                            mount(entity, seat, player);
                            doors.setDoorState(getId(), DoorsModule.DoorState.CLOSING);
                        }
                    });
                } else {
                    mount(entity, seat, player);
                }
            }
        }
        return true;
    }

    public void mount(BaseVehicleEntity<?> vehicleEntity, PartSeat seat, EntityPlayer context) {
        Vector3fPool.openPool();
        if (!MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.PlayerInteract(context, vehicleEntity, seat))) {
            seat.interact(vehicleEntity, context);
        }
        Vector3fPool.closePool();
    }

    @Nullable
    public PartSeat getLinkedSeat(BaseVehicleEntity<?> vehicleEntity) {
        return vehicleEntity.getPackInfo().getPartsByType(PartSeat.class).stream()
                .filter(seat -> seat.getLinkedDoor() != null && seat.getLinkedDoor().equalsIgnoreCase(getPartName()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        super.appendTo(owner);
        if(partName == null){
            partName = oldPartName;
        }
        MutableBoundingBox box = new MutableBoundingBox(getScale()).offset(getPosition());
        collisionsHelper.addCollisionShape(new IShapeInfo() {
            @Override
            public Vector3f getPosition() {
                return PartDoor.this.getPosition();
            }

            @Override
            public Vector3f getSize() {
                return getScale();
            }

            @Override
            public MutableBoundingBox getBoundingBox() {
                return box;
            }
        });
        ObjModelPath carModelPath = DynamXUtils.getModelPath(getPackName(), owner.getModel());
        collisionsHelper.loadCollisions(this, carModelPath, getPartName(), new Vector3f(), owner.getScaleModifier(), ObjectCollisionsHelper.CollisionType.PROP, owner.isUseComplexCollisions());
    }

    @Override
    public void addModules(PackPhysicsEntity<?, ?> entity, ModuleListBuilder modules) {
        if (!modules.hasModuleOfClass(DoorsModule.class)) {
            modules.add(new DoorsModule((BaseVehicleEntity<?>) entity));
        }
    }

    @Override
    public Vector3f getCenterOfMass() {
        return new Vector3f();
    }

    @Override
    public ItemStack getPickedResult(int metadata) {
        return ItemStack.EMPTY;
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
        return "PartDoor named " + getPartName();
    }

    @Override
    public String getFullName() {
        return super.getFullName();
    }

    @Override
    public void getBox(MutableBoundingBox out) {
        out.setTo(new MutableBoundingBox(getScale()));
    }

    @Override
    public void drawParts(BaseVehicleEntity<?> entity, RenderPhysicsEntity<?> render, ModularVehicleInfo packInfo, byte textureId, float partialTicks) {
        List<PartDoor> doors = packInfo.getPartsByType(PartDoor.class);
        DoorsModule module = entity != null ? entity.getModuleByType(DoorsModule.class) : null;
        for (byte id = 0; id < doors.size(); id++) {
            PartDoor door = doors.get(id);
            GlStateManager.pushMatrix();
            if (!door.isEnabled() || module == null) {
                Vector3f pos = Vector3fPool.get().addLocal(door.getCarAttachPoint());
                pos.subtract(door.getDoorAttachPoint(), pos);
                GlStateManager.translate(pos.x, pos.y, pos.z);
            } else if (module.getTransforms().containsKey(id)) {
                SynchronizedRigidBodyTransform sync = module.getTransforms().get(id);
                RigidBodyTransform transform = sync.getTransform();
                RigidBodyTransform prev = sync.getPrevTransform();

                Vector3f pos = Vector3fPool.get(prev.getPosition()).addLocal(transform.getPosition().subtract(prev.getPosition(), Vector3fPool.get()).multLocal(partialTicks));
                GlStateManager.rotate(ClientDynamXUtils.computeInterpolatedGlQuaternion(entity.prevRenderRotation, entity.renderRotation, partialTicks, true));
                GlStateManager.translate(
                        (float) -(entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks),
                        (float) -(entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks),
                        (float) -(entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks));
                GlStateManager.translate(pos.x, pos.y, pos.z);
                GlStateManager.rotate(ClientDynamXUtils.computeInterpolatedGlQuaternion(prev.getRotation(), transform.getRotation(), partialTicks));
            }
            ObjModelRenderer vehicleModel = DynamXContext.getObjModelRegistry().getModel(packInfo.getModel());
            GlStateManager.scale(packInfo.getScaleModifier().x, packInfo.getScaleModifier().y, packInfo.getScaleModifier().z);
            render.renderModelGroup(vehicleModel, door.getPartName(), entity, textureId);
            GlStateManager.scale(1 / packInfo.getScaleModifier().x, 1 / packInfo.getScaleModifier().y, 1 / packInfo.getScaleModifier().z);
            GlStateManager.popMatrix();
        }
    }

    @Override
    public String[] getRenderedParts() {
        return new String[] {getPartName()};
    }

    @Override
    public Vector3f getScaleModifier() {
        return getScaleModifier(owner);
    }
}
