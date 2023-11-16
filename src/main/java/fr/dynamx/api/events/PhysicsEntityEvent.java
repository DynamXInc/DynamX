package fr.dynamx.api.events;

import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.renderer.ObjModelRenderer;
import fr.dynamx.common.entities.ModularPhysicsEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.items.DynamXItemSpawner;
import fr.dynamx.utils.debug.renderer.DebugRenderer;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.GenericEvent;
import net.minecraftforge.fml.common.eventhandler.IGenericEvent;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.List;

public class PhysicsEntityEvent extends Event {
    @Getter
    private final Side side;
    private final PhysicsEntity<?> physicsEntity;

    public PhysicsEntityEvent(Side side, PhysicsEntity<?> physicsEntity) {
        this.physicsEntity = physicsEntity;
        this.side = side;
    }

    public PhysicsEntity<?> getEntity() {
        return physicsEntity;
    }

    public enum Phase {
        PRE, POST
    }

    /**
     * Fired when an entity is being spawned
     */
    @Cancelable
    public static class Spawn extends PhysicsEntityEvent {

        @Getter
        private final PhysicsEntity<?> physicsEntity;
        @Getter
        private final World world;
        @Getter
        @Nullable
        private final EntityPlayer player;
        @Getter
        private final DynamXItemSpawner<?> itemSpawner;
        @Getter
        private final Vec3d pos;

        /**
         * @param world         the physics world where the physics entity will be added
         * @param physicsEntity the physics entity being spawned
         * @param player        the player who is spawning the entity, can be null in case of a command for example
         * @param item          item used to spawn the entity
         * @param pos           block pos of the raycast
         */
        public Spawn(World world, PhysicsEntity<?> physicsEntity, EntityPlayer player, DynamXItemSpawner<?> item, Vec3d pos) {
            super(Side.SERVER, physicsEntity);
            this.world = world;
            this.physicsEntity = physicsEntity;
            this.player = player;
            this.itemSpawner = item;
            this.pos = pos;
        }
    }

    /**
     * Fired on server side when a player tries to kill a physics entity
     */
    @Cancelable
    public static class Attacked extends PhysicsEntityEvent {
        @Getter
        private final Entity sourceEntity;
        @Getter
        private final DamageSource damageSource;

        /**
         * @param sourceEntity  the entity who tries to kill the entity
         * @param physicsEntity the entity concerned
         * @param damageSource  the source of the damage
         */
        public Attacked(PhysicsEntity<?> physicsEntity, Entity sourceEntity, DamageSource damageSource) {
            super(Side.SERVER, physicsEntity);
            this.sourceEntity = sourceEntity;
            this.damageSource = damageSource;
        }
    }

    /**
     * Fired when a physics entity has just initialized its properties and its physic
     */
    public static class Init extends PhysicsEntityEvent {
        /**
         * Tells if the entity is using physics (for example a client entity with no prediction may not be using physics)
         */
        @Getter
        private final boolean usesPhysics;

        /**
         * @param physicsEntity The initialized entity
         * @param usesPhysics   True if the entity is using physics (for example a client entity with no prediction may not be using physics)
         */
        public Init(Side side, PhysicsEntity<?> physicsEntity, boolean usesPhysics) {
            super(side, physicsEntity);
            this.usesPhysics = usesPhysics;
        }
    }

    /**
     * Fired each tick when a physics entity is updated (called for entity update and physics update)
     *
     * @see UpdateType
     */
    public static class Update extends PhysicsEntityEvent {
        /**
         * The update type
         */
        @Getter
        private final UpdateType type;
        /**
         * If physics are simulated in this update <br> If false, the physics handler may be null
         */
        @Getter
        private final boolean simulatePhysics;

        /**
         * @param physicsEntity   The updated entity
         * @param type            The update type
         * @param simulatePhysics If physics are simulated in this update <br> If false, the physics handler may be null
         */
        public Update(Side side, PhysicsEntity<?> physicsEntity, UpdateType type, boolean simulatePhysics) {
            super(side, physicsEntity);
            this.type = type;
            this.simulatePhysics = simulatePhysics;
        }
    }

    /**
     * Fired when rendering a physics entity, before and after the render, with <strong>no</strong> pos and rotations transformations applied <br>
     * All phases are cancellable, except POST
     */
    public static class Render extends PhysicsEntityEvent {
        /**
         * The renderer of the entity
         */
        @Getter
        private final RenderPhysicsEntity<?> renderer;
        /**
         * The render type
         */
        @Getter
        private final Type renderType;
        /**
         * Render x, y and z pos
         */
        @Getter
        private final double x, y, z;
        /**
         * Partials render ticks
         */
        @Getter
        private final float partialTicks;
        /**
         * The render pass <br>
         * 0 for solid objects <br>
         * 1 for translucent objects <br>
         * Some types of render event are fired for both
         */
        @Getter
        private final int renderPass;

        public Render(PhysicsEntity<?> physicsEntity, RenderPhysicsEntity<?> renderer, Type renderType, double x, double y, double z, float partialTicks, int renderPass) {
            super(Side.CLIENT, physicsEntity);
            this.renderer = renderer;
            this.x = x;
            this.y = y;
            this.z = z;
            this.renderType = renderType;
            this.partialTicks = partialTicks;
            this.renderPass = renderPass;
        }

        public enum Type {
            ENTITY, RIDDING_PLAYERS, DEBUG, POST
        }
    }

    /**
     * Called when the renderer on an entity is created <br>
     * You can add debug renderers for your addon, depending on the type of the entity <br>
     * You can select for which entities you want to receive this event, supported generic types are "{@link fr.dynamx.common.entities.BaseVehicleEntity}", "{@link fr.dynamx.common.entities.PropsEntity}", "{@link fr.dynamx.common.entities.RagdollEntity}" and "{@link fr.dynamx.common.entities.vehicles.DoorEntity}"
     *
     * @see DebugRenderer
     * @see RenderPhysicsEntity
     */
    //Note : don't add parameters to PhysicsEntity : this would break the event on the fml side
    public static class InitRenderer<T extends PhysicsEntity> extends GenericEvent<T> {
        /**
         * The renderer for this type of entity
         */
        @Getter
        private final RenderPhysicsEntity<?> renderer;

        public InitRenderer(Class<T> type, RenderPhysicsEntity<?> renderer) {
            super(type);
            this.renderer = renderer;
        }

        /**
         * Adds the debug renders to the list of the entity renderer
         */
        public void addDebugRenderers(DebugRenderer<?>... renderers) {
            this.renderer.addDebugRenderers(renderers);
        }
    }

    /**
     * Fired each tick when a physics entity is updated, on server side
     */
    public static class ServerUpdate extends Update {
        /**
         * @param physicsEntity   The updated entity
         * @param simulatePhysics If physics are simulated in this update <br> If false, the physics handler may be null
         */
        public ServerUpdate(PhysicsEntity<?> physicsEntity, UpdateType type, boolean simulatePhysics) {
            super(Side.SERVER, physicsEntity, type, simulatePhysics);
        }
    }

    /**
     * Fired each tick when a physics entity is updated, on client side
     */
    public static class ClientUpdate extends Update {
        /**
         * @param physicsEntity   The updated entity
         * @param simulatePhysics If physics are simulated in this update <br> If false, the physics handler may be null
         */
        public ClientUpdate(PhysicsEntity<?> physicsEntity, UpdateType type, boolean simulatePhysics) {
            super(Side.CLIENT, physicsEntity, type, simulatePhysics);
        }
    }

    /**
     * {@link Update} types
     */
    public enum UpdateType {
        /**
         * Called on vanilla entity update, in minecraft thread
         */
        POST_ENTITY_UPDATE,
        /**
         * Called before ticking the physics world (can be in an external thread) <br>
         * Here you can give the "input" to the physics world, i.e. your controls, your forces, etc
         */
        PRE_PHYSICS_UPDATE,
        /**
         * Called after ticking the physics world (can be in an external thread) <br>
         * Here you can get the results of your "input" : the new position, the new rotation, etc
         */
        POST_PHYSICS_UPDATE
    }

    /**
     * Called when the module list of a vehicle is created <br>
     * The given list contains all base modules of the entity <br>
     * You can modify the module list to add or remove ones <br>
     * Take care of their order : a propulsion module should be added before an engine module <br> <br>
     * <strong>Note that you have a method to add your modules in your ISubInfoTypes</strong> <br> <br>
     * You can select for which entities you want to receive this event, supported generic types are "{@link fr.dynamx.common.entities.BaseVehicleEntity}", "{@link fr.dynamx.common.entities.PropsEntity}", "{@link fr.dynamx.common.entities.RagdollEntity}" and "{@link fr.dynamx.common.entities.vehicles.DoorEntity}"
     *
     * @see IPhysicsModule
     * @see ModularPhysicsEntity
     */
    //Note : don't add parameters to ModularPhysicsEntity : this would break the event on the fml side
    public static class CreateModules<T extends ModularPhysicsEntity> extends PhysicsEntityEvent implements IGenericEvent<T> {
        private final Class<T> type;
        @Getter
        private final List<IPhysicsModule<?>> moduleList;

        public CreateModules(Class<T> type, T entity, List<IPhysicsModule<?>> moduleList, Side side) {
            super(side, entity);
            this.type = type;
            this.moduleList = moduleList;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T getEntity() {
            return (T) super.getEntity();
        }

        @Override
        public Type getGenericType() {
            return type;
        }
    }
}
