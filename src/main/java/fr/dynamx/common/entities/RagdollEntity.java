package fr.dynamx.common.entities;

import com.jme3.bullet.joints.Constraint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.modules.AttachModule;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.network.sync.AttachedBodySynchronizer;
import fr.dynamx.common.network.sync.variables.EntityTransformsVariable;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.modules.MovableModule;
import fr.dynamx.api.network.sync.SynchronizedEntityVariable;
import fr.dynamx.common.physics.entities.EntityPhysicsHandler;
import fr.dynamx.common.physics.entities.EnumRagdollBodyPart;
import fr.dynamx.common.physics.entities.RagdollPhysics;
import fr.dynamx.common.physics.joints.EntityJoint;
import fr.dynamx.common.physics.joints.EntityJointsHandler;
import fr.dynamx.common.physics.joints.JointHandler;
import fr.dynamx.common.physics.joints.JointHandlerRegistry;
import fr.dynamx.common.physics.utils.RigidBodyTransform;
import fr.dynamx.common.physics.utils.SynchronizedRigidBodyTransform;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SPacketEntityEquipment;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SynchronizedEntityVariable.SynchronizedPhysicsModule()
public class RagdollEntity extends ModularPhysicsEntity<RagdollPhysics<?>> implements AttachedBodySynchronizer {
    private static final DataParameter<String> SKIN = EntityDataManager.createKey(RagdollEntity.class, DataSerializers.STRING);

    public static final Vector3f HEAD_BOX_SIZE = new Vector3f(0.25f, 0.25f, 0.25f);
    public static final Vector3f CHEST_BOX_SIZE = new Vector3f(0.24f, 0.375f, 0.129f);
    public static final Vector3f LIMB_BOX_SIZE = new Vector3f(0.129f, 0.375f, 0.129f);

    public static final Vector3f HEAD_ATTACH_POINT = new Vector3f(0f, -0.23f, 0);
    public static final Vector3f LIMB_ATTACH_POINT = new Vector3f(0f, 0.35f, 0);

    public static final Vector3f HEAD_BODY_ATTACH_POINT = new Vector3f(0, 0.41f, 0);
    public static final Vector3f RIGHT_ARM_ATTACH_POINT = new Vector3f(-0.369f, 0.375f, 0);
    public static final Vector3f LEFT_ARM_ATTACH_POINT = new Vector3f(0.369f, 0.375f, 0);
    public static final Vector3f RIGHT_LEG_ATTACH_POINT = new Vector3f(-0.13f, -0.42f, 0);
    public static final Vector3f LEFT_LEG_ATTACH_POINT = new Vector3f(0.13f, -0.42f, 0);

    private final List<MutableBoundingBox> unrotatedBoxes = new ArrayList<>();
    private final HashMap<Byte, SynchronizedRigidBodyTransform> transforms = new HashMap<>();

    @SynchronizedEntityVariable(name = "parts_pos")
    private final EntityTransformsVariable synchronizedTransforms = new EntityTransformsVariable(this, this);

    private short handlingTime;
    private EntityPlayer handledPlayer;

    private static final DataParameter<Integer> HANDLED_PLAYER_ID = EntityDataManager.createKey(RagdollEntity.class, DataSerializers.VARINT);

    private final EntityJointsHandler handler = new EntityJointsHandler(this);
    private final MovableModule movableModule = new MovableModule(this);

    //private final NonNullList<ItemStack> inventoryHands = NonNullList.<ItemStack>withSize(2, ItemStack.EMPTY);
    private final NonNullList<ItemStack> inventoryArmor = NonNullList.withSize(4, ItemStack.EMPTY);
    //private final NonNullList<ItemStack> handInventory = NonNullList.withSize(2, ItemStack.EMPTY);
    private final NonNullList<ItemStack> armorArray = NonNullList.withSize(4, ItemStack.EMPTY);

    public RagdollEntity(World world) {
        super(world);
        this.handlingTime = -1;
        this.handledPlayer = null;
    }

    public RagdollEntity(World world, Vector3f pos, float spawnRotationAngle, String skin) {
        this(world, pos, spawnRotationAngle, skin, (short) -1, null);
    }

    public RagdollEntity(World world, Vector3f pos, float spawnRotationAngle, String skin, short handlingTime, EntityPlayer handledPlayer) {
        super(world, pos, spawnRotationAngle);
        this.handlingTime = handlingTime;
        this.handledPlayer = handledPlayer;
        setSkin(skin);
        if (handledPlayer != null) {
            setHandledPlayer(handledPlayer.getEntityId());
            NonNullList<ItemStack> armorInventory = handledPlayer.inventory.armorInventory;
            for (int i = 0; i < armorInventory.size(); i++) {
                ItemStack s = armorInventory.get(i);
                inventoryArmor.set(i, s);
            }
        }
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(SKIN, "");
        dataManager.register(HANDLED_PLAYER_ID, -1);
    }

    /**
     * @param skin A player name or a string resource location
     */
    public void setSkin(String skin) {
        dataManager.set(SKIN, skin);
    }

    public String getSkin() {
        return dataManager.get(SKIN);
    }


    public void setHandledPlayer(int id) {
        dataManager.set(HANDLED_PLAYER_ID, id);
    }

    public int getHandledPlayer() {
        return dataManager.get(HANDLED_PLAYER_ID);
    }

    @Override
    public int getSyncTickRate() {
        return 2;
    }

    @Override
    public boolean initEntityProperties() {
        super.initEntityProperties();
        for (EnumRagdollBodyPart part : EnumRagdollBodyPart.values()) {
            RigidBodyTransform t = new RigidBodyTransform();
            Quaternion localQuat = new Quaternion().fromAngleNormalAxis((float) Math.toRadians(-rotationYaw), new Vector3f(0, 1, 0));
            Vector3f pos = DynamXGeometry.rotateVectorByQuaternion(part.getChestAttachPoint(), localQuat);
            t.setPosition(physicsPosition.add(pos));
            t.setRotation(localQuat);

            transforms.put((byte) part.ordinal(), new SynchronizedRigidBodyTransform(t));
        }
        return true;
    }

    @Override
    public void initPhysicsEntity(boolean usePhysics) {
        super.initPhysicsEntity(usePhysics);
        if (!world.isRemote) {
            DynamXMain.proxy.scheduleTask(world, () -> {
                for (EnumRagdollBodyPart enumBodyPart : EnumRagdollBodyPart.values()) {
                    if (!enumBodyPart.equals(EnumRagdollBodyPart.CHEST)) {
                        JointHandlerRegistry.createJointWithSelf(RagdollJointsHandler.JOINT_HANDLER_NAME, this, (byte) enumBodyPart.ordinal());
                    }
                }
            });
        }
    }

    @Override
    protected RagdollPhysics<?> createPhysicsHandler() {
        return new RagdollPhysics<>(this);
    }

    private RagdollJointsHandler attachModule;

    @Override
    public void createModules(ModuleListBuilder modules) {
        attachModule = new RagdollJointsHandler(this);
        movableModule.initSubModules(modules, this);
    }

    @Override
    protected void fireCreateModulesEvent(Side side) {
        //Don't simplify the generic type, for fml
        MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.CreateModules<>(RagdollEntity.class, this, moduleList, side));
    }

    @Override
    public List<MutableBoundingBox> getCollisionBoxes() {
        unrotatedBoxes.clear();
        MutableBoundingBox b = new MutableBoundingBox(-.4, -1, -.4, .4, .9, .4);
        b.offset(physicsPosition);
        unrotatedBoxes.add(b);
        return unrotatedBoxes;
    }

    @Override
    public <D extends IPhysicsModule<?>> D getModuleByType(Class<D> attachModuleClass) {
        if (attachModuleClass.hashCode() == RagdollJointsHandler.class.hashCode()) {
            return (D) attachModule;
        } else if (attachModuleClass == MovableModule.class)
            return (D) movableModule;
        return null;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        handler.readFromNBT(compound);
        setSkin(compound.getString("skin"));
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        handler.writeToNBT(compound);
        compound.setString("skin", getSkin());
    }

    @Override
    public void onRemovedFromWorld() {
        if (handledPlayer != null) {
            handledPlayer.eyeHeight = handledPlayer.getDefaultEyeHeight();
            handledPlayer.setInvisible(false);
            if (handledPlayer.getRidingEntity() == null && !DynamXContext.getWalkingPlayers().containsKey(handledPlayer))
                DynamXContext.getPlayerToCollision().get(handledPlayer).addToWorld();
        }
        handler.onRemovedFromWorld();
        super.onRemovedFromWorld();
    }

    @Override
    public EntityJointsHandler getJointsHandler() {
        return handler;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        synchronizedTransforms.setChanged(true);
        handler.updateEntity();
        if (handledPlayer != null && handlingTime > 0) {
            handlingTime--;
            if (handlingTime == 0) {
                handlingTime = -1;
                setDead();
            }
        }
        if (!world.isRemote) {
            for (EntityEquipmentSlot entityequipmentslot : EntityEquipmentSlot.values()) {
                ItemStack itemstack;

                switch (entityequipmentslot.getSlotType()) {
                    case HAND:
                        //itemstack = this.handInventory.get(entityequipmentslot.getIndex());
                        itemstack = ItemStack.EMPTY;
                        break;
                    case ARMOR:
                        itemstack = this.armorArray.get(entityequipmentslot.getIndex());
                        break;
                    default:
                        continue;
                }

                ItemStack itemstack1 = this.getItemStackFromSlot(entityequipmentslot);

                if (!ItemStack.areItemStacksEqual(itemstack1, itemstack)) {
                    if (!ItemStack.areItemStacksEqualUsingNBTShareTag(itemstack1, itemstack)) {
                        ((WorldServer) this.world).getEntityTracker().sendToTracking(this, new SPacketEntityEquipment(this.getEntityId(), entityequipmentslot, itemstack1));
                    }

                    switch (entityequipmentslot.getSlotType()) {
                        case HAND:
                            //this.handInventory.set(entityequipmentslot.getIndex(), itemstack1.isEmpty() ? ItemStack.EMPTY : itemstack1.copy());
                            break;
                        case ARMOR:
                            this.armorArray.set(entityequipmentslot.getIndex(), itemstack1.isEmpty() ? ItemStack.EMPTY : itemstack1.copy());
                    }
                }
            }
        }

        if (world.isRemote) {
            if (handledPlayer == null) {
                Entity entityByID = world.getEntityByID(getHandledPlayer());
                if (entityByID instanceof EntityPlayer)
                    handledPlayer = (EntityPlayer) entityByID;
            }
        }


        if (handledPlayer != null) {
            handledPlayer.motionX = 0;
            handledPlayer.motionY = 0;
            handledPlayer.motionZ = 0;
            handledPlayer.setPosition(posX, posY, posZ);
            handledPlayer.eyeHeight = 0;
        }
    }

    @Override
    public void updateMinecraftPos() {
        super.updateMinecraftPos();
        for (EnumRagdollBodyPart part : EnumRagdollBodyPart.values()) {
            transforms.get((byte) part.ordinal()).updatePos();
        }
    }

    @Override
    public void preUpdatePhysics(boolean simulatingPhysics) {
        super.preUpdatePhysics(simulatingPhysics);
        movableModule.preUpdatePhysics(simulatingPhysics);
        if (simulatingPhysics) {
            for (EnumRagdollBodyPart part : EnumRagdollBodyPart.values()) {
                PhysicsRigidBody body = physicsHandler.getBodyParts().get(part);
                transforms.get((byte) part.ordinal()).getPhysicTransform().set(body);
            }
        }
    }

    @Override
    public String getName() {
        return "DynamXRagdoll." + getEntityId();
    }

    @Override
    public void setDead() {
        super.setDead();
        handler.onSetDead();
    }

    @Override
    public Map<Byte, SynchronizedRigidBodyTransform> getTransforms() {
        return transforms;
    }

    @Override
    public void setPhysicsTransform(byte jointId, RigidBodyTransform transform) {
        physicsHandler.getBodyParts().get(EnumRagdollBodyPart.values()[jointId]).setPhysicsLocation(transform.getPosition());
        physicsHandler.getBodyParts().get(EnumRagdollBodyPart.values()[jointId]).setPhysicsRotation(transform.getRotation());
    }

    /*public Iterable<ItemStack> getHeldEquipment()
    {
        return this.inventoryHands;
    }*/

    public Iterable<ItemStack> getArmorInventoryList() {
        return this.inventoryArmor;
    }

    public ItemStack getItemStackFromSlot(EntityEquipmentSlot slotIn) {
        switch (slotIn.getSlotType()) {
            //case HAND:
            //  return this.inventoryHands.get(slotIn.getIndex());
            case ARMOR:
                return this.inventoryArmor.get(slotIn.getIndex());
            default:
                return ItemStack.EMPTY;
        }
    }

    @Override
    public void setItemStackToSlot(EntityEquipmentSlot slotIn, ItemStack stack) {
        switch (slotIn.getSlotType()) {
            case HAND:
                //this.inventoryHands.set(slotIn.getIndex(), stack);
                break;
            case ARMOR:
                this.inventoryArmor.set(slotIn.getIndex(), stack);
        }
    }

    public static class RagdollJointsHandler implements AttachModule.AttachToSelfModule, IPhysicsModule<EntityPhysicsHandler<?>> {
        public static final ResourceLocation JOINT_HANDLER_NAME = new ResourceLocation(DynamXConstants.ID, "ragdoll_parts");
        protected final RagdollEntity entity;

        static {
            JointHandlerRegistry.register(new JointHandler(JOINT_HANDLER_NAME, RagdollEntity.class, RagdollEntity.class, RagdollJointsHandler.class));
        }

        public RagdollJointsHandler(RagdollEntity entity) {
            this.entity = entity;
        }

        @Override
        public boolean canCreateJoint(PhysicsEntity<?> withEntity, byte jointId) {
            return true;
        }

        @Override
        public void onJointDestroyed(EntityJoint<?> joint) {
        }

        @Override
        public Constraint createJoint(byte jointId) {
            return RagdollPhysics.createBodyPartJoint(entity, EnumRagdollBodyPart.values()[jointId]);
        }
    }
}
