package fr.dynamx.common.network.sync.v3;

import fr.dynamx.api.network.sync.v3.PosSynchronizedVariable;
import fr.dynamx.api.network.sync.v3.SynchronizedVariableSerializer;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.type.vehicle.PartWheelInfo;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.common.physics.entities.parts.wheel.WheelState;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.optimization.HashMapPool;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.Map;

public class DynamXSynchronizedVariables
{
    public static final ResourceLocation POS = new ResourceLocation(DynamXConstants.ID, "pos");

    public static final ResourceLocation CONTROLS = new ResourceLocation(DynamXConstants.ID, "controls");
    public static final ResourceLocation SPEED_LIMIT = new ResourceLocation(DynamXConstants.ID, "speed_limit");
    public static final ResourceLocation ENGINE_PROPERTIES = new ResourceLocation(DynamXConstants.ID, "engine_properties");

    public static final ResourceLocation WHEEL_INFOS = new ResourceLocation(DynamXConstants.ID, "wheel_infos");
    public static final ResourceLocation WHEEL_STATES = new ResourceLocation(DynamXConstants.ID, "wheel_states");
    public static final ResourceLocation WHEEL_PROPERTIES = new ResourceLocation(DynamXConstants.ID, "wheel_properties");
    public static final ResourceLocation WHEEL_VISUALS = new ResourceLocation(DynamXConstants.ID, "wheel_visuals");

    public static final ResourceLocation MOVABLE_MOVER = new ResourceLocation(DynamXConstants.ID, "MOVABLE_MOVER");
    public static final ResourceLocation MOVABLE_PICK_DISTANCE = new ResourceLocation(DynamXConstants.ID, "MOVABLE_PICK_DISTANCE");
    public static final ResourceLocation MOVABLE_PICK_POSITION = new ResourceLocation(DynamXConstants.ID, "MOVABLE_PICK_POSITION");
    public static final ResourceLocation MOVABLE_PICKER = new ResourceLocation(DynamXConstants.ID, "MOVABLE_PICKER");
    public static final ResourceLocation MOVABLE_PICKED_ENTITY = new ResourceLocation(DynamXConstants.ID, "MOVABLE_PICKED_ENTITY");
    public static final ResourceLocation MOVABLE_IS_PICKED = new ResourceLocation(DynamXConstants.ID, "MOVABLE_IS_PICKED");

    public static final ResourceLocation DOORS_STATES = new ResourceLocation(DynamXConstants.ID, "DOORS_STATES");

    public static final SynchronizedVariableSerializer<fr.dynamx.api.network.sync.v3.PosSynchronizedVariable.EntityPositionData> posSerializer = new SynchronizedVariableSerializer<fr.dynamx.api.network.sync.v3.PosSynchronizedVariable.EntityPositionData>() {
        @Override
        public void writeObject(ByteBuf buf, fr.dynamx.api.network.sync.v3.PosSynchronizedVariable.EntityPositionData object) {
            buf.writeBoolean(object.isBodyActive());
            DynamXUtils.writeVector3f(buf, object.getPosition());
            DynamXUtils.writeQuaternion(buf, object.getRotation());
            if (object.isBodyActive()) {
                DynamXUtils.writeVector3f(buf, object.getLinearVel());
                DynamXUtils.writeVector3f(buf, object.getRotationalVel());
            }
        }

        @Override
        public PosSynchronizedVariable.EntityPositionData readObject(ByteBuf buffer) {
            //TODO PAS COOL NEW
            PosSynchronizedVariable.EntityPositionData result = new PosSynchronizedVariable.EntityPositionData(buffer.readBoolean(), DynamXUtils.readVector3f(buffer), DynamXUtils.readQuaternion(buffer));
            if (result.isBodyActive()) {
                result.getLinearVel().set(DynamXUtils.readVector3f(buffer));
                result.getRotationalVel().set(DynamXUtils.readVector3f(buffer));
            }
            return result;
        }
    };

    public static final SynchronizedVariableSerializer<Map<Byte, PartWheelInfo>> wheelInfosSerializer = new SynchronizedVariableSerializer<Map<Byte, PartWheelInfo>>() {
        @Override
        public void writeObject(ByteBuf buf, Map<Byte, PartWheelInfo> object) {
            buf.writeInt(object.size());
            object.forEach((id, info) -> {
                buf.writeByte(id);
                ByteBufUtils.writeUTF8String(buf, info.getFullName());
            });
        }

        @Override
        public Map<Byte, PartWheelInfo> readObject(ByteBuf buf) {
            Map<Byte, PartWheelInfo> currentValue = HashMapPool.get();
            int size = buf.readInt();
            for (byte i = 0; i < size; i++)
                currentValue.put(buf.readByte(), DynamXObjectLoaders.WHEELS.findInfo(ByteBufUtils.readUTF8String(buf)));
            return currentValue;
        }
    };

    public static final SynchronizedVariableSerializer<WheelState[]> wheelStatesSerializer = new SynchronizedVariableSerializer<WheelState[]>() {
        @Override
        public void writeObject(ByteBuf buf, WheelState[] object) {
            buf.writeInt(object.length);
            for (WheelState f : object) {
                buf.writeByte(f.ordinal());
            }
        }

        @Override
        public WheelState[] readObject(ByteBuf buf) {
            WheelState[] currentValue = new WheelState[buf.readInt()];
            for (byte i = 0; i < currentValue.length; i++) {
                currentValue[i] = WheelState.values()[buf.readByte()];
            }
            return currentValue;
        }
    };

    public static final SynchronizedVariableSerializer<Map<Byte, DoorsModule.DoorState>> doorsStatesSerializer = new SynchronizedVariableSerializer<Map<Byte, DoorsModule.DoorState>>() {
        @Override
        public void writeObject(ByteBuf buf, Map<Byte, DoorsModule.DoorState> doorsState) {
            buf.writeByte(doorsState.size());
            doorsState.forEach((i, s) -> {
                buf.writeByte(i);
                buf.writeInt(s.ordinal());
            });
        }

        @Override
        public Map<Byte, DoorsModule.DoorState> readObject(ByteBuf buf) {
            Map<Byte, DoorsModule.DoorState> currentValue = HashMapPool.get();
            int size = buf.readByte();
            for (int i = 0; i < size; i++) {
                currentValue.put(buf.readByte(), DoorsModule.DoorState.values()[buf.readInt()]);
            }
            return currentValue;
        }
    };
}
