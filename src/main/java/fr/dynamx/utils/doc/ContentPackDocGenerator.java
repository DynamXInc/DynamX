package fr.dynamx.utils.doc;

import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.loader.PackFilePropertyData;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Scanner;
import java.util.stream.Collectors;

public class ContentPackDocGenerator {
    private static boolean hasInit = false;

    public static void generateDoc(DocLocale locale, File docDir, Class<?> forClass, String name, Collection<PackFilePropertyData<?>> data) {
        if (data.isEmpty()) {
            System.out.println("Ignoring " + name);
            return;
        }
        System.out.println("Generating "+name+".md");
        data = data.stream().sorted(Comparator.comparing(PackFilePropertyData::getConfigFieldName)).collect(Collectors.toList());
        StringBuilder builder = new StringBuilder();
        if(data.stream().anyMatch(PackFilePropertyData::isRequired)) {
            builder.append('\n').append("##### ").append(locale.format("category.REQUIRED")).append('\n');
            builder.append("|").append(locale.format("title.name")).append("|")
                    .append(locale.format("title.type")).append("|")
                    .append(locale.format("title.description")).append("|")
                    .append(locale.format("title.example")).append("|").append("\n");
            builder.append("| -------- | ------------- | ------------------ | ---------------------------- | \n");
            data.forEach(d -> d.writeDocLine(builder, locale, DocType.REQUIRED));
        }
        if (data.stream().anyMatch(d -> !d.isRequired())) {
            builder.append('\n').append("##### ").append(locale.format("category.OPTIONAL")).append('\n');
            builder.append("|").append(locale.format("title.name")).append("|")
                    .append(locale.format("title.type")).append("|")
                    .append(locale.format("title.description")).append("|")
                    .append(locale.format("title.default_value")).append("|").append("\n");
            builder.append("| -------- | ------------- | ------------------ | ---------------------------- | \n");
            data.forEach(d -> d.writeDocLine(builder, locale, DocType.OPTIONAL));
        }
        //data.forEach(d -> d.writeDocLine(builder, locale, DocType.DEPRECATED));
        docDir.mkdirs();
        // Write ALL_DOC file
        try {
            File file = new File(docDir, "ALL_DOC.md");
            if (!hasInit) {
                file.delete();
            }
            FileWriter writer = new FileWriter(file, true);
            writer.write("\n ## " + name +" : \n");
            writer.write("\n" + locale.format("info.desc."+name) + "\n");
            writer.write(builder.toString());
            writer.close();
            //System.out.println("Generated " + file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Write Parts_Reference file
        if(forClass.isAssignableFrom(ISubInfoType.class)) {
            System.out.println(">> Is SubInfo " + name);
        }
        // Generate the other files
        docDir = new File(docDir, "doc_files");
        docDir.mkdirs();
        File generated = new File(docDir, "generated");
        generated.mkdirs();
        if (hasInit) {
            docDir = generated;
        } else {
            hasInit = true;
        }
        boolean found = false;
        String pattern = "${" + name + ".md}";
        for (File source : docDir.listFiles()) {
            if (source.isFile()) {
                try {
                    Scanner sc = new Scanner(source);
                    StringBuilder nbuilder = new StringBuilder();
                    String line;
                    while (sc.hasNextLine()) {
                        line = sc.nextLine();
                        if (line.contains(pattern)) {
                            found = true;
                            line = line.replace(pattern, builder.toString());
                        }
                        nbuilder.append(line).append("\n");
                    }
                    sc.close();
                    FileWriter writer = new FileWriter(new File(generated, source.getName()));
                    writer.write(nbuilder.toString());
                    writer.close();
                    //System.out.println("Generated " + source + " doc");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (!found) {
            DynamXMain.log.warn("[DOC] Doc of " + name + " not replaced : pattern " + pattern + " not found in doc_files !");
        } else {
            DynamXMain.log.info("[DOC] Generated " + name + " doc");
        }
    }

    public enum DocType {
        REQUIRED, OPTIONAL
    }
}
