package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPartContainer;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.registry.*;
import fr.dynamx.api.dxmodel.DxModelPath;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.client.renders.scene.EntityRenderContext;
import fr.dynamx.client.renders.scene.SceneGraph;
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
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import javax.vecmath.Vector2f;
import java.util.Collections;
import java.util.List;

@RegisteredSubInfoType(name = "door", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER}, strictName = false)
public class PartDoor extends InteractivePart<BaseVehicleEntity<?>, ModularVehicleInfo> implements IPhysicsPackInfo, IDrawablePart<BaseVehicleEntity<?>, ModularVehicleInfo>, IPartContainer<PartDoor> {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER})
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
        if ("PartName".equals(key))
            return new IPackFilePropertyFixer.FixResult("ObjectName", false);
        return null;
    };
    @Getter
    @PackFileProperty(configNames = "ObjectName", required = false, description = "PartDoor.object_name", defaultValue = "Suffix after 'Door_' in part name")
    private String objectName;

    @Getter
    @PackFileProperty(configNames = "LocalCarAttachPoint", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y)
    private Vector3f carAttachPoint = new Vector3f();
    @Getter
    @PackFileProperty(configNames = "LocalDoorAttachPoint", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y)
    private Vector3f doorAttachPoint = new Vector3f();
    @Getter
    @PackFileProperty(configNames = "AttachStrength", required = false, defaultValue = "400")
    private int attachStrength = 400;

    @Getter
    @PackFileProperty(configNames = "Axis", required = false, defaultValue = "Y_ROT")
    private DynamXPhysicsHelper.EnumPhysicsAxis axisToUse = DynamXPhysicsHelper.EnumPhysicsAxis.Y_ROT;
    @Getter
    @PackFileProperty(configNames = "OpenedDoorAngleLimit", required = false, defaultValue = "0 0")
    private Vector2f openLimit = new Vector2f();
    @Getter
    @PackFileProperty(configNames = "ClosedDoorAngleLimit", required = false, defaultValue = "0 0")
    private Vector2f closeLimit = new Vector2f();
    @Getter
    @PackFileProperty(configNames = "DoorOpenForce", required = false, defaultValue = "1 200")
    private Vector2f openMotor = new Vector2f(1, 200);
    @Getter
    @PackFileProperty(configNames = "DoorCloseForce", required = false, defaultValue = "-1.5 300")
    private Vector2f closeMotor = new Vector2f(-1.5f, 300);

    @Getter
    @PackFileProperty(configNames = "AutoMountDelay", required = false, defaultValue = "40")
    private byte mountDelay = (byte) 40;
    @Getter
    @PackFileProperty(configNames = "DoorCloseTime", required = false, defaultValue = "25")
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

    public PartDoor(ModularVehicleInfo owner, String partName) {
        super(owner, partName, 0, 0);
        this.objectName = partName.replaceFirst("Door_", "");
    }

    @Override
    public DynamXDebugOption getDebugOption() {
        return DynamXDebugOptions.DOOR_ATTACH_POINTS;
    }

    @Override
    public boolean interact(BaseVehicleEntity<?> entity, EntityPlayer player) {
        DoorsModule doors = ((IModuleContainer.IDoorContainer) entity).getDoors();
        if (doors == null)
            return false;
        if (isEnabled() && !doors.isDoorAttached(getId())) {
            if (!entity.world.isRemote) {
                doors.spawnDoor(this);
            }
        } else if (!isPlayerMounting()) {
            PartEntitySeat seat = getLinkedSeat(entity);
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

    public void mount(BaseVehicleEntity<?> vehicleEntity, PartEntitySeat seat, EntityPlayer context) {
        Vector3fPool.openPool();
        if (!MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.PlayerInteract(context, vehicleEntity, seat))) {
            seat.interact(vehicleEntity, context);
        }
        Vector3fPool.closePool();
    }

    @Nullable
    public PartEntitySeat getLinkedSeat(BaseVehicleEntity<?> vehicleEntity) {
        return vehicleEntity.getPackInfo().getPartsByType(PartEntitySeat.class).stream()
                .filter(seat -> seat.getLinkedDoor() != null && seat.getLinkedDoor().equalsIgnoreCase(getPartName()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        super.appendTo(owner);
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
        DxModelPath carModelPath = DynamXUtils.getModelPath(getPackName(), owner.getModel());
        collisionsHelper.loadCollisions(this, carModelPath, getObjectName(), new Vector3f(), 0, owner.isUseComplexCollisions(), owner.getScaleModifier(), ObjectCollisionsHelper.CollisionType.PROP);
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
    public float getAngularDamping() {
        return 0;
    }

    @Override
    public float getLinearDamping() {
        return 0;
    }

    @Override
    public float getRenderDistance() {
        return owner.getRenderDistance();
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
    public Vector3f getScaleModifier() {
        return getScaleModifier(owner);
    }

    @Override
    public List<BasePart<PartDoor>> getAllParts() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public void addPart(BasePart<PartDoor> partDoorBasePart) {
        throw new IllegalStateException("Cannot add part to a door");
    }

    @Override
    public void addSubProperty(ISubInfoType<PartDoor> property) {
        throw new IllegalStateException("Cannot add sub property to a door");
    }

    @Override
    public List<ISubInfoType<PartDoor>> getSubProperties() {
        return Collections.EMPTY_LIST;
    }

    private SceneGraph<?, ?> sceneGraph;

    @Override
    public SceneGraph<?, ?> getSceneGraph() {
        if (sceneGraph == null) {
            sceneGraph = createSceneGraph(owner.getScaleModifier(), null);
        }
        return sceneGraph;
    }

    @Override
    public boolean isLinkedToEntity() {
        return false;
    }

    @Override
    public String getNodeName() {
        return getPartName();
    }

    @Override
    public SceneGraph<BaseVehicleEntity<?>, ModularVehicleInfo> createSceneGraph(Vector3f modelScale, List<SceneGraph<BaseVehicleEntity<?>, ModularVehicleInfo>> childGraph) {
        return new PartDoorNode<>(this, modelScale, childGraph);
    }

    class PartDoorNode<T extends BaseVehicleEntity<?>, A extends IPhysicsPackInfo> extends SceneGraph.Node<T, A> {
        public PartDoorNode(PartDoor door, Vector3f scale, List<SceneGraph<T, A>> linkedChilds) {
            super(door.getPosition(), null, scale, linkedChilds);
        }

        @Override
        public void render(@Nullable T entity, EntityRenderContext context, A packInfo) {
            GlStateManager.pushMatrix();
            DoorsModule module = entity != null ? entity.getModuleByType(DoorsModule.class) : null;
            if (!isEnabled() || module == null) {
                Vector3f pos = Vector3fPool.get().addLocal(getCarAttachPoint());
                pos.subtract(getDoorAttachPoint(), pos);
                GlStateManager.translate(pos.x, pos.y, pos.z);
            } else if (module.getTransforms().containsKey(getId())) {
                float partialTicks = context.getPartialTicks();
                SynchronizedRigidBodyTransform sync = module.getTransforms().get(getId());
                RigidBodyTransform transform = sync.getTransform();
                RigidBodyTransform prev = sync.getPrevTransform();
                Vector3f pos = Vector3fPool.get(prev.getPosition()).addLocal(transform.getPosition().subtract(prev.getPosition(), Vector3fPool.get()).multLocal(partialTicks));
                GlStateManager.translate(pos.x, pos.y, pos.z);
                GlStateManager.rotate(ClientDynamXUtils.computeInterpolatedGlQuaternion(prev.getRotation(), transform.getRotation(), partialTicks));
            }
            GlStateManager.scale(scale.x, scale.y, scale.z);
            context.getRender().renderModelGroup(context.getModel(), getObjectName(), entity, context.getTextureId(), false);
            renderChildren(entity, context, packInfo);
            GlStateManager.popMatrix();
        }


        @Override
        public void renderDebug(@Nullable T entity, EntityRenderContext context, A packInfo) {
            if (DynamXDebugOptions.DOOR_ATTACH_POINTS.isActive()) {
                //if (entity instanceof BaseVehicleEntity) {
                GlStateManager.pushMatrix();
                GlStateManager.rotate(ClientDynamXUtils.computeInterpolatedGlQuaternion(entity.prevRenderRotation, entity.renderRotation, context.getPartialTicks()));
                Vector3f point = getCarAttachPoint();
                RenderGlobal.drawBoundingBox(point.x - 0, point.y - 0.05f,
                        point.z - 0.05f, point.x + 0.05f, point.y + 0.05f, point.z + 0.05f,
                        1f, 1, 1, 1);
                transformForDebug();
                MutableBoundingBox box = new MutableBoundingBox();//collisionsHelper.getShapes().get(0).getBoundingBox();//getBox();
                getBox(box);
                //box = DynamXContext.getCollisionHandler().rotateBB(Vector3fPool.get(), box, entity.physicsRotation);
                RenderGlobal.drawBoundingBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                        0, 0, 1, 1);
                GlStateManager.popMatrix();
                /*} else if (entity instanceof DoorEntity) {
                    if (((DoorEntity<?>) entity).getPackInfo() != null) {
                        point = ((DoorEntity<?>) entity).getPackInfo().getDoorAttachPoint();
                    }
                    RenderGlobal.drawBoundingBox(point.x - 0, point.y - 0.05f,
                            point.z - 0.05f, point.x + 0.05f, point.y + 0.05f, point.z + 0.05f,
                            0.5f, 0, 1, 1);
                }*/
            }
            super.renderDebug(entity, context, packInfo);
        }
    }
}
