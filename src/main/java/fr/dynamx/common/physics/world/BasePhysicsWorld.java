package fr.dynamx.common.physics.world;

import com.jme3.bullet.PhysicsSoftSpace;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.math.Vector3f;
import fr.dynamx.api.events.PhysicsEvent;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.api.physics.entities.EntityPhysicsState;
import fr.dynamx.api.physics.terrain.ITerrainManager;
import fr.dynamx.client.network.ClientPhysicsSyncManager;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.physics.CollisionsHandler;
import fr.dynamx.common.physics.terrain.PhysicsWorldTerrain;
import fr.dynamx.common.physics.utils.PhysicsWorldOperation;
import fr.dynamx.server.network.ServerPhysicsSyncManager;
import fr.dynamx.utils.PhysicsEntityException;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.BoundingBoxPool;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.TransformPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for the two DynamX physics worlds
 */
public abstract class BasePhysicsWorld implements IPhysicsWorld {
    protected final PhysicsSoftSpace dynamicsWorld;
    protected final PhysicsWorldTerrain manager;
    protected final World mcWorld;

    protected final Set<PhysicsJoint> joints = new HashSet<>();
    protected final List<PhysicsEntity<?>> entities = new ArrayList<>();

    protected final ConcurrentLinkedQueue<Runnable> scheduledTasks = new ConcurrentLinkedQueue<>();
    protected final ConcurrentLinkedQueue<PhysicsWorldOperation<?>> operations = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean scheduledTasksLock = new AtomicBoolean();

    public BasePhysicsWorld(World world, boolean isRemoteWorld) {
        Vector3fPool.openPool(); //Open a pool for the whole session, the Vector3f created here may be used forever
        TransformPool.getPool().openSubPool();
        BoundingBoxPool.getPool().openSubPool();

        this.mcWorld = world;

        Vector3f min = new Vector3f(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
        Vector3f max = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);

        PhysicsSpace.BroadphaseType bPhase = PhysicsSpace.BroadphaseType.DBVT;
        IPhysicsWorld physicsWorld = this;
        dynamicsWorld = new PhysicsSoftSpace(min, max, bPhase) {
            @Override
            public void onContactStarted(long manifoldId) {
                // memory leak fix : don't call super method : bullets stores all collision events in a queue
            }

            @Override
            public void onContactProcessed(PhysicsCollisionObject pcoA, PhysicsCollisionObject pcoB, long contactPointId) {
                // memory leak fix : don't call super method : bullets stores all collision events in a queue
                CollisionsHandler.handleCollision(physicsWorld,new PhysicsCollisionEvent(pcoA, pcoB, contactPointId), (BulletShapeType<?>) pcoA.getUserObject(), (BulletShapeType<?>) pcoB.getUserObject());
            }

            @Override
            public void onContactEnded(long manifoldId) {
            }
        };
        manager = new PhysicsWorldTerrain(this, mcWorld, isRemoteWorld);
    }

    /**
     * Processes pending operations (adding/removing objects to the physics world)
     *
     * @param profiler The current profiler
     */
    protected void flushOperations(Profiler profiler) {
        profiler.start(Profiler.Profiles.ADD_REMOVE_BODIES);
        {
            try {
                while (!scheduledTasks.isEmpty()) {
                    scheduledTasks.remove().run();
                }
            } catch (Exception e) {
                scheduledTasksLock.set(true);
                DynamXMain.log.error("[SCHED_ERR] There is " + scheduledTasks.size() + " real scheduled tasks remaining");
                DynamXMain.log.error("[SCHED_ERR] Error executing task !", e);
                try {
                    DynamXMain.log.error("[SCHED_ERR] Scheduled tasks : " + scheduledTasks);
                } catch (Exception exception) {
                    DynamXMain.log.fatal("[SCHED_ERR] Additional error while printing current tasks", e);
                }
                throw e;
            }
            while (!operations.isEmpty()) {
                operations.remove().execute(dynamicsWorld, joints, entities);
            }
        }
        profiler.end(Profiler.Profiles.ADD_REMOVE_BODIES);
    }

    /**
     * Ticks the physics world
     *
     * @param profiler The current profiler
     */
    protected void stepSimulationImpl(Profiler profiler) {
        Vector3fPool.openPool();
        //Process pending operations
        flushOperations(profiler);
        //Check entity statuses and load terrain around them
        profiler.start(Profiler.Profiles.LOAD_SHAPES);
        entities.forEach(e -> {
            if (e.physicsHandler != null) {
                EntityPhysicsState physicalState = e.physicsHandler.getPhysicsState();
                switch (physicalState) {
                    case UNFREEZE:
                        e.physicsHandler.setFreezePhysics(false);
                    case ENABLE:
                        e.getTerrainCache().update(manager, profiler);

                        //FIXME TEST TO PREVENT DESPAWNING ON DEDICATED SERVER, BUT NOT IN SOLO BECAUSE THREADED
                        if (this instanceof BuiltinPhysicsWorld) {
                            e.physicsHandler.setPhysicsState(EntityPhysicsState.FREEZE);
                        }
                        break;
                    case FREEZE:
                        e.physicsHandler.setPhysicsState(EntityPhysicsState.WILL_FREEZE);
                        break;
                    case WILL_FREEZE:
                        e.physicsHandler.setFreezePhysics(true);
                        e.physicsHandler.setPhysicsState(EntityPhysicsState.FROZEN);
                        break;
                }
            }
        });
        profiler.end(Profiler.Profiles.LOAD_SHAPES);
        Vector3fPool.closePool();

        //Update collisions handler
        CollisionsHandler.tick();

        //Pre-tick each entity before the physics engine tick
        //Read the input data and send it to the physics
        profiler.start(Profiler.Profiles.PHYSICS_TICK_ENTITIES_PRE);
        entities.forEach(e -> {
            QuaternionPool.openPool();
            Vector3fPool.openPool();
            try {
                //e.getNetwork().onPrePhysicsTick(profiler);
                e.getSynchronizer().onPrePhysicsTick(profiler);
            } catch (Exception ex) {
                throw new PhysicsEntityException(e, "prePhysicsTick", ex);
            }
            QuaternionPool.closePool();
            Vector3fPool.closePool();
        });
        profiler.end(Profiler.Profiles.PHYSICS_TICK_ENTITIES_PRE);

        //Update sync system
        if (mcWorld.isRemote) {
            ClientPhysicsSyncManager.tick();
        } else if (mcWorld.getMinecraftServer().isDedicatedServer()) {
            ServerPhysicsSyncManager.tick(profiler);
        }

        //Tick the physics engine
        //long pre = System.currentTimeMillis();
        profiler.start(Profiler.Profiles.BULLET_STEP_SIM);
        DynamXContext.getPhysicsSimulationMode(Side.SERVER).updatePhysicsWorld(dynamicsWorld);

        //Post-tick each entity after the physics engine tick
        //Retrieves the simulated data
        profiler.start(Profiler.Profiles.PHYSICS_TICK_ENTITIES_POST);
        entities.forEach(e -> {
            QuaternionPool.openPool();
            Vector3fPool.openPool();
            try {
                //e.getNetwork().onPostPhysicsTick(profiler);
                e.getSynchronizer().onPostPhysicsTick(profiler);
            } catch (Exception ex) {
                throw new PhysicsEntityException(e, "postPhysicsTick", ex);
            }
            QuaternionPool.closePool();
            Vector3fPool.closePool();
        });
        profiler.end(Profiler.Profiles.PHYSICS_TICK_ENTITIES_POST);

        // if (false && CmdNetworkConfig.sync_buff)
        //   System.out.println("Took " + (System.currentTimeMillis() - pre) + " ms");
        MinecraftForge.EVENT_BUS.post(new PhysicsEvent.StepSimulation(this, DynamXContext.getPhysicsSimulationMode(Side.SERVER).getTimeStep()));
        profiler.end(Profiler.Profiles.BULLET_STEP_SIM);
    }

    @Override
    public void addOperation(PhysicsWorldOperation<?> operation) {
        operations.add(operation);
    }

    @Override
    public void schedule(Runnable r) {
        if (!scheduledTasksLock.get())
            scheduledTasks.add(r);
        else
            DynamXMain.log.error("Adding physics tacks is locked due to a previous execution exception");
    }

    @Override
    public PhysicsWorldTerrain getTerrainManager() {
        return manager;
    }

    @Override
    public boolean ownsWorld(World mcWorld) {
        return mcWorld.equals(this.mcWorld);
    }

    @Override
    public void tickStart() {
    }

    @Override
    public void tickEnd() {
    }

    @Override
    public int getLoadedEntityCount() {
        return entities.size();
    }

    @Override
    public PhysicsSoftSpace getDynamicsWorld() {
        return this.dynamicsWorld;
    }

    @Override
    public void clearAll() {
        for (PhysicsCollisionObject rb : this.dynamicsWorld.getRigidBodyList()) {
            this.dynamicsWorld.removeCollisionObject(rb);
        }
        for (PhysicsJoint jt : this.joints) {
            this.dynamicsWorld.removeJoint(jt);
        }
        entities.clear();
        joints.clear();
        getTerrainManager().onWorldUnload();
        DynamXContext.getPhysicsWorldPerDimensionMap().remove(mcWorld.provider.getDimension());
    }
}