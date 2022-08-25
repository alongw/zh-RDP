package com.microsoft.cll.android;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Critical events are never queued in memory, instead they are directly written to disk.
 */
public class CriticalEventHandler extends AbstractHandler
{
    private final String TAG = "AndroidCll-CriticalEventHandler";

    /**
     * Creates an event handler for critical events
     * @param logger A logger to use
     * @param filePath The filepath where we will store events
     */
    public CriticalEventHandler(ILogger logger, String filePath, ClientTelemetry clientTelemetry)
    {
        super(logger, filePath, clientTelemetry);
        this.fileStorage        = new FileStorage(criticalEventFileExtension, logger, filePath, this);
    }

    /**
     * Adds a critical event to disk storage
     * @param event The event to add
     * @throws Exception An exception if we cannot add
     */
    @Override
    public synchronized void add(String event, List<String> ids) throws IOException, FileStorage.FileFullException
    {
        Tuple<String, List<String>> tuple = new Tuple(event, ids);

        boolean canAddResult = ensureCanAdd(tuple, EventEnums.Persistence.PersistenceCritical);
        if (!canAddResult)
        {
            // Drop event
            clientTelemetry.IncrementEventsDroppedDueToQuota();
            logger.warn(TAG, "Out of storage space for critical events. Logged event was dropped.");
        }

        // If file is full flush and close file, then open a new one
        if (!fileStorage.canAdd(tuple))
        {
            logger.info(TAG, "Closing full file and opening a new one");
            fileStorage.close();
            fileStorage = new FileStorage(criticalEventFileExtension, logger, filePath, this);
        }

        fileStorage.add(tuple);
        totalStorageUsed.getAndAdd(event.length());
        fileStorage.flush();
    }

    /**
     * 1) Close the current file, so it will get uploaded immediately.
     * 2) Get the list of all non open files on disk.
     * 3) Create a new file for any incoming events
     * @return The list of non open critical event files
     */
    @Override
    public synchronized List<IStorage> getFilesForDraining()
    {
        List<IStorage> storageList;

        // Don't close the current file if it is empty
        if(fileStorage.size() > 0) {
            fileStorage.close();
            storageList = getFilesByExtensionForDraining(criticalEventFileExtension);
            fileStorage = new FileStorage(criticalEventFileExtension, logger, filePath, this);
        } else {
            storageList = getFilesByExtensionForDraining(criticalEventFileExtension);
        }

        return storageList;
    }

    /**
     * Nothing to do here since we always flush immediately after writing critical events
     */
    @Override
    public void close()
    {
        logger.info(TAG, "Closing critical file");
        fileStorage.close();
    }

    /**
     * Decrement the storage used by the file we are discarding
     */
    @Override
    public void dispose(IStorage storage)
    {
        this.totalStorageUsed.getAndAdd(-1 * storage.size());
    }
}