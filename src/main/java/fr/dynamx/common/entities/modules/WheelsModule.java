package fr.dynamx.common.entities.modules;

import com.jme3.bullet.objects.VehicleWheel;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.audio.EnumSoundState;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.IPropulsionModule;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.network.sync.v3.SynchronizationRules;
import fr.dynamx.api.network.sync.v3.SynchronizedEntityVariable;
import fr.dynamx.api.network.sync.v3.SynchronizedEntityVariableFactory;
import fr.dynamx.api.network.sync.v3.SynchronizedVariableSerializer;
import fr.dynamx.api.obj.IObjObject;
import fr.dynamx.api.physics.entities.IPropulsionHandler;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.ObjModelClient;
import fr.dynamx.client.renders.vehicle.RenderBaseVehicle;
import fr.dynamx.client.sound.SkiddingSound;
import fr.dynamx.client.sound.VehicleSound;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.parts.PartWheel;
import fr.dynamx.common.contentpack.type.vehicle.PartWheelInfo;
import fr.dynamx.common.contentpack.type.vehicle.SteeringWheelInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.network.sync.v3.DynamXSynchronizedVariables;
import fr.dynamx.common.obj.texture.TextureData;
import fr.dynamx.common.physics.entities.BaseWheeledVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.modules.WheelsPhysicsHandler;
import fr.dynamx.common.physics.entities.parts.wheel.WheelPhysics;
import fr.dynamx.common.physics.entities.parts.wheel.WheelState;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static fr.dynamx.api.entities.VehicleEntityProperties.getPropertyIndex;
import static fr.dynamx.client.ClientProxy.SOUND_HANDLER;

/**
 * Basic wheel implementation <br>
 * Works with an {@link EngineModule} but you can use your own engines
 *
 * @see WheelsPhysicsHandler
 */
public class WheelsModule implements IPropulsionModule<BaseWheeledVehiclePhysicsHandler<?>>, IPhysicsModule.IEntityUpdateListener, IPhysicsModule.IPhysicsUpdateListener, IPhysicsModule.IDrawableModule<BaseVehicleEntity<?>> {
    protected final SynchronizedEntityVariable<Map<Byte, PartWheelInfo>> wheelInfos = new SynchronizedEntityVariable<>((variable, value) -> {
        value.forEach(this::setWheelInfo);
    }, SynchronizationRules.CONTROLS_TO_SPECTATORS, new SynchronizedVariableSerializer<Map<Byte, PartWheelInfo>>() {
        @Override
        public void writeObject(ByteBuf buf, Map<Byte, PartWheelInfo> object) {
            buf.writeInt(object.size());
            object.forEach((id, info) -> {
                buf.writeByte(id);
                ByteBufUtils.writeUTF8String(buf, info.getFullName());
            });
        }

        @Override
        public Map<Byte, PartWheelInfo> readObject(ByteBuf buf, Map<Byte, PartWheelInfo> currentValue) {
            int size = buf.readInt();
            for (byte i = 0; i < size; i++)
                currentValue.put(buf.readByte(), DynamXObjectLoaders.WHEELS.findInfo(ByteBufUtils.readUTF8String(buf)));
            return currentValue;
        }
    }, new HashMap<>());
    /**
     * Wheels visual states, based on the physical states
     */
    protected SynchronizedEntityVariable<WheelState[]> wheelsStates;
    // [0;4] SkidInfo [4;8] Friction [8;12] longitudinal [12;16] lateral [16;20] getRotationDelta
    // todo clean wheelProperties system
    public SynchronizedEntityVariable<float[]> wheelProperties = new SynchronizedEntityVariable<>(SynchronizationRules.PHYSICS_TO_SPECTATORS, SynchronizedEntityVariableFactory.floatArraySerializer);

    protected byte[] wheelsTextureId;

    /**
     * Entity visual properties, accessible via the {@link IPhysicsModule}s
     */
    public SynchronizedEntityVariable<float[]> visualProperties = new SynchronizedEntityVariable<>(SynchronizationRules.PHYSICS_TO_SPECTATORS, SynchronizedEntityVariableFactory.floatArraySerializer);
    /**
     * Entity prev visual properties
     */
    public float[] prevVisualProperties;

    protected final BaseVehicleEntity<? extends BaseWheeledVehiclePhysicsHandler<?>> entity;
    protected WheelsPhysicsHandler wheelsPhysics;

    public WheelsModule(BaseVehicleEntity<? extends BaseWheeledVehiclePhysicsHandler<?>> entity) {
        this.entity = entity;
        this.wheelsStates = new SynchronizedEntityVariable<>((variable, value) -> {
            if (!entity.getSynchronizer().getSimulationHolder().ownsControls(FMLCommonHandler.instance().getEffectiveSide())) {
                if (DynamXMain.proxy.shouldUseBulletSimulation(entity.world)) {
                    for (int i = 0; i < value.length; i++) {
                        if (variable.get()[i] != value[i]) {
                            if (value[i] == WheelState.REMOVED)
                                ((WheelsPhysicsHandler) getPhysicsHandler()).removeWheel((byte) i);
                            else
                                ((WheelsPhysicsHandler) getPhysicsHandler()).getWheel(i).setFlattened(value[i] == WheelState.ADDED_FLATTENED);
                        }
                    }
                }
            }
        }, SynchronizationRules.CONTROLS_TO_SPECTATORS, new SynchronizedVariableSerializer<WheelState[]>() {
            @Override
            public void writeObject(ByteBuf buf, WheelState[] object) {
                buf.writeInt(object.length);
                for (WheelState f : object) {
                    buf.writeByte(f.ordinal());
                }
            }

            @Override
            public WheelState[] readObject(ByteBuf buf, WheelState[] currentValue) {
                currentValue = new WheelState[buf.readInt()];
                for (byte i = 0; i < currentValue.length; i++) {
                    currentValue[i] = WheelState.values()[buf.readByte()];
                }
                return currentValue;
            }
        });
    }

    public void setWheelInfo(byte partIndex, PartWheelInfo info) {
        if (wheelInfos.get().get(partIndex) != info) {
            VehicleEntityEvent.ChangeVehicleWheelEvent event = new VehicleEntityEvent.ChangeVehicleWheelEvent(FMLCommonHandler.instance().getEffectiveSide(), entity, this, wheelInfos.get().get(partIndex), info, partIndex);
            if (!MinecraftForge.EVENT_BUS.post(event)) {
                wheelInfos.get().put(partIndex, event.getNewWheel());
                wheelInfos.setChanged(true);
                if (wheelsPhysics != null)
                    wheelsPhysics.getWheelByPartIndex(partIndex).setWheelInfo(event.getNewWheel());
                if (entity.getEntityTextureID() != -1)
                    handleTextureID(entity.getEntityTextureID(), entity);
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleTextureID(byte metadata, BaseVehicleEntity<?> packInfo) {
        TextureData chassis = packInfo.getPackInfo().getTextures().get(metadata);
        if (chassis == null)
            chassis = packInfo.getPackInfo().getTextures().get((byte) 0);
        for (byte i = 0; i < wheelsTextureId.length; i++) {
            wheelsTextureId[i] = getWheelInfo(i).getIdForTexture(chassis.getName());
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
        wheelProperties.set(new float[wheelCount * VehicleEntityProperties.EnumWheelProperties.values().length]);
        for (PartWheel part : entity.getPackInfo().getPartsByType(PartWheel.class)) {
            wheelInfos.get().put(part.getId(), part.getDefaultWheelInfo());
        }
        wheelInfos.setChanged(true);
        wheelsStates.set(new WheelState[wheelCount]);
        this.wheelsTextureId = new byte[wheelCount];
        for (int i = 0; i < wheelCount; i++) {
            wheelsStates.get()[i] = WheelState.ADDED;
            wheelsTextureId[i] = -1;
        }
        visualProperties.set(new float[wheelCount * VehicleEntityProperties.EnumVisualProperties.values().length]);
        prevVisualProperties = new float[visualProperties.get().length];
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
        if (simulatePhysics) {
            if (entity.ticksExisted > 10) {
                for (int i = 0; i < wheelsPhysics.vehicleWheelData.size(); i++) {
                    WheelPhysics w = wheelsPhysics.vehicleWheelData.get(i);
                    if (w != null) {
                        Vector3f pos = Vector3fPool.get();
                        w.getPhysicsWheel().getCollisionLocation(pos);
                        BlockPos bp = new BlockPos(pos.x, Math.ceil(pos.y) - 1, pos.z);
                        IBlockState blockState = entity.world.getBlockState(bp);
                        float[] floats = ContentPackLoader.getBlockFriction(blockState.getBlock());
                        if (entity.world.getBiome(bp).canRain() && entity.world.isRaining() && entity.world.canBlockSeeSky(bp))
                            w.setGrip((w.isFlattened() ? 0.16f : 1) * floats[1]);
                        else
                            w.setGrip((w.isFlattened() ? 0.16f : 1) * floats[0]);
                        if (wheelsStates.get()[i] == WheelState.ADDED && w.isFlattened()) {
                            wheelsStates.get()[i] = WheelState.ADDED_FLATTENED;
                            wheelsStates.setChanged(true);
                        }
                        else if (wheelsStates.get()[i] == WheelState.ADDED_FLATTENED && !w.isFlattened()) {
                            wheelsStates.get()[i] = WheelState.ADDED;
                            wheelsStates.setChanged(true);
                        }
                    }
                }
            }
            wheelsPhysics.update();
        }
    }

    @Override
    public void postUpdatePhysics(boolean simulatingPhysics) {
        System.arraycopy(visualProperties.get(), 0, prevVisualProperties, 0, prevVisualProperties.length);
        if (simulatingPhysics)
            updateVisualProperties(visualProperties.get(), prevVisualProperties);
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

    public void updateVisualProperties(float[] visualProperties, float[] prevVisualProperties) {
        if (wheelsPhysics != null) {
            byte n = wheelsPhysics.getNumWheels();

            for (int i = 0; i < n; i++) {
                VehicleWheel info = wheelsPhysics.getHandler().getPhysicsVehicle().getWheel(i);
                if (info.isFrontWheel()) {
                    visualProperties[VehicleEntityProperties.getPropertyIndex(i, VehicleEntityProperties.EnumVisualProperties.STEERANGLE)] = (info.getSteerAngle() * DynamXGeometry.radToDeg);
                }

                int ind = VehicleEntityProperties.getPropertyIndex(i, VehicleEntityProperties.EnumVisualProperties.ROTATIONANGLE);
                //Update prevRotation, so we have -180<prevRotationYaw-rotationYaw<180 to avoid visual glitch
                float[] angles = DynamXMath.interpolateAngle((info.getRotationAngle() * DynamXGeometry.radToDeg) % 360, visualProperties[ind], 1);
                prevVisualProperties[ind] = angles[0];
                visualProperties[ind] = angles[1];

                visualProperties[VehicleEntityProperties.getPropertyIndex(i, VehicleEntityProperties.EnumVisualProperties.SUSPENSIONLENGTH)] = info.getSuspensionLength() + info.getRestLength();
                Vector3f pos = Vector3fPool.get();
                info.getCollisionLocation(pos);
                visualProperties[VehicleEntityProperties.getPropertyIndex(i, VehicleEntityProperties.EnumVisualProperties.COLLISIONX)] = pos.x;
                visualProperties[VehicleEntityProperties.getPropertyIndex(i, VehicleEntityProperties.EnumVisualProperties.COLLISIONY)] = pos.y;
                visualProperties[VehicleEntityProperties.getPropertyIndex(i, VehicleEntityProperties.EnumVisualProperties.COLLISIONZ)] = pos.z;
            }
            this.visualProperties.setChanged(true);
            for (byte b = 0; b < n; b++) {
                WheelPhysics w = wheelsPhysics.getWheel(b);
                if (w != null) {
                    if (w.getSkidInfo() != wheelProperties.get()[getPropertyIndex(b, VehicleEntityProperties.EnumWheelProperties.SKIDINFO)]) {
                        wheelProperties.get()[getPropertyIndex(b, VehicleEntityProperties.EnumWheelProperties.SKIDINFO)] = w.getSkidInfo();
                        wheelProperties.setChanged(true);
                    }
                    if (DynamXDebugOptions.WHEEL_ADVANCED_DATA.isActive()) {
                        wheelProperties.get()[getPropertyIndex(b, VehicleEntityProperties.EnumWheelProperties.FRICTION)] = w.getFriction();
                        wheelProperties.get()[getPropertyIndex(b, VehicleEntityProperties.EnumWheelProperties.PACEJKALONGITUDINAL)] = wheelsPhysics.pacejkaMagicFormula.longitudinal[b] / 10000;
                        wheelProperties.get()[getPropertyIndex(b, VehicleEntityProperties.EnumWheelProperties.PACEJKALATERAL)] = wheelsPhysics.pacejkaMagicFormula.lateral[b] / 10000;
                        wheelProperties.get()[getPropertyIndex(b, VehicleEntityProperties.EnumWheelProperties.ROTATIONDELTA)] = w.getDeltaRotation();
                        wheelProperties.setChanged(true);
                    }
                }
            }
        }
    }

    @Override
    public IPropulsionHandler getPhysicsHandler() {
        return wheelsPhysics;
    }

    @Override
    public float[] getPropulsionProperties() {
        return wheelProperties.get();
    }

    @Override
    public void addSynchronizedVariables(Side side, SimulationHolder simulationHolder) {
        entity.getSynchronizer().registerVariable(DynamXSynchronizedVariables.WHEEL_INFOS, wheelInfos);
        entity.getSynchronizer().registerVariable(DynamXSynchronizedVariables.WHEEL_STATES, wheelsStates);
        entity.getSynchronizer().registerVariable(DynamXSynchronizedVariables.WHEEL_PROPERTIES, wheelProperties);
        entity.getSynchronizer().registerVariable(DynamXSynchronizedVariables.WHEEL_VISUALS, visualProperties);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawParts(RenderPhysicsEntity<?> render, float partialTicks, BaseVehicleEntity<?> carEntity) {
        ObjModelClient vehicleModel = DynamXContext.getObjModelRegistry().getModel(carEntity.getPackInfo().getModel());
        /* Rendering the steering wheel */
        SteeringWheelInfo info = carEntity.getPackInfo().getSubPropertyByType(SteeringWheelInfo.class);
        if (info != null && !carEntity.getModuleByType(WheelsModule.class).getWheelInfos().isEmpty()) { //If has steering and wheels AND at least one wheel (think to loading errors)
            IObjObject steeringWheel = vehicleModel.getObjObject(info.getPartName());
            if (steeringWheel != null) {
                if (!MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.RenderVehicleEntityEvent(VehicleEntityEvent.RenderVehicleEntityEvent.Type.STEERING_WHEEL, (RenderBaseVehicle<?>) render, carEntity, PhysicsEntityEvent.Phase.PRE, partialTicks))) {
                    GlStateManager.pushMatrix();
                    Vector3f center = info.getSteeringWheelPosition();
                    //Translation to the steering wheel rotation point (and render pos)
                    GlStateManager.translate(center.x, center.y, center.z);

                    //Apply steering wheel base rotation
                    if (info.getSteeringWheelBaseRotation() != null)
                        GlStateManager.rotate(GlQuaternionPool.get(info.getSteeringWheelBaseRotation()));
                    //Rotate the steering wheel
                    int directingWheel = VehicleEntityProperties.getPropertyIndex(carEntity.getPackInfo().getDirectingWheel(), VehicleEntityProperties.EnumVisualProperties.STEERANGLE);
                    WheelsModule m = carEntity.getModuleByType(WheelsModule.class);
                    GlStateManager.rotate(-(m.prevVisualProperties[directingWheel] + (m.visualProperties.get()[directingWheel] - m.prevVisualProperties[directingWheel]) * partialTicks), 0F, 0F, 1F);

                    //Scale it
                    GlStateManager.scale(carEntity.getPackInfo().getScaleModifier().x, carEntity.getPackInfo().getScaleModifier().y, carEntity.getPackInfo().getScaleModifier().z);
                    //Render it
                    vehicleModel.renderGroup(steeringWheel, carEntity.getEntityTextureID());
                    GlStateManager.popMatrix();
                    MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.RenderVehicleEntityEvent(VehicleEntityEvent.RenderVehicleEntityEvent.Type.STEERING_WHEEL, (RenderBaseVehicle<?>) render, carEntity, PhysicsEntityEvent.Phase.POST, partialTicks));
                }
            }
        }

        if (!MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.RenderVehicleEntityEvent(VehicleEntityEvent.RenderVehicleEntityEvent.Type.PROPULSION, (RenderBaseVehicle<?>) render, carEntity, PhysicsEntityEvent.Phase.PRE, partialTicks))) {
            if (getPropulsionProperties() != null) {
                this.entity.getPackInfo().getPartsByType(PartWheel.class).forEach(partWheel -> {
                    if (wheelsStates.get()[partWheel.getId()] != WheelState.REMOVED) {
                        renderWheel(render, partWheel, partialTicks);
                    }
                });
            }
            MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.RenderVehicleEntityEvent(VehicleEntityEvent.RenderVehicleEntityEvent.Type.PROPULSION, (RenderBaseVehicle<?>) render, carEntity, PhysicsEntityEvent.Phase.POST, partialTicks));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void spawnPropulsionParticles(RenderPhysicsEntity<?> render, float partialTicks) {
        //Dust particles when the vehicle friction is very low
        entity.getPackInfo().getPartsByType(PartWheel.class).forEach(partWheel -> {
            PartWheelInfo info = getWheelInfo(partWheel.getId());
            if (info.enableRendering() && info.getSkidParticle() != null) {
                if (((IModuleContainer.IPropulsionContainer<?>) entity).getPropulsion().getPropulsionProperties()[VehicleEntityProperties.getPropertyIndex(partWheel.getId(), VehicleEntityProperties.EnumWheelProperties.SKIDINFO)] < 0.1f) {
                    entity.world.spawnParticle(info.getSkidParticle(), visualProperties.get()[VehicleEntityProperties.getPropertyIndex(partWheel.getId(), VehicleEntityProperties.EnumVisualProperties.COLLISIONX)],
                            visualProperties.get()[VehicleEntityProperties.getPropertyIndex(partWheel.getId(), VehicleEntityProperties.EnumVisualProperties.COLLISIONY)],
                            visualProperties.get()[VehicleEntityProperties.getPropertyIndex(partWheel.getId(), VehicleEntityProperties.EnumVisualProperties.COLLISIONZ)],
                            0, 0, 0);
                }
            }
        });
    }

    @SideOnly(Side.CLIENT)
    protected void renderWheel(RenderPhysicsEntity<?> render, PartWheel partWheel, float partialTicks) {
        int index;
        Quaternion baseRotation = partWheel.getSuspensionAxis();
        PartWheelInfo info = getWheelInfo(partWheel.getId());
        if (info.enableRendering()) {
            GlStateManager.pushMatrix();
            {
                /* Translation to the wheel rotation point */
                GlStateManager.translate(partWheel.getRotationPoint().x, partWheel.getRotationPoint().y, partWheel.getRotationPoint().z);

                /* Apply wheel base rotation */
                if (baseRotation.getW() != 0)
                    GlStateManager.rotate(GlQuaternionPool.get(baseRotation));

                /* Suspension translation */
                index = VehicleEntityProperties.getPropertyIndex(partWheel.getId(), VehicleEntityProperties.EnumVisualProperties.SUSPENSIONLENGTH);
                GlStateManager.translate(0, -(prevVisualProperties[index] + (visualProperties.get()[index] - prevVisualProperties[index]) * partialTicks) + 0.2, 0);

                /* Steering rotation*/
                if (partWheel.isWheelIsSteerable()) {
                    index = VehicleEntityProperties.getPropertyIndex(partWheel.getId(), VehicleEntityProperties.EnumVisualProperties.STEERANGLE);
                    GlStateManager.rotate((prevVisualProperties[index] + (visualProperties.get()[index] - prevVisualProperties[index]) * partialTicks), 0.0F, 1.0F, 0.0F);
                }

                /* Render mudguard */
                if (partWheel.getMudGuardPartName() != null) {
                    GlStateManager.scale(entity.getPackInfo().getScaleModifier().x, entity.getPackInfo().getScaleModifier().y, entity.getPackInfo().getScaleModifier().z);
                    DynamXContext.getObjModelRegistry().getModel(this.entity.getPackInfo().getModel()).renderGroups(partWheel.getMudGuardPartName(), entity.getEntityTextureID());
                }
            }
            GlStateManager.popMatrix();

            GlStateManager.pushMatrix();
            {
                /* Translation to the wheel rotation point */
                GlStateManager.translate(partWheel.getRotationPoint().x, partWheel.getRotationPoint().y, partWheel.getRotationPoint().z);

                /* Apply wheel base rotation */
                if (baseRotation.getW() != 0)
                    GlStateManager.rotate(GlQuaternionPool.get(baseRotation));

                /* Suspension translation */
                index = VehicleEntityProperties.getPropertyIndex(partWheel.getId(), VehicleEntityProperties.EnumVisualProperties.SUSPENSIONLENGTH);
                GlStateManager.translate(0, -(prevVisualProperties[index] + (visualProperties.get()[index] - prevVisualProperties[index]) * partialTicks), 0);

                /* Steering rotation*/
                if (partWheel.isWheelIsSteerable()) {
                    index = VehicleEntityProperties.getPropertyIndex(partWheel.getId(), VehicleEntityProperties.EnumVisualProperties.STEERANGLE);
                    GlStateManager.rotate((prevVisualProperties[index] + (visualProperties.get()[index] - prevVisualProperties[index]) * partialTicks), 0.0F, 1.0F, 0.0F);
                }

                //Remove wheel base rotation
                if (baseRotation.getW() != 0)
                    GlStateManager.rotate(GlQuaternionPool.get(baseRotation.inverse()));

                // Translate to render pos, from rotation pos
                GlStateManager.translate(partWheel.getPosition().x - partWheel.getRotationPoint().x, partWheel.getPosition().y - partWheel.getRotationPoint().y, partWheel.getPosition().z - partWheel.getRotationPoint().z);

                index = VehicleEntityProperties.getPropertyIndex(partWheel.getId(), VehicleEntityProperties.EnumVisualProperties.ROTATIONANGLE);
                //Fix sign problems for wheel rotation
                float prev = prevVisualProperties[index];
                if (prev - visualProperties.get()[index] > 180)
                    prev -= 360;
                if (prev - visualProperties.get()[index] < -180)
                    prev += 360;
                //Then render
                if (partWheel.isRight()) {
                    /* Wheel rotation (Right-Side)*/
                    GlStateManager.rotate(180, 0, 1, 0);
                    GlStateManager.rotate((prev + (visualProperties.get()[index] - prev) * partialTicks), -1.0F, 0.0F, 0.0F);
                } else {
                    /* Wheel rotation (Left-Side)*/
                    GlStateManager.rotate(-(prev + (visualProperties.get()[index] - prev) * partialTicks), -1.0F, 0.0F, 0.0F);
                }
                /*Rendering the wheels */
                ObjModelClient model = DynamXContext.getObjModelRegistry().getModel(info.getModel());
                //Scale
                GlStateManager.scale(info.getScaleModifier().x, info.getScaleModifier().y, info.getScaleModifier().z);
                //If the wheel is not flattened, or the model does not supports flattening
                if (wheelsStates.get()[partWheel.getId()] != WheelState.ADDED_FLATTENED || !model.renderGroups("rim", wheelsTextureId[partWheel.getId()])) {
                    render.renderModel(model, entity, wheelsTextureId[partWheel.getId()]);
                }
            }
            GlStateManager.popMatrix();
        }
    }

    public final Map<Integer, VehicleSound> sounds = new HashMap<>();
    private VehicleSound lastVehicleSound;
    private VehicleSound currentVehicleSound;

    //temporaire
    private int mySoundId;
    private int skiddingTime;

    @Override
    public boolean listenEntityUpdates(Side side) {
        return side.isClient();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateEntity() {
        // if (!MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.UpdateVehicleSoundEntityEvent(entity, this, PhysicsEntityEvent.Phase.PRE))) {
        if (entity.getPackInfo() != null && false) { //TODO ENABLE & IMPROVE
            //if (engineInfo != null && engineInfo.getEngineSounds() != null) {
            if (sounds.isEmpty()) { //Sounds are not initialized
                sounds.put(0, new SkiddingSound("skidding", entity, this));
                mySoundId = 0;
            }
            if (true) {
                        /*if (engineProperties != null) {
                            boolean forInterior = Minecraft.getMinecraft().gameSettings.thirdPersonView == 0 && entity.isRidingOrBeingRiddenBy(Minecraft.getMinecraft().player);
                            float rpm = engineProperties[VehicleEntityProperties.EnumEngineProperties.REVS.ordinal()] * engineInfo.getMaxRevs();
                            lastVehicleSound = currentVehicleSound;
                            if (currentVehicleSound == null || !currentVehicleSound.shouldPlay(rpm, forInterior)) {
                                sounds.forEach((id, vehicleSound) -> {
                                    if (vehicleSound.shouldPlay(rpm, forInterior)) {
                                        this.currentVehicleSound = vehicleSound;
                                    }
                                });
                            }
                        }*/
                int numSkdding = 0;
                for (int i = 0; i < entity.getPackInfo().getPartsByType(PartWheel.class).size(); i++) {
                    if (wheelProperties.get()[getPropertyIndex(i, VehicleEntityProperties.EnumWheelProperties.SKIDINFO)] < 0.1f)
                        numSkdding++;
                }
                if (numSkdding > 0) {
                    skiddingTime++;
                } else {
                    skiddingTime -= 5;
                }
                if (skiddingTime > 10) { //cooldown to start the skidding sound
                    currentVehicleSound = sounds.get(mySoundId);
                    skiddingTime = 10;
                } else if (skiddingTime <= 0) {
                    currentVehicleSound = null;
                    skiddingTime = 0;
                }
                if (currentVehicleSound != lastVehicleSound) //if playing sound changed
                {
                    if (lastVehicleSound != null)
                        SOUND_HANDLER.stopSound(lastVehicleSound);
                    if (currentVehicleSound != null) {
                        if (currentVehicleSound.getState() == EnumSoundState.STOPPING) //already playing
                            currentVehicleSound.onStarted();
                        else if (currentVehicleSound.getState() == EnumSoundState.STOPPED)
                            SOUND_HANDLER.playStreamingSound(Vector3fPool.get(currentVehicleSound.getPosX(), currentVehicleSound.getPosY(), currentVehicleSound.getPosZ()), currentVehicleSound);
                    }
                    lastVehicleSound = currentVehicleSound;
                }
            } else {
                if (currentVehicleSound != null)
                    SOUND_HANDLER.stopSound(currentVehicleSound);
                currentVehicleSound = lastVehicleSound = null;
            }
            //   }
        }
        //    MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.UpdateVehicleSoundEntityEvent(entity, this, PhysicsEntityEvent.Phase.POST));
        //  }
    }
}
