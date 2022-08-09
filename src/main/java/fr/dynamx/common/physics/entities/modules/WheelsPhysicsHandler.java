package fr.dynamx.common.physics.entities.modules;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.jme3.bullet.objects.VehicleWheel;
import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.modules.IEngineModule;
import fr.dynamx.api.physics.entities.IEnginePhysicsHandler;
import fr.dynamx.api.physics.entities.IPropulsionHandler;
import fr.dynamx.common.contentpack.parts.PartWheel;
import fr.dynamx.common.contentpack.type.vehicle.PartWheelInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.WheelsModule;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.BaseWheeledVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.parts.wheel.PacejkaMagicFormula;
import fr.dynamx.common.physics.entities.parts.wheel.WheelPhysicsHandler;
import fr.dynamx.common.physics.entities.parts.wheel.WheelState;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @see IPropulsionHandler
 * @see WheelsModule
 */
public class WheelsPhysicsHandler implements IPropulsionHandler {
    private final WheelsModule module;
    private final BaseWheeledVehiclePhysicsHandler<? extends BaseVehicleEntity<?>> handler;

    public BiMap<Byte, Byte> wheelIDByPartID = HashBiMap.create();
    public List<WheelPhysicsHandler> vehicleWheelPhysicsHandlers = new ArrayList<>();

    public PacejkaMagicFormula pacejkaMagicFormula;

    private static final Vector3f direction = new Vector3f(0, -1, 0);
    private static final Vector3f axle = new Vector3f(-1, 0, 0);

    @Getter
    private boolean isAccelerating;

    public WheelsPhysicsHandler(WheelsModule module, BaseWheeledVehiclePhysicsHandler<? extends BaseVehicleEntity<?>> handler) {
        this.module = module;
        this.handler = handler;
    }

    public BaseWheeledVehiclePhysicsHandler<? extends BaseVehicleEntity<?>> getHandler() {
        return handler;
    }

    public byte getNumWheels() {
        return (byte) vehicleWheelPhysicsHandlers.size();
    }

    public WheelPhysicsHandler getWheel(int index) {
        return vehicleWheelPhysicsHandlers.get(index);
    }

    public WheelPhysicsHandler getWheelByPartIndex(byte index) {
        return vehicleWheelPhysicsHandlers.get(wheelIDByPartID.get(index));
    }

    public void init() {
        for (PartWheel partWheel : handler.getPackInfo().getPartsByType(PartWheel.class)) {
            partWheel.addPart(handler.getHandledEntity());
        }
        pacejkaMagicFormula = new PacejkaMagicFormula(this);
    }

    public void addWheel(PartWheel partWheel, PartWheelInfo wheelInfo) {
        Vector3f wheelPosition = Vector3fPool.get(partWheel.getPosition()).addLocal(handler.getPackInfo().getCenterOfMass()).addLocal(0, -wheelInfo.getSuspensionRestLength(), 0);
        VehicleWheel vehicleWheel = handler.getPhysicsVehicle().addWheel(wheelPosition, direction, axle, wheelInfo.getSuspensionRestLength(), wheelInfo.getWheelRadius(), partWheel.isWheelIsSteerable());
        byte index = (byte) (handler.getPhysicsVehicle().getNumWheels() - 1);
        WheelPhysicsHandler wheelPhysicsHandler = new WheelPhysicsHandler(handler.getPhysicsVehicle(), vehicleWheel, index, partWheel);
        for (Map.Entry<Byte, Byte> entry : wheelIDByPartID.entrySet()) {
            byte newWheelID = entry.getValue();
            if (newWheelID >= index)
                entry.setValue(++newWheelID);
        }
        vehicleWheelPhysicsHandlers.add(wheelPhysicsHandler);
        wheelIDByPartID.put(partWheel.getId(), index);
    }

    public void update() {
        pacejkaMagicFormula.update();
    }

    public void removeWheel(byte partID) {
        if (getNumWheels() > 0) {
            if (wheelIDByPartID.get(partID) != null) {
                byte wheelID = wheelIDByPartID.get(partID);
                for (WheelPhysicsHandler wheelPhysicsHandler : vehicleWheelPhysicsHandlers) {
                    if (wheelPhysicsHandler.getWheelIndex() > wheelID)
                        wheelPhysicsHandler.setWheelIndex((byte) (wheelPhysicsHandler.getWheelIndex() - 1));
                }
                for (PartWheel partWheel : handler.getPackInfo().getPartsByType(PartWheel.class)) {
                    if (partID == partWheel.getId()) {
                        module.getWheelsStates()[partID] = WheelState.REMOVED; //removed state
                        handler.getPhysicsVehicle().removeWheel(wheelID);
                        wheelIDByPartID.remove(partID, wheelID);
                        vehicleWheelPhysicsHandlers.remove(wheelID);
                    }
                }
                for (Map.Entry<Byte, Byte> entry : wheelIDByPartID.entrySet()) {
                    byte newWheelID = entry.getValue();
                    if (newWheelID > wheelID)
                        entry.setValue(--newWheelID);
                }
            }
        }
    }

    @Override
    public void accelerate(IEngineModule engine, float strength, float speedLimit) {
        IEnginePhysicsHandler module = engine.getPhysicsHandler();
        if (module.getEngine().isStarted()) {
            for (WheelPhysicsHandler wheelPhysicsHandler : vehicleWheelPhysicsHandlers) {
                if (wheelPhysicsHandler.isDrivingWheel()) { //si la roue est motrice
                    if (strength != 0 && module.isEngaged() && Math.abs(handler.getSpeed(BaseVehiclePhysicsHandler.SpeedUnit.KMH)) < speedLimit) //pas au point mort
                    {
                        float power = (module.getEngine().getPowerOutputAtRevs());

                        // so the faster we go, the less force the vehicle can apply.
                        // this simulates making it harder to accelerate at higher speeds
                        // realistically this makes it difficult to achieve the max speed.
                        float speedRatio = 1.0f;// - (getSpeed(SpeedUnit.KMH) / (strength != 0 ? getGearBox().getMaxSpeed(SpeedUnit.KMH) : (getGearBox().getActiveGear().getStart() + getGearBox().getActiveGear().getEnd() / 4)));
                        //permet d'avancer à vitesse constante si on touche aucune "pédale"
                        //System.out.println(power+" "+getEngine().getRevs());
                        speedRatio = MathHelper.clamp(speedRatio, 0, 1);

                        // how much this wheel is "skidding".
                        //float skid = 1-wheel.getVehicleWheel().skidInfo;

                        // System.out.println("Delta rot "+wheel.getRotationDelta()+" "+getSpeed(SpeedUnit.KMH));

                        wheelPhysicsHandler.accelerate(power * speedRatio * strength * 2);//(getGearBox().getActiveGearNum() < 0 ? -1 : 1));
                        isAccelerating = true;
                    } else
                        wheelPhysicsHandler.accelerate(0);
                } else {
                    // we always set this because the wheel could be "broken down" over time.
                    wheelPhysicsHandler.accelerate(0);
                }
            }
        }
    }

    @Override
    public void disengageEngine() {
        for (WheelPhysicsHandler wheelPhysicsHandler : vehicleWheelPhysicsHandlers) {
            wheelPhysicsHandler.accelerate(0);
        }
    }

    @Override
    public void brake(float strength) {
        for (WheelPhysicsHandler wheelPhysicsHandler : vehicleWheelPhysicsHandlers) {
            wheelPhysicsHandler.brake(strength, 0);
        }
    }

    @Override
    public void handbrake(float strength) {
        // just apply the brakes to the rear wheels.
        for (WheelPhysicsHandler wheel : vehicleWheelPhysicsHandlers) {
            if (wheel.isHandBrakingWheel())
                wheel.brake(0, strength);
        }
    }

    @Override
    public void steer(float strength) {
        for (WheelPhysicsHandler wheelPhysicsHandler : vehicleWheelPhysicsHandlers) {
            wheelPhysicsHandler.steer(strength);
        }
    }

    @Override
    public void applyEngineBraking(IEngineModule engine) {
        disengageEngine();
        for (int i = 0; i < getNumWheels(); i++) {
            WheelPhysicsHandler wheelPhysicsHandler = getWheel(i);
            // if the wheel is not "connected" to the engine, don't slow the wheel down using engine braking.
            // so if the wheel has 1 acceleration force, apply full engine braking.
            // but if the wheel has 0 acceleration force, it's not "connected" to the engine.

            if (wheelPhysicsHandler.isDrivingWheel())
                wheelPhysicsHandler.brake(0, 0, engine.getPhysicsHandler().getEngine().getBraking());
        }
    }

    public List<WheelPhysicsHandler> getWheels() {
        return vehicleWheelPhysicsHandlers;
    }
}
