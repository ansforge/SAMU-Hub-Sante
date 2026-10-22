package loadtesting.launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static loadtesting.ConfigUtils.getEnvVar;
import static loadtesting.ConfigUtils.getNumericEnvVar;

/**
 * Runs every Gatling simulation in its own JVM, in parallel.
 * The gatling-amqp plugin cannot handle several RabbitMQ connections within a single JVM, hence one process per simulation.
 * Exits with 0 only if every simulation succeeded before the timeout.
 */
public final class TmcLauncher {
    private static final Logger log = LoggerFactory.getLogger(TmcLauncher.class);

    static final String SIMULATIONS_RESOURCE = "tmc-simulations.txt";
    private static final String GATLING_MAIN_CLASS = "io.gatling.app.Gatling";
    // JVM flags the Gatling Gradle plugin passes to its forked JVMs; required by Gatling on Java 17+
    private static final List<String> GATLING_JVM_FLAGS = List.of(
            "-XX:MaxInlineLevel=20",
            "-XX:MaxTrivialSize=12",
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED"
    );
    private static final String DEFAULT_CHILD_JVM_OPTS = "-Xmx512m";
    private static final int DEFAULT_TIMEOUT_MINUTES = 120;
    private static final long KILL_GRACE_SECONDS = 10;

    private TmcLauncher() {
    }

    public static void main(String[] args) throws Exception {
        List<String> simulations = selectSimulations(loadSimulations(), getEnvVar("TMC_SIMULATIONS"));
        Path resultsDir = Paths.get(getEnvVarOrDefault("TMC_RESULTS_DIR",
                Paths.get(System.getProperty("java.io.tmpdir"), "tmc-results").toString()));
        Files.createDirectories(resultsDir);
        List<String> childJvmOpts = splitOptions(getEnvVarOrDefault("TMC_CHILD_JVM_OPTS", DEFAULT_CHILD_JVM_OPTS));
        int timeoutMinutes = getNumericEnvVar("TMC_TIMEOUT_MINUTES", DEFAULT_TIMEOUT_MINUTES);

        log.info("Running {} simulation(s) in parallel, timeout {} min, results in {}", simulations.size(), timeoutMinutes, resultsDir);

        Map<String, Process> processes = new LinkedHashMap<>();
        // Kubernetes stops the pod with SIGTERM on the launcher (PID 1): children must not outlive it
        List<Process> started = new CopyOnWriteArrayList<>();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> kill(started)));
        for (String simulation : simulations) {
            Process process = start(simulation, childJvmOpts, resultsDir);
            started.add(process);
            processes.put(simulation, process);
        }

        long deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(timeoutMinutes);
        Map<String, String> failures = new LinkedHashMap<>();
        List<Process> timedOut = new ArrayList<>();
        for (Map.Entry<String, Process> entry : processes.entrySet()) {
            Process process = entry.getValue();
            long remaining = Math.max(0, deadline - System.nanoTime());
            if (!process.waitFor(remaining, TimeUnit.NANOSECONDS)) {
                timedOut.add(process);
                failures.put(entry.getKey(), "timed out after " + timeoutMinutes + " min");
            } else if (process.exitValue() != 0) {
                failures.put(entry.getKey(), "exit code " + process.exitValue());
            }
        }
        kill(timedOut);

        if (!failures.isEmpty()) {
            failures.forEach((simulation, reason) -> log.error("Simulation {} failed: {}", simulation, reason));
            System.exit(1);
        }
        log.info("All {} simulation(s) succeeded", simulations.size());
    }

    static List<String> loadSimulations() throws IOException {
        try (InputStream in = TmcLauncher.class.getClassLoader().getResourceAsStream(SIMULATIONS_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource " + SIMULATIONS_RESOURCE);
            }
            return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .toList();
        }
    }

    /**
     * Keeps only the simulations listed in {@code filter} (comma-separated, fully qualified or simple names), or all if blank.
     */
    static List<String> selectSimulations(List<String> available, String filter) {
        if (filter == null || filter.isBlank()) {
            if (available.isEmpty()) {
                throw new IllegalStateException("No simulation found in " + SIMULATIONS_RESOURCE);
            }
            return available;
        }
        List<String> selected = new ArrayList<>();
        for (String name : filter.split(",")) {
            String wanted = name.trim();
            if (wanted.isEmpty()) {
                continue;
            }
            String match = available.stream()
                    .filter(simulation -> simulation.equals(wanted) || simpleName(simulation).equals(wanted))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown simulation " + wanted + ", available: " + available));
            if (!selected.contains(match)) {
                selected.add(match);
            }
        }
        if (selected.isEmpty()) {
            throw new IllegalArgumentException("TMC_SIMULATIONS selects no simulation: " + filter);
        }
        return selected;
    }

    private static Process start(String simulation, List<String> childJvmOpts, Path resultsDir) throws IOException {
        String name = simpleName(simulation);
        List<String> command = new ArrayList<>();
        command.add(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
        command.addAll(GATLING_JVM_FLAGS);
        command.add("-Djava.util.prefs.userRoot=" + Paths.get(System.getProperty("java.io.tmpdir"), "java-prefs"));
        command.addAll(childJvmOpts);
        command.addAll(List.of("-cp", System.getProperty("java.class.path"), GATLING_MAIN_CLASS,
                "-s", simulation,
                "-rf", resultsDir.resolve(name + "-" + System.currentTimeMillis()).toString()));

        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        Thread pump = new Thread(() -> forwardOutput(name, process.getInputStream()), "output-" + name);
        pump.setDaemon(true);
        pump.start();
        log.info("Started {} (pid {})", simulation, process.pid());
        return process;
    }

    private static void forwardOutput(String name, InputStream output) {
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

    /**
     * Asks every process to stop, then force-kills those still alive after a shared grace period.
     */
    private static void kill(List<Process> processes) {
        processes.forEach(Process::destroy);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(KILL_GRACE_SECONDS);
        for (Process process : processes) {
            try {
                long remaining = Math.max(0, deadline - System.nanoTime());
                if (!process.waitFor(remaining, TimeUnit.NANOSECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }
    }

    private static String getEnvVarOrDefault(String key, String defaultValue) {
        String value = getEnvVar(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static List<String> splitOptions(String options) {
        return Arrays.stream(options.trim().split("\\s+")).filter(option -> !option.isEmpty()).toList();
    }

    private static String simpleName(String simulation) {
        return simulation.substring(simulation.lastIndexOf('.') + 1);
    }
}
