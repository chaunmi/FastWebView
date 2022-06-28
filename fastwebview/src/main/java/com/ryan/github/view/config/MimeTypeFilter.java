package com.ryan.github.view.config;

/**
 * filter some mime type resources without caching.
 * <p>
 * Created by Ryan
 * 2018/2/11 下午2:56
 */
public interface MimeTypeFilter {

    boolean isContains(String mimeType);

    void addMimeType(String mimeType);

    void removeMimeType(String mimeType);

    void clear();

}
