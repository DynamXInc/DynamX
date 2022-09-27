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
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.client.camera.CameraSystem;
import fr.dynamx.client.handlers.ClientDebugSystem;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.parts.PartSeat;
import fr.dynamx.common.contentpack.type.vehicle.EngineInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.common.entities.modules.EngineModule;
import fr.dynamx.common.network.packets.MessageChangeDoorState;
import fr.dynamx.utils.DynamXConstants;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CarController implements IVehicleController {
    public static final ResourceLocation STYLE = new ResourceLocation(DynamXConstants.ID, "css/vehicle_hud.css");
    private static final Minecraft MC = Minecraft.getMinecraft();

    public static final KeyBinding car_engineOn = new KeyBinding("key.startEngine", Keyboard.KEY_O, "key.categories." + DynamXConstants.ID);
    public static final KeyBinding car_brake = new KeyBinding("key.brake", Keyboard.KEY_SPACE, "key.categories." + DynamXConstants.ID);
    public static final KeyBinding speedLimiter = new KeyBinding("key.speedlimit", Keyboard.KEY_J, "key.categories." + DynamXConstants.ID);
    public static final KeyBinding toggleLockDoor = new KeyBinding("key.toggleLockDoor", Keyboard.KEY_Y, "key.categories." + DynamXConstants.ID);

    //TODO CREATE EVENT TO INIT THIS ?
    private static HudIcons hudIcons;

    public static void registerControls() {
        ClientRegistry.registerKeyBinding(car_brake);
        ClientRegistry.registerKeyBinding(car_engineOn);
        ClientRegistry.registerKeyBinding(speedLimiter);
        ClientRegistry.registerKeyBinding(toggleLockDoor);
    }

    /**
     * @return The current hud icons
     */
    public static HudIcons getHudIcons() {
        return hudIcons;
    }

    /**
     * Sets the current hud icons
     *
     * @param hudIcons The new hud icons
     */
    public static void setHudIcons(HudIcons hudIcons) {
        CarController.hudIcons = hudIcons;
    }

    protected final BaseVehicleEntity<?> entity;
    protected final EngineModule engine;

    @Getter
    @Setter
    private boolean accelerating, handbraking, reversing;
    @Getter
    @Setter
    private boolean turningLeft, turningRight, isEngineStarted;
    @Getter
    @Setter
    private float speedLimit;
    @Getter
    @Setter
    private byte onCooldown;

    /**
     * @param entity is assumed to implement {@link IModuleContainer.ISeatsContainer}
     */
    public CarController(BaseVehicleEntity<?> entity, EngineModule engine) {
        this.entity = entity;
        this.engine = engine;

        isEngineStarted = engine.isEngineStarted();
        handbraking = engine.isHandBraking();
        speedLimit = engine.getSpeedLimit();

        CameraSystem.setCameraZoom(entity.getPackInfo().getDefaultZoomLevel());

        while (car_brake.isPressed()) ;
        while (speedLimiter.isPressed()) ;
        while (car_engineOn.isPressed()) ;
        while (toggleLockDoor.isPressed()) ;
    }

    @Override
    public void update() {
        if (((IModuleContainer.ISeatsContainer) entity).getSeats().isLocalPlayerDriving() && engine.getEngineProperties() != null) {
            if (accelerating != MC.gameSettings.keyBindForward.isKeyDown()) {
                accelerating = MC.gameSettings.keyBindForward.isKeyDown();
            }
            if (reversing != MC.gameSettings.keyBindBack.isKeyDown()) {
                reversing = MC.gameSettings.keyBindBack.isKeyDown();
            }
            if (engine.getEngineProperties()[VehicleEntityProperties.EnumEngineProperties.ACTIVE_GEAR.ordinal()] == 0) //point mort
            {
                if (car_brake.isPressed()) {
                    handbraking = !handbraking;
                }
            } else if (handbraking != car_brake.isKeyDown()) {
                handbraking = car_brake.isKeyDown();
            }
            if (turningLeft != MC.gameSettings.keyBindLeft.isKeyDown() || turningRight != MC.gameSettings.keyBindRight.isKeyDown()) {
                turningLeft = MC.gameSettings.keyBindLeft.isKeyDown();
                turningRight = MC.gameSettings.keyBindRight.isKeyDown();
            }
            if (onCooldown > 0)
                onCooldown--;
            if (car_engineOn.isPressed()) {
                if (onCooldown == 0) {
                    isEngineStarted = !isEngineStarted;
                    onCooldown = 40;
                }
            }
            if (speedLimiter.isPressed()) {
                if (speedLimit == Float.MAX_VALUE)
                    speedLimit = Math.abs(engine.getEngineProperties()[0]);
                else
                    speedLimit = Float.MAX_VALUE;
            }

            if (toggleLockDoor.isPressed()) {
                if (onCooldown == 0) {
                    if (entity instanceof IModuleContainer.IDoorContainer && ((IModuleContainer.IDoorContainer) entity).getDoors() != null) {
                        PartSeat seat = ((IModuleContainer.ISeatsContainer) entity).getSeats().getRidingSeat(MC.player);
                        DoorsModule doors = ((IModuleContainer.IDoorContainer) entity).getDoors();
                        if (seat.getLinkedPartDoor(entity) == null)
                            return;
                        DynamXContext.getNetwork().sendToServer(new MessageChangeDoorState(entity, doors.getInverseCurrentState(seat.getLinkedPartDoor(entity).getId()), (byte) -1));
                    }
                    onCooldown = 30;
                }
            }

            MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.VehicleControllerUpdateEvent<>(entity, this));
            int controls = 0;
            if (accelerating)
                controls = controls | 1;
            if (handbraking)
                controls = controls | 2;
            if (reversing)
                controls = controls | 4;
            if (turningLeft)
                controls = controls | 8;
            if (turningRight)
                controls = controls | 16;
            if (isEngineStarted)
                controls = controls | 32;
            engine.setControls(controls);
            engine.setSpeedLimit(speedLimit);
        }
    }

    //HUD

    @Override
    @SideOnly(Side.CLIENT)
    public GuiComponent<?> createHud() {
        GuiPanel panel = new GuiPanel();
        float maxRpm = entity.getPackInfo().getSubPropertyByType(EngineInfo.class).getMaxRevs() + 3000; // todo CONFIGURABLE
        float scale = 90f / 300;
        GuiPanel speed = new SpeedometerPanel(this, scale, maxRpm);
        speed.setCssClass("speed_pane");
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
            speed.addTickListener(() -> {
                hudIcons.tick(icons);
            });
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
