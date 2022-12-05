package fr.dynamx.common.entities.modules;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.modules.IEngineModule;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.IPropulsionModule;
import fr.dynamx.api.physics.entities.IPropulsionHandler;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.common.contentpack.type.vehicle.BoatEngineInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.vehicles.BoatEntity;
import fr.dynamx.common.entities.vehicles.BoatPhysicsHandler;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BoatPropellerModule implements IPropulsionModule<BoatPhysicsHandler<?>>, IPhysicsModule.IDrawableModule<BaseVehicleEntity<?>> {
    private final BoatEngineInfo info;
    private final BoatEntity<?> boat;

    public <T extends BoatPhysicsHandler<?>> BoatPropellerModule(BoatEntity<?> boatEntity) {
        this.boat = boatEntity;
        this.info = boatEntity.getPackInfo().getSubPropertyByType(BoatEngineInfo.class);
    }

    @Override
    public IPropulsionHandler getPhysicsHandler() {
        return new BoatPropellerHandler();
    }

    @Override
    public float[] getPropulsionProperties() {
        return new float[0];
    }


    @Override
    @SideOnly(Side.CLIENT)
    public void spawnPropulsionParticles(RenderPhysicsEntity<?> render, float partialTicks) {

    }

    @Override
    public void drawParts(RenderPhysicsEntity<?> render, float partialTicks, BaseVehicleEntity<?> entity) {

    }

    public class BoatPropellerHandler implements IPropulsionHandler {


        private float accelerationForce = 5000;
        private float brakeForce = -3000;
        private float steerForce = 300;
        @Getter
        @Setter
        private float accelerationFactor = 1;
        @Getter
        @Setter
        private float brakeFactor = 1;
        @Getter
        @Setter
        private float steerFactor = 1;


        @Override
        public void accelerate(IEngineModule module, float strength, float speedLimit) {
            Vector3f look = new Vector3f(0, 0, 1);
            look = DynamXGeometry.rotateVectorByQuaternion(look, boat.physicsRotation);
            look.multLocal(getAccelerationForce() * strength);
            Vector3f rotatedPos = DynamXGeometry.rotateVectorByQuaternion(info.getPosition(), boat.physicsRotation);
            boat.physicsHandler.getCollisionObject().applyForce(look, rotatedPos);
        }

        @Override
        public void disengageEngine() {

        }

        @Override
        public void brake(float strength) {
            Vector3f look = new Vector3f(0, 0, 1);
            look = DynamXGeometry.rotateVectorByQuaternion(look, boat.physicsRotation);
            look.multLocal(getBrakeForce() * strength);
            boat.physicsHandler.getCollisionObject().applyForce(look, new Vector3f());
        }

        @Override
        public void handbrake(float strength) {
        }

        @Override
        public void steer(float strength) {
            Vector3f look = new Vector3f(-1, 0, 0);
            look = DynamXGeometry.rotateVectorByQuaternion(look, boat.physicsRotation);
            look.multLocal(getSteerForce() * strength * boat.physicsHandler.getLinearVelocity().length() / 3);
            Vector3f linearFactor = boat.physicsHandler.getCollisionObject().getLinearFactor(Vector3fPool.get());
            Vector3f rotatedPos = DynamXGeometry.rotateVectorByQuaternion(info.getPosition(), boat.physicsRotation);
            boat.physicsHandler.getCollisionObject().applyTorque(rotatedPos.cross(look.multLocal(linearFactor)));
        }

        @Override
        public void applyEngineBraking(IEngineModule engine) {

        }

        public float getAccelerationForce(){
            return accelerationForce * accelerationFactor;
        }
        public float getBrakeForce(){
            return brakeForce * brakeFactor;
        }
        public float getSteerForce(){
            return steerForce * steerFactor;
        }
    }
}
