package loadtesting.launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static loadtesting.ConfigUtils.getEnvVar;

/**
 * Runs every Gatling simulation in its own JVM, in parallel.
 * The gatling-amqp plugin cannot handle several RabbitMQ connections within a single JVM, hence one process per simulation.
 * Exits with 0 only if every simulation succeeded.
 */
public final class TmcLauncher {
    private static final Logger log = LoggerFactory.getLogger(TmcLauncher.class);

    private static final String SIMULATIONS_LIST = "tmc-simulations.txt";
    private static final String TMP_DIR = System.getProperty("java.io.tmpdir");
    private static final List<String> CHILD_JVM_FLAGS = List.of(
            "-Xmx256m",
            "-XX:MaxInlineLevel=20",
            "-XX:MaxTrivialSize=12",
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
            "-Djava.util.prefs.userRoot=" + Paths.get(TMP_DIR, "java-prefs")
    );

    private TmcLauncher() {
    }

    public static void main(String[] args) throws Exception {
        List<String> simulations = selectSimulations(loadSimulations(), getEnvVar("TMC_SIMULATIONS"));
        String resultsDir = getEnvVar("TMC_RESULTS_DIR");
        if (resultsDir == null || resultsDir.isBlank()) {
            resultsDir = Paths.get(TMP_DIR, "tmc-results").toString();
        }
        log.info("Running {} simulation(s) in parallel, results in {}", simulations.size(), resultsDir);

        // Children must not outlive the launcher (Gradle cancellation, Ctrl+C)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> ProcessHandle.current().descendants().forEach(ProcessHandle::destroy)));
        Map<String, Process> processes = new LinkedHashMap<>();
        for (String simulation : simulations) {
            processes.put(simulation, start(simulation, resultsDir));
        }

        List<String> failures = new ArrayList<>();
        for (Map.Entry<String, Process> entry : processes.entrySet()) {
            int exitCode = entry.getValue().waitFor();
            if (exitCode != 0) {
                failures.add(entry.getKey() + " (exit code " + exitCode + ")");
            }
        }
        if (!failures.isEmpty()) {
            log.error("Failed simulation(s): {}", failures);
            System.exit(1);
        }
        log.info("All {} simulation(s) succeeded", simulations.size());
    }

    private static List<String> loadSimulations() throws IOException {
        try (InputStream in = TmcLauncher.class.getClassLoader().getResourceAsStream(SIMULATIONS_LIST)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource " + SIMULATIONS_LIST);
            }
            return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .toList();
        }
    }

    /**
     * Keeps only the simulations whose simple name is listed in {@code filter} (comma-separated), or all if blank.
     */
    private static List<String> selectSimulations(List<String> available, String filter) {
        if (filter == null || filter.isBlank()) {
            return available;
        }
        Set<String> wanted = Arrays.stream(filter.split(",")).map(String::trim).filter(name -> !name.isEmpty())
                .collect(Collectors.toSet());
        List<String> selected = available.stream().filter(simulation -> wanted.contains(simpleName(simulation))).toList();
        if (selected.size() != wanted.size()) {
            throw new IllegalArgumentException("Unknown simulation in TMC_SIMULATIONS=" + filter + ", available: " + available);
        }
        return selected;
    }

    private static Process start(String simulation, String resultsDir) throws IOException {
        String name = simpleName(simulation);
        List<String> command = new ArrayList<>();
        command.add(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
        command.addAll(CHILD_JVM_FLAGS);
        command.addAll(List.of("-cp", System.getProperty("java.class.path"), "io.gatling.app.Gatling",
                "-s", simulation, "-rf", resultsDir));

        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        Thread pump = new Thread(() -> appendClassPrefixInLog(name, process.getInputStream()), "output-" + name);
        pump.setDaemon(true);
        pump.start();
        log.info("Started {} (pid {})", simulation, process.pid());
        return process;
    }

    private static void appendClassPrefixInLog(String name, InputStream output) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(output, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[" + name + "] " + line);
            }
        } catch (IOException e) {
            // Expected when the process is killed: its output stream gets closed under the reader
            log.debug("Output of simulation {} closed", name, e);
        }
    }

    private static String simpleName(String simulation) {
        return simulation.substring(simulation.lastIndexOf('.') + 1);
    }
}
