package fr.dynamx.common.entities.modules.movables;

import com.jme3.bullet.objects.PhysicsRigidBody;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.common.entities.modules.MovableModule;
import fr.dynamx.common.items.tools.ItemWrench;
import fr.dynamx.common.network.packets.MessageSyncPlayerPicking;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.PhysicsRaycastResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.function.Predicate;

public class PickingObjectHelper {
    public static void handlePickingControl(MovableModule.Action moduleAction, EntityPlayer player) {
        World world = player.world;
        if (!player.capabilities.isCreativeMode && !(player.getHeldItemMainhand().getItem() instanceof ItemWrench)
                && !DynamXConfig.allowPlayersToMoveObjects || moduleAction.getMovableAction() == MovableModule.EnumAction.ATTACH_OBJECTS) {
            return;
        }
        Vector3fPool.openPool();
        QuaternionPool.openPool();
        if (!DynamXContext.getPlayerPickingObjects().containsKey(player.getEntityId())) {
            switch (moduleAction.getMovableAction()) {
                case PICK:
                    startPicking(moduleAction, player);
                    break;
                case TAKE:
                    startTaking(moduleAction, world, player);
                    break;
            }
        } else {
            Entity entity = world.getEntityByID(DynamXContext.getPlayerPickingObjects().get(player.getEntityId()));
            if (entity instanceof PhysicsEntity) {
                PhysicsEntity<?> physicsEntity = (PhysicsEntity<?>) entity;
                MovableModule movableModule = physicsEntity.getModuleByType(MovableModule.class);
                if (movableModule != null) {
                    switch (movableModule.usingAction) {
                        case PICK:
                            controlPicking(moduleAction, movableModule);
                            break;
                        case TAKE:
                            controlTaking(moduleAction, movableModule);
                            break;
                    }
                }
            } else { //If the entity does not exist, stop holding it
                DynamXContext.getPlayerPickingObjects().remove(player.getEntityId());
            }
        }
        //Copy map to avoid concurrency errors
        //TODO use map pool
        DynamXContext.getNetwork().sendToClientFromOtherThread(new MessageSyncPlayerPicking(new HashMap<>(DynamXContext.getPlayerPickingObjects())), EnumPacketTarget.ALL, null);
        Vector3fPool.closePool();
        QuaternionPool.closePool();
    }

    public static void handlePlayerDisconnection(EntityPlayer player) {
        World world = player.world;
        if (!player.capabilities.isCreativeMode && !(player.getHeldItemMainhand().getItem() instanceof ItemWrench)
                && !DynamXConfig.allowPlayersToMoveObjects) {
            return;
        }
        Entity entity = world.getEntityByID(DynamXContext.getPlayerPickingObjects().get(player.getEntityId()));
        if (entity instanceof PhysicsEntity) {
            PhysicsEntity<?> physicsEntity = (PhysicsEntity<?>) entity;
            MovableModule movableModule = physicsEntity.getModuleByType(MovableModule.class);
            if (movableModule != null) {
                switch (movableModule.usingAction) {
                    case PICK:
                        controlPicking(new MovableModule.Action(MovableModule.EnumAction.UNPICK), movableModule);
                        break;
                    case TAKE:
                        controlTaking(new MovableModule.Action(MovableModule.EnumAction.UNTAKE), movableModule);
                        break;
                }
            }
        } else { //If the entity does not exist, stop holding it
            DynamXContext.getPlayerPickingObjects().remove(player.getEntityId());
        }
        //Copy map to avoid concurrency errors
        //TODO use map pool
        DynamXContext.getNetwork().sendToClientFromOtherThread(new MessageSyncPlayerPicking(new HashMap<>(DynamXContext.getPlayerPickingObjects())), EnumPacketTarget.ALL, null);
    }

    private static void startPicking(MovableModule.Action moduleAction, EntityPlayer player) {
        int distanceMax = (int) moduleAction.getInfo()[0];

        Predicate<EnumBulletShapeType> predicateShape = p -> !p.isTerrain() && !p.isPlayer();

        PhysicsRaycastResult result = DynamXUtils.castRayFromEntity(player, distanceMax, predicateShape);

        if (result != null) {
            BulletShapeType<?> shapeType = (BulletShapeType<?>) result.hitBody.getUserObject();
            PhysicsEntity<?> physicsEntity = null;                //TODO PhysicsEntity<?>) ((SPPhysicsEntityNetHandler)shapeType.getObjectIn().getNetwork()).getOtherSideEntity();
            if (shapeType.getObjectIn() instanceof PhysicsEntity) {
                physicsEntity = (PhysicsEntity<?>) shapeType.getObjectIn();
            } else if (shapeType.getObjectIn() instanceof DoorsModule.DoorPhysics) {
                physicsEntity = ((DoorsModule.DoorPhysics) shapeType.getObjectIn()).getModule().vehicleEntity;
            }
            if (physicsEntity == null)
                return;
            MovableModule movableModule = physicsEntity.getModuleByType(MovableModule.class);
            //use entity id because instances depends on the side
            if (movableModule != null
                    && (!DynamXContext.getWalkingPlayers().containsKey(player)
                    || physicsEntity.getEntityId() != DynamXContext.getWalkingPlayers().get(player).getEntityId())) {
                movableModule.usingAction = MovableModule.EnumAction.PICK;
                if(result.hitBody instanceof PhysicsRigidBody) {
                    movableModule.pickObjects.pickObject(player, physicsEntity, (PhysicsRigidBody) result.hitBody, result.hitPos,
                            result.hitPos.subtract(result.fromVec).length());
                }
            }

        }
    }

    private static void startTaking(MovableModule.Action moduleAction, World world, EntityPlayer player) {
        Entity targetEntity = world.getEntityByID((int) moduleAction.getInfo()[0]);
        if (targetEntity instanceof PhysicsEntity) {
            MovableModule movableModule = ((PhysicsEntity<?>) targetEntity).getModuleByType(MovableModule.class);
            if (movableModule != null) {
                movableModule.usingAction = MovableModule.EnumAction.TAKE;
                movableModule.moveObjects.pickObject(player, (PhysicsEntity<?>) targetEntity);
            }
        }
    }

    private static void controlPicking(MovableModule.Action moduleAction, MovableModule movableModule) {
        switch (moduleAction.getMovableAction()) {
            case UNPICK:
                movableModule.pickObjects.unPickObject();
                break;
            case LENGTH_CHANGE:
                boolean mouseWheelInc = (boolean) moduleAction.getInfo()[0];
                int distanceMax = (int) moduleAction.getInfo()[1];
                movableModule.pickObjects.getPickDistance().set(MathHelper.clamp(
                        movableModule.pickObjects.getPickDistance().get() + (mouseWheelInc ? 1 : -1), 1.5f, distanceMax));
                break;
            case FREEZE_OBJECT:
                if (movableModule.pickObjects.getHitBody().getMass() > 0)
                    movableModule.pickObjects.getHitBody().setMass(0);
                break;
        }
    }

    private static void controlTaking(MovableModule.Action moduleAction, MovableModule movableModule) {
        switch (moduleAction.getMovableAction()) {
            case UNTAKE:
                movableModule.moveObjects.unPickObject();
                break;
            case THROW:
                int force = (int) moduleAction.getInfo()[0] / 2;
                force = Math.min(force, 20);
                //System.out.println("Force is "+force);
                movableModule.moveObjects.throwObject(force);
                break;
        }
    }
}
