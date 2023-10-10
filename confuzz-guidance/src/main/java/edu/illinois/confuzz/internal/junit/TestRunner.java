package edu.illinois.confuzz.internal.junit;

import edu.berkeley.cs.jqf.fuzz.junit.TrialRunner;
import edu.illinois.confuzz.internal.ConfuzzGuidance;
import org.junit.After;
import org.junit.Before;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.internal.runners.statements.RunAfters;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;

public class TestRunner extends TrialRunner {
    public TestRunner(Class<?> testClass, FrameworkMethod method, Object[] args) throws InitializationError {
        super(testClass, method, args);
    }

    /**
     * wraps the method to run with a statement that invokes methods annotated with @Before and @BeforeClass
     * @param method method to run
     * @param target test class instance
     * @param statement statement to run
     * @return
     */
    @Override
    protected Statement withBefores(FrameworkMethod method, Object target,
                                    Statement statement) {
        List<FrameworkMethod> befores;
        if (Boolean.getBoolean(ConfuzzGuidance.needClassMethod)) {
            befores = new LinkedList<>(getTestClass().getAnnotatedMethods(BeforeClass.class));
            befores.addAll(getTestClass().getAnnotatedMethods(Before.class));
        } else {
            befores = new LinkedList<>(getTestClass().getAnnotatedMethods(Before.class));
            System.setProperty(ConfuzzGuidance.needClassMethod, "true");
        }
        return befores.isEmpty() ? statement : new RunBefores(statement, befores, target);
    }

    @Override
    protected Statement withAfters(FrameworkMethod method, Object target,
                                   Statement statement) {
        List<FrameworkMethod> afters = new LinkedList<>(getTestClass().getAnnotatedMethods(AfterClass.class));
        afters.addAll(getTestClass().getAnnotatedMethods(After.class));
        return afters.isEmpty() ? statement : new RunAfters(statement, afters, target);
    }
}
