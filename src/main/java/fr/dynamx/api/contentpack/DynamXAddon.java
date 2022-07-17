package fr.dynamx.api.contentpack;

import net.minecraftforge.fml.relauncher.Side;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents an addon for DynamX <br>
 * You <strong>should</strong> use @AddonEventSubscriber on a static method to catch addon initialization (like @EventHandler for Forge, but with static method and no parameter)
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface DynamXAddon {
    /**
     * @return The id of the mod owning this addon
     */
    String modid();

    /**
     * @return The name of the addon (used for logging)
     */
    String name();

    /**
     * @return The version of the addon (used for addon compatibility)
     */
    String version();

    /**
     * @return The sides where the addon should be loaded, client and server by default
     */
    Side[] sides() default {Side.CLIENT, Side.SERVER};

    /**
     * @return A boolean indicating if a client without this addon should be disconnected when trying to connect to a server having this addon loaded
     */
    boolean requiredOnClient() default true;

    /**
     * See {@link DynamXAddon}
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface AddonEventSubscriber {
    }
}
