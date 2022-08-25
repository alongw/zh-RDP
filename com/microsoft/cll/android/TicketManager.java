package com.microsoft.cll.android;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TicketManager
{
    private final String TAG = "AndroidCll-TicketManager";
    private final ITicketCallback callback;
    private final ILogger logger;
    private final Map<String, String> tickets;
    private boolean needDeviceTicket = true;

    public TicketManager(ITicketCallback callback, ILogger logger)
    {
        this.callback = callback;
        this.logger = logger;
        this.tickets = new HashMap<String, String>();
    }

    /**
     * Once a batch is ready to be sent getHeaders should be called.
     * @param shouldForceRefresh If a ticket is expired then shouldForceRefresh should be set to true
     *                           to indicate to the callback that we need a non-cached ticket
     * @return An object that contains the ticket headers that should be sent along with the event.
     */
    public TicketHeaders getHeaders(boolean shouldForceRefresh)
    {
        // We can't get headers if the callback is null, and
        // there is no point in sending any of the ticket headers
        // if there are no tickets to send.
        if (callback == null || tickets.isEmpty())
        {
            return null;
        }

        TicketHeaders headers = new TicketHeaders();
        headers.authXToken = callback.getAuthXToken(shouldForceRefresh);
        headers.xtokens = tickets;

        // If one of the tickets in the xtokens header contains device claims then we don't
        // need to send this header.
        if (needDeviceTicket)
        {
            headers.msaDeviceTicket = callback.getMsaDeviceTicket(shouldForceRefresh);
        }

        return headers;
    }

    /**
     * Saves tickets and ids for an event to send later when the batch is full
     * @param ids A list of ids associated with an event
     */
    public void addTickets(List<String> ids)
    {
        // If event was logged without any id's then the tickets will be null.
        if (ids == null || callback == null)
        {
            return;
        }

        for (String ticketId : ids)
        {
            // Only look up the ticket if we don't already have it
            if (!tickets.containsKey(ticketId))
            {
                logger.info(TAG, "Getting ticket for " + ticketId);
                TicketObject ticket = callback.getXTicketForXuid(ticketId);
                String ticketString = ticket.ticket;

                // Check to see if the ticket has device claims.
                // If it does then when we send this batch we don't need to send
                // a msaDeviceTicket along with the tokens
                if (ticket.hasDeviceClaims)
                {
                    needDeviceTicket = false;
                    ticketString = "rp:" + (ticketString == null ? "" : ticketString);
                }

                tickets.put(ticketId, ticketString);
            }
            else
            {
                logger.info(TAG, "We already have a ticket for this id, skipping.");
            }
        }
    }

    /**
     * Between batches you must call clean!
     * If you don't then previous tickets will be sent along with the new batch.
     */
    public void clean()
    {
        tickets.clear();
        needDeviceTicket = true;
    }
}
