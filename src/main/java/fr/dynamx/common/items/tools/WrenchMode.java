package fr.dynamx.common.items.tools;

import com.jme3.bullet.joints.JointEnd;
import com.jme3.bullet.objects.PhysicsRigidBody;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.parts.PartSeat;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.PropsEntity;
import fr.dynamx.common.entities.modules.MovableModule;
import fr.dynamx.common.entities.modules.SeatsModule;
import fr.dynamx.common.entities.modules.TrailerAttachModule;
import fr.dynamx.common.entities.modules.movables.AttachObjects;
import fr.dynamx.common.entities.vehicles.CarEntity;
import fr.dynamx.common.entities.vehicles.TrailerEntity;
import fr.dynamx.common.handlers.TaskScheduler;
import fr.dynamx.common.items.ItemProps;
import fr.dynamx.common.network.packets.MessageDebugRequest;
import fr.dynamx.common.physics.joints.JointHandlerRegistry;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.PhysicsRaycastResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
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
        public void onWrenchRightClick(EntityPlayer playerIn, EnumHand handIn) {
            if (!playerIn.world.isRemote) {
                ItemStack itemOffhand = playerIn.getHeldItemOffhand();
                Item item = itemOffhand.getItem();
                if (item instanceof ItemProps) {
                    PropsEntity<?> spawnEntity = ((ItemProps<?>) item).getSpawnEntity(playerIn.world, playerIn,
                            Vector3fPool.get(playerIn.posX, playerIn.posY + 1.25, playerIn.posZ), playerIn.rotationYaw % 360.0F, item.getMetadata(itemOffhand));
                    playerIn.world.spawnEntity(spawnEntity);
                    spawnEntity.setPhysicsInitCallback((modularEntity, physicsHandler) -> {
                        physicsHandler.setLinearVelocity(DynamXUtils.toVector3f(playerIn.getLookVec()).multLocal(20));
                    });
                }
            }

        }
    };

    private final String label;
    private final String initials;

    protected WrenchMode(String label, TextFormatting color) {
        this.label = label;
        this.initials = Arrays.stream(label.split("_")).map(s -> s.substring(0, 1) + '.').collect(Collectors.joining("", color.toString(), "")).toUpperCase();
        WRENCH_MODES.add(this);
    }

    public String getLabel() {
        return "wrench.mode." + label;
    }

    public String getInitials() {
        return initials;
    }

    public String getMessage() {
        return "wrench.mode.set." + label;
    }

    public void onWrenchLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
    }

    public void onWrenchRightClick(EntityPlayer playerIn, EnumHand handIn) {
    }

    public void onWrenchRightClickClient(EntityPlayer playerIn, EnumHand handIn) {
    }

    public void onInteractWithEntity(EntityPlayer player, PhysicsEntity<?> targetEntity, boolean isSneaking) {
    }

    public static List<WrenchMode> getWrenchModes() {
        return WRENCH_MODES;
    }

    public static void switchMode(EntityPlayer player, ItemStack s) {
        if (!s.hasTagCompound()) s.setTagCompound(new NBTTagCompound());
        int l = s.getTagCompound().getInteger("mode") + 1;
        if (l >= WRENCH_MODES.size()) {
            l = 0;
        }
        s.getTagCompound().setInteger("mode", l);
        if (!player.capabilities.isCreativeMode) {
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
        player.sendMessage(new TextComponentTranslation(WRENCH_MODES.get(l).getMessage()));
    }

    public static void setMode(EntityPlayer player, ItemStack s, int mode) {
        if (!s.hasTagCompound()) s.setTagCompound(new NBTTagCompound());
        if (mode >= WRENCH_MODES.size()) {
            mode = 0;
        }
        s.getTagCompound().setInteger("mode", mode);
        if (!player.capabilities.isCreativeMode) {
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
        player.sendMessage(new TextComponentTranslation(WRENCH_MODES.get(mode).getMessage()));
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

    public static WrenchMode getCurrentMode(ItemStack s) {
        if (s.hasTagCompound()) {
            int l = s.getTagCompound().getInteger("mode");
            return l < WRENCH_MODES.size() ? WRENCH_MODES.get(l) : NONE;
        }
        return NONE;
    }

    public static boolean isCurrentMode(ItemStack stack, WrenchMode mode) {
        return getCurrentMode(stack) == mode;
    }

    private static class AttachObjectsWrenchMode extends WrenchMode {
        public AttachObjectsWrenchMode() {
            super("attach_objects", TextFormatting.RED);
        }

        @Override
        public void onWrenchLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
            act(player, false);
        }

        @Override
        public void onInteractWithEntity(EntityPlayer player, PhysicsEntity<?> targetEntity, boolean isSneaking) {
            act(player, true);
        }

        private void act(EntityPlayer player, boolean shouldWeldObjects) {
            QuaternionPool.openPool();
            Vector3fPool.openPool();
            Predicate<EnumBulletShapeType> predicateShape = p -> !p.isPlayer();

            PhysicsRaycastResult result = DynamXUtils.castRayFromEntity(player, 30, predicateShape);

            if (result != null) {
                BulletShapeType<?> shapeType = (BulletShapeType<?>) result.hitBody.getUserObject();

                ItemStack itemStack = player.getHeldItemMainhand();
                if (!ItemWrench.hasEntity(itemStack)) {
                    if (!shapeType.getType().isTerrain()) {
                        MovableModule movableModule = ((PhysicsEntity<?>) shapeType.getObjectIn()).getModuleByType(MovableModule.class);
                        movableModule.attachObjects.initObject(result.hitBody, result.hitPos, JointEnd.A);
                        ItemWrench.writeEntity(itemStack, (PhysicsEntity<?>) shapeType.getObjectIn());
                    } else {
                        player.sendMessage(new TextComponentString("§cYou must first click on an entity"));
                    }
                } else {
                    PhysicsEntity<?> containedEntity = ItemWrench.getEntity(itemStack, player.world);
                    if (containedEntity != null) {
                        AttachObjects attachObjects = containedEntity.getModuleByType(MovableModule.class).attachObjects;
                        if (!shapeType.getType().isTerrain()) {
                            attachObjects.initObject(result.hitBody, result.hitPos, JointEnd.B);
                        } else if (!shouldWeldObjects) {
                            // Single ended joint
                            attachObjects.initObject((PhysicsRigidBody) containedEntity.physicsHandler.getCollisionObject(), result.hitPos, JointEnd.A);
                        }
                        IPhysicsWorld physicsWorld = DynamXContext.getPhysicsWorld(containedEntity.world);
                        if (shapeType.getType().isBulletEntity()) {
                            physicsWorld.schedule(() -> JointHandlerRegistry.createJointWithOther(MovableModule.JOINT_NAME, containedEntity, (PhysicsEntity<?>) shapeType.getObjectIn(), (byte) (shouldWeldObjects ? 2 : 1)));
                        } else {
                            physicsWorld.schedule(() -> JointHandlerRegistry.createJointWithSelf(MovableModule.JOINT_NAME, containedEntity, (byte) (shouldWeldObjects ? 2 : 1)));
                        }
                        ItemWrench.removeEntity(itemStack);
                    }
                }
            }
            Vector3fPool.closePool();
            QuaternionPool.closePool();
        }
    }

    private static class AttachTrailersWrenchMode extends WrenchMode {
        public AttachTrailersWrenchMode() {
            super("attach_trailers", TextFormatting.GREEN);
        }

        @Override
        public void onInteractWithEntity(EntityPlayer player, PhysicsEntity<?> physicsEntity, boolean isSneaking) {
            if (isSneaking && physicsEntity instanceof BaseVehicleEntity) {
                BaseVehicleEntity<?> vehicleEntity = (BaseVehicleEntity<?>) physicsEntity;
                PhysicsEntity<?> physicsEntityTemp = ItemWrench.getEntity(player.getHeldItemMainhand(), player.world);
                if (physicsEntityTemp instanceof BaseVehicleEntity) {
                    BaseVehicleEntity<?> temp = (BaseVehicleEntity<?>) physicsEntityTemp;
                    BaseVehicleEntity<?> car = vehicleEntity instanceof CarEntity ? vehicleEntity : temp instanceof CarEntity ? temp : null;
                    BaseVehicleEntity<?> trailer = vehicleEntity instanceof TrailerEntity ? vehicleEntity : temp instanceof TrailerEntity ? temp : null;
                    if (car != null && trailer != null && car.getModuleByType(TrailerAttachModule.class) != null && trailer.getModuleByType(TrailerAttachModule.class) != null
                            && car.getModuleByType(TrailerAttachModule.class).getConnectedEntity() == -1 && trailer.getModuleByType(TrailerAttachModule.class).getConnectedEntity() == -1) {
                        DynamXUtils.attachTrailer(player, car, trailer);
                    } else {
                        TextComponentTranslation msg = new TextComponentTranslation("trailer.attach.fail", temp.getPackInfo().getName(), vehicleEntity.getPackInfo().getName());
                        msg.getStyle().setColor(TextFormatting.RED);
                        player.sendMessage(msg);
                    }
                    ItemWrench.removeEntity(player.getHeldItemMainhand());
                } else {
                    if (vehicleEntity.getModuleByType(TrailerAttachModule.class) != null && vehicleEntity.getModuleByType(TrailerAttachModule.class).getConnectedEntity() == -1) {
                        ItemWrench.writeEntity(player.getHeldItemMainhand(), vehicleEntity);
                        player.sendMessage(new TextComponentTranslation("trailer.wrench.first"));
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
        public void onWrenchRightClickClient(EntityPlayer playerIn, EnumHand handIn) {
            super.onWrenchRightClick(playerIn, handIn);
            //ClientDynamXUtils.playerToRagdoll(playerIn, new Vector3f(20, 20, 20));
        }

        @Override
        public void onInteractWithEntity(EntityPlayer context, PhysicsEntity<?> physicsEntity, boolean isSneaking) {
            if (isSneaking) {
                NBTTagCompound tag = new NBTTagCompound();
                context.sendMessage(new TextComponentString("Respawning !"));
                physicsEntity.writeToNBT(tag);
                tag.setTag("Pos", DynamXUtils.newDoubleNBTList(physicsEntity.posX, physicsEntity.posY + 3, physicsEntity.posZ));
                tag.setTag("Rotation", DynamXUtils.newFloatNBTList(physicsEntity.rotationYaw, 0));
                try {
                    PhysicsEntity<?> e = ObfuscationReflectionHelper.findConstructor(physicsEntity.getClass(), World.class).newInstance(context.world);
                    e.readFromNBT(tag);
                    physicsEntity.setDead(); //if no error in reflexion
                    TaskScheduler.schedule(new TaskScheduler.ScheduledTask((short) 2) {
                        @Override
                        public void run() {
                            context.world.spawnEntity(e);
                        }
                    });
                } catch (InstantiationException | IllegalAccessException |
                         InvocationTargetException instantiationException) {
                    context.sendMessage(new TextComponentString(TextFormatting.RED + " An error occurred"));
                    DynamXMain.log.fatal("Cannot respawn entity " + physicsEntity, instantiationException);
                }
            }
        }
    }

    private static class ChangeTextureWrenchMode extends WrenchMode {
        public ChangeTextureWrenchMode() {
            super("change_skins", TextFormatting.BLUE);
        }

        @Override
        public void onInteractWithEntity(EntityPlayer player, PhysicsEntity<?> targetEntity, boolean isSneaking) {
            if (targetEntity instanceof BaseVehicleEntity) {
                BaseVehicleEntity<?> vehicleEntity = (BaseVehicleEntity<?>) targetEntity;
                //TODO TAKE HIDDEN VARIANTS IN ACCOUNT
                if (vehicleEntity.getMetadata() + 1 < vehicleEntity.getPackInfo().getMaxTextureMetadata()) {
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

        HashMap<EntityPlayer, Entity> playerEntityHashMap = new HashMap<>();

        @Override
        public void onWrenchLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
            if (!(entity instanceof BaseVehicleEntity)) {
                playerEntityHashMap.put(player, entity);
                player.sendMessage(new TextComponentString("Entity selected: " + entity.getName()));
            } else {
                player.sendMessage(new TextComponentString("You can not mount a vehicle to a vehicle"));
            }
        }

        @Override
        public void onInteractWithEntity(EntityPlayer context, PhysicsEntity<?> physicsEntity, boolean isSneaking) {
            if (physicsEntity instanceof BaseVehicleEntity) {
                BaseVehicleEntity<?> baseVehicleEntity = (BaseVehicleEntity<?>) physicsEntity;
                SeatsModule seatsModule = baseVehicleEntity.getModuleByType(SeatsModule.class);
                Entity entity = playerEntityHashMap.remove(context);
                if (entity != null) {
                    for (Object object : baseVehicleEntity.getPackInfo().getPartsByType(PartSeat.class)) {
                        PartSeat partSeat = (PartSeat) object;
                        if (!partSeat.isDriver()) {
                            SeatsModule seats = ((IModuleContainer.ISeatsContainer) baseVehicleEntity).getSeats();
                            Entity seatRider = seats.getSeatToPassengerMap().get(partSeat);
                            if (seatRider == null) {
                                partSeat.mount(baseVehicleEntity, seatsModule, entity);
                                context.sendMessage(new TextComponentString("Entity added to vehicle"));
                                return;
                            }
                        }
                    }
                    context.sendMessage(new TextComponentString("No seat for entity was found"));
                }
            }
        }
    }
}
