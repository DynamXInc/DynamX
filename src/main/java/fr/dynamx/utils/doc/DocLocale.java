package fr.dynamx.utils.doc;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import fr.dynamx.common.DynamXMain;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.IllegalFormatException;
import java.util.Map;
import java.util.regex.Pattern;

@SideOnly(Side.CLIENT)
public class DocLocale {
    /**
     * Splits on "="
     */
    private static final Splitter SPLITTER = Splitter.on('=').limit(2);
    private static final Pattern PATTERN = Pattern.compile("%(\\d+\\$)?[\\d.]*[df]");
    Map<String, String> properties = Maps.newHashMap();
    private boolean unicode;

    /**
     * For each domain $D and language $L, attempts to load the resource $D:lang/$L.lang
     */
    public synchronized void loadLocaleDataFiles(String localeName) {
        this.properties.clear();

        File f = new File(new File("Doc", "langs"), localeName);
        System.out.println("Loading " + f);
        try {
            loadLocaleData(new FileInputStream(f));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.checkUnicode();
    }

    public boolean isUnicode() {
        return this.unicode;
    }

    private void checkUnicode() {
        this.unicode = false;
        int i = 0;
        int j = 0;

        for (String s : this.properties.values()) {
            int k = s.length();
            j += k;

            for (int l = 0; l < k; ++l) {
                if (s.charAt(l) >= 256) {
                    ++i;
                }
            }
        }

        float f = (float) i / (float) j;
        this.unicode = (double) f > 0.1D;
    }

    private void loadLocaleData(InputStream inputStreamIn) throws IOException {
        inputStreamIn = net.minecraftforge.fml.common.FMLCommonHandler.instance().loadLanguage(properties, inputStreamIn);
        if (inputStreamIn == null) return;
        for (String s : IOUtils.readLines(inputStreamIn, StandardCharsets.UTF_8)) {
            if (!s.isEmpty() && s.charAt(0) != '#') {
                String[] astring = Iterables.toArray(SPLITTER.split(s), String.class);

                if (astring != null && astring.length == 2) {
                    String s1 = astring[0];
                    String s2 = PATTERN.matcher(astring[1]).replaceAll("%$1s");
                    this.properties.put(s1, s2);
                }
            }
        }
    }

    /**
     * Returns the translation, or the key itself if the key could not be translated.
     */
    private String translateKeyPrivate(String translateKey) {
        String s = this.properties.get(translateKey);
        if (s == null) {
            DynamXMain.log.error("[DOC] Translation for " + translateKey + " not found !");
            return translateKey;
        }
        return s;
    }

    /**
     * Calls String.format(translateKey(key), params)
     */
    public String format(String translateKey, Object... parameters) {
        String s = this.translateKeyPrivate(translateKey);

        try {
            return String.format(s, parameters);
        } catch (IllegalFormatException var5) {
            return "Format error: " + s;
        }
    }

    public boolean hasKey(String key) {
        return this.properties.containsKey(key);
    }
}