package io.github.lc.oss.mc.api;

import java.util.Date;

public interface AbstractEntity {
    String getId();

    Date getModified();

    String getModifiedBy();
}
