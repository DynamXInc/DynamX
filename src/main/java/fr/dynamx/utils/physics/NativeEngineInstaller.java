package fr.dynamx.utils.physics;

import com.jme3.system.JmeSystem;
import com.jme3.system.Platform;
import fr.aym.mps.utils.SSLHelper;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.LibraryInstaller;
import net.minecraftforge.fml.common.ProgressManager;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class NativeEngineInstaller {
    /**
     * Load a Libbulletjme native library.
     *
     * @param directory  (not null, readable, unaffected
     * @param jmeVersion Version of light bullet, used for auto-download
     * @param buildType  "Debug" or "Release"
     * @param flavor     "Sp" or "Dp"
     * @param isMt       Is multithreaded or not
     * @return true after successful load, otherwise false
     */
    public static boolean loadLibbulletjme(File directory, String jmeVersion, String buildType, String flavor, boolean isMt) {
        assert buildType.equals("Debug") || buildType.equals("Release");
        assert flavor.equals("Sp") || flavor.equals("Dp");

        Platform platform = JmeSystem.getPlatform();

        String mtName = isMt ? "Mt" : "";
        String name;
        switch (platform) {
            case Windows32:
            case Windows64:
                name = "bulletjme.dll";
                break;
            case Linux_ARM32:
            case Linux_ARM64:
            case Linux32:
            case Linux64:
                name = "libbulletjme.so";
                break;
            case MacOSX32:
            case MacOSX64:
                name = "libbulletjme.dylib";
                break;
            default:
                throw new RuntimeException("platform = " + platform);
        }

        File file = new File(directory, platform + buildType + flavor + mtName + "_" + jmeVersion + "_" + name);
        String absoluteFilename = file.getAbsolutePath();
        boolean success = false;
        if (!file.exists()) {
            DynamXMain.log.warn("Cannot load native physics engine : " + absoluteFilename + " not found !");
        } else if (!file.canRead()) {
            DynamXMain.log.fatal("Cannot load native physics engine : " + absoluteFilename + " not readable !");
        } else {
            System.load(absoluteFilename);
            success = true;
        }
        if (!success) {
            ProgressManager.ProgressBar bar = ProgressManager.push("Download native physics engine...", 101);
            try {
                bar.step("SSL");
                if ((DynamXConstants.DYNAMX_CERT != null || DynamXConstants.DYNAMX_AUX_CERT != null) && SSLHelper.shouldInstallCert())
                    SSLHelper.installCertificates(DynamXConstants.DYNAMX_CERT, DynamXConstants.DYNAMX_AUX_CERT);
                success = downloadJme(bar, file, platform + buildType + flavor + "_" + name, jmeVersion);
                System.load(absoluteFilename);
            } catch (IOException e) {
                DynamXMain.log.fatal("Cannot auto-download light bullet", e);
                fillPercentBar(bar, bar.getStep(), bar.getSteps());
                success = false;
            }
            ProgressManager.pop(bar);
        }
        return success;
    }

    private static boolean downloadJme(ProgressManager.ProgressBar bar, File to, String name, String jmeVersion) throws IOException {
        DynamXMain.log.info("Installing lightbulletjme native physics engine...");
        bar.step("0%");
        URL url = new URL("https://github.com/stephengold/Libbulletjme/releases/download/" + jmeVersion + "/" + name);
        download(bar, url, to);
        DynamXMain.log.info("Downloaded native physics engine from " + url + " to " + to);
        fillPercentBar(bar, bar.getStep(), bar.getSteps());
        return true;
    }

    public static void download(ProgressManager.ProgressBar bar, URL from, File to) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) from.openConnection();
        // Adding some user agents
        //connection.addRequestProperty("User-Agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.124 Safari/537.36");
        InputStream in = new BufferedInputStream(connection.getInputStream());
        bar.step("1%");
        FileOutputStream out = new FileOutputStream(to);
        byte[] buf = new byte[1024];
        int n, read = 0;
        int size = connection.getContentLength();
        while (-1 != (n = in.read(buf))) {
            out.write(buf, 0, n);
            read += n;
            fillPercentBar(bar, bar.getStep(), Integer.divideUnsigned(read * 100, size));
        }
        in.close();
        out.close();
    }

    public static void fillPercentBar(ProgressManager.ProgressBar bar, int curPercent, int toPercent) {
        while (curPercent < toPercent) {
            curPercent++;
            bar.step(curPercent + "%");
        }
    }
}
