package fr.dynamx.utils;

import org.apache.logging.log4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class LibraryInstaller {
    private static SSLContext dynamXSSLContext;

    /**
     *
     */
    public static boolean loadACsGuis(Logger logger, SSLContext dynamXSSLContext, File directory, String defaultACsGuisVersion) {
        LibraryInstaller.dynamXSSLContext = dynamXSSLContext;
        File file = new File(directory, "ACsGuis-" + defaultACsGuisVersion + ".jar");
        String absoluteFilename = file.getAbsolutePath();
        boolean success = false;
        if (!file.exists()) {
            for (File file1 : directory.listFiles()) {
                if (file1.getName().contains("ACsGuis")) {
                    logger.info("ACsGuis detected (custom version) : " + file1);
                    success = true;
                    break;
                }
            }
        } else if (!file.canRead()) {
            logger.fatal("Cannot load ACsGuis : " + absoluteFilename + " not readable !");
        } else {
            logger.info("ACsGuis detected (recommended version) : " + file);
            success = true;
        }
        if (!success) {
            logger.warn("Cannot find ACsGuis : " + absoluteFilename + " not found !");
            try {
                success = downloadACsGuis(logger, file, defaultACsGuisVersion);
            } catch (IOException e) {
                logger.fatal("Cannot auto-download ACsGuis", e);
                success = false;
            }
        }
        return success;
    }

    private static boolean downloadACsGuis(Logger logger, File to, String acsGuisVersion) throws IOException {
        logger.info("Installing ACsGuis library...");
        URL url = new URL(String.format(DynamXConstants.ACS_GUIS_BASE_URL, acsGuisVersion));
        LibraryInstaller.download(url, to);
        logger.info("Downloaded ACsGuis lib from " + url + " to " + to);
        return true;
    }

    public static void download(URL from, File to) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) from.openConnection();
        if (connection instanceof HttpsURLConnection) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(dynamXSSLContext.getSocketFactory());
        }
        // Adding some user agents
        //connection.addRequestProperty("User-Agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.124 Safari/537.36");
        InputStream in = new BufferedInputStream(connection.getInputStream());
        FileOutputStream out = new FileOutputStream(to);
        byte[] buf = new byte[1024];
        int n, read = 0;
        int size = connection.getContentLength();
        while (-1 != (n = in.read(buf))) {
            out.write(buf, 0, n);
            read += n;
        }
        in.close();
        out.close();
    }

    public static SSLContext getDynamXSSLContext() {
        if(dynamXSSLContext == null) {
            throw new IllegalStateException("SSLContext not initialized");
        }
        return dynamXSSLContext;
    }
}
