package fr.dynamx.client.gui;

import fr.aym.acsguis.api.GuiAPIClientHelper;
import fr.aym.acsguis.component.GuiComponent;
import fr.aym.acsguis.component.layout.GridLayout;
import fr.aym.acsguis.component.layout.GuiScaler;
import fr.aym.acsguis.component.panel.GuiFrame;
import fr.aym.acsguis.component.panel.GuiPanel;
import fr.aym.acsguis.component.panel.GuiScrollPane;
import fr.aym.acsguis.component.style.ComponentStyleManager;
import fr.aym.acsguis.component.textarea.GuiLabel;
import fr.aym.acsguis.component.textarea.GuiSearchField;
import fr.aym.acsguis.component.textarea.GuiTextArea;
import fr.aym.acslib.api.services.error.*;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.errors.DynamXErrorManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


//TODO CLEAN
public class GuiLoadingErrors extends GuiFrame {
    public static final ResourceLocation STYLE = new ResourceLocation(DynamXConstants.ID, "css/loading_errors.css");

    private final GuiPanel summary;
    private GuiComponent<?> displayed;

    public GuiLoadingErrors() {
        super(new GuiScaler.Identity());
        setCssId("root");

        summary = new GuiScrollPane() {
            @Override
            public List<GuiComponent> getReversedChildComponents() {
                List<GuiComponent> list = super.getReversedChildComponents();
                list.sort(GuiComponent::compareTo);
                Collections.reverse(list);
                return list;
            }
        };
        summary.setLayout(new GridLayout(-1, 20, 2, GridLayout.GridDirection.HORIZONTAL, 1));
        fillSummary(null);
        summary.setFocused(true);
        add(summary);
        DynamXErrorManager.printErrors(ErrorLevel.ADVICE);
    }

    private void fillSummary(String filter) {
        summary.removeAllChilds();
        summary.add(new GuiLabel(TextFormatting.DARK_AQUA + "Errors while loading DynamX and the content packs").getStyle().setPaddingLeft(2).setPaddingTop(2).getOwner());
        summary.add(new GuiLabel(TextFormatting.GRAY + "Click on any category to view it, press escape to go back").getStyle().setPaddingLeft(2).setPaddingTop(2).getOwner());

        Map<ResourceLocation, LocatedErrorList> allErrors = DynamXErrorManager.getErrorManager().getAllErrors();
        if(filter != null)
            allErrors = allErrors.entrySet().stream().filter(entry ->
                    entry.getValue().getErrors().stream().anyMatch(error -> (entry.getKey() + "/" + error.getObject()).equals(filter))
            ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!allErrors.isEmpty()) {
            /* todo search field
            List<String> names = new ArrayList<>();
            allErrors.forEach((location, errorList) -> {
                errorList.getErrors().forEach(errorData -> names.add(location + "/" + errorData.getObject()));
            });
            GuiSearchField2 field = new GuiSearchField2(this::fillSummary, 14, 10) {
                @Override
                public List<String> generateAvailableNames() {
                    return names.stream().distinct().collect(Collectors.toList());
                }
            };
            field.setCssId("search_field");
            summary.add(field);*/

            allErrors.entrySet().stream().sorted(Comparator.comparingInt(e -> -e.getValue().getHighestErrorLevel().ordinal())).forEachOrdered(entry -> {
                LocatedErrorList locatedErrorList = entry.getValue();
                StringBuilder title = new StringBuilder(locatedErrorList.getHighestErrorLevel().color.toString() + entry.getKey() + " : ");
                long fatals = locatedErrorList.getErrors().stream().filter(er -> er.getLevel() == ErrorLevel.FATAL).count();
                if (fatals > 0)
                    title.append(ErrorLevel.FATAL.color.toString()).append(fatals).append(" fatal error(s) ");
                fatals = locatedErrorList.getErrors().stream().filter(er -> er.getLevel() == ErrorLevel.HIGH).count();
                if (fatals > 0)
                    title.append(ErrorLevel.HIGH.color.toString()).append(fatals).append(" error(s) ");
                fatals = locatedErrorList.getErrors().stream().filter(er -> er.getLevel() == ErrorLevel.LOW).count();
                if (fatals > 0)
                    title.append(ErrorLevel.LOW.color.toString()).append(fatals).append(" warning(s) ");
                fatals = locatedErrorList.getErrors().stream().filter(er -> er.getLevel() == ErrorLevel.ADVICE).count();
                if (fatals > 0)
                    title.append(ErrorLevel.ADVICE.color.toString()).append(fatals).append(" advice(s)");
                summary.add(new GuiLabel("+ " + title).getStyle().setPaddingLeft(2).setPaddingTop(2).getOwner().addClickListener((x, y, b) ->
                        showErrors(false, new ArrayList<>(), entry.getKey(), locatedErrorList)
                ));
            });
        } else {
            summary.add(new GuiLabel("No error found here"));
        }
    }

    private int setDeployed(GuiTextArea label, ResourceLocation location, String object, List<ErrorData> errorListPerObject, boolean deploy) {
        label.setMaxTextLength(50000);
        StringBuilder title = new StringBuilder(object + " : ");
        int height = 20;
        if (deploy) {
            final StringBuilder text = new StringBuilder("- " + title + "\n");
            int length = text.length();
            for (int j = 0; j < length - 3; j++) {
                text.append('-');
            }
            text.append('\n');
            Map<String, List<ErrorData>> errorsPerType = ErrorManagerService.groupBy(errorListPerObject, ErrorData::getGenericType);
            errorsPerType.forEach(((s2, errorListPerType) -> {
                errorListPerType.sort(ErrorManagerService.ERROR_COMPARATOR);
                ErrorData errorType = errorListPerType.get(0);
                text.append("\n");
                text.append(errorType.getLevel().color).append("==> Level: ").append(errorType.getLevel()).append("\n").append(TextFormatting.LIGHT_PURPLE);
                ErrorFormatter formatter = errorType.getCategory().getErrorFormatter(s2);
                formatter.formatError(text, false, errorListPerType);
            }));
            label.setText(text.toString());
            height = mc.fontRenderer.FONT_HEIGHT * GuiAPIClientHelper.trimTextToWidth(text.toString(), getWidth()).size() + 10;
        } else {
            long fatals = errorListPerObject.stream().filter(er -> er.getLevel() == ErrorLevel.FATAL).count();
            if (fatals > 0)
                title.append(ErrorLevel.FATAL.color.toString()).append(fatals).append(" fatal error(s) ");
            fatals = errorListPerObject.stream().filter(er -> er.getLevel() == ErrorLevel.HIGH).count();
            if (fatals > 0)
                title.append(ErrorLevel.HIGH.color.toString()).append(fatals).append(" error(s) ");
            fatals = errorListPerObject.stream().filter(er -> er.getLevel() == ErrorLevel.LOW).count();
            if (fatals > 0)
                title.append(ErrorLevel.LOW.color.toString()).append(fatals).append(" warning(s) ");
            fatals = errorListPerObject.stream().filter(er -> er.getLevel() == ErrorLevel.ADVICE).count();
            if (fatals > 0)
                title.append(ErrorLevel.ADVICE.color.toString()).append(fatals).append(" advice(s)");
            label.setText("+ " + TextFormatting.GREEN + title);
        }
        return height;
    }

    private void showErrors(boolean deployAll, List<Integer> deployObjs, ResourceLocation location, LocatedErrorList locatedErrorList) {
        if (displayed != null)
            remove(displayed);
        else
            remove(summary);
        GuiPanel errorsPanel = new GuiScrollPane();
        Map<GuiComponent<?>, Integer> heightMap = new HashMap<>();
        errorsPanel.setLayout(new GridLayout(-1, 20, 2, GridLayout.GridDirection.HORIZONTAL, 1) {
            private final Map<ComponentStyleManager, Integer> seenElements = new HashMap<>();
            private ComponentStyleManager lastElement;

            @Override
            public int getY(ComponentStyleManager target) {
                if(!seenElements.containsKey(target)) {
                    if(lastElement != null)
                        seenElements.put(target, (int) (lastElement.getYPos().getRawValue() + lastElement.getHeight().getRawValue() + 2));
                    else
                        seenElements.put(target, 0);
                    lastElement = target;
                }
                return seenElements.get(target);
            }

            @Override
            public int getHeight(ComponentStyleManager target) {
                return heightMap.containsKey(target.getOwner()) ? heightMap.get(target.getOwner()) : super.getHeight(target);
            }

            @Override
            public void clear() {
                super.clear();
                seenElements.clear();
                lastElement = null;
            }
        });
        errorsPanel.add(new GuiLabel(TextFormatting.DARK_AQUA + "Errors while loading " + location).getStyle().setPaddingLeft(2).setPaddingTop(2).getOwner());
        errorsPanel.add(new GuiLabel(TextFormatting.GRAY + "Click on any category to view it, press escape to go back").getStyle().setPaddingLeft(2).setPaddingTop(2).getOwner());
        errorsPanel.add(new GuiLabel(TextFormatting.GRAY + "  -> Show all").addClickListener((x, y, b) ->
            showErrors(!deployAll, new ArrayList<>(), location, locatedErrorList)
        ));

        AtomicInteger i = new AtomicInteger();
        Collection<ErrorData> errorList = locatedErrorList.getErrors();
        Map<String, List<ErrorData>> errorsPerObject = ErrorManagerService.groupBy(errorList, ErrorData::getObject);
        errorsPerObject.forEach((s1, errorListPerObject) -> {
            int id = i.get();
            GuiTextArea label = new GuiTextArea();
            label.setEditable(false);
            heightMap.put(label, setDeployed(label, location, s1, errorListPerObject, deployAll != deployObjs.contains(id)));
            errorsPanel.add(label.getStyle().setPaddingLeft(2).setPaddingTop(2).getOwner().addClickListener((x, y, b) -> {
                if (deployObjs.contains(id))
                    deployObjs.remove((Integer) id);
                else
                    deployObjs.add(id);
                heightMap.put(label, setDeployed(label, location, s1, errorListPerObject, deployAll != deployObjs.contains(id)));
                errorsPanel.getLayout().clear();
                errorsPanel.getStyle().refreshCss(false, "layout change");
                //showErrors(deployAll, deployObjs, location, locatedErrorList);
            }));
            i.getAndIncrement();
        });
        errorsPanel.add(new GuiLabel(TextFormatting.DARK_AQUA + "  <- Go back").addClickListener((x, y, b) ->
            goBack()
        ));
        displayed = errorsPanel;
        errorsPanel.setFocused(true);
        add(errorsPanel.getStyle().setForegroundColor(0x88FF88).setBackgroundColor(0xDD222222).getOwner());
    }

    @Override
    public void onKeyTyped(char typedChar, int keyCode) {
        if (keyCode == 1) {
            if (displayed != null) {
                goBack();
            } else if (mc.world != null)
                mc.displayGuiScreen(new GuiDnxDebug().getGuiScreen());
            else
                mc.displayGuiScreen(null);
        } else
            super.onKeyTyped(typedChar, keyCode);
    }

    private void goBack() {
        remove(displayed);
        add(summary);
        summary.setFocused(true);
        displayed = null;
    }

    @Override
    public List<ResourceLocation> getCssStyles() {
        return Collections.singletonList(STYLE);
    }

    @Override
    public boolean usesDefaultStyle() {
        return false;
    }

    @Override
    public boolean needsCssReload() {
        return false;
    }
}
