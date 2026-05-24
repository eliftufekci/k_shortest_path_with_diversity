import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.zip.GZIPInputStream;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Downloads and prepares graph files from the internet.
 *
 * Downloaded graphs:
 * 1. web-Google.txt  - Web linkler grafı (SNAP)
 * 2. RoadCOL.gr      - USA Road Colorado (DIMACS)
 * 3. RoadFLA.gr      - USA Road Florida (DIMACS)
 * 4. WikiTalk.txt    - Wikipedia Talk network (SNAP)
 */
public class DownloadGraphs {
    private static final String BASE_DIR   = "graph-data";
    private static final int    BUFFER_SIZE = 8192;

    private static void downloadFile(String urlString, String destFile) throws IOException {
        System.out.println("  [Downloading] " + urlString);

        File destPath = new File(destFile);
        destPath.getParentFile().mkdirs();

        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();

        // Trust-all configuration to bypass SSL certificate validation issues
        if (conn instanceof HttpsURLConnection) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }}, new java.security.SecureRandom());

                HttpsURLConnection httpsConn = (HttpsURLConnection) conn;
                httpsConn.setSSLSocketFactory(sslContext.getSocketFactory());
                httpsConn.setHostnameVerifier((hostname, session) -> true);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new IOException("SSL context initialization failed: " + e.getMessage(), e);
            }
        }

        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);

        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long totalBytes = 0;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;

                if (totalBytes % (1024 * 1024) == 0) {
                    System.out.print(".");
                }
            }
            System.out.println("\n  [Completed] Downloaded: " + totalBytes + " bytes");
        }
    }

    private static void extractGzip(String gzipFile, String outputFile) throws IOException {
        System.out.println("  [Extracting] " + gzipFile + " -> " + outputFile);

        try (GZIPInputStream  gzipIn = new GZIPInputStream(new FileInputStream(gzipFile));
             FileOutputStream out    = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = gzipIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    /**
     * Removes the first {@code linesToRemove} header lines from the file.
     * Overwrites the original file.
     */
    private static void removeHeaderLines(String filePath, int linesToRemove) throws IOException {
        System.out.println("  [Cleanup] Removing first " + linesToRemove + " header lines from: " + filePath);

        File   original = new File(filePath);
        File   tmp      = new File(filePath + ".tmp");

        try (BufferedReader reader = new BufferedReader(new FileReader(original));
             BufferedWriter writer = new BufferedWriter(new FileWriter(tmp))) {

            // Skip first N lines
            for (int i = 0; i < linesToRemove; i++) {
                if (reader.readLine() == null) break; // File is shorter than expected
            }

            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
        }

        Files.move(tmp.toPath(), original.toPath(), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("  [Success] Header lines removed from: " + filePath);
    }

    private static void downloadAndExtract(String url,
                                           String filenameGz,
                                           String filenameOut,
                                           int headerLinesToRemove) throws IOException {
        File gzFile  = new File(filenameGz);
        File outFile = new File(filenameOut);

        if (!gzFile.exists()) {
            downloadFile(url, filenameGz);
        } else {
            System.out.println("  [Skip] Archive already exists: " + filenameGz);
        }

        if (!outFile.exists()) {
            extractGzip(filenameGz, filenameOut);
            if (headerLinesToRemove > 0) {
                removeHeaderLines(filenameOut, headerLinesToRemove);
            }
        } else {
            System.out.println("  [Skip] File already exists: " + filenameOut);
        }

        // Delete GZIP file
        if (gzFile.exists()) {
            gzFile.delete();
            System.out.println("  [Cleanup] Removed archive: " + filenameGz);
        }

        System.out.println("  [Done] Prepared: " + filenameOut);
    }

    /** Backward compatibility: download and extract without removing header lines */
    private static void downloadAndExtract(String url,
                                           String filenameGz,
                                           String filenameOut) throws IOException {
        downloadAndExtract(url, filenameGz, filenameOut, 0);
    }

    public static void downloadAndPrepareGraphs() throws IOException {
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║   Starting Graph Download                  ║");
        System.out.println("╚════════════════════════════════════════════╝\n");

        // Create base directory
        new File(BASE_DIR).mkdirs();

        try {
            // ==================== WEB GOOGLE GRAPH ====================
            System.out.println("\n[1/4] WEB GOOGLE GRAPH");
            System.out.println("─────────────────────────────────────────────");
            downloadAndExtract(
                    "https://snap.stanford.edu/data/web-Google.txt.gz",
                    BASE_DIR + "/web-Google.txt.gz",
                    BASE_DIR + "/web-Google.txt",
                    4   // First 4 lines are comments
            );

            // ==================== ROAD COLORADO ====================
            System.out.println("\n[2/4] ROAD COLORADO GRAPH (RoadCOL)");
            System.out.println("─────────────────────────────────────────────");
            downloadAndExtract(
                    "https://www.diag.uniroma1.it/challenge9/data/USA-road-d/USA-road-d.COL.gr.gz",
                    BASE_DIR + "/RoadCOL.gr.gz",
                    BASE_DIR + "/RoadCOL.gr"
            );

            // ==================== ROAD FLORIDA ====================
            System.out.println("\n[3/4] ROAD FLORIDA GRAPH (RoadFLA)");
            System.out.println("─────────────────────────────────────────────");
            downloadAndExtract(
                    "https://www.diag.uniroma1.it/challenge9/data/USA-road-d/USA-road-d.FLA.gr.gz",
                    BASE_DIR + "/RoadFLA.gr.gz",
                    BASE_DIR + "/RoadFLA.gr"
            );

            // ==================== WIKI TALK ====================
            System.out.println("\n[4/4] WIKI TALK GRAPH (WikiTalk)");
            System.out.println("─────────────────────────────────────────────");
            downloadAndExtract(
                    "https://snap.stanford.edu/data/wiki-Talk.txt.gz",
                    BASE_DIR + "/WikiTalk.txt.gz",
                    BASE_DIR + "/WikiTalk.txt",
                    4   // First 4 lines are comments
            );

            System.out.println("\n╔════════════════════════════════════════════╗");
            System.out.println("║   All graphs downloaded successfully!       ║");
            System.out.println("╚════════════════════════════════════════════╝\n");

        } catch (IOException e) {
            System.err.println("\n❌ Error downloading graphs: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}