package fr.dynamx.utils;

import fr.aym.mps.utils.ProtectionException;
import fr.aym.mps.utils.SSLHelper;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

public class LibraryInstaller {
    /**
     *
     */
    public static boolean loadACsGuis(Logger logger, File directory, String defaultACsGuisVersion) {
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
                if ((DynamXConstants.DYNAMX_CERT != null || DynamXConstants.DYNAMX_AUX_CERT != null) && SSLHelper.shouldInstallCert())
                    installCertificates(logger, DynamXConstants.DYNAMX_CERT, DynamXConstants.DYNAMX_AUX_CERT);
                success = downloadACsGuis(logger, file, defaultACsGuisVersion);
            } catch (IOException e) {
                logger.fatal("Cannot auto-download ACsGuis", e);
                success = false;
            }
        }
        return success;
    }

    //FIXME PUT IN LIB, BUT WITH CUSTOM LOGGER IN PARAMETER
    private static void installCertificates(Logger log, String sslCertificateFilePath, String sslCertificateFilePathAux) {
        try {
            /*if (disableSSLCertification) {
                trustAllCerts();
            } else */
            {
                log.info("[MPS] Installing root server's certificate for " + sslCertificateFilePath + " and " + sslCertificateFilePathAux);
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                Path ksPath = Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts");
                keyStore.load(Files.newInputStream(ksPath), "changeit".toCharArray());
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                if (sslCertificateFilePath != null) {
                    loadCertificate(sslCertificateFilePath, keyStore, cf);
                }

                if (sslCertificateFilePathAux != null) {
                    loadCertificate(sslCertificateFilePathAux, keyStore, cf);
                }

                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(keyStore);
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), null);
                SSLContext.setDefault(sslContext);
            }

        } catch (Exception var7) {
            throw new ProtectionException("Cannot setup SSL", var7);
        }
    }

    private static void loadCertificate(String sslCertificateFilePath, KeyStore keyStore, CertificateFactory cf) throws IOException, CertificateException, KeyStoreException {
        InputStream s = SSLHelper.class.getResourceAsStream("/" + sslCertificateFilePath);
        if (s == null) {
            throw new FileNotFoundException(sslCertificateFilePath + " certificate not found !");
        } else {
            InputStream caInput = new BufferedInputStream(s);
            Throwable var5 = null;

            try {
                Certificate crt = cf.generateCertificate(caInput);
                keyStore.setCertificateEntry(sslCertificateFilePath, crt);
            } catch (Throwable var14) {
                var5 = var14;
                throw var14;
            } finally {
                if (caInput != null) {
                    if (var5 != null) {
                        try {
                            caInput.close();
                        } catch (Throwable var13) {
                            var5.addSuppressed(var13);
                        }
                    } else {
                        caInput.close();
                    }
                }

            }

        }
    }

    private static boolean downloadACsGuis(Logger logger, File to, String jmeVersion) throws IOException {
        logger.info("Installing ACsGuis library...");
        URL url = new URL(DynamXConstants.ACS_GUIS_BASE_URL + jmeVersion + ".jar");
        LibraryInstaller.download(url, to);
        logger.info("Downloaded ACsGuis lib from " + url + " to " + to);
        return true;
    }

    public static void download(URL from, File to) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) from.openConnection();
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
}
