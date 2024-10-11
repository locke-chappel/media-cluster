package io.github.lc.oss.mc.scheduler;

import org.h2.mvstore.MVStoreException;
import org.springframework.boot.builder.SpringApplicationBuilder;

public class Application {
    public static void main(String[] args) {
        Application app = new Application();
        app.run(args);
    }

    /*
     * Exposed for testing
     */
    void run(String[] args) {
        try {
            SpringApplicationBuilder builder = this.build();
            builder.run(args);
        } catch (Exception e) {
            Throwable ex = e;
            while (!this.isRoot(ex)) {
                ex = ex.getCause();
            }

            if (ex instanceof MVStoreException) {
                System.err.println("Database driver error. Likely due to version upgrade. "
                        + "You may need to backup your database using an older version of "
                        + "the version, delete the database file, start the newer "
                        + "version of the scheduler, and then restore the backup.");
                this.exit();
            }
        }
    }

    private boolean isRoot(Throwable t) {
        if (t.getCause() == null) {
            return true;
        }

        if (t.getCause().equals(t)) {
            return true;
        }

        return false;
    }

    /*
     * Exposed for testing
     */
    SpringApplicationBuilder build() {
        return new SpringApplicationBuilder(ApplicationConfig.class);
    }

    /*
     * Exposed for testing
     */
    void exit() {
        /*
         * Note: missing code coverage - properly testing this call in JDK17+ is
         * non-trivial, deferred for now. This should be the _only_ untested line of
         * code in the coverage report.
         */
        System.exit(1);
    }

    /*
     * Exposed for testing
     */
    Application() {
    }
}
