package fr.aym.acslib.api;

import fr.aym.acslib.impl.ACsPlatform;
import net.minecraftforge.fml.common.event.FMLStateEvent;

/**
 * An {@link ACsPlatform} service <br>
 * Should have the {@link ACsRegisteredService} annotation to be loaded <br>
 * Required here in DynamX because of the coremod loading system :c
 */
public interface ACsService {
    /**
     * @return The name of the service (should be unique per service-type, but two services with the same name can be loaded, and the newer (higher version) will be kept)
     */
    String getName();

    /**
     * @return The version of the service (used for service compatibility)
     */
    String getVersion();

    /**
     * Notifies of an fml loading event
     */
    default void onFMLStateEvent(FMLStateEvent event) {
    }
}