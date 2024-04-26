package fr.dynamx.common.network.sync.variables;

import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.client.handlers.ClientDebugSystem;
import fr.dynamx.client.handlers.ClientEventHandler;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.Entity;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class NetworkActivityTracker {
    public static final Map<Integer, Map<PhysicsEntity<?>, EntitySyncData>> syncDebug = new ConcurrentHashMap<>();
    public static final Map<Integer, Integer> lastPings = new HashMap<>();
    public static int lastPing;

    public static int lastTime;
    public static int viewIndex = -1;
    public static int viewEntity = -1;

    public static int lastLog;

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
        if (!(e instanceof PhysicsEntity)) {
            drawAVGData(fontRenderer, size);
            return;
        }
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

    public static void drawAVGData(FontRenderer fontRenderer, int size) {
        int viewIndex = NetworkActivityTracker.viewIndex;
        if (viewIndex == -1)
            viewIndex = lastTime;
        int start = viewIndex - size;
        int y = 2;
        if (lastPings.size() > 1) {
            int avgPing = 0, minPing = 0, maxPing = 0;
            for (int i = lastPing; i > Math.max(0, lastPing - 20); i--) {
                if (lastPings.containsKey(i)) {
                    int ping = lastPings.get(i);
                    avgPing += ping;
                    if (minPing == 0 || ping < minPing) {
                        minPing = ping;
                    }
                    if (maxPing == 0 || ping > maxPing) {
                        maxPing = ping;
                    }
                }
            }
            avgPing /= lastPings.size();
            fontRenderer.drawString("===============", 2, y, Color.GRAY.getRGB());
            y += fontRenderer.FONT_HEIGHT;
            fontRenderer.drawString("Avg Ping " + avgPing + " Min " + minPing + " Max " + maxPing, 2, y, Color.BLUE.getRGB());
            y += fontRenderer.FONT_HEIGHT;
        }
        for (int i = viewIndex; i >= start; i--) {
            if (!syncDebug.containsKey(i) || getDebugAt(i).isEmpty()) {
                continue;
            }
            //float avgDistance = 0, minDistance = 0, maxDistance = 0;
            int avgSize = 0, minSize = 0, maxSize = 0, totalSize = 0;
            int avgVars = 0, minVars = 0, maxVars = 0;
            int avgTimeDeltaPlus = 0, minTimeDeltaPlus = 0, maxTimeDeltaPlus = 0;
            int avgTimeDeltaLess = 0, minTimeDeltaLess = 0, maxTimeDeltaLess = 0;
            int avgPackets = 0, minPackets = 0, maxPackets = 0;
            int avgMissOrderCount = 0, minMissOrderCount = 0, maxMissOrderCount = 0;
            int avgMissOrderTime = 0, minMissOrderTime = 0, maxMissOrderTime = 0;
            Map<String, Integer> varCounts = new HashMap<>();
            Map<String, Integer> varSizes = new HashMap<>();
            for (EntitySyncData data : getDebugAt(i).values()) {
                //avgDistance += data.entityDistance;
                avgSize += data.messageSize;
                avgVars += data.receivedVars.size();
                /*if (minDistance == 0 || data.entityDistance < minDistance) {
                    minDistance = data.entityDistance;
                }
                if (maxDistance == 0 || data.entityDistance > maxDistance) {
                    maxDistance = data.entityDistance;
                }*/
                if (minSize == 0 || data.messageSize < minSize) {
                    minSize = data.messageSize;
                }
                if (maxSize == 0 || data.messageSize > maxSize) {
                    maxSize = data.messageSize;
                }
                if (minVars == 0 || data.receivedVars.size() < minVars) {
                    minVars = data.receivedVars.size();
                }
                if (maxVars == 0 || data.receivedVars.size() > maxVars) {
                    maxVars = data.receivedVars.size();
                }
                avgTimeDeltaPlus += data.timeDeltaPlus;
                avgTimeDeltaLess += data.timeDeltaLess;
                if (minTimeDeltaPlus == 0 || data.timeDeltaPlus < minTimeDeltaPlus) {
                    minTimeDeltaPlus = data.timeDeltaPlus;
                }
                if (maxTimeDeltaPlus == 0 || data.timeDeltaPlus > maxTimeDeltaPlus) {
                    maxTimeDeltaPlus = data.timeDeltaPlus;
                }
                if (minTimeDeltaLess == 0 || data.timeDeltaLess < minTimeDeltaLess) {
                    minTimeDeltaLess = data.timeDeltaLess;
                }
                if (maxTimeDeltaLess == 0 || data.timeDeltaLess > maxTimeDeltaLess) {
                    maxTimeDeltaLess = data.timeDeltaLess;
                }
                avgPackets += data.packetCount;
                if (minPackets == 0 || data.packetCount < minPackets) {
                    minPackets = data.packetCount;
                }
                if (maxPackets == 0 || data.packetCount > maxPackets) {
                    maxPackets = data.packetCount;
                }
                avgMissOrderCount += data.missOrderCount;
                if (minMissOrderCount == 0 || data.missOrderCount < minMissOrderCount) {
                    minMissOrderCount = data.missOrderCount;
                }
                if (maxMissOrderCount == 0 || data.missOrderCount > maxMissOrderCount) {
                    maxMissOrderCount = data.missOrderCount;
                }
                avgMissOrderTime += data.missOrderTime;
                if (minMissOrderTime == 0 || data.missOrderTime < minMissOrderTime) {
                    minMissOrderTime = data.missOrderTime;
                }
                if (maxMissOrderTime == 0 || data.missOrderTime > maxMissOrderTime) {
                    maxMissOrderTime = data.missOrderTime;
                }
                for (String s : data.receivedVars) {
                    varCounts.put(s, varCounts.getOrDefault(s, 0) + 1);
                }
                if (data.moduleSizes != null) {
                    for (Map.Entry<String, Integer> e : data.moduleSizes.entrySet()) {
                        varSizes.put(e.getKey(), varSizes.getOrDefault(e.getKey(), 0) + e.getValue());
                    }
                }
            }
            //avgDistance /= getDebugAt(i).size();
            totalSize = avgSize;
            avgSize /= getDebugAt(i).size();
            avgVars /= getDebugAt(i).size();
            avgTimeDeltaPlus /= getDebugAt(i).size();
            avgTimeDeltaLess /= getDebugAt(i).size();
            avgPackets /= getDebugAt(i).size();
            avgMissOrderCount /= getDebugAt(i).size();
            avgMissOrderTime /= getDebugAt(i).size();
            varCounts = varCounts.entrySet().stream().sorted(Comparator.comparingInt(e -> -e.getValue())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
            Map<String, Integer> finalVarCounts = varCounts;
            //varSizes = varSizes.entrySet().stream().map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue() / finalVarCounts.getOrDefault(e.getKey(), 1))).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
            fontRenderer.drawString("===============", 2, y, Color.GRAY.getRGB());
            y += fontRenderer.FONT_HEIGHT;
            fontRenderer.drawString("Time " + i, 2, y, Color.GREEN.getRGB());
            y += fontRenderer.FONT_HEIGHT;
            fontRenderer.drawString(/*"Avg Distance " + avgDistance + " Min " + minDistance + " Max " + maxDistance + "    //    " + */"Total size " + totalSize + " Avg Size " + avgSize + " Min " + minSize + " Max " + maxSize
                    + "    //    " + "Avg Vars " + avgVars + " Min " + minVars + " Max " + maxVars, 2, y, Color.GRAY.getRGB());
            y += fontRenderer.FONT_HEIGHT;
            Map<String, Integer> finalVarSizes = varSizes;
            fontRenderer.drawString("Counts/sizes: " + varCounts.entrySet().stream().map(e -> e.getKey() + " : " + e.getValue() + "/" + finalVarSizes.getOrDefault(e.getKey(), -1)).collect(Collectors.joining(", "))
                    , 2, y, Color.RED.getRGB());
            y += fontRenderer.FONT_HEIGHT;
            fontRenderer.drawString("ATD+ " + avgTimeDeltaPlus + " Mi " + minTimeDeltaPlus + " Ma " + maxTimeDeltaPlus
                    + " // " + "ATD- " + avgTimeDeltaLess + " Mi " + minTimeDeltaLess + " Ma " + maxTimeDeltaLess
                    + " // " + "AMC " + avgMissOrderCount + " Mi " + minMissOrderCount + " Ma " + maxMissOrderCount
                    + " // " + "AMT " + avgMissOrderTime + " Mi " + minMissOrderTime + " Ma " + maxMissOrderTime
                    + " // " + "AP " + avgPackets + " Mi " + minPackets + " Ma " + maxPackets + " // " + "Ent " + getDebugAt(i).size(), 2, y, Color.MAGENTA.getRGB());
            y += fontRenderer.FONT_HEIGHT;

            if (i == viewIndex && i != lastLog) {
                lastLog = i;
                String sb = "=========== Time " + i + " ===========" + "\n" +
                        "Total size " + totalSize + " Avg Size " + avgSize + " Min " + minSize + " Max " + maxSize + " // " +
                        "Avg Vars " + avgVars + " Min " + minVars + " Max " + maxVars + "\n" +
                        "Counts/sizes: " + varCounts.entrySet().stream().map(e -> e.getKey() + " : " + e.getValue() + "/" + finalVarSizes.getOrDefault(e.getKey(), -1)).collect(Collectors.joining(", ")) + "\n" +
                        "ATD+ " + avgTimeDeltaPlus + " Mi " + minTimeDeltaPlus + " Ma " + maxTimeDeltaPlus + " // " +
                        "ATD- " + avgTimeDeltaLess + " Mi " + minTimeDeltaLess + " Ma " + maxTimeDeltaLess + "\n" +
                        "AMC " + avgMissOrderCount + " Mi " + minMissOrderCount + " Ma " + maxMissOrderCount + " // " +
                        "AMT " + avgMissOrderTime + " Mi " + minMissOrderTime + " Ma " + maxMissOrderTime + "\n" +
                        "AP " + avgPackets + " Mi " + minPackets + " Ma " + maxPackets + " // " +
                        "Ent " + getDebugAt(i).size();
                System.out.println(sb);
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
        lastTime = ClientEventHandler.MC.player.ticksExisted / 10;
        if (!syncDebug.containsKey(lastTime))
            syncDebug.put(lastTime, new ConcurrentHashMap<>());
        if (!getDebugAt(lastTime).containsKey(entity))
            getDebugAt(lastTime).put(entity, new EntitySyncData(entity.getSynchronizer().getSimulationHolder(), entity.getSynchronizer().getSynchronizedVariables().values().stream().map(EntityVariable::getName).collect(Collectors.toList()), -1, -1, null, -1, -1, -1, -1));
        getDebugAt(lastTime).get(entity).sentVars.addAll(variables.stream().map(EntityVariable::getName).collect(Collectors.toList()));

        if (viewIndex == -1)
            syncDebug.keySet().removeIf(i -> i < lastTime - 2 * 60);
    }

    public static void addReceivedVars(PhysicsEntity<?> entity, Collection<EntityVariable<?>> variables, int messageSize, Map<String, Integer> moduleSizes, int timeDelta, int simTime) {
        if(!ClientDebugSystem.enableDebugDrawing || !DynamXDebugOptions.FULL_NETWORK_DEBUG.isActive()) {
            return;
        }
        lastTime = ClientEventHandler.MC.player.ticksExisted / 10;
        //System.out.println("Rcv " + lastTime+" "+variables.stream().map(SynchronizedEntityVariable::getName).collect(Collectors.toList()));
        if (!syncDebug.containsKey(lastTime))
            syncDebug.put(lastTime, new ConcurrentHashMap<>());
        if (!getDebugAt(lastTime).containsKey(entity)) {
            int missOrderTime = 0;
            if (syncDebug.containsKey(lastTime - 1) && getDebugAt(lastTime - 1).containsKey(entity)) {
                EntitySyncData data = getDebugAt(lastTime - 1).get(entity);
                if ((simTime - data.lastSimTime) > 4 || simTime < data.lastSimTime) {
                    missOrderTime += Math.abs(simTime - data.lastSimTime) - 1;
                    if (missOrderTime == 19) { //Sleeping entities
                        missOrderTime = 0;
                    } else {
                        System.out.println("/!\\ Step miss order " + entity + " " + simTime + " " + data.lastSimTime + " " + (simTime - data.lastSimTime));
                    }
                }
            }
            getDebugAt(lastTime).put(entity, new EntitySyncData(entity.getSynchronizer().getSimulationHolder(), entity.getSynchronizer().getSynchronizedVariables().values().stream().map(EntityVariable::getName).collect(Collectors.toList()), entity.getDistance(ClientEventHandler.MC.player),
                    messageSize, moduleSizes, timeDelta > 1 ? timeDelta - 1 : 0, timeDelta < 1 ? timeDelta - 1 : 0, simTime, missOrderTime));
        } else {
            EntitySyncData data = getDebugAt(lastTime).get(entity);
            data.messageSize += messageSize;
            moduleSizes.forEach((k, v) -> data.moduleSizes.put(k, data.moduleSizes.getOrDefault(k, 0) + v));
            data.timeDeltaPlus += timeDelta > 1 ? timeDelta - 1 : 0;
            data.timeDeltaLess += timeDelta < 1 ? timeDelta - 1 : 0;
            if ((simTime - data.lastSimTime) > 4 || simTime < data.lastSimTime) {
                System.out.println("/!\\ Miss order " + entity + " " + simTime + " " + data.lastSimTime + " " + (simTime - data.lastSimTime));
                data.missOrderTime += Math.abs(simTime - data.lastSimTime) - 1;
                data.missOrderCount += Math.abs(simTime - data.lastSimTime) > 1 ? 1 : 0;
            }
            data.lastSimTime = simTime;
            data.packetCount++;
        }
        getDebugAt(lastTime).get(entity).receivedVars.addAll(variables.stream().map(EntityVariable::getName).collect(Collectors.toList()));

        if (viewIndex == -1)
            syncDebug.keySet().removeIf(i -> i < lastTime - 2 * 60);
    }

    public static void addPing(int pingMs) {
        lastPing = ClientEventHandler.MC.player.ticksExisted;
        lastPings.put(lastPing, pingMs);
        if (viewIndex == -1)
            lastPings.keySet().removeIf(i -> i < lastPing - 20 * 60);
    }

    public static class EntitySyncData {
        public SimulationHolder simulationHolder;
        public List<String> activeVars;
        public List<String> sentVars = new ArrayList<>();
        public List<String> receivedVars = new ArrayList<>();
        public float entityDistance;
        public int messageSize;
        public Map<String, Integer> moduleSizes;
        public int timeDeltaPlus;
        public int timeDeltaLess;
        public int packetCount;

        public int lastSimTime;
        public int missOrderCount;
        public int missOrderTime;

        //TODO PRINT DATA IN LOG

        public EntitySyncData(SimulationHolder simulationHolder, List<String> activeVars, float entityDistance, int messageSize, Map<String, Integer> moduleSizes, int timeDeltaPlus, int timeDeltaLess, int simTime, int missOrderTime) {
            this.simulationHolder = simulationHolder;
            this.activeVars = activeVars;
            this.entityDistance = entityDistance;
            this.messageSize = messageSize;
            this.moduleSizes = moduleSizes;
            this.timeDeltaPlus = timeDeltaPlus;
            this.timeDeltaLess = timeDeltaLess;
            this.packetCount = 1;
            this.lastSimTime = simTime;
            this.missOrderCount = missOrderTime > 0 ? 1 : 0;
            this.missOrderTime = missOrderTime;
        }
    }
}
