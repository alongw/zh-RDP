package com.microsoft.cll.android;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * For normal events we queue up 10 at a time then flush to disk to avoid excessive writing.
 */
public class NormalEventHandler extends AbstractHandler {
    private final String TAG = "AndroidCll-NormalEventHandler";
    private ArrayBlockingQueue<Tuple<String, List<String>>> queueStorage;
    private final int queueSize = SettingsStore.getCllSettingsAsInt(SettingsStore.Settings.NORMALEVENTMEMORYQUEUESIZE);

    public NormalEventHandler(ILogger logger, String filePath, ClientTelemetry clientTelemetry) {
        super(logger, filePath, clientTelemetry);
        this.fileStorage        = new FileStorage(normalEventFileExtension, logger, filePath, this);
        this.queueStorage       = new ArrayBlockingQueue<Tuple<String, List<String>>>(queueSize);
    }

    /**
     * Adds an event to a queue that gets written to disk after 10 events are in the queue
     * Events added to queue aren't guaranteed to be sent. If file storage is full, events written to queue won't be written
     * to disk later.
     */
    @Override
    public synchronized void add(String event, List<String> ids) {
        Tuple<String, List<String>> tuple = new Tuple<String, List<String>>(event, ids);
        // If the queue is full write to disk
        if(!queueStorage.offer(tuple)) {
            writeQueueToDisk();
            queueStorage.offer(tuple);
        }
    }

    /**
     * Drains events from queue (opening a new file if necessary), and writes the file(s) to disk
     * We don't actually call close on the file since we really just need to flush to disk to ensure events aren't lost.
     */
    @Override
    public void close() {
        logger.info(TAG, "Closing normal file");
        writeQueueToDisk();
        fileStorage.close();
    }

    @Override
    public void dispose(IStorage storage) {
        this.totalStorageUsed.getAndAdd(-1 * storage.size());
    }

    /**
     * 1) Close the current file, so it will get uploaded immediately.
     * 2) Get the list of all non open files on disk.
     * 3) Create a new file for any incoming events
     * @return The list of non open normal event files
     */
    @Override
    public synchronized List<IStorage> getFilesForDraining() {
        if(queueStorage.size() > 0) {
            writeQueueToDisk();
        }

        List<IStorage> storageList;

        // Don't close the current file if it is empty
        if(fileStorage.size() > 0) {
            fileStorage.close();
            storageList = getFilesByExtensionForDraining(normalEventFileExtension);
            fileStorage = new FileStorage(normalEventFileExtension, logger, filePath, this);
        } else {
            storageList = getFilesByExtensionForDraining(normalEventFileExtension);
        }


        return storageList;
    }

    /**
     * Writes items from the queue to non-volatile storage
     */
    synchronized void writeQueueToDisk()
    {
        try
        {
            List<Tuple<String, List<String>>> events = new ArrayList<Tuple<String, List<String>>>(queueSize);
            queueStorage.drainTo(events);
            logger.info(TAG, "Writing " + events.size() + " events to disk");

            for (Tuple serializedEvent : events)
            {
                boolean canAddResult = ensureCanAdd(serializedEvent, EventEnums.Persistence.PersistenceNormal);
                if (!canAddResult)
                {
                    // Drop event
                    clientTelemetry.IncrementEventsDroppedDueToQuota();
                    logger.warn(TAG, "Out of storage space for normal events. Logged event was dropped.");
                    continue;
                }

                // If file is full flush and close file, then open a new one
                if (!fileStorage.canAdd(serializedEvent))
                {
                    logger.info(TAG, "Closing full file and opening a new one");
                    fileStorage.close();
                    fileStorage = new FileStorage(normalEventFileExtension, logger, filePath, this);
                }

                fileStorage.add(serializedEvent);
                totalStorageUsed.getAndAdd(((String)serializedEvent.a).length());
            }
        }
        catch (Exception e)
        {
            logger.error(TAG, "Could not write events to disk");
        }

        // After all events are written flush backing queue to disk
        fileStorage.flush();
    }
}