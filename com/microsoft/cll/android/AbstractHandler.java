package com.microsoft.cll.android;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An Abstract Handler class
 */
public abstract class AbstractHandler {
    private final String TAG = "AndroidCll-AbstractHandler";
    protected final ILogger logger;
    protected final ClientTelemetry clientTelemetry;
    protected FileStorage fileStorage;
    protected String filePath;

    protected final static String criticalEventFileExtension = ".crit.cllevent";
    protected final static String normalEventFileExtension = ".norm.cllevent";
    protected static AtomicLong totalStorageUsed = new AtomicLong(0);

    public AbstractHandler(ILogger logger, String filePath, ClientTelemetry clientTelemetry) {
        this.filePath       = filePath;
        this.logger         = logger;
        this.clientTelemetry = clientTelemetry;

        setFileStorageUsed();
    }

    public abstract void add(String event, List<String> ids) throws IOException, FileStorage.FileFullException;

    public abstract List<IStorage> getFilesForDraining();

    public abstract void close();

    public abstract void dispose(IStorage storage);

    /**
     * Checks to see if there is room to add this event
     * @param serializedEvent The event to add
     * @return True if we can or false if we can't
     */
    public boolean canAdd(Tuple serializedEvent) {
        if(totalStorageUsed.get() + ((String)serializedEvent.a).length() <= SettingsStore.getCllSettingsAsInt(SettingsStore.Settings.MAXFILESSPACE)) {
            return true;
        }

        return false;
    }

    /**
     * Get all files that aren't currently being written to
     * @return A list of files with a specific extension
     */
    protected List<IStorage> getFilesByExtensionForDraining(final String fileExtension) {
        List<IStorage> fullFiles = new ArrayList<IStorage>();
        for(File file : findExistingFiles(fileExtension)) {
            try {
                IStorage storage = new FileStorage(logger, file.getAbsolutePath(), this);
                fullFiles.add(storage);
                storage.close();
            }catch (Exception e) {
                logger.info(TAG, "File " + file.getName() + " is in use still");
            }
        }

        return fullFiles;
    }

    /**
     * Looks for any existing critical cll event files on disk
     * @param fileExtension The file extension of files to lok for
     * @return An array of files
     */
    protected File[] findExistingFiles(final String fileExtension) {
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase();
                if (lowercaseName.endsWith(fileExtension)) {
                    return true;
                } else {
                    return false;
                }
            }
        };

        File[] files = new File(filePath).listFiles(filter);
        if(files == null) {
            files = new File[] {};
        }

        return files;
    }

    /**
     * Sets the storage used by all files on disk
     */
    private void setFileStorageUsed() {
        totalStorageUsed.set(0);

        // Get space used by critical files
        for(File file : findExistingFiles(criticalEventFileExtension)) {
            totalStorageUsed.getAndAdd(file.length());
        }

        // Get space used by normal files
        for(File file : findExistingFiles(normalEventFileExtension)) {
            totalStorageUsed.getAndAdd(file.length());
        }
    }

    protected boolean ensureCanAdd(Tuple<String, List<String>> tuple, EventEnums.Persistence persistence)
    {
        int maxAttempts = SettingsStore.getCllSettingsAsInt(SettingsStore.Settings.MAXCRITICALCANADDATTEMPTS);
        boolean considerCritical = (persistence == EventEnums.Persistence.PersistenceCritical);

        int attempts = 0;
        boolean dropFileResult = true;
        boolean canAddResult = canAdd(tuple);

        while (!canAddResult && attempts < maxAttempts && dropFileResult)
        {
            logger.warn(TAG, "Out of storage space. Attempting to drop one oldest file.");

            // try dropping the oldest normal persistence event file,
            // or, if allowed and in absence of normal files, critical.
            dropFileResult = dropOldestFile(considerCritical);
            canAddResult = canAdd(tuple);
            attempts++;
        }

        return canAddResult;
    }

    /**
     * Drop the least recent file from the disk (unless it is the current normal file that is being written to)
     */
    protected boolean dropOldestFile(boolean includingCritical)
    {
        File[] files = findExistingFiles(normalEventFileExtension);

        // We don't want to delete the current normal file since it may be getting written to right now
        // and deleting it will cause problems with the current lock state.
        if (files.length <= 1)
        {
            if (includingCritical)
            {
                files = findExistingFiles(criticalEventFileExtension);
            }
        }

        if (files.length <= 1)
        {
            logger.info(TAG, "There are no files to delete");
            return false;
        }

        long lastModified  = files[0].lastModified();
        File lastModifiedFile = files[0];
        for (File file : files)
        {
            // Newer files have a larger lastModified value. So we want to find the file with the smallest value
            if(file.lastModified() < lastModified)
            {
                lastModified = file.lastModified();
                lastModifiedFile = file;
            }
        }

        long fileLength = lastModifiedFile.length();
        boolean result = deleteFile(lastModifiedFile);

        if (result)
        {
            totalStorageUsed.getAndAdd(-fileLength);
        }

        return result;
    }

    private boolean deleteFile(File file)
    {
        boolean result = false;

        try
        {
            result = file.delete();
        }
        catch (Exception e)
        {
            logger.info(TAG, "Exception while deleting the file: " + e.toString());
        }

        return result;
    }
}