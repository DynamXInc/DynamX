package fr.dynamx.common.entities.modules;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.entities.modules.IEngineModule;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.IPropulsionModule;
import fr.dynamx.api.physics.entities.IPropulsionHandler;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.common.contentpack.type.vehicle.BoatPropellerInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.vehicles.BoatEntity;
import fr.dynamx.common.entities.vehicles.BoatPhysicsHandler;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Objects;

public class BoatPropellerModule implements IPropulsionModule<BoatPhysicsHandler<?>>, IPackInfoReloadListener {
    private BoatPropellerInfo info;
    private final BoatEntity<?> boat;

    public BoatPropellerModule(BoatEntity<?> boatEntity) {
        this.boat = boatEntity;
        onPackInfosReloaded();
    }

    @Override
    public void onPackInfosReloaded() {
        this.info = Objects.requireNonNull(boat.getPackInfo().getSubPropertyByType(BoatPropellerInfo.class));
    }

    @Override
    public IPropulsionHandler getPhysicsHandler() {
        return new BoatPropellerHandler();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void spawnPropulsionParticles(RenderPhysicsEntity<?> render, float partialTicks) {
        //todo particules autour de l'hélice ?
    }

    public class BoatPropellerHandler implements IPropulsionHandler {
        @Override
        public void accelerate(IEngineModule module, float strength, float speedLimit) {
            Vector3f look = DynamXGeometry.FORWARD_DIRECTION;
            look = DynamXGeometry.rotateVectorByQuaternion(look, boat.physicsRotation);
            look.multLocal(getAccelerationForce() * strength);
            Vector3f rotatedPos = DynamXGeometry.rotateVectorByQuaternion(info.getPosition(), boat.physicsRotation);
            boat.physicsHandler.getCollisionObject().applyForce(look, rotatedPos);
        }

        @Override
        public void disengageEngine() {}

        @Override
        public void brake(float strength) {
            Vector3f look = DynamXGeometry.FORWARD_DIRECTION;
            look = DynamXGeometry.rotateVectorByQuaternion(look, boat.physicsRotation);
            look.multLocal(-getBrakeForce() * strength);
            boat.physicsHandler.getCollisionObject().applyForce(look, Vector3fPool.get());
        }

        @Override
        public void handbrake(float strength) {}

        @Override
        public void steer(float strength) {
            Vector3f look = Vector3fPool.get(-1, 0, 0);
            look = DynamXGeometry.rotateVectorByQuaternion(look, boat.physicsRotation);
            look.multLocal(getSteerForce() * strength * boat.physicsHandler.getLinearVelocity().length() / 3);
            Vector3f linearFactor = boat.physicsHandler.getCollisionObject().getLinearFactor(Vector3fPool.get());
            Vector3f rotatedPos = DynamXGeometry.rotateVectorByQuaternion(info.getPosition(), boat.physicsRotation);
            boat.physicsHandler.getCollisionObject().applyTorque(rotatedPos.cross(look.multLocal(linearFactor)));
        }

        @Override
        public void applyEngineBraking(IEngineModule engine) {}

        public float getAccelerationForce(){
            return info.getAccelerationForce();
        }
        public float getBrakeForce(){
            return info.getBrakeForce();
        }
        public float getSteerForce(){
            return info.getSteerForce();
        }
    }
}
