package fr.dynamx.common.entities.modules;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.common.contentpack.type.vehicle.BoatEngineInfo;
import fr.dynamx.common.entities.vehicles.BoatEntity;
import fr.dynamx.utils.maths.DynamXGeometry;

public class BoatEngineModule implements IPhysicsModule<BoatEntity.BoatPhysicsHandler<?>> {
    private final BoatEngineInfo info;
    private final BoatEntity<?> boat;

    public <T extends BoatEntity.BoatPhysicsHandler<?>> BoatEngineModule(BoatEntity<?> boatEntity) {
        this.boat = boatEntity;
        this.info = boatEntity.getPackInfo().getSubPropertyByType(BoatEngineInfo.class);
    }

    public BoatEngineHandler getPhysicsHandler() {
        return new BoatEngineHandler();
    }

    public class BoatEngineHandler {
        //TODO CHANGE ENGINE
        public void accelerate(CarEngineModule module, float strength, float speedLimit) {
            Vector3f look = new Vector3f(0, 0, 1);
            look = DynamXGeometry.rotateVectorByQuaternion(look, boat.physicsRotation);
            look.multLocal(100 * strength);
            //boat.physicsHandler.forces.add(new Force(look, new Vector3f()));
            if (strength != 0)
                System.out.println("Accel " + strength + " " + look);
        }

        public void brake(float strength) {
            Vector3f look = new Vector3f(0, 0, 1);
            look = DynamXGeometry.rotateVectorByQuaternion(look, boat.physicsRotation);
            look.multLocal(-100 * strength);
            //boat.physicsHandler.forces.add(new Force(look, new Vector3f()));
            if (strength != 0)
                System.out.println("Brake " + strength + " " + look);
        }

        public void handbrake(float strength) {
            Vector3f look = boat.physicsHandler.getLinearVelocity();
            look.multLocal(-0.8f);
            // boat.physicEntity.forces.add(new Force(look, new Vector3f()));
        }

        public void steer(float strength) {
            Vector3f look = new Vector3f(1, 0, 0);
            look = DynamXGeometry.rotateVectorByQuaternion(look, boat.physicsRotation);
            look.multLocal(10 * strength);
            //boat.physicsHandler.forces.add(new Force(look, Trigonometry.rotateVectorByQuaternion(info.getPosition(), boat.physicsRotation)));
            if (strength != 0)
                System.out.println("Turn " + strength + " " + look);
        }
    }
}
