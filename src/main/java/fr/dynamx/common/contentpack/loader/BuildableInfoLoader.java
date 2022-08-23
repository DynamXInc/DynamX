package fr.dynamx.common.contentpack.loader;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.IInfoOwner;
import fr.dynamx.api.contentpack.object.IShapeContainer;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoTypeOwner;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.sync.PackSyncHandler;
import fr.dynamx.common.contentpack.type.ObjectInfo;
import fr.dynamx.utils.errors.DynamXErrorManager;
import net.minecraftforge.fml.common.ProgressManager;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static fr.dynamx.common.DynamXMain.log;

/**
 * Automatic loader of specific buildable info objects ({@link ModularVehicleInfoBuilder} for example)
 *
 * @param <A> The builders class
 * @param <T> The objects class
 * @param <C> The owners class
 * @see SubInfoTypeOwner.BuildableSubInfoTypeOwner
 * @see ObjectLoader
 */
public class BuildableInfoLoader<A extends SubInfoTypeOwner.BuildableSubInfoTypeOwner<A, T>, T extends ObjectInfo<?>, C extends IInfoOwner<?>> extends ObjectLoader<T, C, A> {
    public final List<A> vehiclesToLoad = new ArrayList<>();
    private final BiFunction<String, String, A> builderCreator;
    private final Function<T, C> itemCreator;

    /**
     * @param prefix         The prefix used to detect associated .dnx files
     * @param builderCreator A function matching an object packName and name with its builder class
     * @param itemCreator
     */
    public BuildableInfoLoader(String prefix, BiFunction<String, String, A> builderCreator, Function<T, C> itemCreator, @Nonnull SubInfoTypesRegistry<A> infoTypesRegistry) {
        super(prefix, null, infoTypesRegistry);
        this.builderCreator = builderCreator;
        this.itemCreator = itemCreator;
    }

    @Override
    public void clear(boolean hot) {
        super.clear(hot);
        vehiclesToLoad.clear();
    }

    @Override
    public boolean load(String loadingPack, String configName, BufferedReader inputStream, boolean hot) throws IOException {
        if (configName.startsWith(getPrefix())) {
            // Create the instance of the class that contains the vehicle information
            A info = builderCreator.apply(loadingPack, configName);
            if (vehiclesToLoad.stream().anyMatch(vInfo -> vInfo.getFullName().equals(info.getFullName())))
                throw new IllegalArgumentException("Found a duplicated pack file " + configName + " in pack " + loadingPack + " !");
            readInfoWithSubInfos(info, inputStream);
            info.onComplete(hot);
            //Add the vehicle information to the vehicles list
            vehiclesToLoad.add(info);
            return true;
        }
        return false;
    }

    @Override
    public boolean hasPostLoad() {
        return !vehiclesToLoad.isEmpty();
    }

    /**
     * Generates shapes and match builders with their objects infos
     *
     * @param hot True if it's an hot reload
     */
    @Override
    public void postLoad(boolean hot) {
        //Match vehicles with their engines, sounds and wheels
        ProgressManager.ProgressBar bar1 = ProgressManager.push("Generating " + getPrefix() + " shapes", vehiclesToLoad.size());
        for (A info : vehiclesToLoad) {
            bar1.step(info.getFullName());
            try {
                ((IShapeContainer) info).generateShape();
            } catch (Exception e) {
                ((IShapeContainer) info).markFailedShape();
                //DynamXMain.log.fatal("Cannot load physics collision shape of " + info.getFullName() + " !", e);
                DynamXErrorManager.addError(info.getPackName(), "collision_shape_error", ErrorLevel.FATAL, info.getName(), null, e);
            }
            if (!info.isErrored()) {
                try {
                    T vehicleInfo = info.build();
                    super.loadItems(vehicleInfo, hot);
                } catch (Exception e) {
                    //log.error("Cannot complete vehicle " + info + " !", e);
                    DynamXErrorManager.addError(info.getPackName(), "complete_vehicle_error", ErrorLevel.FATAL, info.getName(), null, e);
                }
            } else {
                log.info("Ignoring errored vehicle " + info.getFullName() + ". See previous errors.");
            }
        }
        ProgressManager.pop(bar1);
        if (super.hasPostLoad()) {
            super.postLoad(hot);
        }
    }

    /**
     * @return Maps a built info with the right item
     */
    public C getItem(T from) {
        return itemCreator.apply(from);
    }

    @Override
    public void hashObjects(PackSyncHandler hacheur, Map<String, byte[]> data) {
        vehiclesToLoad.forEach((ob) -> data.put(ob.getFullName(), hacheur.hash(ob)));
    }

    @Override
    public void encodeObjects(List<String> objects, Map<String, byte[]> out) {
        objects.forEach(o -> {
            Optional<A> opt = vehiclesToLoad.stream().filter(a -> a.getFullName().equals(o.substring(1))).findFirst();
            if (!opt.isPresent())
                throw new IllegalArgumentException("Object " + o.substring(1) + " not found for pack sync in " + getPrefix());
            encodeAObject(out, o, opt.get());
        });
    }

    @Override
    public void receiveObjects(Map<String, byte[]> objects) {
        objects.forEach((o, d) -> {
            String of = o.substring(1);
            String pack = of.substring(0, of.indexOf('.'));
            String object = of.substring(of.indexOf('.') + 1);
            if (o.charAt(0) == '*' || o.charAt(0) == '-') {
                A obj;
                if (o.charAt(0) == '*') {
                    Optional<A> opt = vehiclesToLoad.stream().filter(a -> a.getFullName().equals(of)).findFirst();
                    if (!opt.isPresent())
                        throw new IllegalArgumentException("Object " + o.substring(1) + " not found for pack sync in " + getPrefix());
                    obj = opt.get();
                } else
                    obj = builderCreator.apply(pack, object);
                try {
                    decodeAObject(obj, d);
                    obj.onComplete(o.charAt(0) == '*');
                    //Add the vehicle information to the vehicles list
                    vehiclesToLoad.add(obj);
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException("Cannot decode  " + of, e);
                }
            } else if (o.charAt(0) == '+') {
                if (!infos.containsKey(of))
                    DynamXMain.log.warn("[PACK_SYNC] Cannot remove " + of + " : not found x)");
                infos.remove(of);
                vehiclesToLoad.removeIf(a -> a.getFullName().equals(of));
            } else {
                throw new IllegalArgumentException("Wrong delta : " + o);
            }
        });
        postLoad(true);
        DynamXMain.log.info("[PACK_SYNC] Synced " + getPrefix());
    }
}
