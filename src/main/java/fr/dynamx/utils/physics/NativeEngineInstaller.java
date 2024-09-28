package fr.dynamx.utils.physics;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.system.JmeSystem;
import com.jme3.system.Platform;
import fr.aym.mps.utils.UserErrorMessageException;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.LibraryInstaller;
import net.minecraftforge.fml.common.ProgressManager;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.logging.Level;

public class NativeEngineInstaller {
    /**
     * Load a Libbulletjme native library.
     *
     * @param directory  (not null, readable, unaffected
     * @param jmeVersion Version of light bullet, used for auto-download
     * @param buildType  "Debug" or "Release"
     * @param flavor     "Sp" or "Dp"
     * @param isMt       Is multithreaded or not
     */
    public static void loadLibbulletjme(File directory, String jmeVersion, String buildType, String flavor, boolean isMt) throws UserErrorMessageException {
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
            case MacOSX_ARM64:
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
                bar.step("Download physics engine...");
                downloadJme(bar, file, platform + buildType + flavor + "_" + name, jmeVersion);
                System.load(absoluteFilename);
            } catch (ConnectException | UnknownHostException e) {
                DynamXMain.log.fatal("Cannot connect to the server to download physics engine", e);
                throw new UserErrorMessageException("Cannot connect to the server to download DynamX physics engine", e,
                        "Cannot download the DynamX physics engine",
                        "Cannot connect to the server, please check your internet connection and try again later",
                        "Please also verify that you have the latest version of DynamX",
                        "",
                        "If needed, you can find help on DynamX's discord server");
            } catch (SSLHandshakeException e) {
                DynamXMain.log.fatal("SSL Handshake error while trying to download physics engine", e);
                throw new UserErrorMessageException("SSL Handshake error while trying to download DynamX physics engine", e,
                        "Cannot download the DynamX physics engine",
                        "An SSL error occurred, please check your internet connection and try again later",
                        "Please also ensure that you have the latest version of DynamX",
                        "",
                        "If needed, you can find help on DynamX's discord server");
            } catch (IOException e) {
                DynamXMain.log.fatal("Cannot auto-download light bullet", e);
                throw new UserErrorMessageException("Cannot auto-download DynamX physics engine", e,
                        "Cannot download the DynamX physics engine",
                        "An error occurred while trying to download the DynamX physics engine",
                        "Please check your internet connection and try again later",
                        "Please also ensure that you have the latest version of DynamX",
                        "More information can be found in the log file.",
                        "",
                        "If needed, you can find help on DynamX's discord server");
            } finally {
                fillPercentBar(bar, bar.getStep(), bar.getSteps());
                ProgressManager.pop(bar);
            }
        }
        PhysicsRigidBody.logger2.setLevel(Level.OFF); // disable logging for physicsrigedbody to avoid spamming the console
    }

    private static void downloadJme(ProgressManager.ProgressBar bar, File to, String name, String jmeVersion) throws IOException {
        DynamXMain.log.info("Installing lightbulletjme native physics engine...");
        bar.step("0%");
        URL url = new URL(DynamXConstants.LIBBULLET_BASE_URL + jmeVersion + "/" + name);
        download(bar, url, to);
        DynamXMain.log.info("Downloaded native physics engine from " + url + " to " + to);
        fillPercentBar(bar, bar.getStep(), bar.getSteps());
    }

    public static void download(ProgressManager.ProgressBar bar, URL from, File to) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) from.openConnection();
        if (connection instanceof HttpsURLConnection) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(LibraryInstaller.getDynamXSSLContext().getSocketFactory());
        }
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
