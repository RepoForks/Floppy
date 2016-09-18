package io.fabianterhorst.floppy;

import org.nustaq.serialization.FSTConfiguration;

import java.io.File;
import java.io.IOException;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

/**
 * Created by fabianterhorst on 18.09.16.
 */

public class Disk {

    private static FSTConfiguration mConfig = FSTConfiguration.createDefaultConfiguration();

    private String mName;

    private String mPath;

    private String mFilesDir;

    public Disk(String name, String path) {
        this.mName = name;
        this.mPath = path;
        createDiskDir();
    }

    public void write(String key, Object object) {
        final File originalFile = getOriginalFile(key);
        final File backupFile = makeBackupFile(originalFile);
        // Rename the current file so it may be used as a backup during the next read
        if (originalFile.exists()) {
            //Rename original to backup
            if (!backupFile.exists()) {
                if (!originalFile.renameTo(backupFile)) {
                    throw new RuntimeException("Couldn't rename file " + originalFile
                            + " to backup file " + backupFile);
                }
            } else {
                //Backup exist -> original file is broken and must be deleted
                //noinspection ResultOfMethodCallIgnored
                originalFile.delete();
            }
        }
        try {
            BufferedSink bufferedSink = Okio.buffer(Okio.sink(originalFile));
            bufferedSink.write(mConfig.asByteArray(object));
            bufferedSink.flush();
            bufferedSink.close(); //also close file stream
            // Writing was successful, delete the backup file if there is one.
            //noinspection ResultOfMethodCallIgnored
            backupFile.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public <T> T read(String key) {
        final File originalFile = getOriginalFile(key);
        final File backupFile = makeBackupFile(originalFile);
        if (backupFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            originalFile.delete();
            //noinspection ResultOfMethodCallIgnored
            backupFile.renameTo(originalFile);
        }

        if (!exist(key)) {
            return null;
        }

        try {
            BufferedSource bufferedSource = Okio.buffer(Okio.source(originalFile));
            return (T) mConfig.asObject(bufferedSource.readByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized boolean exist(String key) {
        final File originalFile = getOriginalFile(key);
        return originalFile.exists();
    }

    public void delete(String key) {
        final File originalFile = getOriginalFile(key);
        if (!originalFile.exists()) {
            return;
        }

        boolean deleted = originalFile.delete();
        if (!deleted) {
            throw new RuntimeException("Couldn't delete file " + originalFile
                    + " for table " + key);
        }
    }

    public void deleteAll() {
        final String dbPath = getDbPath(mPath, mName);
        if (!deleteDirectory(dbPath)) {
            System.out.print("Couldn't delete Floppy dir " + dbPath);
        }
    }

    private File getOriginalFile(String key) {
        final String tablePath = mFilesDir + File.separator + key + ".pt";
        return new File(tablePath);
    }

    private String getDbPath(String path, String dbName) {
        return path + File.separator + dbName;
    }

    private void createDiskDir() {
        mFilesDir = getDbPath(mPath, mName);
        if (!new File(mFilesDir).exists()) {
            boolean isReady = new File(mFilesDir).mkdirs();
            if (!isReady) {
                throw new RuntimeException("Couldn't create Floppy dir: " + mFilesDir);
            }
        }
    }

    private static boolean deleteDirectory(String dirPath) {
        File directory = new File(dirPath);
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (null != files) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file.toString());
                    } else {
                        //noinspection ResultOfMethodCallIgnored
                        file.delete();
                    }
                }
            }
        }
        return directory.delete();
    }

    private File makeBackupFile(File originalFile) {
        return new File(originalFile.getPath() + ".bak");
    }
}