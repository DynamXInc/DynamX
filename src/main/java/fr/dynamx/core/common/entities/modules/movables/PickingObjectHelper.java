package fr.dynamx.core.common.entities.modules.movables;

import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.core.common.DynamXContext;
import fr.dynamx.core.common.entities.PhysicsEntity;
import fr.dynamx.core.common.entities.modules.DoorsModule;
import fr.dynamx.core.common.entities.modules.MovableModule;
import fr.dynamx.core.common.items.tools.ItemWrench;
import fr.dynamx.core.common.network.packets.MessageSyncPlayerPicking;
import fr.dynamx.core.utils.DynamXUtils;
import fr.dynamx.core.utils.optimization.QuaternionPool;
import fr.dynamx.core.utils.physics.PhysicsRaycastResult;
import fr.dynamx.forge.DynamXConfig;
import fr.hermes.api.utils.HmEntityLogicMatcher;
import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.entities.HmPlayerEntity;
import fr.hermes.api.mc.world.HmWorld;
import fr.dynamx.core.utils.optimization.JmeVector3fPool;

import java.util.HashMap;
import java.util.function.Predicate;

public class PickingObjectHelper {
    public static void handlePickingControl(MovableModule.Action moduleAction, HmPlayerEntity player) {
        HmWorld world = player.hm$getWorld();
        if (!player.hm$isCreativeMode() && !(player.hm$getHeldItemMainHand().hm$getItem() instanceof ItemWrench)
                && !DynamXConfig.allowPlayersToMoveObjects || moduleAction.getMovableAction() == MovableModule.EnumAction.ATTACH_OBJECTS) {
            return;
        }
        JmeVector3fPool.openPool();
        QuaternionPool.openPool();
        if (!DynamXContext.getPlayerPickingObjects().containsKey(player.hm$getEntityId())) {
            switch (moduleAction.getMovableAction()) {
                case PICK:
                    startPicking(moduleAction, player);
                    break;
                case TAKE:
                    startTaking(moduleAction, world, player);
                    break;
            }
        } else {
            HmEntity entity = world.hm$getEntityByID(DynamXContext.getPlayerPickingObjects().get(player.hm$getEntityId()));
            PhysicsEntity<?> physicsEntity = HmEntityLogicMatcher.cast(entity, PhysicsEntity.class);
            if (physicsEntity != null) {
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
                DynamXContext.getPlayerPickingObjects().remove(player.hm$getEntityId());
            }
        }
        //Copy map to avoid concurrency errors
        //TODO use map pool
        DynamXContext.getNetwork().sendToClientFromOtherThread(new MessageSyncPlayerPicking(new HashMap<>(DynamXContext.getPlayerPickingObjects())), EnumPacketTarget.ALL, null);
        JmeVector3fPool.closePool();
        QuaternionPool.closePool();
    }

    public static void handlePlayerDisconnection(HmPlayerEntity player) {
        HmWorld world = player.hm$getWorld();
        if (!player.hm$isCreativeMode() && !(player.hm$getHeldItemMainHand().hm$getItem() instanceof ItemWrench)
                && !DynamXConfig.allowPlayersToMoveObjects) {
            return;
        }
        HmEntity entity = world.hm$getEntityByID(DynamXContext.getPlayerPickingObjects().get(player.hm$getEntityId()));
        PhysicsEntity<?> physicsEntity = HmEntityLogicMatcher.cast(entity, PhysicsEntity.class);
        if (physicsEntity != null) {
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
            DynamXContext.getPlayerPickingObjects().remove(player.hm$getEntityId());
        }
        //Copy map to avoid concurrency errors
        //TODO use map pool
        DynamXContext.getNetwork().sendToClientFromOtherThread(new MessageSyncPlayerPicking(new HashMap<>(DynamXContext.getPlayerPickingObjects())), EnumPacketTarget.ALL, null);
    }

    private static void startPicking(MovableModule.Action moduleAction, HmPlayerEntity player) {
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
                movableModule.pickObjects.pickObject(player, physicsEntity, result.hitBody, result.hitPos,
                        result.hitPos.subtract(result.fromVec).length());
            }

        }
    }

    private static void startTaking(MovableModule.Action moduleAction, HmWorld world, HmPlayerEntity player) {
        HmEntity targetEntity = world.hm$getEntityByID((int) moduleAction.getInfo()[0]);
        PhysicsEntity<?> physicsEntity = HmEntityLogicMatcher.cast(targetEntity, PhysicsEntity.class);
            if (physicsEntity != null) {
            MovableModule movableModule = physicsEntity.getModuleByType(MovableModule.class);
            if (movableModule != null) {
                movableModule.usingAction = MovableModule.EnumAction.TAKE;
                movableModule.moveObjects.pickObject(player, physicsEntity);
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
