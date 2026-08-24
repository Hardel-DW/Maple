package fr.hardel.mapple.metrics;

import java.lang.management.ManagementFactory;
import javax.management.JMException;
import javax.management.ObjectName;

public final class ClassHistogram {
    private ClassHistogram() {
    }

    public static String capture() {
        try {
            Object[] arguments = {new String[0]};
            String[] signature = {String[].class.getName()};
            return (String) ManagementFactory.getPlatformMBeanServer().invoke(new ObjectName("com.sun.management:type=DiagnosticCommand"), "gcClassHistogram", arguments, signature);
        } catch (JMException exception) {
            throw new IllegalStateException("Failed to capture the class histogram", exception);
        }
    }
}
