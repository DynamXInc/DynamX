package fr.dynamx.client.handlers.hud;

import fr.aym.acsguis.component.GuiComponent;
import fr.aym.acsguis.component.panel.GuiPanel;
import fr.aym.acsguis.component.style.AutoStyleHandler;
import fr.aym.acsguis.component.style.ComponentStyleManager;
import fr.aym.acsguis.component.textarea.GuiLabel;
import fr.aym.acsguis.component.textarea.UpdatableGuiLabel;
import fr.aym.acsguis.cssengine.selectors.EnumSelectorContext;
import fr.aym.acsguis.cssengine.style.EnumCssStyleProperties;
import fr.aym.acsguis.utils.GuiConstants;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.client.handlers.ClientDebugSystem;
import fr.dynamx.client.handlers.KeyHandler;
import fr.dynamx.common.contentpack.type.vehicle.CarEngineInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.CarEngineModule;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.client.ClientDynamXUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CarController extends BaseController {
    public static final ResourceLocation STYLE = new ResourceLocation(DynamXConstants.ID, "css/vehicle_hud.css");

    //TODO CREATE EVENT TO INIT THIS ?
    @Getter
    @Setter
    private static HudIcons hudIcons;

    protected final CarEngineModule engine;

    @Getter
    @Setter
    private float speedLimit;

    /**
     * @param entity is assumed to implement {@link IModuleContainer.ISeatsContainer}
     */
    public CarController(BaseVehicleEntity<?> entity, CarEngineModule engine) {
        super(entity, engine);
        this.engine = engine;
        speedLimit = engine.getSpeedLimit();
    }

    @Override
    protected void updateControls() {
        if (engine.getEngineProperties() != null) {
            if (engine.getEngineProperties()[VehicleEntityProperties.EnumEngineProperties.ACTIVE_GEAR.ordinal()] != 0) // a gear is active
                handbraking = KeyHandler.KEY_HANDBRAKE.isKeyDown();
            if (KeyHandler.KEY_SPEED_LIMITIER.isPressed()) {
                if (speedLimit == Float.MAX_VALUE)
                    speedLimit = Math.abs(engine.getEngineProperties()[0]);
                else
                    speedLimit = Float.MAX_VALUE;
            }
            if (KeyHandler.KEY_ATTACH_TRAILER.isPressed())
                ClientDynamXUtils.attachTrailer();
            MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.ControllerUpdate<>(entity, this));
            int controls = 0;
            if (accelerating)
                controls = controls | 2;
            if (handbraking)
                controls = controls | 32;
            if (reversing)
                controls = controls | 4;
            if (turningLeft)
                controls = controls | 8;
            if (turningRight)
                controls = controls | 16;
            if (isEngineStarted)
                controls = controls | 1;
            engine.setControls(controls);
            engine.setSpeedLimit(speedLimit);
        }
    }

    //HUD

    @Override
    @SideOnly(Side.CLIENT)
    public GuiComponent<?> createHud() {
        GuiPanel panel = new GuiPanel();
        float maxRpm = entity.getPackInfo().getSubPropertyByType(CarEngineInfo.class).getMaxRevs() + 3000; // todo CONFIGURABLE
        float scale = 90f / 300;
        GuiPanel speed = new SpeedometerPanel(this, scale, maxRpm);
        speed.setCssClass("speed_pane");
        speed.setCssId("speedometer_texture");
        float[] engineProperties = engine.getEngineProperties();
        speed.add(new UpdatableGuiLabel("%s", s -> String.format(s, engine.isEngineStarted() ? Math.abs((int) engineProperties[VehicleEntityProperties.EnumEngineProperties.SPEED.ordinal()]) : "--", "")).setCssId("engine_speed"));
        // speed.add(new UpdatableGuiLabel("%d", s -> String.format(s, (int) (engineProperties[VehicleEntityProperties.EnumEngineProperties.REVS.ordinal()] * entity.getPackInfo().getSubPropertyByType(EngineInfo.class).getMaxRevs()), "")).setCssId("engine_rpm"));

        speed.add(new UpdatableGuiLabel("%s", s -> String.format(s, getGearString((int) engineProperties[VehicleEntityProperties.EnumEngineProperties.ACTIVE_GEAR.ordinal()]))).setCssId("engine_gear"));
        addRpmCounter(speed, scale, maxRpm);
        if (hudIcons != null) {
            GuiComponent<?>[] icons = new GuiComponent[hudIcons.iconCount()];
            for (int i = 0; i < icons.length; i++) {
                int finalI = i;
                speed.add(icons[i] = new GuiPanel() {
                    @Override
                    public boolean isVisible() {
                        return hudIcons.isVisible(finalI);
                    }
                }.setCssId("icon_" + i).setCssClass("hud_icon"));
                hudIcons.initIcon(i, icons[i]);
            }
            speed.addTickListener(() -> hudIcons.tick(icons));
        }
        panel.add(speed);

        panel.add(new UpdatableGuiLabel("hud.car.speedlimit", s -> {
            if (speedLimit != Float.MAX_VALUE) {
                return I18n.format(s, (int) speedLimit);
            } else {
                return "";
            }
        }).setCssId("speed_limit"));

        //Debug
        String cclass = ClientDebugSystem.enableDebugDrawing ? "hud_label_debug" : "hud_label_hidden";
        panel.add(new UpdatableGuiLabel("Handbrake : %s", s -> String.format(s, (engine.isHandBraking() ? "§cON" : "§aOFF"))).setCssId("handbrake_state").setCssClass(cclass));
        panel.add(new UpdatableGuiLabel("Sounds : %s", s -> String.format(s, (engine.getCurrentEngineSound() == null ? "none" : engine.getCurrentEngineSound().getSoundName()))).setCssId("engine_sounds").setCssClass(cclass));
        panel.setCssId("engine_hud");

        return panel;
    }

    @Override
    public List<ResourceLocation> getHudCssStyles() {
        return Collections.singletonList(STYLE);
    }

    protected String getGearString(int gear) {
        return gear == -1 ? "R" : gear == 0 ? (engine.isHandBraking() ? TextFormatting.RED + "P" : "N") : "" + gear;
    }

    protected void addRpmCounter(GuiPanel speedometer, float scale, float maxRpm) {
        int nmL = (int) (maxRpm / 1000);

        for (int i = 1; i <= nmL; i++) {
            double angle = ((i * 3f / 2 * Math.PI) / nmL) - Math.PI / 3;

            float r = (150 - 29) * scale;

            double halfLetter = 6 * scale;

            double x = (45 - Math.cos(angle) * r) - Math.abs(halfLetter * Math.cos(angle));
            double y = (45 - Math.sin(angle) * r) - Math.abs(halfLetter * Math.sin(angle)) - 2;

            speedometer.add(new GuiLabel("" + i).setCssClass("rpm_letter").getStyle().addAutoStyleHandler(new AutoStyleHandler<ComponentStyleManager>() {
                @Override
                public boolean handleProperty(EnumCssStyleProperties property, EnumSelectorContext context, ComponentStyleManager target) {
                    if (property == EnumCssStyleProperties.LEFT) {
                        target.getXPos().setAbsolute(-(float) x, GuiConstants.ENUM_RELATIVE_POS.END);
                        return true;
                    }
                    if (property == EnumCssStyleProperties.TOP) {
                        target.getYPos().setAbsolute((float) y);
                        return true;
                    }
                    if (property == EnumCssStyleProperties.COLOR) {
                        if (angle > Math.PI - Math.PI / 3) {
                            target.setForegroundColor(0xFFE23F3F);
                        }
                        return true;
                    }
                    return false;
                }

                @Override
                public Collection<EnumCssStyleProperties> getModifiedProperties(ComponentStyleManager target) {
                    return Arrays.asList(EnumCssStyleProperties.LEFT, EnumCssStyleProperties.TOP, EnumCssStyleProperties.COLOR);
                }
            }).getOwner());
        }
    }
}
