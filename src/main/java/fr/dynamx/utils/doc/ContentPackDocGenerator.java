package fr.dynamx.utils.doc;

import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.loader.PackFilePropertyData;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Scanner;

public class ContentPackDocGenerator {
    private static final DocLocale locale = new DocLocale();
    private static boolean hasInit;

    public static void generateDoc(String name, String lang, Collection<PackFilePropertyData<?>> data) {
        if (data.isEmpty()) {
            System.out.println("Ignoring " + name);
            return;
        }
        if (!hasInit) {
            System.out.println("Doc locale " + lang);
            locale.loadLocaleDataFiles("doc_" + lang + ".lang");
        }

        System.out.println("Generating "+name+".md");
        StringBuilder builder = new StringBuilder();
        builder.append('\n').append("##### ").append(locale.format("category.REQUIRED"));
        builder.append("|").append(locale.format("title.name")).append("|")
                .append(locale.format("title.type")).append("|")
                .append(locale.format("title.description")).append("|")
                .append(locale.format("title.example")).append("|").append("\n");
        builder.append("| -------- | ------------- | ------------------ | ---------------------------- | ---------------- |\n");
        data.forEach(d -> d.writeDocLine(builder, locale, DocType.REQUIRED));

        builder.append('\n').append("##### ").append(locale.format("category.OPTIONAL"));
        builder.append("|").append(locale.format("title.name")).append("|")
                .append(locale.format("title.type")).append("|")
                .append(locale.format("title.description")).append("|")
                .append(locale.format("title.default_value")).append("|").append("\n");
        builder.append("| -------- | ------------- | ------------------ | ---------------------------- | ---------------- |\n");
        data.forEach(d -> d.writeDocLine(builder, locale, DocType.OPTIONAL));
        //data.forEach(d -> d.writeDocLine(builder, locale, DocType.DEPRECATED));
        File docDir = new File("Doc");
        docDir.mkdirs();
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
