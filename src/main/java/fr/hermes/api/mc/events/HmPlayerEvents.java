package fr.hermes.api.mc.events;

import fr.hermes.api.events.HmEvent;
import fr.hermes.api.events.HmEventPhase;
import fr.hermes.api.events.HmEventResult;
import fr.hermes.api.events.ListenableHmEvent;
import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.entities.HmPlayerEntity;
import fr.hermes.api.mc.entities.HmServerPlayerEntity;
import fr.hermes.api.mc.items.HmItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;
import org.joml.Vector3i;

/**
 * Registry of Minecraft common player events
 */
public final class HmPlayerEvents {
    /**
     * Callback for player join events.
     */
    @FunctionalInterface
    public interface PlayerJoinCallback {
        void onPlayerJoin(HmServerPlayerEntity player);
    }

    /**
     * Called when a player joins the server.
     */
    public static final HmEvent<PlayerJoinCallback> JOIN = ListenableHmEvent.create(event ->
            (player) -> event.forEach(l -> l.onPlayerJoin(player))
    );

    /**
     * Callback for player leave events.
     */
    @FunctionalInterface
    public interface PlayerLeaveCallback {
        void onPlayerLeave(HmServerPlayerEntity player);
    }

    /**
     * Called when a player leaves the server.
     */
    public static final HmEvent<PlayerLeaveCallback> LEAVE = ListenableHmEvent.create(event ->
            (player) -> event.forEach(l -> l.onPlayerLeave(player))
    );

    /**
     * Callback for player tick events.
     */
    @FunctionalInterface
    public interface PlayerTickCallback {
        void onPlayerTick(HmPlayerEntity player, HmEventPhase phase);
    }

    /**
     * Called when a player ticks.
     */
    public static final HmEvent<PlayerTickCallback> TICK = ListenableHmEvent.create(event ->
            (player, phase) -> event.forEach(l -> l.onPlayerTick(player, phase))
    );

    /**
     * Callback for player start tracking events.
     */
    @FunctionalInterface
    public interface PlayerStartTrackingCallback {
        void onPlayerStartTracking(HmServerPlayerEntity player, HmEntity entity);
    }

    /**
     * Called when a player starts tracking an entity.
     */
    public static final HmEvent<PlayerStartTrackingCallback> START_TRACKING = ListenableHmEvent.create(event ->
            (player, entity) -> event.forEach(l -> l.onPlayerStartTracking(player, entity))
    );

    /**
     * Callback for player right click on block event.
     */
    @FunctionalInterface
    public interface PlayerRightClickBlockCallback {
        void onPlayerRightClickBlock(HmPlayerEntity player, EnumHand hand, BlockPos pos, Vector3f hitVec);
    }

    /**
     * Called when a player right clicks a block.
     */
    public static final HmEvent<PlayerRightClickBlockCallback> RIGHT_CLICK_BLOCK = ListenableHmEvent.create(event ->
            (player, hand, pos, hitVec) -> event.forEach(l -> l.onPlayerRightClickBlock(player, hand, pos, hitVec))
    );

    /**
     * Callback for player right click on item event.
     */
    @FunctionalInterface
    public interface PlayerRightClickItemCallback {
        void onPlayerRightClickItem(HmPlayerEntity player, EnumHand hand, HmItemStack itemStack);
    }

    /**
     * Called when a player right clicks an item.
     */
    public static final HmEvent<PlayerRightClickItemCallback> RIGHT_CLICK_ITEM = ListenableHmEvent.create(event ->
            (player, hand, itemStack) -> event.forEach(l -> l.onPlayerRightClickItem(player, hand, itemStack))
    );

    /**
     * Callback for player right click on entity event.
     */
    @FunctionalInterface
    public interface PlayerRightClickEntityCallback {
        HmEventResult<Void> onPlayerRightClickEntity(HmPlayerEntity player, EnumHand hand, HmEntity target);
    }

    /**
     * Called when a player right clicks an entity.
     */
    public static final HmEvent<PlayerRightClickEntityCallback> RIGHT_CLICK_ENTITY = ListenableHmEvent.create(event ->
            (player, hand, target) -> event.process(l -> l.onPlayerRightClickEntity(player, hand, target))
    );
}
