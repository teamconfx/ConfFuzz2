package edu.illinois.confuzz.internal;

import edu.neu.ccs.prl.meringue.Replayer;
import edu.neu.ccs.prl.meringue.ReplayerManager;

public class CoverageForkMain {
    private CoverageForkMain() {
        throw new AssertionError(
                getClass().getSimpleName() + " is a static utility class and should not be instantiated");
    }

    public static void main(String[] args) throws Throwable {
        String testClassName = args[0];
        String testMethodName = args[1];
        String replayerClassName = args[2];
        int maxTraceSize = Integer.parseInt(args[3]);
        int port = Integer.parseInt(args[4]);
        Replayer replayer = (Replayer) Class.forName(replayerClassName).getDeclaredConstructor().newInstance();
        replayer.configure(testClassName, testMethodName, CoverageForkMain.class.getClassLoader());
        try (ReplayerManager manager = new ReplayerManager(port, maxTraceSize)) {
            replayer.accept(manager);
        }
    }
}
