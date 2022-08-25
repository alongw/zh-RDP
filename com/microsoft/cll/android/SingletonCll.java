package com.microsoft.cll.android;

import com.microsoft.telemetry.Base;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import com.microsoft.cll.android.EventEnums.Latency;
import com.microsoft.cll.android.EventEnums.Persistence;
import com.microsoft.cll.android.EventEnums.Sensitivity;

/**
 * A singleton version of the cll
 */
public class SingletonCll implements ISingletonCll
{
    public CorrelationVector correlationVector;

    protected final ClientTelemetry clientTelemetry;
    protected final String TAG = "AndroidCll-SingletonCll";
    protected final List<ICllEvents> cllEvents;

    protected EventHandler eventHandler;
    protected ILogger logger;
    protected PartA partA;
    protected SettingsSync settingsSync;
    protected SnapshotScheduler snapshotScheduler;

    private final AtomicBoolean isChanging;
    private final AtomicBoolean isPaused;
    private final AtomicBoolean isStarted;

    private ScheduledExecutorService executor;
    private static SingletonCll Instance;
    private static Object InstanceLock = new Object();
    private ITicketCallback ticketCallback;

    public static ISingletonCll getInstance(String iKey, ILogger logger, String eventDir, PartA partA, CorrelationVector correlationVector)
    {
        if (Instance == null)
        {
            synchronized (InstanceLock)
            {
                if(Instance == null)
                {
                    Instance = new SingletonCll(iKey, logger, eventDir, partA, correlationVector);
                }
            }
        }
        return Instance;
    }

    /**
     * Initializes the CLL with the given provider.
     */
    private SingletonCll(String iKey, ILogger logger, String eventDir, PartA partA, CorrelationVector correlationVector)
    {
        if(iKey == null || iKey == "")
        {
            throw new IllegalArgumentException("iKey cannot be null or \"\"");
        }

        logger.setVerbosity(Verbosity.NONE);

        this.correlationVector   = correlationVector;
        this.logger              = logger;
        this.partA               = partA;
        this.clientTelemetry     = new ClientTelemetry();
        this.cllEvents           = new ArrayList<ICllEvents>();
        this.eventHandler        = new EventHandler(clientTelemetry, cllEvents, logger, eventDir);
        this.isChanging          = new AtomicBoolean(false);
        this.isStarted           = new AtomicBoolean(false);
        this.isPaused            = new AtomicBoolean(false);
        this.settingsSync        = new SettingsSync(clientTelemetry, logger, iKey, partA);
        this.snapshotScheduler   = new SnapshotScheduler(clientTelemetry, logger, this);

        setEndpointUrl(SettingsStore.getCllSettingsAsString(SettingsStore.Settings.VORTEXPRODURL));
    }

    /**
     * Starts the queue-draining background thread and uploader. Start must be called prior
     * to logging events in order to start the queue-draining background thread
     * and uploader.
     */
    public void start()
    {
        if(this.isChanging.compareAndSet(false, true))
        {
            if(!this.isStarted.get())
            {
                this.executor = Executors.newScheduledThreadPool(3);
                this.snapshotScheduler.start(this.executor);
                this.eventHandler.start(this.executor);
                this.settingsSync.start(this.executor);

                this.isStarted.set(true);
            }

            this.isChanging.set(false);
        }
    }

    /**
     * Stops the background thread and uploader.
     */
    public void stop()
    {
        // Ensure we are actually started and then if we are attempt to shut is down.
        // NOTE: right now re are not guaranteeing that when we return we are actually stopped.
        if (this.isChanging.compareAndSet(false, true))
        {
            if(this.isStarted.get())
            {
                this.eventHandler.stop();
                this.settingsSync.stop();
                this.snapshotScheduler.stop();
                this.executor.shutdown();

                this.isStarted.set(false);
            }

            for(ICllEvents event : this.cllEvents)
            {
                event.stopped();
            }

            this.isChanging.set(false);
        }
    }

    /**
     * Puts the Cll in a paused state, allowing it to accept events but not
     * upload until it is resumed.
     */
    public void pause()
    {
        // Ensure we are actually started and then if we are attempt to shut is down.
        // NOTE: right now re are not guaranteeing that when we return we are actually stopped.
        if (this.isChanging.compareAndSet(false, true))
        {
            if(this.isStarted.get() && !this.isPaused.get())
            {
                this.eventHandler.pause();
                this.settingsSync.pause();
                this.snapshotScheduler.pause();
                this.executor.shutdown();

                this.isPaused.set(true);
            }

            this.isChanging.set(false);
        }
    }

    /**
     * Resume the Cll from a paused state, allowing uploads to resume.
     * During resume an upload will automatically be triggered.
     */
    public void resume()
    {
        // Ensure we are actually started and then if we are attempt to shut is down.
        // NOTE: right now we are not guaranteeing that when we return we are actually started.
        if (this.isChanging.compareAndSet(false, true))
        {
            if(this.isStarted.get() && this.isPaused.get())
            {
                // There is no way to restart an executor, we must create a new one instead
                this.executor = Executors.newScheduledThreadPool(SettingsStore.getCllSettingsAsInt(SettingsStore.Settings.THREADSTOUSEWITHEXECUTOR));
                this.snapshotScheduler.resume(this.executor);
                this.eventHandler.resume(this.executor);
                this.settingsSync.resume(this.executor);

                this.isPaused.set(false);
            }

            this.isChanging.set(false);
        }
    }

    /**
     * Allow the host application to set the verbosity to help with debugging during runtime
     * @param verbosity - The verbosity to use
     */
    public void setDebugVerbosity(Verbosity verbosity)
    {
        logger.setVerbosity(verbosity);
    }

    /**
     * Log event with specified Latency/Persistence/Sensitivity/SampleRate overrides
     * use the corresponding *Unspecified enum values to keep the value defined for the event in bond schema.
     *
     * @param event
     * @param latency
     * @param persistence
     * @param sensitivity
     * @param sampleRate
     * @param ids
     */
    public void log(final Base event, Latency latency, Persistence persistence, EnumSet<Sensitivity> sensitivity, double sampleRate, List<String> ids)
    {
        if (!this.isStarted.get())
        {
            this.logger.error(TAG, "Cll must be started before logging events");
            return;
        }

        if(ids != null && ticketCallback == null)
        {
            this.logger.error(TAG, "You must set the ticket callback if you want to log ids with your events");
            return;
        }

        final SerializedEvent serializedEvent = this.partA.populate(event, latency, persistence, sensitivity, sampleRate, ids);
        this.eventHandler.log(serializedEvent, ids);
    }

    /**
     * Uploads all events in the queue
     */
    public void send()
    {
        if(this.isStarted.get())
        {
            this.eventHandler.send();
        }
        else
        {
            logger.info(TAG, "Cannot send while the CLL is stopped.");
        }
    }

    /**
     * Sets the URL used to send events to
     * setEndpointUrl must be called before events can be sent
     *
     * @param url
     *            Url, including protocol and port
     */
    public void setEndpointUrl(final String url)
    {
        this.eventHandler.setEndpointUrl(url);
    }

    /**
     * Sets whether we should use the legacy part A fields or not.
     * @param value True if we should, false if we should not
     */
    public void useLegacyCS(boolean value) {
        partA.useLegacyCS(value);
    }

    /**
     * Sets the experiment id
     * @param id
     *           The experiment id
     */
    public void setExperimentId(String id) {
        partA.setExpId(id);
    }

    public void synchronize() {
        eventHandler.synchronize();
    }

    @Override
    public void SubscribeCllEvents(ICllEvents cllEvents)
    {

    }

    /**
     * Used for testing so we can inject a custom EventSender to test events
     * @param sender
     */
    protected void setEventSender(EventSender sender)
    {
        eventHandler.setSender(sender);
    }

    public void setAppUserId(String userId) {
        partA.setAppUserId(userId);
    }

    public String getAppUserId() {
        return partA.getAppUserId();
    }

    public void setXuidCallback(ITicketCallback callback)
    {
        ticketCallback = callback;
        if(isStarted.get() || isPaused.get())
        {
            logger.warn(TAG, "Xuid callback must be set before start.");
            return;
        }

        eventHandler.setXuidCallback(callback);
    }
}
