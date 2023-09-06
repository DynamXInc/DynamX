package fr.dynamx.utils.doc;

import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.common.contentpack.loader.PackFilePropertyData;
import fr.dynamx.common.contentpack.loader.SubInfoTypeAnnotationCache;
import net.minecraftforge.fml.common.ModContainerFactory;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.discovery.ContainerType;
import net.minecraftforge.fml.common.discovery.DirectoryDiscoverer;
import net.minecraftforge.fml.common.discovery.ModCandidate;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class DocGeneratorMain implements INamedObject {
    public static void main(String[] args) {
        Map<String, Class<?>> classesToLoad = new HashMap<>();
        try {
            DirectoryDiscoverer discoverer = new DirectoryDiscoverer();
            File file = new File("H:\\Modding\\DynamX\\out\\production\\DynamX.main");
            ModCandidate candidate = new ModCandidate(file, file, ContainerType.DIR);
            ASMDataTable table = new ASMDataTable();
            //candidate.explore(table);
            ReflectionHelper.setPrivateValue(ModCandidate.class, candidate, table, "table");
            ModContainerFactory.modTypes.clear();
            discoverer.discover(candidate, table);
            System.out.println(table.getAll(PackFileProperty.class.getName()));
            for (ASMDataTable.ASMData asmData : table.getAll(PackFileProperty.class.getName())) {
                if (!classesToLoad.containsKey(asmData.getClassName()))
                    classesToLoad.put(asmData.getClassName(), Class.forName(asmData.getClassName()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Classes implémentant INamedObject :");
        System.out.println("======");
        DocLocale locale = new DocLocale();
        String lang = "fr_fr";
        System.out.println("Doc locale " + lang);
        locale.loadLocaleDataFiles(new File(new File(new File("run", "Doc"), "langs"), "doc_" + lang + ".lang"));
        File docDir = new File("run", "Doc");
        for (Class<?> clazz : classesToLoad.values()) {
            System.out.println(clazz.getName());
            Map<String, PackFilePropertyData<?>> packFileProperties = SubInfoTypeAnnotationCache.getOrLoadData(clazz);
            ContentPackDocGenerator.generateDoc(locale, docDir, clazz, clazz.getSimpleName(), packFileProperties.values());
            System.out.println("Found " + packFileProperties.size() + " fields in " + clazz.getName());
        }
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getPackName() {
        return null;
    }
}
