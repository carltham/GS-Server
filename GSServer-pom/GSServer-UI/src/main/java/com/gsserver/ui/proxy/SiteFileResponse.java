package com.gsserver.ui.proxy;

/**
 * The nginx reverse-proxy site file for editing.
 *
 * @param path absolute path of the site file on the server
 * @param exists whether the file already exists on disk
 * @param content the file's current content when it exists and is readable, otherwise a prefilled
 *     template that proxies to this application
 */
public record SiteFileResponse(String path, boolean exists, String content) {}
