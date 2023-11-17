package fr.dynamx.utils.errors;

import fr.aym.acsguis.api.ACsGuiApi;
import fr.aym.acslib.ACsLib;
import fr.aym.acslib.api.services.error.*;
import fr.aym.mps.ModProtectionSystem;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Arrays;

public class DynamXErrorManager {
    private static final ErrorManagerService errorManager = ACsLib.getPlatform().provideService(ErrorManagerService.class);

    public static final ErrorCategory INIT_ERRORS = errorManager.createErrorCategory(new ResourceLocation(DynamXConstants.ID, "general_errors"), "DynamX errors");
    /**
     * PACK error type : notifies of pack loading errors
     */
    public static final ErrorCategory PACKS_ERRORS = errorManager.createErrorCategory(new ResourceLocation(DynamXConstants.ID, "general_errors"), "DynamX errors");
    /**
     * MODEL error type : notifies of model loading errors
     */
    public static final ErrorCategory MODEL_ERRORS = errorManager.createErrorCategory(new ResourceLocation(DynamXConstants.ID, "general_errors"), "DynamX errors");
    /**
     * UPDATES error type : notifies of DynamX updates
     */
    public static ErrorCategory UPDATES = getErrorManager().createErrorCategory(new ResourceLocation(DynamXConstants.ID, "majs"), "Updates");

    /**
     * The current {@link ErrorManagerService} instance
     */
    public static ErrorManagerService getErrorManager() {
        return errorManager;
    }


    public static void addPackError(String pack, String genericType, ErrorLevel errorLevel, String object, String message) {
        addError(pack, PACKS_ERRORS, genericType, errorLevel, object, message);
    }

    public static void addError(String pack, ErrorCategory errorCategory, String genericType, ErrorLevel errorLevel, String object, String message) {
        errorManager.addError(pack, errorCategory, genericType, errorLevel, object, message);
    }

    public static void addError(String pack, ErrorCategory errorCategory, String genericType, ErrorLevel errorLevel, String object, String message, Exception exception) {
        errorManager.addError(pack, errorCategory, genericType, errorLevel, object, message, exception);
    }

    public static void addError(String pack, ErrorCategory errorCategory, String genericType, ErrorLevel errorLevel, String object, String message, Exception exception, int priority) {
        errorManager.addError(pack, errorCategory, genericType, errorLevel, object, message, exception, priority);
    }

    public static void printErrors(Side side, ErrorLevel minLevel) {
        if(errorManager.getAllErrors().values().stream().anyMatch(e -> e.getHighestErrorLevel().ordinal() >= minLevel.ordinal())) {
            DynamXMain.log.error("==== DynamX loading errors ====");
            if(side.isClient())
                errorManager.printErrors(DynamXMain.log, Arrays.asList(INIT_ERRORS, PACKS_ERRORS, MODEL_ERRORS, ACsLib.getPlatform().getACsLibErrorCategory(), ACsGuiApi.getCssErrorType(), ModProtectionSystem.getMpsErrorCategory()), minLevel);
            else
                errorManager.printErrors(DynamXMain.log, Arrays.asList(INIT_ERRORS, PACKS_ERRORS, MODEL_ERRORS, ACsLib.getPlatform().getACsLibErrorCategory(), ModProtectionSystem.getMpsErrorCategory()), minLevel);
        }
    }

    public static void registerErrorFormatter(ErrorCategory category, String genericType, ErrorFormatter errorFormatter) {
        category.registerErrorFormatter(genericType, errorFormatter);
    }

    private static String getErrorName(ErrorData errorData, boolean addStackTrace) {
       String translated = I18n.translateToLocal("dynamx.error."+errorData.getGenericType());
       return addStackTrace ? (translated + " (" + errorData.getGenericType() +")") : translated;
    }

    public static final ErrorFormatter FORMATTER_SINGLE_ERROR = (errorBuilder, addStackTrace, errors) -> {
        for(ErrorData error : errors) {
            if(error.getMessage() != null)
                errorBuilder.append("- ").append(getErrorName(error, addStackTrace)).append(" : ").append("\n").append(error.getMessage()).append("\n");
            else
                errorBuilder.append("- ").append(getErrorName(error, addStackTrace)).append(" : ").append("\n");
            if(error.getException() != null) {
                errorBuilder.append(error.getException().toString()).append("\n");
                if(addStackTrace) {
                    Throwable parent = error.getException();
                    while (parent != null) {
                        StackTraceElement[] stackTrace = parent.getStackTrace();
                        for (int i = 0; i < Math.min(stackTrace.length, 10); i++) {
                            errorBuilder.append(stackTrace[i].toString()).append("\n");
                        }
                        parent = parent.getCause();
                        if(parent != null)
                            errorBuilder.append("Caused by: ").append(parent).append("\n");
                    }
                }
            }
        }
    };
    public static final ErrorFormatter FORMATTER_MULTIPLE_ERROR_ONE_LI = (errorBuilder, addStackTrace, errors) -> {
        errorBuilder.append("- ").append(getErrorName(errors.get(0), addStackTrace)).append(" : ").append("\n");
        errorBuilder.append(errors.stream().map(ErrorData::getMessage).reduce((a, b) -> a + ", " + b).orElse("reduce error"));
        errorBuilder.append("\n");
    };
    public static final ErrorFormatter FORMATTER_MULTIPLE_ERROR = (errorBuilder, addStackTrace, errors) -> {
        errorBuilder.append("- ").append(getErrorName(errors.get(0), addStackTrace)).append(" : ").append("\n");
        if(errors.size() > 1) {
            for (ErrorData error : errors) {
                if (error.getMessage() != null)
                    errorBuilder.append("-> ").append(error.getMessage()).append("\n");
            }
        } else if(errors.get(0).getMessage() != null)
            errorBuilder.append(errors.get(0).getMessage()).append("\n");
    };

    static {
        registerErrorFormatter(INIT_ERRORS, "mps_error", FORMATTER_SINGLE_ERROR);
        registerErrorFormatter(INIT_ERRORS, "loading_tasks", FORMATTER_SINGLE_ERROR);
        registerErrorFormatter(INIT_ERRORS, "addon_init_error", FORMATTER_SINGLE_ERROR);
        registerErrorFormatter(INIT_ERRORS, "addon_load_error", FORMATTER_SINGLE_ERROR); //FORMAT
        registerErrorFormatter(INIT_ERRORS, "addon_error", FORMATTER_SINGLE_ERROR); //FORMAT
        registerErrorFormatter(INIT_ERRORS, "res_pack_load_fail", FORMATTER_SINGLE_ERROR);
        registerErrorFormatter(UPDATES, "updates", FORMATTER_SINGLE_ERROR);
        registerErrorFormatter(PACKS_ERRORS, "required_property", FORMATTER_MULTIPLE_ERROR_ONE_LI);
        registerErrorFormatter(PACKS_ERRORS, "obj_duplicated_custom_textures", FORMATTER_MULTIPLE_ERROR);
        registerErrorFormatter(PACKS_ERRORS, "armor_error", FORMATTER_MULTIPLE_ERROR);
        registerErrorFormatter(PACKS_ERRORS, "syntax_error", FORMATTER_MULTIPLE_ERROR);
        registerErrorFormatter(PACKS_ERRORS, "deprecated_prop", FORMATTER_MULTIPLE_ERROR);
        registerErrorFormatter(PACKS_ERRORS, "deprecated_prop_format", FORMATTER_MULTIPLE_ERROR);
        registerErrorFormatter(PACKS_ERRORS, "missing_prop", FORMATTER_MULTIPLE_ERROR);
        registerErrorFormatter(PACKS_ERRORS, "unknown_sub_info", FORMATTER_MULTIPLE_ERROR_ONE_LI);
        registerErrorFormatter(PACKS_ERRORS, "deprecated_seat_config", FORMATTER_MULTIPLE_ERROR_ONE_LI);
        registerErrorFormatter(PACKS_ERRORS, "deprecated_door_config", FORMATTER_MULTIPLE_ERROR_ONE_LI);
        registerErrorFormatter(PACKS_ERRORS, "sound_error", FORMATTER_MULTIPLE_ERROR);
        registerErrorFormatter(PACKS_ERRORS, "config_error", FORMATTER_MULTIPLE_ERROR);
        registerErrorFormatter(PACKS_ERRORS, "pack_requirements", FORMATTER_MULTIPLE_ERROR); //FORMAT
        registerErrorFormatter(PACKS_ERRORS, "collision_shape_error", FORMATTER_SINGLE_ERROR);
        registerErrorFormatter(PACKS_ERRORS, "complete_object_error", FORMATTER_SINGLE_ERROR);
        registerErrorFormatter(PACKS_ERRORS, "property_parse_error", FORMATTER_MULTIPLE_ERROR); //FORMAT
        registerErrorFormatter(PACKS_ERRORS, "pack_load_fail", FORMATTER_SINGLE_ERROR);
        registerErrorFormatter(PACKS_ERRORS, "missing_pack_info", FORMATTER_MULTIPLE_ERROR);  //FORMAT
        registerErrorFormatter(PACKS_ERRORS, "pack_file_load_error", FORMATTER_SINGLE_ERROR);
        registerErrorFormatter(MODEL_ERRORS, "obj_error", FORMATTER_SINGLE_ERROR);
        registerErrorFormatter(MODEL_ERRORS, "obj_none_material", FORMATTER_MULTIPLE_ERROR_ONE_LI);
        registerErrorFormatter(PACKS_ERRORS, "deprecated_light_format", FORMATTER_MULTIPLE_ERROR);
        registerErrorFormatter(PACKS_ERRORS, "wheel_invalid_suspaxis", FORMATTER_MULTIPLE_ERROR);
        registerErrorFormatter(PACKS_ERRORS, "too_many_variants", FORMATTER_MULTIPLE_ERROR);
        registerErrorFormatter(PACKS_ERRORS, "missing_depends_on_node", FORMATTER_MULTIPLE_ERROR_ONE_LI);
    }
}
