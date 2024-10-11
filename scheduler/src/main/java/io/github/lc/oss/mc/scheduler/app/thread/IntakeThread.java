package io.github.lc.oss.mc.scheduler.app.thread;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.github.lc.oss.mc.entity.Constants;
import io.github.lc.oss.mc.scheduler.app.service.JobService;

@Component
public class IntakeThread extends AbstractThread {
    @Autowired
    private JobService jobService;

    @Value("#{pathNormalizer.dirOsAware('${application.media.root}')}")
    private String root;

    @Override
    public void run() {
        this.setRunning(true);

        Path path = this.getMediaRoot();
        try {
            if (!(boolean) Files.getAttribute(path, "basic:isDirectory", LinkOption.NOFOLLOW_LINKS)) {
                this.setRunning(false);
                throw new RuntimeException("application.media.root is not a valid directory");
            }
        } catch (IOException ex) {
            this.setRunning(false);
            throw new RuntimeException("Error checking for media root at " + path, ex);
        }

        FileSystem fs = path.getFileSystem();
        try (WatchService service = fs.newWatchService()) {
            path.register(service, //
                    StandardWatchEventKinds.ENTRY_CREATE, //
                    StandardWatchEventKinds.ENTRY_MODIFY, //
                    StandardWatchEventKinds.ENTRY_DELETE);

            WatchKey key = null;
            while (this.shouldRun()) {
                key = service.take();

                Kind<?> kind = null;
                for (WatchEvent<?> event : key.pollEvents()) {
                    kind = event.kind();

                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        @SuppressWarnings("unchecked")
                        Path newPath = ((WatchEvent<Path>) event).context();

                        this.jobService.newFile(newPath.getFileName().toString());
                    }
                }
            }
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException("Error watching filesystem", ex);
        } finally {
            this.setRunning(false);
        }
    }

    /*
     * Exposed for testing
     */
    protected Path getMediaRoot() {
        return Paths.get(this.root + Constants.FILE_SEPARATOR + "new");
    }
}
