package fr.dynamx.common.network.sync.vars;

import com.jme3.math.Vector3f;
import fr.dynamx.api.network.sync.PhysicsEntityNetHandler;
import fr.dynamx.api.network.sync.SyncTarget;
import fr.dynamx.api.network.sync.SynchronizedVariable;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.modules.MovableModule;
import fr.dynamx.common.network.sync.MessagePhysicsEntitySync;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;

public class MovableSynchronizedVariable <A extends PhysicsEntity<?>>  implements SynchronizedVariable<A>
{
    public static final ResourceLocation NAME = new ResourceLocation(DynamXConstants.ID, "movable");

    private Vector3f point = new Vector3f();
    private float pickDistance;
    //private int entityID;
    //private Vector3f point1 = new Vector3f();

    private boolean isPicked;
    private int pickedEntityID;
    private int moverId, pickerId;

    @Override
    public SyncTarget getValueFrom(A entity, PhysicsEntityNetHandler<A> network, Side side, int syncTick) {
        boolean changed = false;
        MovableModule movableModule = entity.getModuleByType(MovableModule.class);

        if(point != movableModule.pickObjects.getLocalPickPosition()) {
            point = movableModule.pickObjects.getLocalPickPosition();
            changed = true;
        }
        if(pickDistance != movableModule.pickObjects.pickDistance) {
            pickDistance = movableModule.pickObjects.pickDistance;
            changed = true;
        }
        if(movableModule.pickObjects.mover != null && pickerId != movableModule.pickObjects.mover.getEntityId()) {
            pickerId = movableModule.pickObjects.mover.getEntityId();
            changed = true;
        }

        if(movableModule.moveObjects.pickedEntity != null) {
            if (pickedEntityID != movableModule.moveObjects.pickedEntity.getEntityId()) {
                pickedEntityID = movableModule.moveObjects.pickedEntity.getEntityId();
                changed = true;
            }
        }
        if (isPicked != movableModule.moveObjects.isPicked) {
            isPicked = movableModule.moveObjects.isPicked;
            changed = true;
        }
        if(movableModule.moveObjects.picker != null && moverId != movableModule.moveObjects.picker.getEntityId()) {
            moverId = movableModule.moveObjects.picker.getEntityId();
            changed = true;
        }

        return changed ? SyncTarget.ALL_CLIENTS : SyncTarget.NONE;
    }

    @Override
    public void setValueTo(A entity, PhysicsEntityNetHandler<A> network, MessagePhysicsEntitySync msg, Side side) {
        MovableModule movableModule = entity.getModuleByType(MovableModule.class);
        movableModule.pickObjects.setLocalPickPosition(point);
        movableModule.pickObjects.pickDistance = pickDistance;
        if(movableModule.moveObjects.isPicked && !isPicked) {
            entity.getNetwork().onPlayerStopControlling(movableModule.moveObjects.picker, false);
        }
        movableModule.moveObjects.isPicked = isPicked;
        Entity entity1 = entity.world.getEntityByID(pickedEntityID);
        if(entity1 instanceof PhysicsEntity<?>){
            movableModule.moveObjects.pickedEntity = (PhysicsEntity<?>) entity1;
        }
        entity1 = entity.world.getEntityByID(pickerId);
        if(entity1 instanceof EntityPlayer){
            if(DynamXContext.getPlayerPickingObjects().containsKey(entity1.getEntityId())) {
                movableModule.pickObjects.mover = (EntityPlayer) entity1;
                entity.getNetwork().onPlayerStartControlling((EntityPlayer) entity1, false);
            }
        }
        entity1 = entity.world.getEntityByID(moverId);
        if(entity1 instanceof EntityPlayer && isPicked){
            if(DynamXContext.getPlayerPickingObjects().containsKey(entity1.getEntityId())) {
                movableModule.moveObjects.picker = (EntityPlayer) entity1;
                entity.getNetwork().onPlayerStartControlling((EntityPlayer) entity1, false);
            }
        }
    }

    @Override
    public void write(ByteBuf buf, boolean compress) {
        DynamXUtils.writeVector3f(buf, point);
        buf.writeFloat(pickDistance);
        buf.writeBoolean(isPicked);
        buf.writeInt(pickedEntityID);
        buf.writeInt(pickerId);
        buf.writeInt(moverId);
        //buf.writeInt(entityID);
    }

    @Override
    public void writeEntityValues(A entity, ByteBuf buf) {
        MovableModule movableModule = entity.getModuleByType(MovableModule.class);
        DynamXUtils.writeVector3f(buf, movableModule.pickObjects.getLocalPickPosition());
        buf.writeFloat(movableModule.pickObjects.pickDistance);
        buf.writeBoolean(movableModule.moveObjects.isPicked);
        Entity pickedEntity = movableModule.moveObjects.pickedEntity;
        buf.writeInt(pickedEntity != null ? pickedEntity.getEntityId() : -1);
        pickedEntity = movableModule.pickObjects.mover;
        buf.writeInt(pickedEntity != null ? pickedEntity.getEntityId() : -1);
        pickedEntity = movableModule.moveObjects.pickedEntity;
        buf.writeInt(pickedEntity != null ? pickedEntity.getEntityId() : -1);
    }

    @Override
    public void read(ByteBuf buf) {
        point = DynamXUtils.readVector3f(buf);
        pickDistance = buf.readFloat();
        isPicked = buf.readBoolean();
        pickedEntityID = buf.readInt();
        pickerId = buf.readInt();
        moverId = buf.readInt();
        //entityID = buf.readInt();
    }
}
