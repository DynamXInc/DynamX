package fr.dynamx.common.entities.vehicles;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.IEngineModule;
import fr.dynamx.api.entities.modules.IPropulsionModule;
import fr.dynamx.api.entities.modules.ISeatsModule;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.api.physics.entities.IPropulsionHandler;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.parts.PartFloat;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.BoatEngineModule;
import fr.dynamx.common.entities.modules.EngineModule;
import fr.dynamx.common.entities.modules.SeatsModule;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class BoatEntity<T extends BoatEntity.BoatPhysicsHandler<?>> extends BaseVehicleEntity<T> implements IModuleContainer.IEngineContainer, IModuleContainer.IPropulsionContainer, IModuleContainer.ISeatsContainer {
    private IEngineModule engine;
    private ISeatsModule seats;
    private IPropulsionModule propulsion;

    public BoatEntity(World world) {
        super(world);
    }

    public BoatEntity(String name, World world, Vector3f pos, float spawnRotationAngle, int metadata) {
        super(name, world, pos, spawnRotationAngle, metadata);
    }

    @Override
    public T createPhysicsHandler() {
        return (T) new BoatPhysicsHandler(this);
    }

    @Override
    public void createModules(ModuleListBuilder modules) {
        //Take care to add seats BEFORE engine (the engine needs to detect dismounts)
        modules.add(seats = new SeatsModule(this));
        //Take care to add propulsion BEFORE engine (the engine needs a propulsion)
        modules.add(propulsion = new BoatEngineModule(this));

        super.createModules(modules);
        engine = modules.getByClass(EngineModule.class);
    }

    @Nonnull
    @Override
    public IEngineModule getEngine() {
        return engine;
    }

    @Nonnull
    @Override
    public IPropulsionModule getPropulsion() {
        return propulsion;
    }

    @Nonnull
    @Override
    public ISeatsModule getSeats() {
        if (seats == null) //We may need seats before modules are created, because of seats sync
            seats = new SeatsModule(this);
        return seats;
    }

    @Override
    public BaseVehicleEntity<?> cast() {
        return this;
    }

    @Override
    protected ModularVehicleInfo createInfo(String infoName) {
        return DynamXObjectLoaders.BOATS.findInfo(infoName);
    }

    public static Vector3f[] ntm = new Vector3f[10];
    public static Vector3f[] ntmdrag = new Vector3f[10];

    public static class BoatPhysicsHandler<A extends BoatEntity<?>> extends BaseVehiclePhysicsHandler<A> {

        public List<PartFloat> floatList = new ArrayList<>();
        public List<Vector3f> buoyForces = new ArrayList<>();


        public BoatPhysicsHandler(A entity) {
            super(entity);
            float offsetX = -1;
            float offsetZ = 0;
            floatList = packInfo.getPartsByType(PartFloat.class);
            for (PartFloat f : floatList) {
                f.size = 1f;
                f.listFloaters.clear();
                AxisAlignedBB bb = f.box;
                float sizeX = (float) (bb.maxX - bb.minX) * 2;
                float sizeY = (float) (bb.maxY - bb.minY);
                float sizeZ = (float) (bb.maxZ - bb.minZ);
                for (int j = 0; j < sizeX / f.size; j++) {
                    for (int k = 0; k < sizeZ / f.size; k++) {
                        Vector3f pos = new Vector3f((float) (bb.minX + j * f.size + offsetX), (float) bb.minY, (float) (bb.minZ + k * f.size + offsetZ));
                        Vector3f center = pos.add(f.size / 2, 0, f.size / 2);
                        f.listFloaters.add(center);
                        buoyForces.add(new Vector3f());
                    }
                }
                break;
            }
        }

        @Override
        public IPropulsionHandler getPropulsion() {
            return getHandledEntity().getPropulsion().getPhysicsHandler(); //BOAT ENGINE
        }

        private void floatPart(float size, Vector3f partPos, float waterLevel, int i) {
            Vector3f localPosRotated = DynamXGeometry.rotateVectorByQuaternion(partPos, getCollisionObject().getPhysicsRotation(QuaternionPool.get()));
            Vector3f inWorldPos = getCollisionObject().getPhysicsLocation(Vector3fPool.get()).add(localPosRotated);

            double dy = waterLevel - inWorldPos.y;
            Vector3f forcer = new Vector3f();

            Vector3f drag = DynamXPhysicsHelper.getVelocityAtPoint(getLinearVelocity(), getAngularVelocity(), partPos);
            if (dy > 0) {
                dy = Math.min(dy, size * 2);
                double vol = dy * size * size * 1000;
                Vector3f fr = getCollisionObject().getGravity(Vector3fPool.get()).multLocal((float) (-vol));

                getCollisionObject().applyForce(fr.multLocal(0.05f), partPos);
                forcer.set(fr);
                buoyForces.get(i).set(fr.mult(0.01f));
            }
            drag.multLocal(1f);
            drag.multLocal(drag.mult(-1));
        }

        @Override
        public void update() {
            Vector3f dragForce = null;//SHOULD NOT BE COMMENTED  DynamXPhysicsHelper.getWaterDrag(getLinearVelocity(), getPackInfo().getDragFactor());

            collisionObject.setContactResponse(false);
            getCollisionObject().setAngularDamping(0.7f);
            getCollisionObject().setLinearDamping(0.7f);
            getCollisionObject().setEnableSleep(false);

            boolean isInLiquid = false;
            int liquidOffset = 0;
            float floatReference;

            for (int offset = -1; offset <= 2; offset++) {
                BlockPos blockPos = new BlockPos(physicsPosition.x, physicsPosition.y + offset, physicsPosition.z);

                if (handledEntity.getEntityWorld().getBlockState(blockPos).getMaterial().isLiquid()) {
                    liquidOffset = offset;
                    isInLiquid = true;
                }
            }

            if (isInLiquid) {
                BlockPos blockPos = new BlockPos(physicsPosition.x, physicsPosition.y + liquidOffset, physicsPosition.z);
                floatReference = (float) handledEntity.getEntityWorld().getBlockState(blockPos).getSelectedBoundingBox(handledEntity.getEntityWorld(), blockPos).maxY - 0.125F;
                int i = 0;
                for (PartFloat f : floatList) {
                    for (Vector3f floater : f.listFloaters) {
                        floatPart(f.size, floater, floatReference, i++);
                    }
                }
            }
            super.update();
        }
    }
}
