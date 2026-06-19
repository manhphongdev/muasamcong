package com.muasamcong.service.storage.impl;

import com.muasamcong.service.storage.StoredFile;
import com.muasamcong.service.storage.SyncFolderEntry;
import com.muasamcong.service.storage.SyncStorageService;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.codelibs.jcifs.smb.impl.SmbFile;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SyncStorageServiceImpl implements SyncStorageService {
    private static final String AUTO_DOWNLOAD_FOLDER = "auto-download";

    private final CachingSessionFactory<SmbFile> smbSessionFactory;

    @Override
    public boolean isDirectory(String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }
        String cleaned = cleanSmbPath(path);
        if (isSmbRelativePath(path)) {
            try (var session = smbSessionFactory.getSession()) {
                return session.exists(cleaned);
            } catch (Exception ex) {
                return false;
            }
        }

        try {
            return Files.isDirectory(Path.of(path.trim()));
        } catch (InvalidPathException ex) {
            return false;
        }
    }

    @Override
    public List<SyncFolderEntry> listDirectories(String parentPath) {
        String cleaned = cleanSmbPath(parentPath);
        if (isSmbRelativePath(parentPath)) {
            try (var session = smbSessionFactory.getSession()) {
                return Arrays.stream(session.list(cleaned))
                        .filter(this::isSmbDirectory)
                        .map(file -> folderEntry(cleaned, file))
                        .toList();
            } catch (IOException ex) {
                throw new IllegalStateException("Cannot list SMB folder: " + cleaned, ex);
            }
        }

        Path parent = Path.of(parentPath.trim()).toAbsolutePath().normalize();
        try (var children = Files.list(parent)) {
            return children
                    .filter(Files::isDirectory)
                    .map(path -> new SyncFolderEntry(path.getFileName().toString(), path.toAbsolutePath().normalize().toString()))
                    .toList();
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot list folder: " + parent, ex);
        }
    }

    @Override
    public String resolveChild(String parentPath, String childName) {
        if (isSmbRelativePath(parentPath)) {
            return joinSmbPath(cleanSmbPath(parentPath), childName);
        }
        return Path.of(parentPath.trim()).resolve(childName).toAbsolutePath().normalize().toString();
    }

    @Override
    public String resolveAutoDownloadPath(String sourcePath) {
        if (isSmbRelativePath(sourcePath)) {
            return joinSmbPath(cleanSmbPath(sourcePath), AUTO_DOWNLOAD_FOLDER);
        }
        return Path.of(sourcePath.trim()).resolve(AUTO_DOWNLOAD_FOLDER).toString();
    }

    @Override
    public StoredFile write(String basePath, String relativePath, String fileName, InputStream inputStream) {
        if (isSmbRelativePath(basePath)) {
            String directory = joinSmbPath(cleanSmbPath(basePath), cleanRelativePath(relativePath));
            String target = joinSmbPath(directory, fileName);
            String tempTarget = target + ".part";
            CountingInputStream countingInputStream = new CountingInputStream(inputStream);
            try (var session = smbSessionFactory.getSession(); countingInputStream) {
                ensureSmbDirectories(session, directory);
                removeIfExists(session, tempTarget);
                session.write(countingInputStream, tempTarget);
                removeIfExists(session, target);
                session.rename(tempTarget, target);
                return new StoredFile(target, countingInputStream.count());
            } catch (IOException ex) {
                cleanupSmbTempFile(tempTarget);
                throw new IllegalStateException("Cannot save SMB file: " + safeMessage(ex), ex);
            }
        }

        Path base = basePath(basePath);
        Path target = base.resolve(cleanRelativePath(relativePath)).resolve(fileName).normalize();
        Path tempTarget = target.resolveSibling(target.getFileName() + ".part");
        ensureInsideBasePath(base, target);
        ensureInsideBasePath(base, tempTarget);
        try {
            Files.createDirectories(target.getParent());
            Files.deleteIfExists(tempTarget);
            try (inputStream; OutputStream outputStream = Files.newOutputStream(
                    tempTarget,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            )) {
                inputStream.transferTo(outputStream);
            }
            moveCompletedFile(tempTarget, target);
            return new StoredFile(target.toAbsolutePath().toString(), Files.size(target));
        } catch (IOException ex) {
            cleanupLocalTempFile(tempTarget);
            throw new IllegalStateException("Cannot save file: " + safeMessage(ex), ex);
        }
    }

    private SyncFolderEntry folderEntry(String parentPath, SmbFile file) {
        String name = cleanSmbName(file.getName());
        return new SyncFolderEntry(name, joinSmbPath(parentPath, name));
    }

    private boolean isSmbDirectory(SmbFile file) {
        try {
            return file.isDirectory();
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isSmbRelativePath(String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }
        String trimmed = path.trim();
        if (trimmed.startsWith("\\\\") || trimmed.matches("^[A-Za-z]:[\\\\/].*")) {
            return false;
        }
        try {
            return !Path.of(trimmed).isAbsolute();
        } catch (InvalidPathException ex) {
            return false;
        }
    }

    private String cleanSmbPath(String path) {
        if (!StringUtils.hasText(path)) {
            return "";
        }
        return cleanRelativePath(path).replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private String cleanRelativePath(String path) {
        if (!StringUtils.hasText(path)) {
            return "";
        }
        String cleaned = StringUtils.cleanPath(path).replace('\\', '/');
        if (cleaned.startsWith("../") || cleaned.equals("..")) {
            throw new IllegalArgumentException("Invalid path");
        }
        return cleaned;
    }

    private String joinSmbPath(String first, String second) {
        String left = cleanSmbPath(first);
        String right = cleanSmbPath(second);
        if (!StringUtils.hasText(left)) {
            return right;
        }
        if (!StringUtils.hasText(right)) {
            return left;
        }
        return left + "/" + right;
    }

    private String cleanSmbName(String name) {
        return name == null ? "" : name.replaceAll("/+$", "");
    }

    private void ensureSmbDirectories(
            org.springframework.integration.file.remote.session.Session<SmbFile> session,
            String directory
    ) throws IOException {
        String cleaned = cleanSmbPath(directory);
        if (!StringUtils.hasText(cleaned)) {
            return;
        }

        Object client = session.getClientInstance();
        if (!(client instanceof SmbFile share)) {
            session.mkdir(cleaned);
            return;
        }

        StringBuilder current = new StringBuilder();
        for (String part : cleaned.split("/")) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            if (!current.isEmpty()) {
                current.append('/');
            }
            current.append(part);
            ensureSmbDirectory(share, current.toString());
        }
    }

    private void ensureSmbDirectory(SmbFile share, String path) throws IOException {
        String directoryPath = cleanSmbPath(path) + "/";
        SmbFile directory = new SmbFile(share, directoryPath);
        if (!directory.exists()) {
            directory.mkdir();
        }
        if (!directory.isDirectory()) {
            throw new IOException("SMB path is not a directory: " + path);
        }
    }

    private void removeIfExists(org.springframework.integration.file.remote.session.Session<SmbFile> session, String path) throws IOException {
        if (session.exists(path)) {
            session.remove(path);
        }
    }

    private void cleanupSmbTempFile(String tempTarget) {
        try (var session = smbSessionFactory.getSession()) {
            removeIfExists(session, tempTarget);
        } catch (Exception ignored) {
        }
    }

    private void moveCompletedFile(Path tempTarget, Path target) throws IOException {
        try {
            Files.move(tempTarget, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            Files.move(tempTarget, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void cleanupLocalTempFile(Path tempTarget) {
        try {
            Files.deleteIfExists(tempTarget);
        } catch (IOException ignored) {
        }
    }

    private Path basePath(String path) {
        if (!StringUtils.hasText(path)) {
            throw new IllegalArgumentException("basePath is required");
        }
        return Path.of(path.trim()).toAbsolutePath().normalize();
    }

    private void ensureInsideBasePath(Path basePath, Path path) {
        if (!path.toAbsolutePath().normalize().startsWith(basePath)) {
            throw new IllegalArgumentException("Invalid path");
        }
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        return StringUtils.hasText(message) ? message : ex.getClass().getSimpleName();
    }

    private static class CountingInputStream extends FilterInputStream {
        private long count;

        CountingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            if (value != -1) {
                count++;
            }
            return value;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int read = super.read(b, off, len);
            if (read > 0) {
                count += read;
            }
            return read;
        }

        long count() {
            return count;
        }
    }
}
