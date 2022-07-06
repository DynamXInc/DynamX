package fr.dynamx.common.physics.terrain;

import fr.dynamx.common.physics.terrain.chunk.ChunkLoadingTicket;
import fr.dynamx.utils.VerticalChunkPos;

import java.util.HashMap;
import java.util.Map;

/**
 * A kindly optimzed map for {@link ChunkLoadingTicket}s, identified by their position
 */
public class ChunkLoadingTicketMap
{
    private final Map<Integer, Map<Integer, Map<VerticalChunkPos, ChunkLoadingTicket>>> ticketsMap = new HashMap<>();

    public ChunkLoadingTicket get(VerticalChunkPos pos) {
        Map<Integer, Map<VerticalChunkPos, ChunkLoadingTicket>> m1 = ticketsMap.computeIfAbsent(pos.x >> 4, k -> new HashMap<>());
        Map<VerticalChunkPos, ChunkLoadingTicket> m2 = m1.computeIfAbsent(pos.z >> 4, k -> new HashMap<>());
        return m2.get(pos);
    }

    public void put(VerticalChunkPos pos, ChunkLoadingTicket ticket) {
        Map<Integer, Map<VerticalChunkPos, ChunkLoadingTicket>> m1 = ticketsMap.computeIfAbsent(pos.x >> 4, k -> new HashMap<>());
        Map<VerticalChunkPos, ChunkLoadingTicket> m2 = m1.computeIfAbsent(pos.z >> 4, k -> new HashMap<>());
        m2.put(pos, ticket);
    }

    public ChunkLoadingTicket remove(VerticalChunkPos pos) {
        Map<Integer, Map<VerticalChunkPos, ChunkLoadingTicket>> m1 = ticketsMap.get(pos.x >> 4);
        if(m1 != null) {
            Map<VerticalChunkPos, ChunkLoadingTicket> m2 = m1.get(pos.z >> 4);
            if(m2 != null) {
                return m2.remove(pos);
            }
        }
        return null;
    }

    public boolean containsKey(VerticalChunkPos pos) {
        Map<Integer, Map<VerticalChunkPos, ChunkLoadingTicket>> m1 = ticketsMap.get(pos.x >> 4);
        if(m1 != null) {
            Map<VerticalChunkPos, ChunkLoadingTicket> m2 = m1.get(pos.z >> 4);
            if(m2 != null) {
                return m2.containsKey(pos);
            }
        }
        return false;
    }

    public void clear() {
        ticketsMap.forEach((i, m) -> {
            m.forEach((j, t) -> {
                t.clear();
            });
            m.clear();
        });
        ticketsMap.clear();
    }
}
