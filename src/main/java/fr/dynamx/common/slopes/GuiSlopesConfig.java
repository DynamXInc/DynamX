package fr.dynamx.common.slopes;

import fr.aym.acsguis.component.button.GuiButton;
import fr.aym.acsguis.component.button.GuiCheckBox;
import fr.aym.acsguis.component.layout.GridLayout;
import fr.aym.acsguis.component.layout.GuiScaler;
import fr.aym.acsguis.component.panel.GuiFrame;
import fr.aym.acsguis.component.panel.GuiScrollPane;
import fr.aym.acsguis.component.textarea.GuiLabel;
import fr.aym.acsguis.component.textarea.GuiSearchField;
import fr.aym.acsguis.event.listeners.mouse.IMouseClickListener;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.network.packets.MessageSlopesConfigGui;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GuiSlopesConfig extends GuiFrame {
    private final IMouseClickListener exitButton;
    private boolean cancelled;

    public GuiSlopesConfig(ItemStack stack) {
        super(new GuiScaler.Identity());
        setCssId("root");
        SlopeBuildingConfig config = stack.hasTagCompound() ? new SlopeBuildingConfig(stack.getTagCompound().getCompoundTag("ptconfig")) :
                new SlopeBuildingConfig();

        add(new GuiLabel(I18n.format("slopes.config.title")).setCssId("main_title"));

        GuiCheckBox b;
        add((b = new GuiCheckBox(I18n.format("slopes.config.dalls"))).setChecked(!config.isEnableSlabs()).setCssId("slopes"));

        GuiCheckBox b2;
        add((b2 = new GuiCheckBox(I18n.format("slopes.config.diags"))).setChecked(config.getDiagDir() == -1).setCssId("diagDir"));

        EnumFacing current = mc.player.getHorizontalFacing();
        add(new GuiLabel(I18n.format("slopes.config.orientation", current.getName())).setCssId("setfacing").addClickListener((x, y, bu) -> {
            config.setFacing(current);
        }));

        add(new GuiLabel(I18n.format("slopes.config.blocks")).setCssId("blacklist_title"));
        GuiSearchField text;
        GuiLabel lab;
        GuiScrollPane pane;
        add((pane = new GuiScrollPane()).setCssId("blacklist"));
        add((text = new GuiSearchField(10, 20) {
            @Override
            public List<String> generateAvailableNames() {
                List<String> list = new ArrayList<>();
                for (ResourceLocation l : Block.REGISTRY.getKeys())
                    list.add(l.toString());
                return list;
            }
        }).setCssId("blacklist_bar"));
        add((lab = new GuiLabel(I18n.format("slopes.config.add"))).setCssId("blacklist_add").addClickListener((x, y, bt) -> {
            ResourceLocation loc = new ResourceLocation(text.getText());
            if (Block.REGISTRY.containsKey(loc)) {
                config.getBlackList().add(Block.REGISTRY.getObject(loc));
                setupBlacklist(config, pane);
            } else
                lab.setText(TextFormatting.RED + I18n.format("slopes.config.blocknotfound"));
        }));

        pane.setLayout(new GridLayout(-1, 10, 1, GridLayout.GridDirection.HORIZONTAL, 1));
        setupBlacklist(config, pane);

        add((new GuiButton(I18n.format("slopes.config.cancel"))).setCssId("quit").addClickListener((x, y, bu) -> {
            cancelled = true;
            Minecraft.getMinecraft().displayGuiScreen(null);
        }));
        add(new GuiButton(I18n.format("slopes.config.apply")).setCssId("refresh").addClickListener(exitButton = (x, y, bu) -> {
            config.setEnableSlabs(!b.isChecked());
            config.setDiagDir(b2.isChecked() ? -1 : 1);
            DynamXContext.getNetwork().sendToServer(new MessageSlopesConfigGui(config.serialize()));
        }));
        setPauseGame(false);
    }

    private void setupBlacklist(SlopeBuildingConfig config, GuiScrollPane pane) {
        pane.removeAllChilds();
        for (Block bo : config.getBlackList()) {
            ResourceLocation bl = bo.getRegistryName();
            pane.add(new GuiLabel(bl.toString()).setCssClass("blacklist_block").addClickListener((x, y, bt) -> {
                config.getBlackList().remove(bo);
                setupBlacklist(config, pane);
            }));
        }
    }

    @Override
    public void guiClose() {
        if (!cancelled) {
            exitButton.onMouseClicked(0, 0, 0);
        }
        super.guiClose();
    }

    @Override
    public List<ResourceLocation> getCssStyles() {
        return Collections.singletonList(new ResourceLocation(DynamXConstants.ID, "css/slope_generator.css"));
    }
}
