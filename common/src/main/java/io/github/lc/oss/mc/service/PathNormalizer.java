package io.github.lc.oss.mc.service;

import org.apache.commons.lang3.StringUtils;

import io.github.lc.oss.mc.entity.Constants;

public class PathNormalizer extends io.github.lc.oss.commons.util.PathNormalizer {
    /**
     * Specialized version of the {@linkplain #dir(String)} function that normalized
     * to the OS specific path instead of normalizing to Unix paths.<br/>
     * <br/>
     * Since this application passes paths to external application we need to
     * normalize the OS format and not a common Java format.
     */
    public String dirOsAware(String path) {
        if (StringUtils.isBlank(path)) {
            return StringUtils.EMPTY;
        }

        String tmp = path;
        if (StringUtils.equals("\\", this.getFileSeparator())) {
            tmp = StringUtils.replace(path, "/", this.getFileSeparator());
        } else {
            tmp = StringUtils.replace(path, "\\", this.getFileSeparator());
        }

        if (!tmp.endsWith(this.getFileSeparator())) {
            tmp += this.getFileSeparator();
        }
        return tmp;
    }

    /*
     * Exposed for testing only
     */
    String getFileSeparator() {
        return Constants.FILE_SEPARATOR;
    }
}
