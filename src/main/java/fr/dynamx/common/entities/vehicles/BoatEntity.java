package fr.dynamx.common.entities.vehicles;

import com.jme3.math.FastMath;
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
            float offsetX = 0f;
            float offsetZ = 0;
            float spacingX = 0f;
            floatList = packInfo.getPartsByType(PartFloat.class);
            for (PartFloat f : floatList) {
                f.size = 1f;
                f.listFloaters.clear();
                AxisAlignedBB bb = f.box;
                float sizeX = (float) (bb.maxX - bb.minX);
                float sizeY = (float) (bb.maxY - bb.minY);
                float sizeZ = (float) (bb.maxZ - bb.minZ);
                Vector3f pos;

                if (f.axis == 0) {
                    for (int i = 0; i < f.lineX; i++) {
                        pos = new Vector3f().set(f.getPosition());
                        float xPos = (float) (bb.minX + i * (f.size + spacingX) + offsetX);
                        pos.addLocal(xPos + f.size / 2, 0, 0);
                        f.listFloaters.add(pos);
                        buoyForces.add(new Vector3f());
                    }
                }else if(f.axis == 2){
                    for (int j = 0; j < f.lineZ; j++) {
                        pos = new Vector3f().set(f.getPosition());
                        float zPos = (float) (bb.minZ + j * (f.size + spacingX) + offsetZ);
                        pos.addLocal(0, -0.01f, -zPos - f.size / 2);
                        f.listFloaters.add(pos);
                        buoyForces.add(new Vector3f());
                    }
                }else if(f.axis == 3){
                    for (int i = 0; i < f.lineX; i++) {
                        for (int j = 0; j < f.lineZ; j++) {
                            pos = new Vector3f().set(f.getPosition());
                            float xPos = (float) (bb.minX + i * (f.size + spacingX) + offsetX);
                            float zPos = (float) (bb.minZ + j * (f.size + spacingX) + offsetZ);
                            pos.addLocal(xPos + f.size / 2, -0.01f, zPos + f.size / 2);
                            f.listFloaters.add(pos);
                            buoyForces.add(new Vector3f());
                        }
                    }
                }else{
                    pos = f.getPosition();
                    f.listFloaters.add(pos);
                    buoyForces.add(new Vector3f());
                }

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

            float area = 4.0F * (FastMath.PI * FastMath.pow(size, 2.0f));

            if (dy > 0) {
                dy = Math.min(dy, area);
                double vol = dy * area * area * 997 * 9.81;
                Vector3f fr = Vector3fPool.get(0, vol, 0);

                collisionObject.applyForce(fr.multLocal(0.05f), partPos);
                //System.out.println(fr);
                forcer.set(fr);
                buoyForces.get(i).set(fr.mult(0.01f));
                //FIXME Experimental drag
                /*Vector3f velocityAtPoint = DynamXPhysicsHelper.getVelocityAtPoint(getLinearVelocity(), getAngularVelocity(), partPos);
                velocityAtPoint.multLocal(-1f);
                float velLength = velocityAtPoint.length();

                float drag = 0.5F * 997 * velLength * 0.005f * area;

                Vector3f dragVect = partPos.normalize().multLocal(drag);

                System.out.println(velLength);
                collisionObject.applyForce(dragVect.multLocal(0.05f), partPos);*/
            }
        }

        @Override
        public void update() {
            //collisionObject.setMass(0);
            Vector3f dragForce = null;//SHOULD NOT BE COMMENTED  DynamXPhysicsHelper.getWaterDrag(getLinearVelocity(), getPackInfo().getDragFactor());

            collisionObject.setContactResponse(false);
            getCollisionObject().setAngularDamping(0.95f);
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
