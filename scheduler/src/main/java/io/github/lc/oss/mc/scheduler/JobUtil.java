package io.github.lc.oss.mc.scheduler;

public class JobUtil {
    public static String getJobKey(Class<?> jobClass) {
        return "application.jobs." + jobClass.getSimpleName();
    }

    private JobUtil() {
    }
}
