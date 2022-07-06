package fr.dynamx.api.network.sync;

import fr.dynamx.api.entities.modules.IVehicleController;

import java.util.List;

/**
 * Client {@link PhysicsEntityNetHandler}, handles controls and hud via the {@link IVehicleController}s
 */
public interface ClientEntityNetHandler
{
    List<IVehicleController> getControllers();
}
