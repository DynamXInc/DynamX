package fr.dynamx.client.gui;

import fr.aym.acsguis.api.ACsGuiApi;
import fr.aym.acsguis.component.button.GuiButton;
import fr.aym.acsguis.component.layout.GridLayout;
import fr.aym.acsguis.component.layout.GuiScaler;
import fr.aym.acsguis.component.panel.GuiFrame;
import fr.aym.acsguis.component.panel.GuiPanel;
import fr.aym.acsguis.component.panel.GuiScrollPane;
import fr.aym.acsguis.component.panel.GuiTabbedPane;
import fr.aym.acsguis.component.textarea.GuiLabel;
import fr.aym.acsguis.utils.GuiCssError;
import fr.dynamx.client.shaders.ShaderManager;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.errors.DynamXErrorManager;
import fr.dynamx.utils.DynamXLoadingTasks;
import fr.dynamx.utils.debug.DynamXDebugOption;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiDnxDebug extends GuiFrame {
    public static final ResourceLocation STYLE = new ResourceLocation(DynamXConstants.ID, "css/dnx_debug.css");

    public GuiDnxDebug() {
        super(new GuiScaler.Identity());

        style.setBackgroundColor(Color.TRANSLUCENT);
        setCssClass("home");
        GuiTabbedPane pane = new GuiTabbedPane();

        GuiPanel general = new GuiPanel();
        general.setCssId("general");
        general.add(new GuiLabel(50, 50, 0, 0, "DynamX debug - general").setCssClass("title"));
        //Options :
        {
            GuiPanel pane1 = generateDebugCategory(DynamXDebugOptions.DebugCategories.GENERAL);

            GuiLabel shaderBox = new GuiLabel("Reload shaders");
            shaderBox.setCssId("reload_shaders").setCssClass("reload_button");
            shaderBox.addClickListener((x, y, bu) -> {
                ShaderManager.init(Minecraft.getMinecraft().getResourceManager());
            });
            pane1.add(shaderBox);

            GuiLabel box = new GuiLabel("Reload packs");
            box.setCssId("reload_packs").setCssClass("reload_button");
            box.addClickListener((x, y, bu) -> {
                box.setEnabled(false);
                box.setText("Reloading...");
                DynamXLoadingTasks.reload(DynamXLoadingTasks.TaskContext.CLIENT, DynamXLoadingTasks.PACK).thenAccept(empty -> {
                    box.setEnabled(true);
                    if (DynamXErrorManager.getErrorManager().hasErrors(DynamXErrorManager.INIT_ERRORS, DynamXErrorManager.PACKS_ERRORS))
                        box.setText(TextFormatting.RED + "Some packs have errors");
                    else
                        box.setText("Packs reloaded");
                });
            });
            pane1.add(box);
            GuiLabel box2 = new GuiLabel("Reload models");
            box2.setCssId("reload_models").setCssClass("reload_button");
            box2.addClickListener((x, y, bu) -> {
                //mc.debugFeedbackTranslated("debug.reload_resourcepacks.message");
                box2.setEnabled(false);
                box2.setText("Reloading...");
                DynamXLoadingTasks.reload(DynamXLoadingTasks.TaskContext.CLIENT, DynamXLoadingTasks.MODEL).thenAccept(empty -> {
                    box2.setEnabled(true);
                    if (DynamXErrorManager.getErrorManager().hasErrors(DynamXErrorManager.MODEL_ERRORS))
                        box2.setText(TextFormatting.RED + "Some models have problems");
                    else
                        box2.setText("Models reloaded");
                });
            });
            pane1.add(box2);
            GuiLabel box3 = new GuiLabel("Reload css styles");
            box3.setCssId("reload_css").setCssClass("reload_button");
            box3.addClickListener((x, y, bu) -> {
                box3.setEnabled(false);
                box3.setText("Reloading...");
                DynamXLoadingTasks.reload(DynamXLoadingTasks.TaskContext.CLIENT, DynamXLoadingTasks.CSS).thenAccept(empty -> {
                    box3.setEnabled(true);
                    if (DynamXErrorManager.getErrorManager().hasErrors(ACsGuiApi.getCssErrorType()))
                        box3.setText(TextFormatting.RED + "Some css styles have errors");
                    else
                        box3.setText("Css styles reloaded");
                });
            });
            pane1.add(box3);
            GuiLabel box4 = new GuiLabel("Reload all");
            box4.setCssId("reload_all").setCssClass("reload_button");
            box4.addClickListener((x, y, bu) -> {
                box4.setEnabled(false);
                box4.setText("Reloading...");
                DynamXLoadingTasks.reload(DynamXLoadingTasks.TaskContext.CLIENT, DynamXLoadingTasks.PACK, DynamXLoadingTasks.MODEL, DynamXLoadingTasks.CSS).thenAccept(empty -> {
                    box4.setEnabled(true);
                    if (DynamXErrorManager.getErrorManager().hasErrors(DynamXErrorManager.INIT_ERRORS, DynamXErrorManager.PACKS_ERRORS, DynamXErrorManager.MODEL_ERRORS, ACsGuiApi.getCssErrorType()))
                        box4.setText(TextFormatting.RED + "Check the errors menu");
                    else
                        box4.setText("Reloading finished");
                });
            });
            pane1.add(box4);

            general.add(pane1);
        }
        pane.addTab("General", general);

        general = new GuiPanel();
        general.setCssId("terrain");
        general.add(new GuiLabel("DynamX debug - terrain").setCssClass("title"));
        general.add(generateDebugCategory(DynamXDebugOptions.DebugCategories.TERRAIN));
        pane.addTab("Terrain debug", general);

        general = new GuiPanel();
        general.add(new GuiLabel("DynamX debug - vehicles").setCssClass("title"));
        general.setCssId("vehicles");
        general.add(generateDebugCategory(DynamXDebugOptions.DebugCategories.VEHICLES));
        pane.addTab("Vehicles debug", general);

        general = new GuiPanel();
        general.setCssId("loadinglog");
        pane.addTab(TextFormatting.GOLD + "Errors", general);
        pane.getTabButton(3).addClickListener((mx, my, button) -> {
            if (button == 0)
                ACsGuiApi.asyncLoadThenShowGui("LoadingErrors", GuiLoadingErrors::new);
        });

        general = new GuiPanel();
        general.setCssId("showcsslog");
        pane.addTab("Log CSS", general);
        pane.getTabButton(4).addClickListener((mx, my, button) -> {
            if (button == 0)
                Minecraft.getMinecraft().addScheduledTask(() -> Minecraft.getMinecraft().displayGuiScreen(new GuiCssError().getGuiScreen()));
        });

        pane.selectTab(0);
        add(pane);

        add(new GuiLabel("Fully designed in CSS with ACsGuis").setCssId("credits"));
    }

    @Override
    public List<ResourceLocation> getCssStyles() {
        return Collections.singletonList(STYLE);
    }

    @Override
    public boolean doesPauseGame() {
        return false;
    }

    @Override
    public boolean needsCssReload() {
        return false;
    }

    protected GuiPanel generateDebugCategory(DynamXDebugOptions.DebugCategories category) {
        String subCategory = null;
        {
            Map<DynamXDebugOption, GuiButton> terrainButtons = new HashMap<>();
            Map<DynamXDebugOption, GuiLabel> terrainLabels = new HashMap<>();
            GuiScrollPane pane1 = new GuiScrollPane();
            pane1.setLayout(new GridLayout(-1, 25, 0, GridLayout.GridDirection.HORIZONTAL, 1));
            for (DynamXDebugOption option : category.getOptions()) {
                if (option.getSubCategory() != null && !option.getSubCategory().equals(subCategory)) {
                    subCategory = option.getSubCategory();
                    GuiLabel label1 = new GuiLabel(subCategory + " :");
                    pane1.add(label1.setCssClass("option-subcategory"));
                }

                GuiPanel line = new GuiPanel();
                line.setCssClass("option-desc");
                boolean active = option.isActive();
                GuiLabel label1 = new GuiLabel("Debug " + option.getDisplayName() + " : " + (active ? "Enabled" : "Disabled"));
                if (option.getDescription() != null)
                    label1.setHoveringText(Collections.singletonList(option.getDescription()));
                terrainLabels.put(option, label1);
                line.add(label1);

                GuiButton b1 = new GuiButton();
                terrainButtons.put(option, b1);
                line.add(b1.setCssClass("switch-button-" + (active ? "on" : "off")).addClickListener((mx, my, button) -> {
                    if (option.isActive())
                        option.disable();
                    else
                        option.enable();
                    for (DynamXDebugOption noption : category.getOptions()) {
                        boolean nactive = noption.isActive();
                        terrainButtons.get(noption).setCssClass("switch-button-" + (nactive ? "on" : "off"));
                        terrainLabels.get(noption).setText("Debug " + noption.getDisplayName() + " : " + (nactive ? "Enabled" : "Disabled"));
                    }
                }));
                pane1.add(line);
            }
            return pane1;
        }
    }
}
