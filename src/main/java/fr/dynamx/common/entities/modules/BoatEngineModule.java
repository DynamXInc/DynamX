package fr.dynamx.common.entities.modules;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.modules.IEngineModule;
import fr.dynamx.api.entities.modules.IPropulsionModule;
import fr.dynamx.api.physics.entities.IPropulsionHandler;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.common.contentpack.type.vehicle.BoatEngineInfo;
import fr.dynamx.common.entities.vehicles.BoatEntity;
import fr.dynamx.utils.maths.DynamXGeometry;
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
            look.multLocal(100 * strength);
            //boat.physicsHandler.forces.add(new Force(look, new Vector3f()));
            if (strength != 0)
                System.out.println("Accel " + strength + " " + look);
        }

        @Override
        public void disengageEngine() {

        }

        @Override
        public void brake(float strength) {
            Vector3f look = new Vector3f(0, 0, 1);
            look = DynamXGeometry.rotateVectorByQuaternion(look, boat.physicsRotation);
            look.multLocal(-100 * strength);
            //boat.physicsHandler.forces.add(new Force(look, new Vector3f()));
            if (strength != 0)
                System.out.println("Brake " + strength + " " + look);
        }

        @Override
        public void handbrake(float strength) {
            Vector3f look = boat.physicsHandler.getLinearVelocity();
            look.multLocal(-0.8f);
            // boat.physicEntity.forces.add(new Force(look, new Vector3f()));
        }

        @Override
        public void steer(float strength) {
            Vector3f look = new Vector3f(1, 0, 0);
            look = DynamXGeometry.rotateVectorByQuaternion(look, boat.physicsRotation);
            look.multLocal(10 * strength);
            //boat.physicsHandler.forces.add(new Force(look, Trigonometry.rotateVectorByQuaternion(info.getPosition(), boat.physicsRotation)));
            if (strength != 0)
                System.out.println("Turn " + strength + " " + look);
        }

        @Override
        public void applyEngineBraking(IEngineModule engine) {

        }
    }
}
