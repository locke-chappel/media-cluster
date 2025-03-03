package io.github.lc.oss.mc.service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.ServiceResponse;
import io.github.lc.oss.mc.entity.Constants;

public class FileService extends AbstractService {
    private static final String EXT_REGEX = "[.][^.]+$";

    @Autowired
    private PathNormalizer pathNormalizer;

    @Value("#{pathNormalizer.dirOsAware('${application.media.root}')}")
    private String root;

    public ServiceResponse<?> moveToNew(String file, String clusterName) {
        ServiceResponse<?> response = new ServiceResponse<>();

        String srcDir = this.getProcessingDir(clusterName);

        String destDir = this.getSchedulerDir("new");
        destDir = this.pathNormalizer.dirOsAware(destDir);

        String srcFile = srcDir + file;
        String destFile = destDir + file;

        if (!this.validatePath(srcFile, srcDir)) {
            this.addMessage(response, Messages.Application.InvalidFilePath);
            return response;
        }

        File src = new File(srcFile);
        File dest = new File(destFile);

        if (!src.exists()) {
            this.addMessage(response, Messages.Application.NotFound);
            return response;
        }

        src.renameTo(dest);

        return response;
    }

    public ServiceResponse<?> moveToProcessing(String file, String clusterName) {
        ServiceResponse<?> response = new ServiceResponse<>();

        String srcDir = this.getSchedulerDir("new");
        srcDir = this.pathNormalizer.dirOsAware(srcDir);

        String destDir = this.getProcessingDir(clusterName);

        String srcFile = srcDir + file;
        String destFile = destDir + file;

        if (!this.validatePath(srcFile, srcDir)) {
            this.addMessage(response, Messages.Application.InvalidFilePath);
            return response;
        }

        if (!this.validatePath(destFile, root)) {
            this.addMessage(response, Messages.Application.InvalidFilePath);
            return response;
        }

        File src = new File(srcFile);
        File dest = new File(destFile);

        if (!src.exists()) {
            this.addMessage(response, Messages.Application.NotFound);
            return response;
        }

        if (dest.exists()) {
            this.addMessage(response, Messages.Application.SourceAlradyExistsInCluster);
            return response;
        }

        src.renameTo(dest);

        return response;
    }

    public ServiceResponse<?> moveToComplete(String source, String ext, String clusterName) {
        ServiceResponse<?> response = new ServiceResponse<>();

        String srcDir = this.getDoneDir(clusterName);
        String destDir = this.pathNormalizer.dirOsAware(this.getSchedulerDir("complete"));
        String file = this.getFileNameWithoutExtension(source) + "." + ext;

        String srcFile = srcDir + file;
        String destFile = destDir + file;

        if (!this.validatePath(srcFile, srcDir)) {
            this.addMessage(response, Messages.Application.InvalidFilePath);
            return response;
        }

        File src = new File(srcFile);
        if (!src.exists()) {
            this.addMessage(response, Messages.Application.NotFound);
            return response;
        }

        File dest = new File(destFile);
        src.renameTo(dest);

        return response;
    }

    public ServiceResponse<?> cleanProcessing(String source, String clusterName) {
        ServiceResponse<?> response = new ServiceResponse<>();

        String tmpDir = this.getProcessingDir(clusterName);
        List<String> toDelete = this.findProcessingFiles(this.getFileNameWithoutExtension(source), clusterName);
        toDelete.stream().forEach(f -> new File(tmpDir + f).delete());

        return response;
    }

    public ServiceResponse<?> cleanTemp(String source, String clusterName) {
        ServiceResponse<?> response = new ServiceResponse<>();

        String tmpDir = this.getTempDir(clusterName);
        List<String> toDelete = this.findTempFiles(this.getFileNameWithoutExtension(source), clusterName);
        toDelete.stream().forEach(f -> new File(tmpDir + f).delete());

        return response;
    }

    private String getFileNameWithoutExtension(String file) {
        int index = file.lastIndexOf(".");

        if (index < 0 || index >= file.length() - 1) {
            return file;
        }

        return file.substring(0, index);
    }

    public String getProcessingDir(String clusterName) {
        return this.pathNormalizer.dirOsAware(this.getClusterDir(clusterName, "processing"));
    }

    public String getTempDir(String clusterName) {
        return this.pathNormalizer.dirOsAware(this.getClusterDir(clusterName, "temp"));
    }

    public String getDoneDir(String clusterName) {
        return this.pathNormalizer.dirOsAware(this.getClusterDir(clusterName, "done"));
    }

    public String getNameWithoutExt(String name) {
        return RegExUtils.replaceFirst(name, FileService.EXT_REGEX, StringUtils.EMPTY);
    }

    public List<String> findNewFiles() {
        return this.listSchedulerFiles(null, "new");
    }

    public List<String> findDoneFiles(String prefix, String clusterName) {
        return this.listClusterFiles(prefix, clusterName, "done");
    }

    public List<String> findProcessingFiles(String prefix, String clusterName) {
        return this.listClusterFiles(prefix, clusterName, "processing");
    }

    public List<String> findTempFiles(String prefix, String clusterName) {
        return this.listClusterFiles(prefix, clusterName, "temp");
    }

    private List<String> listSchedulerFiles(String prefix, String path) {
        return this.listFiles(prefix, this.getSchedulerDir(path));
    }

    private List<String> listClusterFiles(String prefix, String clusterName, String path) {
        return this.listFiles(prefix, this.getClusterDir(clusterName, path));
    }

    private String getSchedulerDir(String path) {
        return this.root + path;
    }

    private String getClusterDir(String clusterName, String path) {
        return this.root + "clusters" + this.getFileSeparator() + clusterName.toLowerCase() + this.getFileSeparator()
                + path;
    }

    private List<String> listFiles(String prefix, String dirPath) {
        return this.toPaths(prefix, new File(dirPath).listFiles());
    }

    private boolean validatePath(String path, String rootDir) {
        Path p = Paths.get(path).normalize().toAbsolutePath();
        Path root = Paths.get(rootDir).normalize().toAbsolutePath();

        return p.startsWith(root + getFileSeparator());
    }

    private List<String> toPaths(String prefix, File... files) {
        if (files == null) {
            return new ArrayList<>();
        }

        return Arrays.stream(files). //
                map(f -> f.getName()). //
                filter(name -> {
                    if (StringUtils.isBlank(prefix)) {
                        return true;
                    }
                    return StringUtils.startsWith(name, prefix);
                }). //
                collect(Collectors.toList());
    }

    /*
     * Exposed for testing only
     */
    String getFileSeparator() {
        return Constants.FILE_SEPARATOR;
    }
}
