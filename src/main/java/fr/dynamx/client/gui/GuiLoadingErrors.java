package fr.dynamx.client.gui;

import fr.aym.acsguis.api.ACsGuiApi;
import fr.aym.acsguis.component.GuiComponent;
import fr.aym.acsguis.component.layout.GuiScaler;
import fr.aym.acsguis.component.panel.GuiFrame;
import fr.aym.acsguis.component.panel.GuiPanel;
import fr.aym.acsguis.component.panel.GuiScrollPane;
import fr.aym.acsguis.component.textarea.GuiLabel;
import fr.aym.acsguis.component.textarea.GuiTextArea;
import fr.aym.acslib.api.services.ErrorTrackingService;
import fr.aym.acslib.impl.services.error_tracking.ACsLibErrorType;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.utils.DynamXLoadingTasks;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

import java.awt.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GuiLoadingErrors extends GuiFrame
{
    private final GuiPanel summary;
    private GuiComponent<?> displayed;

    public GuiLoadingErrors()
    {
        super(10, 10, 2000, 900, new GuiScaler.Identity());
        style.setBackgroundColor(Color.TRANSLUCENT);

        ScaledResolution r = new ScaledResolution(mc);

        int w = r.getScaledWidth()-20;
        summary = new GuiScrollPane(0, 0, w, r.getScaledHeight()-20);

        summary.add(new GuiLabel(0, 0, w, 20, "Errors while loading DynamX and the content packs").getStyle().setPaddingLeft(2).setPaddingTop(2).getOwner());
        summary.add(new GuiLabel(0, 22, w, 20, "Click on any category to view it, then press escape to go back").getStyle().setPaddingLeft(2).setPaddingTop(2).getOwner());
        int i = 2;
        for (Map.Entry<String, ErrorTrackingService.LocatedErrorList> d : DynamXContext.getErrorTracker().getAllErrors().entrySet()) {
            StringBuilder title = new StringBuilder(d.getValue().getColor() + d.getKey() + " : ");
            Collection<ErrorTrackingService.TrackedError> e = d.getValue().getErrors(ErrorTrackingService.TrackedErrorLevel.FATAL);
            if(!e.isEmpty())
                title.append(ErrorTrackingService.TrackedErrorLevel.FATAL.color.toString() + e.size()+" fatal error(s) ");
            e = d.getValue().getErrors(ErrorTrackingService.TrackedErrorLevel.HIGH);
            if(!e.isEmpty())
                title.append(ErrorTrackingService.TrackedErrorLevel.HIGH.color.toString() + e.size()+" error(s) ");
            e = d.getValue().getErrors(ErrorTrackingService.TrackedErrorLevel.LOW);
            if(!e.isEmpty())
                title.append(ErrorTrackingService.TrackedErrorLevel.LOW.color.toString() + e.size()+" warning(s) ");
            e = d.getValue().getErrors(ErrorTrackingService.TrackedErrorLevel.ADVICE);
            if(!e.isEmpty())
                title.append(ErrorTrackingService.TrackedErrorLevel.ADVICE.color.toString() + e.size()+" advice(s)");
            summary.add(new GuiLabel(0, i*22, w, 20, "+ " + title.toString()).getStyle().setPaddingLeft(2).setPaddingTop(2).getOwner().addClickListener((x, y, b) -> {
                remove(summary);

                GuiTextArea error = new GuiTextArea(0, 0, r.getScaledWidth() - 20, r.getScaledHeight() - 20);
                error.setMaxTextLength(Integer.MAX_VALUE);
                error.getStyle().setPaddingTop(4).setPaddingLeft(4);
                error.addResizeListener((w1, h) -> {
                    error.getStyle().getWidth().setAbsolute(w1 - 20);
                    error.getStyle().getHeight().setAbsolute(h - 20);
                });

                final StringBuilder text = new StringBuilder(TextFormatting.BLUE+"Errors in "+d.getKey()+" :" + "\n" + " \n");

                addErrors(d.getValue(), ACsLibErrorType.ACSLIBERROR, text);
                addErrors(d.getValue(), DynamXLoadingTasks.MAJS, text);
                addErrors(d.getValue(), DynamXLoadingTasks.INIT, text);
                addErrors(d.getValue(), DynamXLoadingTasks.PACK, text);
                addErrors(d.getValue(), DynamXLoadingTasks.MODEL, text);
                addErrors(d.getValue(), DynamXContext.getErrorTracker().findErrorType(new ResourceLocation(ACsGuiApi.RES_LOC_ID, "css")), text);

                text.append("\n \n"+TextFormatting.WHITE+"Press escape to go back");

                displayed = error;
                error.setFocused(true);
                error.setText(text.toString());
                error.setEditable(false);
                add(error.getStyle().setForegroundColor(0x88FF88).setBackgroundColor(0xDD222222).getOwner());
            }));
            i++;
        }

        summary.setFocused(true);
        add(summary.getStyle().setForegroundColor(0x88FF88).setBackgroundColor(0xBD222222).getOwner());
        getStyle().getWidth().setAbsolute(r.getScaledWidth()-20);
        getStyle().getHeight().setAbsolute(r.getScaledHeight()-20);
        addResizeListener((w1, h) -> {getStyle().getWidth().setAbsolute(w1-20); getStyle().getHeight().setAbsolute(h-20);});
    }

    private void addErrors(ErrorTrackingService.LocatedErrorList d, ErrorTrackingService.ErrorType t, StringBuilder text)
    {
        Collection<ErrorTrackingService.TrackedError> errors = d.getErrors(t);
        if(!errors.isEmpty())
        {
            text.append(TextFormatting.GRAY+"- Type : "+t.getLabel().toLowerCase()+". \n");
            errors.forEach(s -> text.append(s+" \n \n"));
        }
    }

    @Override
    public void onKeyTyped(char typedChar, int keyCode) {
        if(keyCode == 1) {
            if(displayed != null) {
                remove(displayed);
                add(summary);
                summary.setFocused(true);
                displayed = null;
            }
            else if(mc.world != null)
                mc.displayGuiScreen(new GuiDnxDebug().getGuiScreen());
            else
                mc.displayGuiScreen(null);
        }
        else
            super.onKeyTyped(typedChar, keyCode);
    }

    @Override
    public List<ResourceLocation> getCssStyles() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public boolean usesDefaultStyle() {
        return false;
    }
}
