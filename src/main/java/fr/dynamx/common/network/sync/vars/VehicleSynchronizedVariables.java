package fr.dynamx.common.network.sync.vars;

import com.jme3.bullet.objects.VehicleWheel;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.entities.modules.IEngineModule;
import fr.dynamx.api.network.sync.PhysicsEntityNetHandler;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.network.sync.SyncTarget;
import fr.dynamx.api.network.sync.SynchronizedVariable;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.type.PartWheelInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.common.entities.modules.EngineModule;
import fr.dynamx.common.entities.modules.WheelsModule;
import fr.dynamx.common.network.sync.MessagePhysicsEntitySync;
import fr.dynamx.common.physics.entities.BaseWheeledVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.modules.WheelsPhysicsHandler;
import fr.dynamx.common.physics.entities.parts.wheel.WheelState;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.debug.SyncTracker;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.maths.DynamXMath;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains all basic vehicle synchronized variables (engine, controls, wheels...)
 */
public class VehicleSynchronizedVariables {
    /**
     * Wheel data, used by {@link BaseVehicleEntity} that implement {@link IModuleContainer.IEngineContainer}
     */
    public static class Engine<A extends BaseVehicleEntity<?>> implements SynchronizedVariable<A> {
        public static final ResourceLocation NAME = new ResourceLocation(DynamXConstants.ID, "engine");

        private float[] engineProperties;
        private boolean isEngineStarted;

        private int creationTick;
        private long creationTime;

        private int lastGetValTick;
        private long lastGetValTime;

        private SyncTarget lastTarget;

        public Engine() {
            this.creationTick = FMLCommonHandler.instance().getMinecraftServerInstance() != null ? FMLCommonHandler.instance().getMinecraftServerInstance().getTickCounter() : -1;
            this.creationTime = System.currentTimeMillis();
        }

        @Override
        public SyncTarget getValueFrom(A entity, PhysicsEntityNetHandler<A> network, Side side, int syncTick) {
            IEngineModule<?> engine = ((IModuleContainer.IEngineContainer) entity).getEngine();
            this.lastGetValTick = entity.ticksExisted;
            this.lastGetValTime = System.currentTimeMillis();
            if (engine.getPhysicsHandler().getEngine() != null) {
                boolean changed = false;
                boolean started = engine.getPhysicsHandler().getEngine().isStarted();
                if (started != isEngineStarted) {
                    changed = true;
                    SyncTracker.addChange("engine", "started");
                    this.isEngineStarted = engine.getPhysicsHandler().getEngine().isStarted();
                }
                if (engineProperties == null) { //If not initialized
                    changed = true;
                    engineProperties = engine.getEngineProperties().clone();
                } else { //Detect changes
                    for (int i = 0; i < engineProperties.length; i++) {
                        if (SyncTracker.different(engineProperties[i], engine.getEngineProperties()[i])) {
                            engineProperties[i] = engine.getEngineProperties()[i];
                            changed = true;
                            SyncTracker.addChange("engine", "prop#" + i);
                        }
                    }
                }
                lastTarget = changed ? SyncTarget.nearSpectatorForSide(side) : SyncTarget.NONE;
                return changed ? SyncTarget.nearSpectatorForSide(side) : SyncTarget.NONE;
            } else {//ModulableVehiclePhysicsHandler has no engine (not initialized ?) : send nothing
                lastTarget = SyncTarget.NONE;
                return SyncTarget.NONE;
            }
        }

        @Override
        public void setValueTo(A entity, PhysicsEntityNetHandler<A> network, MessagePhysicsEntitySync msg, Side side) {
            IEngineModule<?> engine = ((IModuleContainer.IEngineContainer) entity).getEngine();
            if (network.getSimulationHolder().isSinglePlayer()) {//In solo mode
                System.arraycopy(engineProperties, 0, engine.getEngineProperties(), 0, engineProperties.length); //Copy because the engineProperties is reused by the server
            } else if (!network.getSimulationHolder().ownsPhysics(side)) { //If we are not the simulator
                System.arraycopy(engineProperties, 0, engine.getEngineProperties(), 0, engineProperties.length); //Copy because the engineProperties is reused by the server
                //engine.setEngineProperties(engineProperties);
                //Sync bullet for prediction :
                {
                    fr.dynamx.common.physics.entities.parts.engine.Engine e = engine.getPhysicsHandler().getEngine();
                    e.setRevs(engineProperties[VehicleEntityProperties.EnumEngineProperties.REVS.ordinal()]);
                    //e.setMaxRevs(engineProperties[2]);
                    engine.getPhysicsHandler().syncActiveGear((int) engineProperties[VehicleEntityProperties.EnumEngineProperties.ACTIVE_GEAR.ordinal()]);
                    //e.setPower(engineProperties[VehicleEntityProperties.EnumEngineProperties.POWER.ordinal()]);
                    //e.setBraking(engineProperties[VehicleEntityProperties.EnumEngineProperties.BRAKING.ordinal()]);
                    e.setStarted(isEngineStarted);
                }
            }
            //else
            //DynamXMain.log.error("Incorrect simulation holder in set engine values : "+network.getSimulationHolder()+" "+side);
        }

        //TODO REMOVE WHEN DEBUGGED
        @Override
        public void validate(Object entity, int step) {
            if (engineProperties == null) {
                throw new NullPointerException("Invalid engine sync at step " + step + ". Entity " + entity + ". Var " + this + ". Last target " + lastTarget +
                        ". Timings: Creation: " + creationTick + " " + (System.currentTimeMillis() - creationTime) + "ms. GetVal: " + lastGetValTick + " " + (System.currentTimeMillis() - lastGetValTime) + "ms");
            }
            //System.out.println("Validate Engine sync at step " + step + ". Entity " + entity + ". Var " + this);
        }

        @Override
        public void write(ByteBuf buf, boolean compress) {
            buf.writeBoolean(isEngineStarted);
            if (engineProperties == null) {
                throw new NullPointerException("EngineProps null and last target " + lastTarget);
            }
            buf.writeInt(engineProperties.length);
            for (float f : engineProperties) {
                buf.writeFloat(f);
            }
        }

        @Override
        public void writeEntityValues(A entity, ByteBuf buf) {
            IEngineModule<?> engine = ((IModuleContainer.IEngineContainer) entity).getEngine();
            buf.writeBoolean(engine.isEngineStarted());
            buf.writeInt(engine.getEngineProperties().length);
            for (float f : engine.getEngineProperties()) {
                buf.writeFloat(f);
            }
        }

        @Override
        public void read(ByteBuf buf) {
            isEngineStarted = buf.readBoolean();
            engineProperties = new float[buf.readInt()];
            for (int i = 0; i < engineProperties.length; i++) {
                engineProperties[i] = buf.readFloat();
            }
        }
    }

    /**
     * Wheel visual data, used by {@link BaseVehicleEntity} that have {@link fr.dynamx.common.entities.modules.WheelsModule}
     */
    public static class WheelVisuals<A extends BaseVehicleEntity<?>> implements SynchronizedVariable<A> {
        public static final ResourceLocation NAME = new ResourceLocation(DynamXConstants.ID, "wvisuals");

        private float[] wheelProperties;

        @Override
        public SyncTarget getValueFrom(A vehicleEntity, PhysicsEntityNetHandler<A> network, Side side, int syncTick) {
            boolean changed = false;
            WheelsModule wheels = ((IModuleContainer.IPropulsionContainer<WheelsModule>) vehicleEntity).getPropulsion();
            if (wheelProperties == null) //If not initialized
            {
                changed = true;
                wheelProperties = wheels.getPropulsionProperties().clone();
            } else { //Detect changes
                for (int i = 0; i < wheelProperties.length; i++) {
                    if (SyncTracker.different(wheelProperties[i], wheels.getPropulsionProperties()[i])) {
                        changed = true;
                        wheelProperties[i] = wheels.getPropulsionProperties()[i];
                        SyncTracker.addChange("wheel_visuals", "prop#" + i);
                    }
                }
            }
            return changed ? SyncTarget.nearSpectatorForSide(side) : SyncTarget.NONE;
        }

        @Override
        public void setValueTo(A entity, PhysicsEntityNetHandler<A> network, MessagePhysicsEntitySync msg, Side side) {
            if (!network.getSimulationHolder().ownsPhysics(side)) {
                WheelsModule wheels = ((IModuleContainer.IPropulsionContainer<WheelsModule>) entity).getPropulsion();
                wheels.wheelProperties = wheelProperties;
            }
        }

        @Override
        public void write(ByteBuf buf, boolean compress) {
            buf.writeInt(wheelProperties.length);
            for (float f : wheelProperties) {
                buf.writeFloat(f);
            }
        }

        @Override
        public void writeEntityValues(A entity, ByteBuf buf) {
            WheelsModule wheels = ((IModuleContainer.IPropulsionContainer<WheelsModule>) entity).getPropulsion();
            buf.writeInt(wheels.wheelProperties.length);
            for (float f : wheels.wheelProperties) {
                buf.writeFloat(f);
            }
        }

        @Override
        public void read(ByteBuf buf) {
            wheelProperties = new float[buf.readInt()];
            for (byte i = 0; i < wheelProperties.length; i++) {
                wheelProperties[i] = buf.readFloat();
            }
        }
    }

    /**
     * Wheel physics data, used by {@link BaseVehicleEntity} that have {@link WheelsModule}
     */
    public static class WheelPhysics<A extends BaseVehicleEntity<?>> implements SynchronizedVariable<A> {
        public static final ResourceLocation NAME = new ResourceLocation(DynamXConstants.ID, "wphysics");

        private WheelState[] wheelsStates;
        private Map<Byte, PartWheelInfo> wheelInfos;
        private boolean received;

        @Override
        public SyncTarget getValueFrom(A vehicleEntity, PhysicsEntityNetHandler<A> network, Side side, int syncTick) {
            boolean changed = false;
            WheelsModule wheels = ((IModuleContainer.IPropulsionContainer<WheelsModule>) vehicleEntity).getPropulsion();
            if (wheelsStates == null) //If not initialized
            {
                changed = true;
                wheelsStates = wheels.getWheelsStates().clone();
                wheelInfos = new HashMap<>(wheels.getWheelInfos());
            } else { //Detect changes
                for (int i = 0; i < wheelsStates.length; i++) {
                    if (wheelsStates[i] != wheels.getWheelsStates()[i]) {
                        changed = true;
                        SyncTracker.addChange("wheel_physics", "state#" + i);
                        wheelsStates[i] = wheels.getWheelsStates()[i];
                    }
                }
                for (Map.Entry<Byte, PartWheelInfo> entry : wheelInfos.entrySet()) {
                    if (entry.getValue() != wheels.getWheelInfo(entry.getKey())) {
                        changed = true;
                        SyncTracker.addChange("wheel_physics", "wheel_info#" + entry.getKey());
                        wheelInfos.put(entry.getKey(), wheels.getWheelInfo(entry.getKey()));
                    }
                }
            }
            return changed ? SyncTarget.nearSpectatorForSide(side) : SyncTarget.NONE;
        }

        @Override
        public void setValueTo(A entity, PhysicsEntityNetHandler<A> network, MessagePhysicsEntitySync msg, Side side) {
            if (msg == null || received) {
                WheelsModule wheels = ((IModuleContainer.IPropulsionContainer<WheelsModule>) entity).getPropulsion();
                if (!network.getSimulationHolder().ownsControls(side)) {
                    if (DynamXMain.proxy.shouldUseBulletSimulation(entity.world)) {
                        for (int i = 0; i < wheelsStates.length; i++) {
                            if (wheels.getWheelsStates()[i] != wheelsStates[i]) {
                                if (wheelsStates[i] == WheelState.REMOVED)
                                    ((WheelsPhysicsHandler) wheels.getPhysicsHandler()).removeWheel((byte) i);
                                else
                                    ((WheelsPhysicsHandler) wheels.getPhysicsHandler()).getWheel(i).setFlattened(wheelsStates[i] == WheelState.ADDED_FLATTENED);
                            }
                        }
                    }
                    wheels.setWheelsStates(wheelsStates);
                }
                wheelInfos.forEach(wheels::setWheelInfo);
                received = false;
            }
        }

        @Override
        public void write(ByteBuf buf, boolean compress) {
            buf.writeInt(wheelsStates.length);
            for (WheelState f : wheelsStates) {
                buf.writeByte(f.ordinal());
            }
            buf.writeInt(wheelInfos.size());
            wheelInfos.forEach((id, info) -> {
                buf.writeByte(id);
                ByteBufUtils.writeUTF8String(buf, info.getFullName());
            });
        }

        @Override
        public void writeEntityValues(A entity, ByteBuf buf) {
            WheelsModule wheels = ((IModuleContainer.IPropulsionContainer<WheelsModule>) entity).getPropulsion();
            buf.writeInt(wheels.getWheelsStates().length);
            for (WheelState f : wheels.getWheelsStates()) {
                buf.writeByte(f.ordinal());
            }
            buf.writeInt(wheels.getWheelInfos().size());
            wheels.getWheelInfos().forEach((id, info) -> {
                buf.writeByte(id);
                ByteBufUtils.writeUTF8String(buf, info.getFullName());
            });
        }

        @Override
        public void read(ByteBuf buf) {
            wheelsStates = new WheelState[buf.readInt()];
            for (byte i = 0; i < wheelsStates.length; i++) {
                wheelsStates[i] = WheelState.values()[buf.readByte()];
            }
            if (wheelInfos == null)
                wheelInfos = new HashMap<>();
            int size = buf.readInt();
            for (byte i = 0; i < size; i++) {
                wheelInfos.put(buf.readByte(), DynamXObjectLoaders.WHEELS.findInfo(ByteBufUtils.readUTF8String(buf)));
            }
            received = true;
        }
    }

    /**
     * Visual effects : wheel rotations and contacts
     */
    public static class Visuals<A extends PhysicsEntity<?>> implements SynchronizedVariable<A> {
        public static final ResourceLocation NAME = new ResourceLocation(DynamXConstants.ID, "visuals");
        private float[] visualProperties;

        private int creationTick;
        private long creationTime;

        private int lastGetValTick;
        private long lastGetValTime;

        private SyncTarget lastTarget;

        public Visuals() {
            this.creationTick = FMLCommonHandler.instance().getMinecraftServerInstance() != null ? FMLCommonHandler.instance().getMinecraftServerInstance().getTickCounter() : -1;
            this.creationTime = System.currentTimeMillis();
        }

        //TODO REMOVE WHEN DEBUGGED
        @Override
        public void validate(Object entity, int step) {
            if (visualProperties == null) {
                throw new NullPointerException("Invalid visuals sync at step " + step + ". Entity " + entity + ". Var " + this + ". Last target " + lastTarget +
                        ". Timings: Creation: " + creationTick + " " + (System.currentTimeMillis() - creationTime) + "ms. GetVal: " + lastGetValTick + " " + (System.currentTimeMillis() - lastGetValTime) + "ms");
            }
            //System.out.println("Validate Engine sync at step " + step + ". Entity " + entity + ". Var " + this);
        }

        @Override
        public SyncTarget getValueFrom(A entity, PhysicsEntityNetHandler<A> network, Side side, int syncTick) {
            this.lastGetValTick = entity.ticksExisted;
            this.lastGetValTime = System.currentTimeMillis();
            boolean changed = false;
            WheelsModule m = ((IModuleContainer.IPropulsionContainer<WheelsModule>) entity).getPropulsion();
            if (visualProperties == null) //If not initialized
            {
                visualProperties = new float[m.visualProperties.length];
                changed = true;
            } else { //Detect changes
                for (int i = 0; i < visualProperties.length; i++) {
                    if (SyncTracker.different(visualProperties[i], m.visualProperties[i], 0.001f)) {
                        changed = true;
                        SyncTracker.addChange("visuals", "prop#" + i + "=" + VehicleEntityProperties.getPropertyByIndex(i) + " delta : " + (visualProperties[i] - m.visualProperties[i]));
                        visualProperties[i] = m.visualProperties[i];
                    }
                }
            }
            lastTarget = changed ? (side.isServer() ? SyncTarget.SERVER : SyncTarget.SPECTATORS_PEDESTRIANS) : SyncTarget.NONE;
            return changed ? (side.isServer() ? SyncTarget.SERVER : SyncTarget.SPECTATORS_PEDESTRIANS) : SyncTarget.NONE;
        }

        @Override
        public void setValueTo(A entity, PhysicsEntityNetHandler<A> network, MessagePhysicsEntitySync msg, Side side) {
            WheelsModule m = ((IModuleContainer.IPropulsionContainer<WheelsModule>) entity).getPropulsion();
            if (network.getSimulationHolder().isSinglePlayer()) { //solo mode
                //System.out.println("RCV SET VISUALS SP "+entity+" "+side);
                //now done in PhysicsEntity to fix render glitch if there is no sync but differences between prev and current values System.arraycopy(entity.visualProperties, 0, entity.prevVisualProperties, 0, entity.prevVisualProperties.length);
                System.arraycopy(visualProperties, 0, m.visualProperties, 0, visualProperties.length); //Copy because the visualProperties is reused by the server
            } else if (!network.getSimulationHolder().ownsPhysics(side)) {//If we are not the simulator
                //System.out.println("RCV SET VISUALS PHYSICS "+entity+" "+side);
                for (int i = 0; i < visualProperties.length; i++) {
                    m.prevVisualProperties[i] = m.visualProperties[i];
                    m.visualProperties[i] = visualProperties[i];
                }
                //Sync bullet for prediction :
                setWheelProperties(entity);
            }
        }

        private void setWheelProperties(A entity) {
            if (entity.physicsHandler instanceof BaseWheeledVehiclePhysicsHandler<?>) //FIXMEOLD IZNOGOOD
            {
                for (int i = 0; i < ((BaseWheeledVehiclePhysicsHandler<?>) entity.physicsHandler).getPhysicsVehicle().getNumWheels(); i++) {
                    VehicleWheel info = ((BaseWheeledVehiclePhysicsHandler<?>) entity.physicsHandler).getPhysicsVehicle().getWheel(i);
                    if (info != null) {
                        info.setSuspensionLength(this.visualProperties[VehicleEntityProperties.getPropertyIndex(i, VehicleEntityProperties.EnumVisualProperties.SUSPENSIONLENGTH)] - info.getRestLength());
                        info.setRotationAngle(this.visualProperties[VehicleEntityProperties.getPropertyIndex(i, VehicleEntityProperties.EnumVisualProperties.ROTATIONANGLE)] / DynamXGeometry.radToDeg);
                    }
                }
            }
        }

        @Override
        public void interpolate(A entity, PhysicsEntityNetHandler<A> network, MessagePhysicsEntitySync msg, int step) {
            //System.out.println("RCV INTERPOL VISUALS "+entity);
            if (!DynamXMain.proxy.ownsSimulation(entity)) {
                WheelsModule m = ((IModuleContainer.IPropulsionContainer<WheelsModule>) entity).getPropulsion();
                float[] angles;
                for (int i = 0; i < visualProperties.length; i++) {
                    byte interpolType = VehicleEntityProperties.getPropertyByIndex(i).type;
                    if (interpolType == 1) {//Angular interpolation
                        //Steering and wheels rotation angles
                        angles = DynamXMath.interpolateAngle(visualProperties[i], m.visualProperties[i], step);
                        m.prevVisualProperties[i] = angles[0]; //Update prevRotation, so we have -180<prevRotationYaw-rotationYaw<180 to avoid visual glitch
                        m.visualProperties[i] = angles[1];
                    } else {//Linear interpolation
                        //Wheels contact points, suspension length
                        m.prevVisualProperties[i] = m.visualProperties[i];
                        m.visualProperties[i] = (float) (m.visualProperties[i] + DynamXMath.interpolateDoubleDelta(visualProperties[i], m.visualProperties[i], step));
                    }
                }
                //Sync bullet for prediction :
                if (step == 1 && DynamXMain.proxy.shouldUseBulletSimulation(entity.world) && visualProperties.length > 0) {
                    setWheelProperties(entity);
                }
            }
        }

        @Override
        public void write(ByteBuf buf, boolean compress) {
            buf.writeInt(visualProperties.length);
            for (float f : visualProperties) {
                buf.writeFloat(f);
            }
        }

        @Override
        public void writeEntityValues(A entity, ByteBuf buf) {
            WheelsModule m = ((IModuleContainer.IPropulsionContainer<WheelsModule>) entity).getPropulsion();
            buf.writeInt(m.visualProperties.length);
            for (float f : m.visualProperties) {
                buf.writeFloat(f);
            }
        }

        @Override
        public void read(ByteBuf buf) {
            visualProperties = new float[buf.readInt()];
            for (int i = 0; i < visualProperties.length; i++) {
                visualProperties[i] = buf.readFloat();
            }
        }
    }

    /**
     * All player inputs for a {@link BaseVehicleEntity} that have an {@link fr.dynamx.common.entities.modules.EngineModule}
     */
    public static class Controls<A extends BaseVehicleEntity<?>> implements SynchronizedVariable<A> {
        public static final ResourceLocation NAME = new ResourceLocation(DynamXConstants.ID, "controls");

        private int controls;
        private float speedLimit;

        @Override
        public SyncTarget getValueFrom(A entity, PhysicsEntityNetHandler<A> network, Side side, int syncTick) {
            boolean changed = syncTick % 30 == 0; //Sync all each 1.5 second, even if it has not changed
            EngineModule engine = (EngineModule) ((IModuleContainer.IEngineContainer) entity).getEngine();
            //Detect changes and update values
            if (controls != engine.getControls()) {
                controls = engine.getControls();
                changed = true;
            }
            if (speedLimit != engine.getSpeedLimit()) {
                speedLimit = engine.getSpeedLimit();
                changed = true;
            }
            if (changed)
                SyncTracker.addChange("controls", "main");
            return changed ? SyncTarget.spectatorForSide(side) : SyncTarget.NONE;
        }

        @Override
        public void setValueTo(A entity, PhysicsEntityNetHandler<A> network, MessagePhysicsEntitySync msg, Side side) {
            EngineModule engine = (EngineModule) ((IModuleContainer.IEngineContainer) entity).getEngine();
            engine.setControls(controls);
            engine.setSpeedLimit(speedLimit);
        }

        @Override
        public void write(ByteBuf buf, boolean compress) {
            buf.writeInt(controls);
            buf.writeFloat(speedLimit);
        }

        @Override
        public void writeEntityValues(A entity, ByteBuf buf) {
            EngineModule engine = (EngineModule) ((IModuleContainer.IEngineContainer) entity).getEngine();
            buf.writeInt(engine.getControls());
            buf.writeFloat(engine.getSpeedLimit());
        }

        @Override
        public void read(ByteBuf buf) {
            controls = buf.readInt();
            speedLimit = buf.readFloat();
        }
    }

    public static class DoorsStatus<A extends PackPhysicsEntity<?, ?>> implements SynchronizedVariable<A> {
        public static final ResourceLocation NAME = new ResourceLocation(DynamXConstants.ID, "doorsstatus");
        //private Map<Byte, Integer> attachedDoors = new HashMap<>();
        private Map<Byte, DoorsModule.DoorState> doorsState = new HashMap<>();

        @Override
        public SyncTarget getValueFrom(A entity, PhysicsEntityNetHandler<A> network, Side side, int syncTick) {
            boolean changed = false;
            if (entity instanceof BaseVehicleEntity<?>) {
                if (entity instanceof IModuleContainer.IDoorContainer) {
                    //Detect changes and update values
                    for (Map.Entry<Byte, DoorsModule.DoorState> entry : ((IModuleContainer.IDoorContainer) entity).getDoors().getDoorsState().entrySet()) {
                        Byte i = entry.getKey();
                        DoorsModule.DoorState s = entry.getValue();
                        if (doorsState.get(i) != s) {
                            doorsState.put(i, s);
                            changed = true;
                        }
                    }
                    /*for (Map.Entry<Byte, DoorEntity<?>> entry : ((BaseVehicleEntity<?>) entity).getAttachedDoors().entrySet()) {
                        Byte id = entry.getKey();
                        int entityID = entry.getValue().getEntityId();
                        if (!attachedDoors.containsKey(id) || attachedDoors.get(id) != entityID) {
                            attachedDoors.put(id, entityID);
                            changed = true;
                        }
                    }*/
                    return changed ? SyncTarget.ALL_CLIENTS : SyncTarget.NONE;
                }
            }
            return SyncTarget.NONE;
        }

        @Override
        public void setValueTo(A vehicleEntity, PhysicsEntityNetHandler<A> network, MessagePhysicsEntitySync msg, Side side) {
            Map<Byte, DoorsModule.DoorState> target = ((IModuleContainer.IDoorContainer) vehicleEntity).getDoors().getDoorsState();
            if (true || network.getSimulationHolder() != SimulationHolder.SERVER_SP) {
                doorsState.forEach((i, b) -> {
                    if (b == DoorsModule.DoorState.OPEN && (!target.containsKey(i) || target.get(i) == DoorsModule.DoorState.CLOSE)) {
                        ((IModuleContainer.IDoorContainer) vehicleEntity).getDoors().setDoorState(i, DoorsModule.DoorState.OPEN);
                    } else if (b == DoorsModule.DoorState.CLOSE && (target.containsKey(i) && target.get(i) == DoorsModule.DoorState.OPEN)) {
                        ((IModuleContainer.IDoorContainer) vehicleEntity).getDoors().setDoorState(i, DoorsModule.DoorState.CLOSE);
                    }
                });
            } else {
                doorsState.forEach((i, b) -> {
                    if (b != target.get(i)) {
                        target.put(i, b);
                        DoorsModule doors = ((IModuleContainer.IDoorContainer) vehicleEntity).getDoors();
                        doors.playDoorSound(b == DoorsModule.DoorState.OPEN ? DoorsModule.DoorState.CLOSE : DoorsModule.DoorState.OPEN);
                    }
                });
            }
            //Map<Byte, DoorEntity<?>> target2 = ((BaseVehicleEntity<?>) vehicleEntity).getAttachedDoors();
            //attachedDoors.forEach((id, entityID) -> target2.put(id, (DoorEntity<?>) vehicleEntity.world.getEntityByID(entityID)));
        }

        @Override
        public void write(ByteBuf buf, boolean compress) {
            buf.writeByte(doorsState.size());
            doorsState.forEach((i, s) -> {
                buf.writeByte(i);
                buf.writeInt(s.ordinal());
            });

            /*buf.writeByte(attachedDoors.size());
            attachedDoors.forEach((i, entityID) -> {
                buf.writeByte(i);
                buf.writeInt(entityID);
            });*/
        }

        @Override
        public void writeEntityValues(A entity, ByteBuf buf) {
            Map<Byte, DoorsModule.DoorState> target = ((IModuleContainer.IDoorContainer) entity).getDoors().getDoorsState();
            buf.writeByte(target.size());
            target.forEach((i, s) -> {
                buf.writeByte(i);
                buf.writeInt(s.ordinal());
            });
        }

        @Override
        public void read(ByteBuf buf) {
            int size = buf.readByte();
            for (int i = 0; i < size; i++) {
                doorsState.put(buf.readByte(), DoorsModule.DoorState.values()[buf.readInt()]);
            }

            /*size = buf.readByte();
            for (int i = 0; i < size; i++) {
                attachedDoors.put(buf.readByte(), buf.readInt());
            }*/
        }
    }
}
