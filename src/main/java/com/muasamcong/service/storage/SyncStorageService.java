package com.muasamcong.service.storage;

import java.io.InputStream;
import java.util.List;

public interface SyncStorageService {
    boolean isDirectory(String path);

    List<SyncFolderEntry> listDirectories(String parentPath);

    String resolveChild(String parentPath, String childName);

    String resolveAutoDownloadPath(String sourcePath);

    StoredFile write(String basePath, String relativePath, String fileName, InputStream inputStream);
}
