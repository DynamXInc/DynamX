package fr.dynamx.common.entities.modules;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.modules.IEngineModule;
import fr.dynamx.api.entities.modules.IPropulsionModule;
import fr.dynamx.api.physics.entities.IPropulsionHandler;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.common.contentpack.type.vehicle.BoatEngineInfo;
import fr.dynamx.common.entities.vehicles.BoatEntity;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BoatEngineModule implements IPropulsionModule<BoatEntity.BoatPhysicsHandler<?>> {
    private final BoatEngineInfo info;
    private final BoatEntity<?> boat;

    public <T extends BoatEntity.BoatPhysicsHandler<?>> BoatEngineModule(BoatEntity<?> boatEntity) {
        this.boat = boatEntity;
        this.info = boatEntity.getPackInfo().getSubPropertyByType(BoatEngineInfo.class);
    }

    @Override
    public IPropulsionHandler getPhysicsHandler() {
        return new BoatEngineHandler();
    }

    @Override
    public float[] getPropulsionProperties() {
        return new float[0];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void spawnPropulsionParticles(RenderPhysicsEntity<?> render, float partialTicks) {

    }

    public class BoatEngineHandler implements IPropulsionHandler {
        @Override
        public void accelerate(IEngineModule module, float strength, float speedLimit) {
            Vector3f look = new Vector3f(0, 0, 1);
            look = DynamXGeometry.rotateVectorByQuaternion(look, boat.physicsRotation);
            look.multLocal(1000 * strength);
            boat.physicsHandler.getCollisionObject().applyForce(look, DynamXGeometry.rotateVectorByQuaternion(new Vector3f(0,-0.5f,-3), boat.physicsRotation));
        }

        @Override
        public void disengageEngine() {

        }

        @Override
        public void brake(float strength) {
            Vector3f look = new Vector3f(0, 0, 1);
            look = DynamXGeometry.rotateVectorByQuaternion(look, boat.physicsRotation);
            look.multLocal(-1000 * strength);
            boat.physicsHandler.getCollisionObject().applyForce(look, new Vector3f());
        }

        @Override
        public void handbrake(float strength) {
            Vector3f look = boat.physicsHandler.getLinearVelocity();
            look.multLocal(-0.8f);
        }

        @Override
        public void steer(float strength) {
            Vector3f look = new Vector3f(-1, 0, 0);
            look = DynamXGeometry.rotateVectorByQuaternion(look, boat.physicsRotation);
            look.multLocal(50 * strength * boat.physicsHandler.getLinearVelocity().length() / 3);
            Vector3f linearFactor = boat.physicsHandler.getCollisionObject().getLinearFactor(Vector3fPool.get());
            boat.physicsHandler.getCollisionObject().applyTorque(DynamXGeometry.rotateVectorByQuaternion(info.getPosition(), boat.physicsRotation).cross(look.multLocal(linearFactor)));
        }

        @Override
        public void applyEngineBraking(IEngineModule engine) {

        }
    }
}
