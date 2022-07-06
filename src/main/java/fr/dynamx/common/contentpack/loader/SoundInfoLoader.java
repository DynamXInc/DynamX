package fr.dynamx.common.contentpack.loader;

import fr.aym.acslib.api.services.ErrorTrackingService;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.type.vehicle.EngineSound;
import fr.dynamx.common.contentpack.type.vehicle.SoundListInfo;
import fr.dynamx.utils.DynamXLoadingTasks;

import java.io.BufferedReader;
import java.util.function.BiFunction;

import static fr.dynamx.common.DynamXMain.log;

/**
 * Sounds files loader
 */
public class SoundInfoLoader extends InfoLoader<SoundListInfo, ISubInfoTypeOwner.Empty>
{
    public SoundInfoLoader(String prefix, BiFunction<String, String, SoundListInfo> assetCreator) {
        super(prefix, assetCreator, null);
    }

    @Override
    protected void readInfo(BufferedReader inputStream, INamedObject info) {
        assert info instanceof SoundListInfo : "Bad SoundInfoLoader usage";
        final EngineSound[] readingCategory = {null};
        //Loaded sounds
        final boolean[] interior = {false};
        inputStream.lines().forEach(s -> {
            if (!s.trim().startsWith("//")) {
                if (s.contains("{")) { //Start of a property
                    String name = s.replace("{", "").trim();
                    switch (name) {
                        case "":
                            break; //Do nothing
                        case "Engine":
                            break; //Do nothing its the only type of sound
                        case "Interior":
                            //Sounds loaded after this line will be interior sounds
                            interior[0] = true;
                            break;
                        case "Exterior":
                            //Sounds loaded after this line will be exterior sounds
                            interior[0] = false;
                            break;
                        case "Starting":
                            //Starting sound
                            readingCategory[0] = new EngineSound(info.getPackName(), new int[]{-1});
                            readingCategory[0].setInterior(interior[0]);
                            break;
                        default:
                            //Engine sounds depending on the rpm
                            if (name.contains("-")) {
                                String[] sp = name.split("-");
                                //Parse min and max rpm
                                readingCategory[0] = new EngineSound(info.getPackName(), new int[]{Integer.parseInt(sp[0]), Integer.parseInt(sp[1])});
                                readingCategory[0].setInterior(interior[0]);
                            } else {
                                log.error("Invalid sound config entry : " + s + " in "+info.getFullName());
                                DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.PACK, info.getPackName(), info.getFullName(), "Invalid sound config entry : " + s, ErrorTrackingService.TrackedErrorLevel.LOW);
                            }
                    }
                } else if (s.contains(":")) {
                    //Property of a reading sound
                    if (readingCategory[0] != null) {
                        String[] split = s.split(":");
                        setFieldValue(readingCategory[0], split[0].trim(), split[1].trim());
                    }
                } else if (s.contains("}")) { //End of a property, if it was a sound, add it to the sound list
                    if (readingCategory[0] != null)
                        ((SoundListInfo)info).addSound(readingCategory[0]);
                    readingCategory[0] = null;
                } else if (s.contains("Engine")) {
                    //We don't care
                } else {
                    log.error("Unknown sound config key " + s+ " in "+info.getFullName());
                    DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.PACK, info.getPackName(), "Error in "+info.getFullName(), "Unknown sound config key " + s, ErrorTrackingService.TrackedErrorLevel.LOW);
                }
            }
        });
    }
}
