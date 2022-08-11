package fr.dynamx.utils.errors;

import fr.aym.acslib.ACsLib;
import fr.aym.acslib.api.services.ErrorManagerService;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.util.ResourceLocation;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DynamXErrorManager {
    private static final ErrorManagerService errorManager = ACsLib.getPlatform().provideService(ErrorManagerService.class);

    public static final ErrorManagerService.ErrorCategory DYNAMX_ERRORS = errorManager.createErrorCategory(new ResourceLocation(DynamXConstants.ID, "general_errors"), "DynamX errors");

    /**
     * The current {@link ErrorManagerService} instance
     */
    public static ErrorManagerService getErrorManager() {
        return errorManager;
    }

    public static void addError(String pack, String genericType, ErrorManagerService.ErrorLevel errorLevel, String object, String message) {
        errorManager.addError(pack, DYNAMX_ERRORS, genericType, errorLevel, object, message);
    }

    public static void addError(String pack, String genericType, ErrorManagerService.ErrorLevel errorLevel, String object, String message, Exception exception) {
        errorManager.addError(pack, DYNAMX_ERRORS, genericType, errorLevel, object, message, exception);
    }

    public static void addError(String pack, String genericType, ErrorManagerService.ErrorLevel errorLevel, String object, String message, Exception exception, int priority) {
        errorManager.addError(pack, DYNAMX_ERRORS, genericType, errorLevel, object, message, exception, priority);
    }

    private static final Comparator<ErrorManagerService.ErrorData> ERROR_COMPARATOR = (e1, e2) -> {
        if (e1.getPriority() != e2.getPriority()) return e2.getPriority() - e1.getPriority();
        return e2.getLevel().ordinal() - e1.getLevel().ordinal();
    };

    private static Map<String, List<ErrorManagerService.ErrorData>> groupBy(Collection<ErrorManagerService.ErrorData> errors, Function<ErrorManagerService.ErrorData, String> property) {
        Map<String, List<ErrorManagerService.ErrorData>> groupedErrors = new LinkedHashMap<>();
        Stream<ErrorManagerService.ErrorData> sorted = errors.stream().sorted(ERROR_COMPARATOR);
        sorted.forEachOrdered(errorData -> {
            String prop = property.apply(errorData);
            if (!groupedErrors.containsKey(prop))
                groupedErrors.put(prop, new ArrayList<>());
            groupedErrors.get(prop).add(errorData);
        });
        return groupedErrors;
    }

    public static void printErrors(ErrorManagerService.ErrorLevel minLevel) {
        Map<ResourceLocation, ErrorManagerService.LocatedErrorList> allErrors = DynamXErrorManager.getErrorManager().getAllErrors();
        if (!allErrors.isEmpty()) {
            allErrors.forEach(((s, locatedErrorList) -> {
                DynamXMain.log.error("------------- " + s + " -------------");
                StringBuilder errorBuilder = new StringBuilder();
                List<ErrorManagerService.ErrorData> errorList = locatedErrorList.getErrors().stream().filter(e -> e.getCategory() == DynamXErrorManager.DYNAMX_ERRORS && e.getLevel().ordinal() >= minLevel.ordinal()).collect(Collectors.toList());
                Map<String, List<ErrorManagerService.ErrorData>> errorsPerObject = groupBy(errorList, ErrorManagerService.ErrorData::getObject);
                errorsPerObject.forEach(((s1, errorData) -> {
                    String title = "In " + s + "/" + s1;
                    errorBuilder.append("\n").append(title).append("\n");
                    for (int i = 0; i < title.length(); i++) {
                        errorBuilder.append('-');
                    }
                    Map<String, List<ErrorManagerService.ErrorData>> errorsPerType = groupBy(errorData, ErrorManagerService.ErrorData::getGenericType);
                    errorsPerType.forEach(((s2, errorData2) -> {
                        errorData2.sort(ERROR_COMPARATOR);
                        errorBuilder.append("\n");
                        errorBuilder.append("==> Level: ").append(errorData2.get(0).getLevel()).append("\n");
                        DynamXErrorRegistry.ErrorFormatter formatter = DynamXErrorRegistry.getErrorFormatter(s2);
                        formatter.formatError(errorBuilder, true, errorData2);
                    }));
                }));
                DynamXMain.log.info(errorBuilder.toString());
            }));
        }
    }
}
