package com.microsoft.cll.android;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;

/**
 * This is a base class for all calls to OneSettings
 */
public abstract class AbstractSettings
{
    protected String endpoint;
    protected String queryParam;
    protected final ClientTelemetry clientTelemetry;
    protected final ILogger logger;
    protected String TAG = "AndroidCll-AbstractSettings";
    protected SettingsStore.Settings ETagSettingName;
    private final PartA partA;
    protected boolean disableUploadOn404 = false;

    protected AbstractSettings(ClientTelemetry clientTelemetry, ILogger logger, PartA partA)
    {
        this.clientTelemetry = clientTelemetry;
        this.logger = logger;
        this.partA = partA;
    }

    /**
     * Retrieves the settings from the url specified
     */
    public JSONObject getSettings()
    {
        logger.info(TAG, "Get Settings");
        URL url;

        try
        {
            url = new URL(endpoint + queryParam);
        }
        catch (MalformedURLException e)
        {
            logger.error(TAG, "Settings URL is invalid");
            return null;
        }

        URLConnection connection = null;
        HttpsURLConnection httpConnection;
        try
        {
            connection = url.openConnection();
            if (connection instanceof HttpsURLConnection)
            {
                clientTelemetry.IncrementSettingsHttpAttempts();
                httpConnection = (HttpsURLConnection) connection;

                int conTimeout = httpConnection.getConnectTimeout();
                int reqTimeout = httpConnection.getReadTimeout();


                httpConnection.setConnectTimeout(SettingsStore.getCllSettingsAsInt(SettingsStore.Settings.HTTPTIMEOUTINTERVAL));
                httpConnection.setReadTimeout(SettingsStore.getCllSettingsAsInt(SettingsStore.Settings.HTTPTIMEOUTINTERVAL));
                httpConnection.setRequestMethod("GET");
                httpConnection.setRequestProperty("Accept", "application/json");
                httpConnection.setRequestProperty("If-None-Match", SettingsStore.getCllSettingsAsString(ETagSettingName));

                long start = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US).getTimeInMillis();
                httpConnection.connect();
                long finish = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US).getTimeInMillis();
                long diff = finish - start;
                clientTelemetry.SetAvgSettingsLatencyMs((int) diff);
                clientTelemetry.SetMaxSettingsLatencyMs((int) diff);

                if(httpConnection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND && disableUploadOn404)
                {
                    // Disable sending events if user gives us an iKey that doesn't follow an allowed format.
                    logger.info(TAG, "Your iKey is invalid. Your events will not be sent!");
                    SettingsStore.updateCllSetting(SettingsStore.Settings.UPLOADENABLED, "false");
                }
                else if(httpConnection.getResponseCode() != HttpURLConnection.HTTP_NOT_FOUND && disableUploadOn404)
                {
                    // We need this so that we are not permanently disabled after one 404.
                    // Only re-enable if host settings returns non 404. If we didn't check for disableUploadOn404 then
                    // the cll settings sync would end up re-enabling upload when we don't want it to.
                    logger.info(TAG, "Your iKey is valid.");
                    SettingsStore.updateCllSetting(SettingsStore.Settings.UPLOADENABLED, "true");
                }

                // Check for success (Only 200 and 304 are considered successful)
                if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK || httpConnection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED)
                {
                    String ETag = httpConnection.getHeaderField("ETAG");
                    if(ETag != null && !ETag.isEmpty())
                    {
                        SettingsStore.updateCllSetting(ETagSettingName, ETag);
                    }
                }
                else
                {
                    clientTelemetry.IncrementSettingsHttpFailures(httpConnection.getResponseCode());
                }

                // Close the connection if this was not a success or there are no new settings
                if(httpConnection.getResponseCode() != HttpURLConnection.HTTP_OK)
                {
                    httpConnection.disconnect();
                    httpConnection = null;
                    // set connection to null so we don't try/catch every time we
                    // make a valid connection
                    connection = null;
                    return null;
                }

                BufferedReader input = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = input.readLine()) != null)
                {
                    result.append(line);
                }

                input.close();
                httpConnection.disconnect();
                httpConnection = null;
                // set connection to null so we don't try/catch every time we
                // make a valid connection
                connection = null;
                return new JSONObject(result.toString());
            }
        }
        catch (IOException e)
        {
            logger.error(TAG, e.getMessage());
            clientTelemetry.IncrementSettingsHttpFailures(-1);
        }
        catch (JSONException e)
        {
            logger.error(TAG, e.getMessage());
        }
        finally
        {
            // close connection if it's still open
            if (connection != null)
            {
                try
                {
                    connection.getInputStream().close();
                }
                catch (Exception e)
                {
                    // swallow exception
                    logger.error(TAG, e.getMessage());
                }
            }
        }

        return null;
    }

    /**
     * Parses the retrieved settings
     */
    public abstract void ParseSettings(JSONObject resultJson);
}
