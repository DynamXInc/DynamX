package fr.dynamx.client.gui;

import fr.aym.acsguis.component.GuiComponent;
import fr.aym.acsguis.component.layout.GuiScaler;
import fr.aym.acsguis.component.panel.GuiFrame;
import fr.aym.acsguis.component.textarea.GuiLabel;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.api.network.sync.ClientEntityNetHandler;
import fr.dynamx.client.camera.CameraSystem;
import fr.dynamx.client.network.ClientPhysicsEntitySynchronizer;
import fr.dynamx.client.network.ClientPhysicsSyncManager;
import fr.dynamx.common.entities.BaseVehicleEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.List;

public class VehicleHud extends GuiFrame {
    private final BaseVehicleEntity<?> riddenEntity;
    private GuiLabel netWarning;
    private final List<ResourceLocation> styleSheets = new ArrayList<>();

    public VehicleHud(IModuleContainer.ISeatsContainer entity) {
        super(new GuiScaler.Identity());
        this.riddenEntity = entity.cast();
        setCssClass("root");
        List<IVehicleController> controllers = new ArrayList<>(((ClientEntityNetHandler) entity.cast().getSynchronizer()).getControllers());
        if (!MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.CreateHud(this, styleSheets, entity.getSeats().isLocalPlayerDriving(), this.riddenEntity, controllers))) {
            controllers.forEach(c ->
            {
                List<ResourceLocation> hudStyle = c.getHudCssStyles();
                if (hudStyle != null)
                    styleSheets.addAll(hudStyle);
                GuiComponent<?> hud = c.createHud();
                if (hud != null) {
                    add(hud);
                }
            });
            if (entity.cast().getSynchronizer() instanceof ClientPhysicsEntitySynchronizer) {
                netWarning = new GuiLabel("");
                netWarning.setCssId("network_warning");
                add(netWarning);
            }
            //add(new GuiLabel("DynamX " + DynamXConstants.VERSION_TYPE + " V." + DynamXConstants.VERSION).setCssId("hud_ea_warning"));
        }
        CameraSystem.setupCamera(entity);
    }

    @Override
    public void tick() {
        super.tick();
        if (netWarning != null & ClientPhysicsSyncManager.hasBadConnection() && riddenEntity.ticksExisted % (20 * 3) < (20 * 2))
            netWarning.setText(ClientPhysicsSyncManager.getPingMessage());
        else if (netWarning != null && !netWarning.getText().isEmpty())
            netWarning.setText("");
    }

    @Override
    public List<ResourceLocation> getCssStyles() {
        return styleSheets;
    }

    @Override
    public boolean needsCssReload() {
        return false;
    }
}
