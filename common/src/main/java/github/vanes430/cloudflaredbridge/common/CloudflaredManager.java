package github.vanes430.cloudflaredbridge.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CloudflaredManager {

    private static final String GITHUB_API_URL = "https://api.github.com/repos/cloudflare/cloudflared/releases/latest";
    private final Path rootDir;
    private final BridgeLogger logger;
    private final Map<String, Process> processes = new ConcurrentHashMap<>();
    
    // Config
    private List<String> tokens = new CopyOnWriteArrayList<>();

    public CloudflaredManager(Path rootDir, BridgeLogger logger) {
        this.rootDir = rootDir;
        this.logger = logger;
    }

    public void init() {
        try {
            if (!Files.exists(rootDir)) {
                Files.createDirectories(rootDir);
            }
            loadConfig();
            checkForUpdatesAndInstall();
        } catch (Exception e) {
            logger.severe("Failed to initialize CloudflaredManager: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadConfig() {
        Path configFile = rootDir.resolve("config.yml");
        if (!Files.exists(configFile)) {
            try {
                // Create default config
                Map<String, Object> data = new HashMap<>();
                data.put("tokenlist", Arrays.asList("replace_with_your_token_here", "another_token_if_needed"));
                Yaml yaml = new Yaml();
                try (Writer writer = new FileWriter(configFile.toFile())) {
                    yaml.dump(data, writer);
                }
                logger.info("Created default config.yml in " + rootDir);
            } catch (IOException e) {
                logger.severe("Could not create default config.yml: " + e.getMessage());
            }
        }

        try (InputStream in = Files.newInputStream(configFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(in);
            if (data != null && data.containsKey("tokenlist")) {
                Object list = data.get("tokenlist");
                if (list instanceof List) {
                    tokens.clear();
                    tokens.addAll((List<String>) list);
                }
            }
        } catch (Exception e) {
            logger.severe("Error loading config.yml: " + e.getMessage());
        }
    }

    public void start(int delaySeconds) {
        if (tokens.isEmpty()) {
            logger.warning("No tokens found in config.yml. Cloudflared will not start.");
            return;
        }

        if (delaySeconds < 5) {
            delaySeconds = 5;
            logger.warning("Delay specified was less than 5 seconds. Enforcing minimum delay of 5 seconds.");
        }

        try {
            checkForUpdatesAndInstall();
        } catch (IOException e) {
            logger.severe("Failed to check for updates: " + e.getMessage());
            if (!Files.exists(getBinaryPath())) {
                return;
            }
        }

        Path binaryPath = getBinaryPath();
        if (!Files.exists(binaryPath)) {
            logger.severe("Cloudflared binary not found! Cannot start.");
            return;
        }

        // Set executable permission on Unix-likes
        if (PlatformUtils.getOS() != PlatformUtils.OS.WINDOWS) {
            binaryPath.toFile().setExecutable(true);
        }

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (token.equals("replace_with_your_token_here") || token.trim().isEmpty()) {
                continue;
            }
            startProcess(token, binaryPath);

            if (i < tokens.size() - 1) {
                try {
                    logger.info("Waiting " + delaySeconds + "s before starting next tunnel...");
                    Thread.sleep(delaySeconds * 1000L);
                } catch (InterruptedException e) {
                    logger.warning("Start sequence interrupted.");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void startProcess(String token, Path binaryPath) {
        if (processes.containsKey(token)) {
            if (processes.get(token).isAlive()) {
                logger.warning("Cloudflared process for a token is already running.");
                return;
            } else {
                processes.remove(token);
            }
        }

        try {
            logger.info("Starting Cloudflared tunnel...");
            ProcessBuilder pb = new ProcessBuilder(
                    binaryPath.toAbsolutePath().toString(),
                    "tunnel", "--no-autoupdate", "run", "--token", token
            );
            pb.directory(rootDir.toFile());
            
            Process p = pb.start();
            
            // Log output in background threads
            inheritIO(p.getInputStream(), "INFO");
            inheritIO(p.getErrorStream(), "ERROR");

            processes.put(token, p);
            logger.info("Started Cloudflared process for token ending in ..." + (token.length() > 5 ? token.substring(token.length() - 5) : token));

        } catch (IOException e) {
            logger.severe("Failed to start cloudflared: " + e.getMessage());
        }
    }

    public void stop() {
        logger.info("Stopping all Cloudflared processes...");
        for (Map.Entry<String, Process> entry : processes.entrySet()) {
            Process p = entry.getValue();
            if (p.isAlive()) {
                p.destroy(); // Try graceful first
                try {
                    // Give it a moment to shut down
                    if (!p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        p.destroyForcibly();
                    }
                } catch (InterruptedException e) {
                    p.destroyForcibly();
                }
            }
        }
        processes.clear();
        logger.info("All Cloudflared processes stopped.");
    }

    private void checkForUpdatesAndInstall() throws IOException {
        logger.info("Checking for Cloudflared updates...");
        String jsonResponse = BridgeUtils.fetchUrl(GITHUB_API_URL);
        JsonObject release = JsonParser.parseString(jsonResponse).getAsJsonObject();
        
        String assetName = getAssetName();
        Path binaryPath = getBinaryPath();
        
        // Check hash from body
        String expectedHash = extractHashFromBody(release.get("body").getAsString(), assetName);
        
        boolean needsInstall = true;
        
        if (Files.exists(binaryPath)) {
            if (expectedHash != null) {
                try {
                    String localHash = BridgeUtils.calculateSha256(binaryPath);
                    if (localHash.equalsIgnoreCase(expectedHash)) {
                        needsInstall = false;
                        logger.info("Cloudflared is up to date.");
                    } else {
                        logger.info("Hash mismatch (Local: " + localHash + ", Expected: " + expectedHash + "). Deleting and re-downloading.");
                        Files.delete(binaryPath);
                    }
                } catch (Exception e) {
                    logger.warning("Could not calculate local hash. Re-installing.");
                }
            } else {
                needsInstall = false;
                logger.warning("Could not find hash for " + assetName + " in release notes. Skipping update check.");
            }
        }

        if (needsInstall) {
            logger.info("Downloading Cloudflared (" + assetName + ")...");
            downloadAndInstall(release, assetName);
        }
    }

    private String extractHashFromBody(String body, String assetName) {
        Pattern pattern = Pattern.compile(Pattern.quote(assetName) + "\s*:\s*([a-fA-F0-9]{64})");
        Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private void downloadAndInstall(JsonObject release, String targetAssetName) throws IOException {
        String downloadUrl = null;
        for (JsonElement el : release.getAsJsonArray("assets")) {
            JsonObject asset = el.getAsJsonObject();
            if (asset.get("name").getAsString().equals(targetAssetName)) {
                downloadUrl = asset.get("browser_download_url").getAsString();
                break;
            }
        }

        if (downloadUrl == null) {
            throw new IOException("Asset " + targetAssetName + " not found in latest release.");
        }

        Path tempPath = rootDir.resolve(targetAssetName);
        BridgeUtils.downloadFile(downloadUrl, tempPath);

        if (targetAssetName.endsWith(".tgz")) {
            extractTarGz(tempPath, rootDir);
            Files.delete(tempPath); 
            Path extracted = rootDir.resolve("cloudflared");
            if (!Files.exists(extracted)) {
                throw new IOException("Extraction failed: 'cloudflared' binary not found.");
            }
        } else {
            Path finalPath = getBinaryPath();
            Files.move(tempPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
        }
        
        logger.info("Cloudflared installed successfully.");
    }

    private Path getBinaryPath() {
        String name = "cloudflared";
        if (PlatformUtils.getOS() == PlatformUtils.OS.WINDOWS) {
            name += ".exe";
        }
        return rootDir.resolve(name);
    }

    private String getAssetName() {
        PlatformUtils.OS os = PlatformUtils.getOS();
        PlatformUtils.Arch arch = PlatformUtils.getArch();

        if (os == PlatformUtils.OS.WINDOWS) {
            if (arch == PlatformUtils.Arch.AMD64) return "cloudflared-windows-amd64.exe";
            if (arch == PlatformUtils.Arch.X86) return "cloudflared-windows-386.exe";
        } else if (os == PlatformUtils.OS.LINUX) {
            if (arch == PlatformUtils.Arch.AMD64) return "cloudflared-linux-amd64";
            if (arch == PlatformUtils.Arch.X86) return "cloudflared-linux-386";
            if (arch == PlatformUtils.Arch.ARM64) return "cloudflared-linux-arm64";
            if (arch == PlatformUtils.Arch.ARM64) return "cloudflared-linux-arm"; 
        } else if (os == PlatformUtils.OS.MACOS) {
            if (arch == PlatformUtils.Arch.AMD64) return "cloudflared-darwin-amd64.tgz";
            if (arch == PlatformUtils.Arch.ARM64) return "cloudflared-darwin-arm64.tgz";
        }
        
        return "cloudflared-linux-amd64";
    }
    
    private void extractTarGz(Path tarGzFile, Path outputDir) throws IOException {
        try (InputStream fi = Files.newInputStream(tarGzFile);
             BufferedInputStream bi = new BufferedInputStream(fi);
             GzipCompressorInputStream gzi = new GzipCompressorInputStream(bi);
             TarArchiveInputStream ti = new TarArchiveInputStream(gzi)) {

            TarArchiveEntry entry;
            while ((entry = ti.getNextTarEntry()) != null) {
                Path entryPath = outputDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    try (OutputStream out = Files.newOutputStream(entryPath)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = ti.read(buffer)) != -1) {
                            out.write(buffer, 0, len);
                        }
                    }
                }
            }
        }
    }

    private void inheritIO(InputStream src, String level) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(src))) {
                String line;
                while ((line = reader.readLine()) != null) {
                }
            } catch (IOException e) {
            }
        }).start();
    }
}
