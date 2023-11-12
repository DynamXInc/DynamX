package fr.dynamx.common.entities.modules;

import com.jme3.bullet.objects.VehicleWheel;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SynchronizationRules;
import fr.dynamx.api.network.sync.SynchronizedEntityVariable;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.sound.VehicleSound;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.parts.PartWheel;
import fr.dynamx.common.contentpack.type.vehicle.PartWheelInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.network.sync.variables.EntityFloatArrayVariable;
import fr.dynamx.common.network.sync.variables.EntityMapVariable;
import fr.dynamx.common.physics.entities.BaseWheeledVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.modules.WheelsPhysicsHandler;
import fr.dynamx.common.physics.entities.parts.wheel.WheelPhysics;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Basic wheel implementation <br>
 * Works with an {@link CarEngineModule} but you can use your own engines
 *
 * @see WheelsPhysicsHandler
 */
@SynchronizedEntityVariable.SynchronizedPhysicsModule(modid = DynamXConstants.ID)
public class WheelsModule implements IPhysicsModule<BaseWheeledVehiclePhysicsHandler<?>>, IPhysicsModule.IPhysicsUpdateListener, IPackInfoReloadListener {
    //TODO CLEAN WHEELS CODE
    @SynchronizedEntityVariable(name = "wheel_infos")
    protected final EntityMapVariable<Map<Byte, PartWheelInfo>, Byte, PartWheelInfo> wheelInfos = new EntityMapVariable<>((variable, value) -> {
        value.forEach(this::setWheelInfo);
    }, SynchronizationRules.CONTROLS_TO_SPECTATORS);
    /**
     * Wheels visual states, based on the physical states
     */
    @SynchronizedEntityVariable(name = "wheel_states")
    protected EntityVariable<WheelState[]> wheelsStates;
    @SynchronizedEntityVariable(name = "skid_infos")
    public EntityFloatArrayVariable skidInfos = new EntityFloatArrayVariable(SynchronizationRules.PHYSICS_TO_SPECTATORS, null);

    @Getter
    protected byte[] wheelsTextureId;

    /**
     * Entity visual properties, accessible via the {@link IPhysicsModule}s
     */
    public float[] visualProperties;
    /**
     * Entity prev visual properties
     */
    public float[] prevVisualProperties;

    protected final BaseVehicleEntity<? extends BaseWheeledVehiclePhysicsHandler<?>> entity;
    protected WheelsPhysicsHandler wheelsPhysics;

    public WheelsModule(BaseVehicleEntity<? extends BaseWheeledVehiclePhysicsHandler<?>> entity) {
        this.entity = entity;
        wheelsStates = new EntityVariable<>((variable, value) -> {
            if (entity.getSynchronizer().getSimulationHolder().ownsControls(FMLCommonHandler.instance().getEffectiveSide())) {
                return;
            }
            if (!DynamXMain.proxy.shouldUseBulletSimulation(entity.world)) {
                return;
            }
            for (int i = 0; i < value.length; i++) {
                if (variable.get()[i] != value[i]) {
                    if (value[i] == WheelState.REMOVED) {
                        getPhysicsHandler().removeWheel((byte) i);
                    } else {
                        getPhysicsHandler().getWheel(i).setFlattened(value[i] == WheelState.ADDED_FLATTENED);
                    }
                }
            }
        }, SynchronizationRules.CONTROLS_TO_SPECTATORS);
    }

    @Override
    public void onPackInfosReloaded() {
        for (PartWheel part : entity.getPackInfo().getPartsByType(PartWheel.class)) {
            if (wheelInfos.get().containsKey(part.getId()) && Objects.equals(wheelInfos.get().get(part.getId()).getFullName(), part.getDefaultWheelInfo().getFullName()))
                setWheelInfo(part.getId(), part.getDefaultWheelInfo());
        }
    }

    public void setWheelInfo(byte partIndex, PartWheelInfo info) {
        if (wheelInfos.get().get(partIndex) == info) {
            return;
        }
        VehicleEntityEvent.ChangeWheel event = new VehicleEntityEvent.ChangeWheel(FMLCommonHandler.instance().getEffectiveSide(), entity, this, wheelInfos.get().get(partIndex), info, partIndex);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            return;
        }
        wheelInfos.put(partIndex, event.getNewWheel());
        if (wheelsPhysics != null)
            wheelsPhysics.getWheelByPartIndex(partIndex).setWheelInfo(event.getNewWheel());
        computeWheelsTextureIds();
    }

    @SideOnly(Side.CLIENT)
    public void computeWheelsTextureIds() {
        if (entity.getEntityTextureID() == -1)
            return;
        String chassis = entity.getPackInfo().getVariantName(entity.getEntityTextureID());
        for (byte i = 0; i < wheelsTextureId.length; i++) {
            wheelsTextureId[i] = getWheelInfo(i).getIdForVariant(chassis);
        }
    }

    @SideOnly(Side.CLIENT)
    public byte getWheelsTextureId(int wheelPartId) {
        return wheelsTextureId[wheelPartId];
    }

    public PartWheelInfo getWheelInfo(byte partIndex) {
        return wheelInfos.get().get(partIndex);
    }

    public Map<Byte, PartWheelInfo> getWheelInfos() {
        return wheelInfos.get();
    }

    /**
     * @return The wheels visual states, based on the physical states
     */
    public WheelState[] getWheelsStates() {
        return wheelsStates.get();
    }

    @Override
    public void initEntityProperties() {
        int wheelCount = entity.getPackInfo().getPartsByType(PartWheel.class).size();
        skidInfos.set(new float[wheelCount]);
        for (PartWheel part : entity.getPackInfo().getPartsByType(PartWheel.class)) {
            wheelInfos.put(part.getId(), part.getDefaultWheelInfo());
        }
        wheelsStates.set(new WheelState[wheelCount]);
        this.wheelsTextureId = new byte[wheelCount];
        for (int i = 0; i < wheelCount; i++) {
            wheelsStates.get()[i] = WheelState.ADDED;
            wheelsTextureId[i] = -1;
        }
        visualProperties = new float[wheelCount * VehicleEntityProperties.EnumVisualProperties.values().length];
        prevVisualProperties = new float[visualProperties.length];
    }

    @Override
    public void initPhysicsEntity(@Nullable BaseWheeledVehiclePhysicsHandler<?> handler) {
        if (handler != null) {
            wheelsPhysics = new WheelsPhysicsHandler(this, handler);
            //Call init after setting "wheels =", because init uses it
            wheelsPhysics.init();
            //Restore previous wheels state
            for (byte i = 0; i < wheelsStates.get().length; i++) {
                if (wheelsStates.get()[i] == WheelState.REMOVED)
                    wheelsPhysics.removeWheel(i);
                else
                    wheelsPhysics.getWheelByPartIndex(i).setFlattened(wheelsStates.get()[i] == WheelState.ADDED_FLATTENED);
            }
        }
    }

    @Override
    public void preUpdatePhysics(boolean simulatePhysics) {
        if (!simulatePhysics) {
            return;
        }
        if (entity.ticksExisted > 10) {
            for (int i = 0; i < wheelsPhysics.vehicleWheelData.size(); i++) {
                WheelPhysics w = wheelsPhysics.vehicleWheelData.get(i);
                if (w == null) {
                    continue;
                }
                Vector3f pos = Vector3fPool.get();
                w.getPhysicsWheel().getCollisionLocation(pos);
                BlockPos bp = new BlockPos(pos.x, Math.ceil(pos.y) - 1, pos.z);
                IBlockState blockState = entity.world.getBlockState(bp);
                float[] frictionValues = ContentPackLoader.getBlockFriction(blockState.getBlock());
                boolean isBlockWet = entity.world.getBiome(bp).canRain() && entity.world.isRaining() && entity.world.canBlockSeeSky(bp);
                float frictionValue = isBlockWet ? frictionValues[1] : frictionValues[0];
                w.setGrip((w.isFlattened() ? 0.16f : 1) * frictionValue);

                WheelState wheelState = wheelsStates.get()[i];
                if (wheelState == WheelState.ADDED && w.isFlattened()) {
                    wheelsStates.get()[i] = WheelState.ADDED_FLATTENED;
                    wheelsStates.setChanged(true);
                } else if (wheelState == WheelState.ADDED_FLATTENED && !w.isFlattened()) {
                    wheelsStates.get()[i] = WheelState.ADDED;
                    wheelsStates.setChanged(true);
                }
            }
        }
        wheelsPhysics.update();
    }

    @Override
    public void postUpdatePhysics(boolean simulatingPhysics) {
        System.arraycopy(visualProperties, 0, prevVisualProperties, 0, prevVisualProperties.length);
        if (simulatingPhysics)
            updateVisualProperties();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        int wheelCount = Math.min(tag.getByte("WheelCount"), wheelInfos.get().size());
        for (byte i = 0; i < wheelCount; i++) {
            PartWheelInfo info = DynamXObjectLoaders.WHEELS.findInfo(tag.getString("WheelInfo" + i));
            if (info != null)
                setWheelInfo(i, info);
            wheelsStates.get()[i] = WheelState.values()[tag.getByte("WheelState" + i)];
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setByte("WheelCount", (byte) wheelsStates.get().length);
        for (byte i = 0; i < wheelsStates.get().length; i++) {
            tag.setByte("WheelState" + i, (byte) wheelsStates.get()[i].ordinal());
            tag.setString("WheelInfo" + i, wheelInfos.get().get(i).getFullName());
        }
    }

    public void updateVisualProperties() {
        if (wheelsPhysics == null) {
            return;
        }
        byte numWheels = wheelsPhysics.getNumWheels();

        for (int i = 0; i < numWheels; i++) {
            VehicleWheel info = wheelsPhysics.getHandler().getPhysicsVehicle().getWheel(i);
            if (info.isFrontWheel()) {
                visualProperties[VehicleEntityProperties.getPropertyIndex(i, VehicleEntityProperties.EnumVisualProperties.STEER_ANGLE)] = (float) Math.toDegrees(info.getSteerAngle());
            }

            int indexRotationAngle = VehicleEntityProperties.getPropertyIndex(i, VehicleEntityProperties.EnumVisualProperties.ROTATION_ANGLE);
            //Update prevRotation, so we have -180<prevRotationYaw-rotationYaw<180 to avoid visual glitch
            float[] angles = DynamXMath.interpolateAngle((float) (Math.toDegrees(info.getRotationAngle()) % 360), visualProperties[indexRotationAngle], 1);
            prevVisualProperties[indexRotationAngle] = angles[0];
            visualProperties[indexRotationAngle] = angles[1];

            visualProperties[VehicleEntityProperties.getPropertyIndex(i, VehicleEntityProperties.EnumVisualProperties.SUSPENSION_LENGTH)] = info.getSuspensionLength();
            Vector3f pos = Vector3fPool.get();
            info.getCollisionLocation(pos);
            visualProperties[VehicleEntityProperties.getPropertyIndex(i, VehicleEntityProperties.EnumVisualProperties.COLLISION_X)] = pos.x;
            visualProperties[VehicleEntityProperties.getPropertyIndex(i, VehicleEntityProperties.EnumVisualProperties.COLLISION_Y)] = pos.y;
            visualProperties[VehicleEntityProperties.getPropertyIndex(i, VehicleEntityProperties.EnumVisualProperties.COLLISION_Z)] = pos.z;
        }
        for (byte b = 0; b < numWheels; b++) {
            WheelPhysics w = wheelsPhysics.getWheel(b);
            if (w != null)
                skidInfos.set(b, w.getSkidInfo());
        }
    }

    public WheelsPhysicsHandler getPhysicsHandler() {
        return wheelsPhysics;
    }

    public float[] getSkidInfos() {
        return skidInfos.get();
    }

    @SideOnly(Side.CLIENT)
    public void spawnPropulsionParticles(RenderPhysicsEntity<?> render, float partialTicks) {
        //Dust particles when the vehicle friction is very low
        entity.getPackInfo().getPartsByType(PartWheel.class).forEach(partWheel -> {
            PartWheelInfo info = getWheelInfo(partWheel.getId());
            if (!info.isModelValid() || info.getSkidParticle() == null) {
                return;
            }
            if (!(skidInfos.get()[partWheel.getId()] < 0.1f)) {
                return;
            }
            entity.world.spawnParticle(info.getSkidParticle(),
                    visualProperties[VehicleEntityProperties.getPropertyIndex(partWheel.getId(), VehicleEntityProperties.EnumVisualProperties.COLLISION_X)],
                    visualProperties[VehicleEntityProperties.getPropertyIndex(partWheel.getId(), VehicleEntityProperties.EnumVisualProperties.COLLISION_Y)],
                    visualProperties[VehicleEntityProperties.getPropertyIndex(partWheel.getId(), VehicleEntityProperties.EnumVisualProperties.COLLISION_Z)],
                    0, 0, 0);
        });
    }

    @Override
    public byte getInitPriority() {
        //Take care to add wheels module BEFORE engine module (an engine needs a propulsion)
        return 10;
    }

    public final Map<Integer, VehicleSound> sounds = new HashMap<>();

    public enum WheelState {
        ADDED,
        ADDED_FLATTENED,
        REMOVED
    }
}
