package fr.dynamx.common.network.sync.variables;

import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.client.handlers.ClientEventHandler;
import fr.dynamx.common.entities.PhysicsEntity;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.Entity;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class NetworkActivityTracker {
    public static final Map<Integer, Map<PhysicsEntity<?>, EntitySyncData>> syncDebug = new HashMap<>();

    public static int lastTime;
    public static int viewIndex = -1;
    public static int viewEntity = -1;

    public static Map<PhysicsEntity<?>, EntitySyncData> getDebugAt(int time) {
        return syncDebug.get(time);
    }

    public static void pause() {
        viewIndex = lastTime;
    }

    public static void resume() {
        viewIndex = -1;
    }

    public static void drawNetworkActivity(FontRenderer fontRenderer, int size) {
        Entity e;
        if (viewEntity != -1)
            e = ClientEventHandler.MC.world.getEntityByID(viewEntity);
        else
            e = ClientEventHandler.MC.objectMouseOver.entityHit;
        if (!(e instanceof PhysicsEntity))
            return;
        PhysicsEntity<?> entity = (PhysicsEntity<?>) e;
        int viewIndex = NetworkActivityTracker.viewIndex;
        if (viewIndex == -1)
            viewIndex = lastTime;
        int start = viewIndex - size;
        int y = 2;
        for (int i = viewIndex; i >= start; i--) {
            if (syncDebug.containsKey(i) && getDebugAt(i).containsKey(entity)) {
                fontRenderer.drawString("===============", 2, y, Color.GRAY.getRGB());
                y += fontRenderer.FONT_HEIGHT;
                y = drawEntityDebug(y, i, entity, fontRenderer);
            }
        }
    }

    public static int drawEntityDebug(int y, int time, PhysicsEntity<?> entity, FontRenderer fontRenderer) {
        EntitySyncData data = getDebugAt(time).get(entity);
        fontRenderer.drawString("-" + entity + " " + data.simulationHolder, 2, y, Color.GRAY.getRGB());
        y += fontRenderer.FONT_HEIGHT;
        for (String s : data.activeVars) {
            boolean rcv = data.receivedVars.contains(s);
            boolean sent = data.sentVars.contains(s);
            Color color = sent ? (rcv ? Color.GREEN : Color.RED) : (rcv ? Color.CYAN : Color.ORANGE);
            fontRenderer.drawString(s + " : " + (rcv ? "R" : " ") + (sent ? "S" : " "), 2, y, color.getRGB());
            y += fontRenderer.FONT_HEIGHT;
        }
        return y;
    }

    public static void addSentVars(PhysicsEntity<?> entity, Collection<EntityVariable<?>> variables) {
        lastTime = ClientEventHandler.MC.player.ticksExisted;
        if (!syncDebug.containsKey(lastTime))
            syncDebug.put(lastTime, new HashMap<>());
        if (!getDebugAt(lastTime).containsKey(entity))
            getDebugAt(lastTime).put(entity, new EntitySyncData(entity.getSynchronizer().getSimulationHolder(), entity.getSynchronizer().getSynchronizedVariables().values().stream().map(EntityVariable::getName).collect(Collectors.toList())));
        getDebugAt(lastTime).get(entity).sentVars.addAll(variables.stream().map(EntityVariable::getName).collect(Collectors.toList()));

        if (viewIndex == -1)
            syncDebug.keySet().removeIf(i -> i < lastTime - 20 * 60);
    }

    public static void addReceivedVars(PhysicsEntity<?> entity, Collection<EntityVariable<?>> variables) {
        lastTime = ClientEventHandler.MC.player.ticksExisted;
        //System.out.println("Rcv " + lastTime+" "+variables.stream().map(SynchronizedEntityVariable::getName).collect(Collectors.toList()));
        if (!syncDebug.containsKey(lastTime))
            syncDebug.put(lastTime, new HashMap<>());
        if (!getDebugAt(lastTime).containsKey(entity))
            getDebugAt(lastTime).put(entity, new EntitySyncData(entity.getSynchronizer().getSimulationHolder(), entity.getSynchronizer().getSynchronizedVariables().values().stream().map(EntityVariable::getName).collect(Collectors.toList())));
        getDebugAt(lastTime).get(entity).receivedVars.addAll(variables.stream().map(EntityVariable::getName).collect(Collectors.toList()));

        if (viewIndex == -1)
            syncDebug.keySet().removeIf(i -> i < lastTime - 20 * 60);
    }

    public static class EntitySyncData {
        public SimulationHolder simulationHolder;
        public List<String> activeVars;
        public List<String> sentVars = new ArrayList<>();
        public List<String> receivedVars = new ArrayList<>();

        public EntitySyncData(SimulationHolder simulationHolder, List<String> activeVars) {
            this.simulationHolder = simulationHolder;
            this.activeVars = activeVars;
        }
    }
}
