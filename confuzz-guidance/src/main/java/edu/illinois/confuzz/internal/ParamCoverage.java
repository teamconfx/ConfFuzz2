package edu.illinois.confuzz.internal;

import edu.berkeley.cs.jqf.fuzz.util.Coverage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ParamCoverage {

    private Set<String> paramSet = new HashSet<>();

    public ParamCoverage() {

    }

    public ParamCoverage(ParamCoverage other) {
        this.paramSet.addAll(other.getParamSet());
    }

    public ParamCoverage(Set<String> paramSet) {
        this.paramSet.addAll(paramSet);
    }

    public void collectParamCoverage() {
        this.paramSet.addAll(ConfigTracker.getCurConfigMap().keySet());
    }

    /**
     *
     * @return
     */
    public boolean updateParamCoverage(ParamCoverage other) {
        boolean changed = false;
        // TODO: a logic to check which parameter byte change causing this coverage increase
        for (String param : other.getParamSet()) {
            if (!paramSet.contains(param)) {
                paramSet.add(param);
                changed = true;
            }
        }
        return changed;
    }

    /**
     * A method to compute the parameter in this coverage but not in the other coverage
     * @return
     */
    public Collection<?> computeNewParamCoverage(ParamCoverage other) {
        Collection<String> newParamCoverage = new HashSet<>();
        for (String param : paramSet) {
            if (!other.getParamSet().contains(param)) {
                newParamCoverage.add(param);
            }
        }
        return newParamCoverage;
    }

    public Set<String> getParamSet() {
        return paramSet;
    }

    public int size() {
        return paramSet.size();
    }

    public void clear() {
        paramSet.clear();
    }

    @Override
    public String toString() {
        String str = "ParamCoverage: \n";
        for (String param : paramSet) {
            str += param + "\t";
        }
        return str;
    }
}
