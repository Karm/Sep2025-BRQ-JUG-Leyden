///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.json:json:20250517

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class testperf {

    private static final long TIMEOUT_NS = Duration.ofSeconds(3).toNanos();

    public static void main(String... args) throws Exception {
        final TreeMap<String, List<String>> cmds = new TreeMap<>();
        cmds.put("HotSpot No AOT", List.of("java", "-jar", "./target/quarkus-app/quarkus-run.jar"));
        cmds.put("HotSpot AOT Good", List.of("java", "-XX:AOTCache=good.aot", "-jar", "./target/quarkus-app/quarkus-run.jar"));
        cmds.put("HotSpot AOT Bad", List.of("java", "-XX:AOTCache=bad.aot", "-jar", "./target/quarkus-app/quarkus-run.jar"));
        cmds.put("Native Image", List.of("./target_native/webapp-1.0.0-SNAPSHOT-runner"));
        cmds.forEach((k, v) -> {
            System.out.println("===========================\n" + k + "\n" + v);
            try {
                for (int i = 0; i < 10; i++) {
                    runProcess(v, true);
                }
                runProcess(v, false);
            } catch (Exception e) {
                System.err.println("Eh..: " + e.getMessage());
            }
        });
    }

    private static void runProcess(List<String> command, boolean silent)
            throws URISyntaxException, IOException, InterruptedException {
        final HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(25)) // eh, sketchy
                .build();
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8080/sunset"))
                .GET()
                .build();
        final Path logFile = Files.createTempFile("quarkus-run-", ".log");
        Process process = null;
        try {
            process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(logFile.toFile())
                    .start();
            long startTimeNs = System.nanoTime();
            if (!silent) {
                System.out.println("Quarkus process started (PID: " + process.pid() + "). Polling for first response...");
            }
            long firstResponseTimeNs = 0;
            while (System.nanoTime() - startTimeNs < TIMEOUT_NS) {
                try {
                    final HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                    if (response.statusCode() == 200 && response.body().length > 0) {
                        firstResponseTimeNs = System.nanoTime() - startTimeNs;
                        if (!silent) {
                            System.out.printf("First HTTP 200 response after %.6f ms%n", firstResponseTimeNs / 1_000_000.0);
                        }
                        break;
                    }
                } catch (java.net.http.HttpConnectTimeoutException | java.net.ConnectException | InterruptedException e) {
                    // nop
                }
                Thread.yield();
            }
            if (firstResponseTimeNs == 0) {
                System.err.println("Meh, giving up.");
            }
            if (!silent) {
                System.out.println("RSS: " + getRSSkB(process.pid()) / 1024 + " MB");
            }
        } finally {
            if (process != null) {
                process.destroy();
                process.waitFor();
            }
            if (logFile != null && Files.exists(logFile)) {
                if (!silent) {
                    Files.readAllLines(logFile).stream()
                            .filter(l -> l.contains("started in")).findFirst()
                            .ifPresent(System.out::println);
                }
                Files.delete(logFile);
            }
        }
    }

    private static final Pattern NUM_PATTERN = Pattern.compile("[ \t]*[0-9]+[ \t]*");
    public static final boolean IS_THIS_WINDOWS = System.getProperty("os.name").matches(".*[Ww]indows.*");
    public static long getRSSkB(long pid) throws IOException, InterruptedException {
        ProcessBuilder pa;
        if (IS_THIS_WINDOWS) {
            // Note that PeakWorkingSetSize might be better, but we would need to change it on Linux too...
            // https://docs.microsoft.com/en-us/windows/win32/cimwin32prov/win32-process
            pa = new ProcessBuilder("wmic", "process", "where", "processid=" + pid, "get", "WorkingSetSize");
        } else {
            pa = new ProcessBuilder("ps", "-p", Long.toString(pid), "-o", "rss=");
        }
        final Map<String, String> envA = pa.environment();
        envA.put("PATH", System.getenv("PATH"));
        pa.redirectErrorStream(true);
        final Process p = pa.start();
        try (BufferedReader processOutputReader =
                new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String l;
            while ((l = processOutputReader.readLine()) != null) {
                if (NUM_PATTERN.matcher(l).matches()) {
                    if (IS_THIS_WINDOWS) {
                        // Qualifiers: DisplayName ("Working Set Size"), Units ("bytes")
                        return Long.parseLong(l.trim()) / 1024L;
                    } else {
                        return Long.parseLong(l.trim());
                    }
                }
            }
            p.waitFor();
        }
        return -1L;
    }
}
