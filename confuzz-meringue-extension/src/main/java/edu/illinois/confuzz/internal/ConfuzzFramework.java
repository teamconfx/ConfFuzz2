package edu.illinois.confuzz.internal;

import edu.neu.ccs.prl.meringue.Replayer;
import edu.neu.ccs.prl.meringue.ZestFramework;

public class ConfuzzFramework extends ZestFramework {
    @Override
    public Class<? extends Replayer> getReplayerClass() {
        return CoverageReplayer.class;
    }

    @Override
    public String getMainClassName() {
        return FuzzForkMain.class.getName();
    }

    @Override
    public String getCoordinate() {
        return "edu.illinois.confuzz:confuzz-meringue-extension";
    }
}