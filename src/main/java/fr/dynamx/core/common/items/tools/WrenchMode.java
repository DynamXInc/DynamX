package fr.dynamx.core.common.items.tools;

import com.jme3.bullet.joints.JointEnd;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.core.common.DynamXContext;
import fr.dynamx.core.common.DynamXMain;
import fr.dynamx.core.common.contentpack.parts.BasePartSeat;
import fr.dynamx.core.common.entities.BaseVehicleEntity;
import fr.dynamx.core.common.entities.PhysicsEntitiesFactory;
import fr.dynamx.core.common.entities.PhysicsEntity;
import fr.dynamx.core.common.entities.PropsEntity;
import fr.dynamx.core.common.entities.modules.MovableModule;
import fr.dynamx.core.common.entities.modules.SeatsModule;
import fr.dynamx.core.common.entities.modules.TrailerAttachModule;
import fr.dynamx.core.common.entities.modules.movables.AttachObjects;
import fr.dynamx.core.common.entities.vehicles.CarEntity;
import fr.dynamx.core.common.entities.vehicles.TrailerEntity;
import fr.dynamx.core.common.handlers.TaskScheduler;
import fr.dynamx.core.common.items.ItemProps;
import fr.dynamx.core.common.network.packets.MessageDebugRequest;
import fr.dynamx.core.common.physics.joints.JointHandlerRegistry;
import fr.dynamx.core.utils.optimization.Vector3fPool;
import fr.dynamx.forge.DynamXConfig;
import fr.dynamx.core.utils.DynamXUtils;
import fr.dynamx.core.utils.optimization.QuaternionPool;
import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.entities.HmEntityFactory;
import fr.hermes.api.mc.entities.HmPlayerEntity;
import fr.hermes.api.mc.items.HmItem;
import fr.hermes.api.mc.items.HmItemStack;
import fr.hermes.api.utils.HmEntityLogicMatcher;
import fr.hermes.forge.JmeVector3fPool;
import fr.dynamx.core.utils.physics.PhysicsRaycastResult;
import lombok.Getter;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class WrenchMode {
    private static final List<WrenchMode> WRENCH_MODES = new ArrayList<>();

    public static final WrenchMode NONE = new WrenchMode("none", TextFormatting.RED);
    public static final WrenchMode CHANGE_TEXTURE = new ChangeTextureWrenchMode();
    public static final WrenchMode ATTACH_TRAILERS = new AttachTrailersWrenchMode();
    public static final WrenchMode ATTACH_OBJECTS = new AttachObjectsWrenchMode();
    public static final WrenchMode REPLACE_ENTITIES = new ReplaceEntitiesWrenchMode();
    public static final WrenchMode ENTITY_SEAT_MODE = new EntitySeatWrenchMode();
    public static final WrenchMode LAUNCH_ENTITIES = new WrenchMode("launch_entities", TextFormatting.GOLD) {
        @Override
        public void onWrenchRightClick(HmPlayerEntity playerIn, EnumHand handIn) {
            if (!playerIn.hm$getWorld().hm$isClient()) {
                HmItemStack itemOffhand = playerIn.hm$getHeldItem(EnumHand.OFF_HAND);
                HmItem item = itemOffhand.hm$getItem();
                if (item instanceof ItemProps) {
                    Vector3f pos = JmeVector3fPool.get(playerIn.hm$getPosX(), playerIn.hm$getPosY() + 1.25, playerIn.hm$getPosZ());
                    PhysicsEntitiesFactory spawnEntity = ((ItemProps<?>) item).getSpawnEntity(playerIn,
                            pos, playerIn.hm$getRotationYaw() % 360.0F, itemOffhand.hm$getMetadata());
                    spawnEntity.setPhysicsInitCallback((modularEntity, physicsHandler) -> {
                        physicsHandler.setLinearVelocity(DynamXUtils.toVector3f(playerIn.hm$getLook()).multLocal(20));
                    });
                    playerIn.hm$getWorld().spawnHmEntity(spawnEntity, Vector3fPool.get(pos));
                }
            }

        }
    };

    private final String label;
    @Getter
    private final String initials;

    protected WrenchMode(String label, TextFormatting color) {
        this.label = label;
        this.initials = Arrays.stream(label.split("_")).map(s -> s.substring(0, 1) + '.').collect(Collectors.joining("", color.toString(), "")).toUpperCase();
        WRENCH_MODES.add(this);
    }

    public String getLabel() {
        return "wrench.mode." + label;
    }

    public String getMessage() {
        return "wrench.mode.set." + label;
    }

    public void onWrenchLeftClickEntity(HmItemStack stack, HmPlayerEntity player, HmEntity entity) {
    }

    public void onWrenchRightClick(HmPlayerEntity playerIn, EnumHand handIn) {
    }

    public void onWrenchRightClickClient(HmPlayerEntity playerIn, EnumHand handIn) {
    }

    public void onInteractWithEntity(HmPlayerEntity player, PhysicsEntity<?> targetEntity, boolean isSneaking) {
    }

    public static List<WrenchMode> getWrenchModes() {
        return WRENCH_MODES;
    }

    public static void switchMode(HmPlayerEntity player, HmItemStack s) {
        NBTTagCompound tag = s.hm$getOrCreateTagCompound();
        int l = tag.getInteger("mode") + 1;
        if (l >= WRENCH_MODES.size()) {
            l = 0;
        }
        tag.setInteger("mode", l);
        if (!player.hm$isCreativeMode()) {
            boolean allowed = false;
            for (int i = 0; i < DynamXConfig.allowedWrenchModes.length; i++) {
                if (DynamXConfig.allowedWrenchModes[i] == l) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                switchMode(player, s);
                return;
            }
        }
        player.hm$sendTranslatedMessage(WRENCH_MODES.get(l).getMessage());
    }

    public static void setMode(HmPlayerEntity player, HmItemStack s, int mode) {
        NBTTagCompound tag = s.hm$getOrCreateTagCompound();
        if (mode >= WRENCH_MODES.size()) {
            mode = 0;
        }
        tag.setInteger("mode", mode);
        if (!player.hm$isCreativeMode()) {
            boolean allowed = false;
            for (int i = 0; i < DynamXConfig.allowedWrenchModes.length; i++) {
                if (DynamXConfig.allowedWrenchModes[i] == mode) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                switchMode(player, s);
                return;
            }
        }
        player.hm$sendTranslatedMessage(WRENCH_MODES.get(mode).getMessage());
    }

    public static void sendWrenchMode(WrenchMode mode) {
        int index = 0;
        for (int i = 0; i < WRENCH_MODES.size(); i++) {
            if (WRENCH_MODES.get(i) == mode) {
                index = i;
                break;
            }
        }
        DynamXContext.getNetwork().sendToServer(new MessageDebugRequest(-15817 - index));
    }

    public static WrenchMode getCurrentMode(HmItemStack s) {
        if (s.hm$hasTagCompound()) {
            int l = s.hm$getTagCompound().getInteger("mode");
            return l < WRENCH_MODES.size() ? WRENCH_MODES.get(l) : NONE;
        }
        return NONE;
    }

    public static boolean isCurrentMode(HmItemStack stack, WrenchMode mode) {
        return getCurrentMode(stack) == mode;
    }

    private static class AttachObjectsWrenchMode extends WrenchMode {
        public AttachObjectsWrenchMode() {
            super("attach_objects", TextFormatting.RED);
        }

        @Override
        public void onWrenchLeftClickEntity(HmItemStack stack, HmPlayerEntity player, HmEntity entity) {
            act(player, false);
        }

        @Override
        public void onInteractWithEntity(HmPlayerEntity player, PhysicsEntity<?> targetEntity, boolean isSneaking) {
            act(player, true);
        }

        private void act(HmPlayerEntity player, boolean shouldWeldObjects) {
            QuaternionPool.openPool();
            JmeVector3fPool.openPool();
            Predicate<EnumBulletShapeType> predicateShape = p -> !p.isPlayer();

            PhysicsRaycastResult result = DynamXUtils.castRayFromEntity(player, 30, predicateShape);

            if (result != null) {
                BulletShapeType<?> shapeType = (BulletShapeType<?>) result.hitBody.getUserObject();

                HmItemStack itemStack = player.hm$getHeldItemMainhand();
                if (!ItemWrench.hasEntity(itemStack)) {
                    if (!shapeType.getType().isTerrain()) {
                        MovableModule movableModule = ((PhysicsEntity<?>) shapeType.getObjectIn()).getModuleByType(MovableModule.class);
                        movableModule.attachObjects.initObject(result.hitBody, result.hitPos, JointEnd.A);
                        ItemWrench.writeEntity(itemStack, (PhysicsEntity<?>) shapeType.getObjectIn());
                    } else {
                        player.hm$sendMessage("§cYou must first click on an entity");
                    }
                } else {
                    PhysicsEntity<?> containedEntity = ItemWrench.getEntity(itemStack, player.hm$getWorld());
                    if (containedEntity != null) {
                        AttachObjects attachObjects = containedEntity.getModuleByType(MovableModule.class).attachObjects;
                        if (!shapeType.getType().isTerrain()) {
                            attachObjects.initObject(result.hitBody, result.hitPos, JointEnd.B);
                        } else if (!shouldWeldObjects) {
                            // Single ended joint
                            attachObjects.initObject((PhysicsRigidBody) containedEntity.physicsHandler.getCollisionObject(), result.hitPos, JointEnd.A);
                        }
                        IPhysicsWorld physicsWorld = DynamXContext.getPhysicsWorld(containedEntity.getWorld());
                        if (shapeType.getType().isBulletEntity()) {
                            physicsWorld.schedule(() -> JointHandlerRegistry.createJointWithOther(MovableModule.JOINT_NAME, containedEntity, (PhysicsEntity<?>) shapeType.getObjectIn(), (byte) (shouldWeldObjects ? 2 : 1)));
                        } else {
                            physicsWorld.schedule(() -> JointHandlerRegistry.createJointWithSelf(MovableModule.JOINT_NAME, containedEntity, (byte) (shouldWeldObjects ? 2 : 1)));
                        }
                        ItemWrench.removeEntity(itemStack);
                    }
                }
            }
            JmeVector3fPool.closePool();
            QuaternionPool.closePool();
        }
    }

    private static class AttachTrailersWrenchMode extends WrenchMode {
        public AttachTrailersWrenchMode() {
            super("attach_trailers", TextFormatting.GREEN);
        }

        @Override
        public void onInteractWithEntity(HmPlayerEntity player, PhysicsEntity<?> physicsEntity, boolean isSneaking) {
            if (isSneaking && physicsEntity instanceof BaseVehicleEntity) {
                BaseVehicleEntity<?> vehicleEntity = (BaseVehicleEntity<?>) physicsEntity;
                PhysicsEntity<?> physicsEntityTemp = ItemWrench.getEntity(player.hm$getHeldItemMainhand(), player.hm$getWorld());
                if (physicsEntityTemp instanceof BaseVehicleEntity) {
                    BaseVehicleEntity<?> temp = (BaseVehicleEntity<?>) physicsEntityTemp;
                    BaseVehicleEntity<?> car = vehicleEntity instanceof CarEntity ? vehicleEntity : temp instanceof CarEntity ? temp : null;
                    BaseVehicleEntity<?> trailer = vehicleEntity instanceof TrailerEntity ? vehicleEntity : temp instanceof TrailerEntity ? temp : null;
                    if (car != null && trailer != null && car.getModuleByType(TrailerAttachModule.class) != null && trailer.getModuleByType(TrailerAttachModule.class) != null
                            && car.getModuleByType(TrailerAttachModule.class).getConnectedEntity() == -1 && trailer.getModuleByType(TrailerAttachModule.class).getConnectedEntity() == -1) {
                        DynamXUtils.attachTrailer(player, car, trailer);
                    } else {
                        player.hm$sendTranslatedMessage("trailer.attach.fail", TextFormatting.RED, temp.getPackInfo().getName(), vehicleEntity.getPackInfo().getName());
                    }
                    ItemWrench.removeEntity(player.hm$getHeldItemMainhand());
                } else {
                    if (vehicleEntity.getModuleByType(TrailerAttachModule.class) != null && vehicleEntity.getModuleByType(TrailerAttachModule.class).getConnectedEntity() == -1) {
                        ItemWrench.writeEntity(player.hm$getHeldItemMainhand(), vehicleEntity);
                        player.hm$sendTranslatedMessage("trailer.wrench.first");
                    }
                }
            }
        }
    }

    private static class ReplaceEntitiesWrenchMode extends WrenchMode {
        public ReplaceEntitiesWrenchMode() {
            super("respawn_entities", TextFormatting.GOLD);
        }

        @Override
        public void onInteractWithEntity(HmPlayerEntity context, PhysicsEntity<?> physicsEntity, boolean isSneaking) {
            if (isSneaking) {
                NBTTagCompound tag = new NBTTagCompound();
                context.hm$sendMessage("Respawning !");
                physicsEntity.getMcEntity().hm$removePassengers();
                physicsEntity.getMcEntity().hm$writeToNbt(tag);
                tag.setTag("Pos", DynamXUtils.newDoubleNBTList(physicsEntity.physicsPosition.x, physicsEntity.physicsPosition.y + 3, physicsEntity.physicsPosition.z));
                tag.setTag("Rotation", DynamXUtils.newFloatNBTList(physicsEntity.getMcEntity().hm$getRotationYaw(), 0));
                // TODO NO NO NO need to use the factory
                PhysicsEntitiesFactory entityFactory = physicsEntity.createEntityFactory();
                entityFactory.setEntityInitCallback(((modularEntity, modules) -> {
                    modularEntity.getMcEntity().hm$readFromNbt(tag);
                }));
                physicsEntity.getMcEntity().hm$setDead(); //if no error in reflexion
                TaskScheduler.schedule(new TaskScheduler.ScheduledTask((short) 2) {
                    @Override
                    public void run() {
                        context.hm$getWorld().spawnHmEntity(entityFactory, Vector3fPool.get(entityFactory.getPos()));
                    }
                });
            }
        }
    }

    private static class ChangeTextureWrenchMode extends WrenchMode {
        public ChangeTextureWrenchMode() {
            super("change_skins", TextFormatting.BLUE);
        }

        @Override
        public void onInteractWithEntity(HmPlayerEntity player, PhysicsEntity<?> targetEntity, boolean isSneaking) {
            if (targetEntity instanceof BaseVehicleEntity) {
                BaseVehicleEntity<?> vehicleEntity = (BaseVehicleEntity<?>) targetEntity;
                //TODO TAKE HIDDEN VARIANTS IN ACCOUNT
                if (vehicleEntity.getMetadata() + 1 < vehicleEntity.getPackInfo().getMaxVariantId()) {
                    vehicleEntity.setMetadata(vehicleEntity.getMetadata() + 1);
                } else {
                    vehicleEntity.setMetadata(0);
                }
            }
        }
    }

    private static class EntitySeatWrenchMode extends WrenchMode {
        public EntitySeatWrenchMode() {
            super("entity_seat", TextFormatting.LIGHT_PURPLE);
        }

        HashMap<HmPlayerEntity, HmEntity> playerEntityHashMap = new HashMap<>();

        @Override
        public void onWrenchLeftClickEntity(HmItemStack stack, HmPlayerEntity player, HmEntity entity) {
            if (!HmEntityLogicMatcher.is(entity, BaseVehicleEntity.class)) {
                playerEntityHashMap.put(player, entity);
                player.hm$sendMessage("Entity selected: " + entity.hm$getName());
            } else {
                player.hm$sendMessage("You can not mount a vehicle to a vehicle");
            }
        }

        @Override
        public void onInteractWithEntity(HmPlayerEntity context, PhysicsEntity<?> physicsEntity, boolean isSneaking) {
            if (physicsEntity instanceof BaseVehicleEntity) {
                BaseVehicleEntity<?> baseVehicleEntity = (BaseVehicleEntity<?>) physicsEntity;
                SeatsModule seatsModule = baseVehicleEntity.getModuleByType(SeatsModule.class);
                HmEntity entity = playerEntityHashMap.remove(context);
                if (entity != null) {
                    for (Object object : baseVehicleEntity.getPackInfo().getPartsByType(BasePartSeat.class)) {
                        BasePartSeat partSeat = (BasePartSeat) object;
                        if (!partSeat.isDriver()) {
                            SeatsModule seats = ((IModuleContainer.ISeatsContainer) baseVehicleEntity).getSeats();
                            HmEntity seatRider = seats.getSeatToPassengerMap().get(partSeat);
                            if (seatRider == null) {
                                partSeat.mountEntity(baseVehicleEntity, seatsModule, entity);
                                context.hm$sendMessage("Entity added to vehicle");
                                return;
                            }
                        }
                    }
                    context.hm$sendMessage("No seat for entity was found");
                }
            }
        }
    }
}
