package com.microsoft.cll.android;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * This class handles sending events to Vortex
 */
public class EventSender
{
    private final String NO_HTTPS_CONN = "URL didn't return HttpsUrlConnection instance.";
    private final String TAG = "AndroidCll-EventSender";
    private final URL endpoint;
    private final ClientTelemetry clientTelemetry;
    private final ILogger logger;

    public EventSender(URL endpoint, ClientTelemetry clientTelemetry, ILogger logger)
    {
        this.endpoint           = endpoint;
        this.clientTelemetry    = clientTelemetry;
        this.logger             = logger;
    }

    /**
     * Sends the events in the body to Vortex
     * @param body The body to send
     * @param compressed Whether the body is compressed or not so we can set the appropriate headers
     * @throws IOException An exception is we cannot connect to Vortex
     */
    public EventSendResult sendEvent(byte[] body, boolean compressed, TicketHeaders ticketHeaders) throws IOException
    {
        EventSendResult sendResult = new EventSendResult();

        // on timeout/exception return error code
        int responseCode = HttpURLConnection.HTTP_INTERNAL_ERROR;
        int retryAfterSeconds = 0;
        final HttpURLConnection connection;
        long start;
        long diff;
        BufferedReader reader;
        InputStream inputStream = null;
        InputStream errorStream = null;

        clientTelemetry.IncrementVortexHttpAttempts();
        connection = this.openConnection(body.length, compressed, ticketHeaders);

        try
        {
            connection.connect();
        }
        finally
        {
            logger.error(TAG, "Error connecting.");
        }

        try
        {
            OutputStream stream = connection.getOutputStream();
            stream.write(body);
            stream.flush();
            stream.close();
        }
        finally
        {
            logger.error(TAG, "Error writing data");
        }

        start = getTime();

        // read response code, response body or error body
        // parse server responses for error codes 200 and 400
        // close response/error stream after finishing
        try
        {
            try
            {
                // get HTTP response code
                responseCode = connection.getResponseCode();

                // if server is busy we should try to accommodate the specified Retry-After interval
                if (responseCode == 429 || responseCode == 503)
                {
                    String retryAfterValueString = connection.getHeaderField("Retry-After");

                    if (retryAfterValueString != null)
                    {
                        try
                        {
                            retryAfterSeconds = Integer.parseInt(retryAfterValueString);
                        }
                        catch (NumberFormatException nfe) {}
                    }

                    // if value is invalid, ignore it
                    if (retryAfterSeconds > 24 * 60 * 60 || retryAfterSeconds < 0)
                    {
                        retryAfterSeconds = 0;
                    }
                }
            }
            catch (IOException e1) {}

            try
            {
                inputStream = connection.getInputStream();

                if (inputStream != null)
                {
                    reader = new BufferedReader(new InputStreamReader(inputStream));
                    processResponseBodyConditionally(reader, responseCode == HttpURLConnection.HTTP_OK);
                }
            }
            catch (IOException e2)
            {
                errorStream = connection.getErrorStream();

                if (errorStream != null)
                {
                    reader = new BufferedReader(new InputStreamReader(errorStream));
                    processResponseBodyConditionally(reader, responseCode == HttpURLConnection.HTTP_BAD_REQUEST);
                }
            }
        }
        finally
        {
            if (inputStream != null)
            {
                inputStream.close();
            }

            if (errorStream != null)
            {
                errorStream.close();
            }

            if (responseCode >= 500 && responseCode < 600)
            {
                logger.error(TAG, "Bad Response Code");
                clientTelemetry.IncrementVortexHttpFailures(connection.getResponseCode());
            }

            diff = getTime() - start;
            clientTelemetry.SetAvgVortexLatencyMs((int) diff); // ~25 days worth of ms can be stored in an int
            clientTelemetry.SetMaxVortexLatencyMs((int) diff);
        }

        sendResult.responseCode = responseCode;
        sendResult.retryAfterSeconds = retryAfterSeconds;

        return sendResult;
    }

    /**
     * Opens a URLConnection to the endpoint. Caller is responsible for closing
     * when finished.
     *
     * @return HttpURLConnection to the resource.
     * @throws IOException
     *             Thrown if connection returned is not an HttpURLConnection
     */
    protected HttpURLConnection openConnection(final int length, boolean compressed, TicketHeaders ticketHeaders) throws IOException
    {
        // prepare x-tickets header
        String ticketString = "";
        if (ticketHeaders != null && !ticketHeaders.xtokens.isEmpty())
        {

            boolean first = true;
            for (Map.Entry<String, String> entry : ticketHeaders.xtokens.entrySet())
            {
                // Only tack a ; on if we have been through the loop once. This also prevents
                // a ; being tacked on after the last ticket.
                if (!first)
                {
                    ticketString += ";";
                }

                ticketString += "\"" + entry.getKey() + "\"=\"" + entry.getValue() + "\"";
                first = false;
            }
        }

        // prepare date format
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        // create a new connection
        final URLConnection connection = this.endpoint.openConnection();

        // fill in the connection properties and init the connection
        if (connection instanceof HttpURLConnection)
        {
            final HttpURLConnection httpsConnection = (HttpURLConnection) connection;
            httpsConnection.setConnectTimeout(SettingsStore.getCllSettingsAsInt(SettingsStore.Settings.HTTPTIMEOUTINTERVAL));
            httpsConnection.setReadTimeout(SettingsStore.getCllSettingsAsInt(SettingsStore.Settings.HTTPTIMEOUTINTERVAL));
            httpsConnection.setInstanceFollowRedirects(false);
            httpsConnection.setUseCaches(false);
            httpsConnection.setDoOutput(true);

            httpsConnection.setRequestMethod("POST");
            httpsConnection.setRequestProperty("Content-Type", "application/x-json-stream; charset=utf-8");
            httpsConnection.setRequestProperty("X-UploadTime", dateFormat.format(new Date()).toString());
            httpsConnection.setRequestProperty("Content-Length", Integer.toString(length));

            // set compression headers
            if (compressed)
            {
                httpsConnection.setRequestProperty("Accept", "application/json");
                httpsConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");
                httpsConnection.setRequestProperty("Content-Encoding", "deflate");
            }

            // set ticket headers
            if (ticketString != "")
            {
                httpsConnection.setRequestProperty("X-Tickets", ticketString);

                // AuthXToken (device x-auth token) is optional if xtokens contains no user x-auth claims
                if (ticketHeaders.authXToken != null && !ticketHeaders.authXToken.isEmpty())
                {
                    httpsConnection.setRequestProperty("X-AuthXToken", ticketHeaders.authXToken);
                }

                // If one of the xtickets has device claims then we don't need to send an msa device ticket too.
                // Note if changing this logic - do update EventQueueWriter.preValidateTickets() method.
                if (ticketHeaders.msaDeviceTicket != null && !ticketHeaders.msaDeviceTicket.isEmpty())
                {
                    httpsConnection.setRequestProperty("X-AuthMsaDeviceTicket", ticketHeaders.msaDeviceTicket);
                }
            }

            return httpsConnection;
        }
        else
        {
            clientTelemetry.IncrementVortexHttpFailures(-1);
            throw new IOException(NO_HTTPS_CONN);
        }
    }

    protected String processResponseBody(BufferedReader reader)
    {
        return processResponseBodyConditionally(reader, true);
    }

    /**
     * Reads the response from the http connection
     * @param reader A reader that is attached to the response body
     */
    protected String processResponseBodyConditionally(BufferedReader reader, boolean parseJson)
    {
        final StringBuilder responseBuilder = new StringBuilder();
        String line;
        try
        {
            while ((line = reader.readLine()) != null)
            {
                responseBuilder.append(line);
            }
        }
        catch (IOException e)
        {
            logger.error(TAG, "Couldn't read response body");
        }

        if (parseJson)
        {
            // Check to see if any events were rejected
            try
            {
                JSONObject jsonObject = new JSONObject(responseBuilder.toString());
                int rejectCount = jsonObject.getInt("rej");
                clientTelemetry.IncrementRejectDropCount(rejectCount);
            } catch (JSONException e)
            {
                logger.info(TAG, e.getMessage());
            } catch (RuntimeException e)
            {
                logger.info(TAG, e.getMessage());
            }
        }

        logger.info(TAG, responseBuilder.toString());
        return responseBuilder.toString();
    }

    /**
     * Gets the current time in milliseconds for UTC
     * @return The time in milliseconds
     */
    private long getTime()
    {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US).getTimeInMillis();
    }
}
