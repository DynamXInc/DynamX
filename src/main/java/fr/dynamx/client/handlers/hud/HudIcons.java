package fr.dynamx.client.handlers.hud;

import fr.aym.acsguis.component.GuiComponent;

/**
 * Api : May change <br>
 * Allows displaying custom icons on the hud speedometer, like the BasicsAddon <br>
 * Only one addon can set on custom hud icon, with {@link CarController}.setHudIcons(..)
 */
public interface HudIcons {
    /**
     * @return The number of custom icons to create
     */
    int iconCount();

    /**
     * Fired when creating the icons with the given id <br>
     * The component automatically haves the css id "icon_componentId" and the css class "hud_icon" <br>
     * You should set the styles you want in your css file to place them on the hud
     *
     * @param componentId The id of the component
     * @param component   The component added
     */
    void initIcon(int componentId, GuiComponent<?> component);

    /**
     * Fired when ticking the hud
     *
     * @param components The custom icons
     */
    void tick(GuiComponent<?>[] components);

    /**
     * Fired at each render frame to check if the component should be visible or not
     *
     * @param componentId The component id
     * @return True to render the component
     */
    boolean isVisible(int componentId);
}
