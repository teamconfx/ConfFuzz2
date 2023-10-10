package edu.illinois.confuzz.internal;

import edu.neu.ccs.prl.meringue.ForkConnection;
import edu.neu.ccs.prl.meringue.JvmLauncher;
import edu.neu.ccs.prl.meringue.report.FailureReport;
import edu.neu.ccs.prl.meringue.Failure;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;

public class CoverageAnalyzer implements Closeable {
    private final JvmLauncher launcher;
    private final ConfuzzCoverageReport coverageReport;
    private final long timeout;
    private final ServerSocket server;
    private final FailureReport failureReport;
    private ForkConnection connection;
    private Process process;

    CoverageAnalyzer(JvmLauncher launcher, ConfuzzCoverageReport coverageReport, FailureReport failureReport, long timeout)
            throws IOException {
        if (timeout < -1) {
            throw new IllegalArgumentException();
        }
        if (launcher == null || coverageReport == null || failureReport == null) {
            throw new NullPointerException();
        }
        this.coverageReport = coverageReport;
        this.failureReport = failureReport;
        this.timeout = timeout;
        // Create a server socket bound to an automatically allocated port
        this.server = new ServerSocket(0);
        this.launcher = launcher.appendArguments(String.valueOf(server.getLocalPort()));
    }

    private void restartConnection() throws IOException {
        closeConnection();
        // Launch the analysis JVM
        this.process = launcher.launch();
        // Connection to the JVM
        this.connection = new ForkConnection(server.accept());
    }

    private boolean isConnected() {
        return connection != null && !connection.isClosed();
    }

    void analyze(File inputFile) throws IOException {
        if (!isConnected()) {
            restartConnection();
        }
        Timer timer = timeout < 0 ? null : startInterrupter(inputFile);
        try {
            connection.send(inputFile);
            byte[] execData = connection.receive(byte[].class);
            Failure failure = null;
            String failureMessage = null;
            if (connection.receive(Boolean.class)) {
                failure = connection.receive(Failure.class);
                failureMessage = connection.receive(String.class);
            }
            if (timer != null) {
                timer.cancel();
            }
            failureReport.record(inputFile, failure, failureMessage);
            coverageReport.writeJacocoExecData(coverageReport.record(inputFile, execData), inputFile);
        } catch (Throwable t) {
            // Input caused fork to fail
            closeConnection();
            restartConnection();
        } finally {
            if (timer != null) {
                timer.cancel();
            }
        }
    }

    private Timer startInterrupter(File inputFile) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                closeConnection();
                System.out.println("Timed out: " + inputFile);
            }
        }, Duration.ofSeconds(timeout).toMillis());
        return timer;
    }

    private void closeConnection() {
        if (connection != null && !connection.isClosed()) {
            try {
                connection.send(null);
            } catch (IOException e) {
                // Failed to send shutdown signal
            }
            connection.close();
            connection = null;
        }
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                //
            }
            process = null;
        }
    }

    @Override
    public void close() throws IOException {
        server.close();
        closeConnection();
    }
}
