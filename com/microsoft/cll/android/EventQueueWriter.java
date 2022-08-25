package com.microsoft.cll.android;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class takes care of getting queued events ready to send
 */
public class EventQueueWriter implements Runnable
{
    protected static AtomicBoolean running = new AtomicBoolean(false);
    protected static ScheduledFuture future;
    private static int backoffSeconds = 0;
    private static int retryAfterBackoffSeconds = 0;
    private static AtomicInteger s_threadCount = new AtomicInteger(0);

    private final String TAG = "AndroidCll-EventQueueWriter";
    private final List<IStorage> storages;
    private final List<String> ids;
    private final List<ICllEvents> cllEvents;
    private final EventBatcher batcher;
    private final SerializedEvent event;
    private final ILogger logger;
    private final ITicketCallback ticketCallback;
    private final ClientTelemetry clientTelemetry;
    private final ScheduledExecutorService executorService;
    private final TicketManager ticketManager;
    private EventSender sender;
    private List<IStorage> removedStorages;
    private EventCompressor compressor;
    private EventHandler handler;
    private URL endpoint;
    private final Random random = new Random();

    /**
     * Constructor for a queue of events
     */
    public EventQueueWriter(URL endpoint, List<IStorage> storages, ClientTelemetry clientTelemetry,
                            List<ICllEvents> cllEvents, ILogger logger, ScheduledExecutorService executorService,
                            ITicketCallback ticketCallback)
    {
        this.cllEvents      = cllEvents;
        this.storages       = storages;
        this.logger         = logger;
        this.ticketCallback = ticketCallback;
        this.batcher        = new EventBatcher();
        this.sender         = new EventSender(endpoint, clientTelemetry, logger);
        this.compressor     = new EventCompressor(logger);
        this.event          = null;
        this.ids            = null;
        this.executorService= executorService;
        this.clientTelemetry= clientTelemetry;
        this.endpoint       = endpoint;
        this.removedStorages= new ArrayList<IStorage>();
        this.ticketManager  = new TicketManager(ticketCallback, logger);
    }

    /**
     * Constructor for a real time event
     */
    public EventQueueWriter(URL endpoint, SerializedEvent event, List<String> ids, ClientTelemetry clientTelemetry,
                            List<ICllEvents> cllEvents, ILogger logger, ScheduledExecutorService executorService,
                            EventHandler handler, ITicketCallback ticketCallback)
    {
        this.cllEvents      = cllEvents;
        this.event          = event;
        this.ids            = ids;
        this.logger         = logger;
        this.ticketCallback = ticketCallback;
        this.sender         = new EventSender(endpoint, clientTelemetry, logger);
        this.batcher        = null;
        this.storages       = null;
        this.executorService= executorService;
        this.clientTelemetry= clientTelemetry;
        this.handler        = handler;
        this.endpoint       = endpoint;
        this.ticketManager  = new TicketManager(ticketCallback, logger);

        clientTelemetry.IncrementEventsQueuedForUpload();
    }

    void setSender(EventSender sender) {
        this.sender = sender;
    }

    @Override
    public void run()
    {
        try
        {
            s_threadCount.getAndAdd(1);

            logger.info(TAG, "Starting upload");

            // Send real time event
            // This must occur before we check for running, otherwise if a normal send is running
            // we might drop this event.
            if (storages == null)
            {
                sendRealTimeEvent(event);
                return;
            }

            // Send events with normal persistence
            if (!running.compareAndSet(false, true))
            {
                logger.info(TAG, "Skipping send, event sending is already in progress on different thread.");
                return;
            }

            send();
            running.set(false);
        }
        finally
        {
            s_threadCount.getAndAdd(-1);
        }
    }

    /**
     * Sends a real time event by itself
     */
    protected void sendRealTimeEvent(SerializedEvent singleEvent)
    {
        // Check to see if this single serialized event is greater than MAX_BUFFER_SIZE, if it is we drop it.
        String eventString = singleEvent.getSerializedData();
        if (eventString.length() > SettingsStore.getCllSettingsAsInt(SettingsStore.Settings.MAXEVENTSIZEINBYTES))
        {
            return;
        }

        boolean sendCompleted = false;

        try
        {
            ticketManager.clean();
            ticketManager.addTickets(ids);
            TicketHeaders ticketHeaders = ticketManager.getHeaders(false);
            byte[] eventData = getEventData(eventString);

            int sendErrorCode = sendRequest(eventData, false, ticketHeaders);

            if (sendErrorCode == 401)
            {
                // If the request failed with a 401 that means that either the AuthXToken or MsaDeviceTicket
                // were invalid or expired. In this scenario we get fresh tickets by forcing the refresh
                // and then trying again. If we still fail then keep the events on disk and try again next time
                ticketHeaders = ticketManager.getHeaders(true);

                // If we fail a second time then write to disk
                sendErrorCode = sendRequest(eventData, false, ticketHeaders);
            }

            // if the server processed all the events in the payload mark it complete
            // 200 means some of the events were accepted,
            // 400 means all were rejected for various reasons which cannot be easily fixed here
            // so we can drop the events (see https://osgwiki.com/wiki/Vortex/ClientProtocol)
            if (sendErrorCode == 200 || sendErrorCode == 400)
            {
                sendCompleted = true;
            }
        }
        catch (IOException e)
        {
            // Edge case for real time events that try to send but don't have network.
            // In this case we need to write to disk
            logger.error(TAG, "Cannot send event");
        }

        if (sendCompleted)
        {
            // Send was a success so cancel backoff if in progress
            cancelBackoff();

            for (ICllEvents event : cllEvents)
            {
                event.sendComplete();
            }
        }
        else
        {
            handler.addToStorage(singleEvent, ids);
        }
    }

    protected void send()
    {
        SendResult sendResult = sendInternal();

        if (sendResult == SendResult.SUCCESS)
        {
            // Stop retry logic on successful send
            cancelBackoff();
        }
        else
        {
            int interval = generateBackoffInterval();

            // If we don't remove these then on next call the drain method will end up creating a new empty file by this name.
            storages.removeAll(removedStorages);

            EventQueueWriter eventQueueWriter = new EventQueueWriter(endpoint, storages, clientTelemetry, cllEvents, logger, executorService, ticketCallback);
            eventQueueWriter.setSender(sender);
            future = executorService.schedule(eventQueueWriter, interval, TimeUnit.SECONDS);
        }
    }

    /**
     * Serializes, batches, and sends events
     */
    private SendResult sendInternal()
    {
        // this method continues until one of the things happens:
        // a) all events from all storages are successfully uploaded
        // b) some event batch fails to be uploaded
        // in case (a) we cancel backoff, in case (b) we continue backoff

        // Ensure that the serialized event string is under MAXEVENTSIZEINBYTES.
        // If it is over MAXEVENTSIZEINBYTES then we should use 2 or more strings and send them
        for (IStorage storage : storages)
        {
            if (executorService.isShutdown())
            {
                return SendResult.SUCCESS;
            }

            // Ensure the ticket manager doesn't contain any data from the last storage.
            ticketManager.clean();

            for (Tuple<String,List<String>> event : storage.drain())
            {
                // Add tickets for this event
                ticketManager.addTickets(event.b);

                this.clientTelemetry.IncrementEventsQueuedForUpload();

                // Check to see if this single serialized event is greater than MAX_BUFFER_SIZE, if it is we drop it.
                if (event.a.length() > SettingsStore.getCllSettingsAsInt(SettingsStore.Settings.MAXEVENTSIZEINBYTES))
                {
                    logger.warn(TAG, "Dropping event because it is too large.");

                    // This could cause big problems if the host application decides to do a ton of processing for each
                    // dropped event.
                    for (ICllEvents cllEvent : cllEvents)
                    {
                        cllEvent.eventDropped(event.a);
                    }

                    continue;
                }

                if (!batcher.tryAddingEventToBatch(event.a))
                {
                    // Full batch, send events
                    logger.info(TAG, "Got a full batch, preparing to send");
                    String batchedEvents = batcher.getBatchedEvents();

                    if (!batcher.tryAddingEventToBatch(event.a))
                    {
                        logger.error(TAG, "Could not add events to an empty batch");
                    }

                    SendResult sendResult = sendBatch(batchedEvents, storage);

                    if (sendResult == SendResult.ERROR)
                    {
                        storage.close();
                        return sendResult;
                    }
                }
            }

            // Send remaining events that didn't fill a whole batch
            logger.info(TAG, "Preparing to send");
            String batchedEvents = batcher.getBatchedEvents();
            SendResult sendResult = sendBatch(batchedEvents, storage);
            storage.close();

            if (sendResult == SendResult.ERROR)
            {
                return sendResult;
            }

            // Only discard events if the send was a success. If it was a token issue
            // then we should keep the events on disk and try again later.
            storage.discard();
        }

        logger.info(TAG, "Sent " + clientTelemetry.snapshot.getEventsQueued() + " events.");

        for (ICllEvents event : cllEvents)
        {
            event.sendComplete();
        }

        return SendResult.SUCCESS;
    }

    private void cancelBackoff()
    {
        future = null;
        backoffSeconds = 0;
    }

    private SendResult sendBatch(String batchedEvents, IStorage storage)
    {
        logger.info(TAG, "Sending Batch of events");

        // This is "" if we upload an empty file which we should just skip
        if (batchedEvents.equals(""))
        {
            removedStorages.add(storage);
            return SendResult.SUCCESS;
        }

        logger.info(TAG, "Compressing events");
        boolean isCompressed = true;
        byte[] eventsData = compressor.compress(batchedEvents);
        if (eventsData == null)
        {
            // compression failed - we'll send uncompressed data
            eventsData = getEventData(batchedEvents);
            isCompressed = false;
        }

        TicketHeaders ticketHeaders = ticketManager.getHeaders(false);
        boolean sendCompleted = false;

        try
        {
            int sendErrorCode = sendRequest(eventsData, isCompressed, ticketHeaders);

            if (sendErrorCode == 401)
            {
                // If the request failed with a 401 that means that either the AuthXToken or MsaDeviceTicket
                // were invalid or expired. In this scenario we get fresh tickets by forcing the refresh
                // and then trying again. If we still fail then keep the events on disk and try again next time
                logger.info(TAG, "We got a 401 while sending the events, refreshing the tokens and trying again");
                ticketHeaders = ticketManager.getHeaders(true);

                sendErrorCode = sendRequest(eventsData, isCompressed, ticketHeaders);

                if (sendErrorCode == 401)
                {
                    logger.info(TAG, "After refreshing the tokens we still got a 401. Most likely we couldn't " +
                            "get new tokens so we will keep these events on disk and try to get new tokens later");
                }
            }

            // if the server processed all the events in the payload mark it complete
            // 200 means some of the events were accepted,
            // 400 means all were rejected for various reasons which cannot be easily fixed here
            // so we can drop the events (see https://osgwiki.com/wiki/Vortex/ClientProtocol)
            if (sendErrorCode == 200 || sendErrorCode == 400)
            {
                sendCompleted = true;
            }
        }
        catch (IOException e)
        {
            logger.error(TAG, "Cannot send event: " + e.getMessage());
        }

        if (sendCompleted)
        {
            return SendResult.SUCCESS;
        }
        else
        {
            // If we run into an error sending events we just return. This ensures we don't lose events
            return SendResult.ERROR;
        }
    }

    /**
     * Sends the events in the body to Vortex
     * @param body The body to send
     * @param isCompressed Whether the body is compressed or not so we can set the appropriate headers
     * @param ticketHeaders Auth tickets & tokens
     * @throws IOException An exception is we cannot connect to Vortex
     */
    private int sendRequest(byte[] body, boolean isCompressed, TicketHeaders ticketHeaders) throws IOException
    {
        if (!preValidateTickets(ticketHeaders))
        {
            return 401;
        }
        else
        {
            EventSendResult sendResult = sender.sendEvent(body, isCompressed, ticketHeaders);

            if (sendResult.retryAfterSeconds > 0)
            {
                retryAfterBackoffSeconds = sendResult.retryAfterSeconds;
            }

            return sendResult.responseCode;
        }
    }

    private boolean preValidateTickets(TicketHeaders ticketHeaders)
    {
        // Run basic pre-validation of tickets to reduce number of calls to Vortex which result in 401.
        // This saves network traffic and reduces 401 errors generated in Vortex.
        // CLL callers could return empty tickets when they don't have one temporarily or else.

        // Requirements to check:
        // 1) tickets in xtokens list are non-empty, while the list itself could be empty
        // 2) if there is a "p" and no "rp:" ticket in xtokens then msaDeviceTicket is required
        // 3) if there is an "x:" ticket in xtokens then authXToken is required
        boolean msaUserTicketFound = false;
        boolean msaDeviceTicketFound = false;
        boolean xauthUserTicketFound = false;
        boolean xauthDeviceTicketFound = false;

        if (ticketHeaders != null && ticketHeaders.xtokens != null && !ticketHeaders.xtokens.isEmpty())
        {
            for (Map.Entry<String, String> ticket : ticketHeaders.xtokens.entrySet())
            {
                String ticketValue = ticket.getValue();

                // check if ticket is empty, or is too short e.g. it's equal to "rp:"
                if (ticketValue == null || ticketValue.length() <= 3)
                {
                    return false;
                }

                if (ticketValue.startsWith("x:"))
                {
                    xauthUserTicketFound = true;
                }

                if (ticketValue.startsWith("p:"))
                {
                    msaUserTicketFound = true;
                }

                if (ticketValue.startsWith("rp:"))
                {
                    msaUserTicketFound = true;
                    msaDeviceTicketFound = true;
                }
            }

            if (ticketHeaders.authXToken != null && !ticketHeaders.authXToken.isEmpty())
            {
                xauthDeviceTicketFound = true;
            }

            if (ticketHeaders.msaDeviceTicket != null && !ticketHeaders.msaDeviceTicket.isEmpty())
            {
                msaDeviceTicketFound = true;
            }
        }

        if (msaUserTicketFound && !msaDeviceTicketFound)
        {
            return false;
        }

        if (xauthUserTicketFound && !xauthDeviceTicketFound)
        {
            return false;
        }

        return true;
    }

    /**
     * Generates a random backoff interval between (0 and k * b^p).
     * k is a constant we multiply by
     * b is the base which we raise to a power
     * p is the power we raise to. where p increases every time we fail until we reach maxretryperiod.
     * @return A retry interval
     */
    int generateBackoffInterval()
    {
        int interval = 0;

        // apply Retry-After override once if it was received
        if (retryAfterBackoffSeconds > 0)
        {
            logger.info(TAG, "Using backoff interval from Retry-After header.");

            interval = retryAfterBackoffSeconds;
            retryAfterBackoffSeconds = 0;
        }
        else
        {
            int startInterval = SettingsStore.getCllSettingsAsInt(SettingsStore.Settings.CONSTANTFORRETRYPERIOD);
            int maxInterval = SettingsStore.getCllSettingsAsInt(SettingsStore.Settings.MAXRETRYPERIOD);
            int exponentBase = SettingsStore.getCllSettingsAsInt(SettingsStore.Settings.BASERETRYPERIOD);

            if (backoffSeconds == 0)
            {
                backoffSeconds = Math.max(0, startInterval);
            }

            if (logger.getVerbosity() == Verbosity.INFO)
            {
                logger.info(TAG, "Generating new backoff interval using \"Random.nextInt(" + (backoffSeconds + 1) + ") seconds\" formula.");
            }

            interval = random.nextInt(backoffSeconds + 1);
            backoffSeconds = Math.min(backoffSeconds * exponentBase, maxInterval);

            if (logger.getVerbosity() == Verbosity.INFO)
            {
                logger.info(TAG, "The generated backoff interval is " + interval + ".");
            }
        }

        return interval;
    }

    private byte[] getEventData(String body)
    {
        return body.getBytes(Charset.forName("UTF-8"));
    }

    public static int getRunningThreadCount()
    {
        return s_threadCount.get();
    }

    enum SendResult
    {
        // send completed, backoff should be reset, events deleted
        SUCCESS,
        // send failed, keep events, continue backoff
        ERROR
    }
}