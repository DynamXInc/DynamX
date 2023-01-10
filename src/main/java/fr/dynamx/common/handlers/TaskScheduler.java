package fr.dynamx.common.handlers;

import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.sync.MessagePhysicsEntitySync;
import fr.dynamx.common.network.sync.MessageSeatsSync;
import fr.dynamx.server.network.ServerPhysicsSyncManager;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A simple delayed tasks scheduler <br>
 * Tasks are executed in the server thread in single player and dedicated servers and in client thread in multiplayer
 *
 * @see ScheduledTask
 */
public class TaskScheduler {
    private static final ConcurrentLinkedQueue<ScheduledTask> tasks = new ConcurrentLinkedQueue<>();

    public static void schedule(ScheduledTask task) {
        tasks.add(task);
    }

    public static void tick() {
        if (!tasks.isEmpty()) {
            List<ScheduledTask> rm = new ArrayList<>();
            tasks.forEach(t -> {
                if (t.execute())
                    rm.add(t);
            });
            rm.forEach(tasks::remove);
        }
    }

    /**
     * A delayed Runnable to execute with the {@link TaskScheduler}
     */
    public abstract static class ScheduledTask implements Runnable {
        private short timeLeft;

        /**
         * @param timeLeft The delay in ticks
         */
        public ScheduledTask(short timeLeft) {
            this.timeLeft = timeLeft;
        }

        public boolean execute() {
            timeLeft--;
            if (timeLeft <= 0) {
                this.run();
                return true;
            }
            return false;
        }
    }

    /**
     * Used for re-sync of entities after player tracking start (seats for example)
     */
    public static class ResyncItem extends ScheduledTask {
        public final PhysicsEntity<?> entity;
        public final EntityPlayerMP target;

        public ResyncItem(PhysicsEntity<?> entity, EntityPlayerMP target) {
            super((byte) 20);
            this.entity = entity;
            this.target = target;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ResyncItem that = (ResyncItem) o;
            return entity.equals(that.entity) &&
                    target.equals(that.target);
        }

        @Override
        public int hashCode() {
            return Objects.hash(entity, target);
        }

        @Override
        public void run() {
            if (target.connection != null && target.connection.getNetworkManager().isChannelOpen()) {
                entity.getSynchronizer().resyncEntity(target);
            } else {
                DynamXMain.log.warn("Skipping resync item of " + entity + " for " + target + " : player not connected");
            }
        }
    }
}
