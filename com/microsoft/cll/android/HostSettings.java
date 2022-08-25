package com.microsoft.cll.android;

import org.json.JSONObject;

import java.util.Iterator;

/**
 * These settings are specific to the host application. They include information such as Sample Rate, Persistence, and Latency for their events
 */
public class HostSettings extends AbstractSettings
{
    private final String baseUrl = "https://settings.data.microsoft.com/settings/v2.0/telemetry/";

    public HostSettings(ClientTelemetry clientTelemetry, ILogger logger, String iKey, PartA partA)
    {
        super(clientTelemetry, logger, partA);

        this.TAG ="AndroidCll-HostSettings";
        this.ETagSettingName = SettingsStore.Settings.HOSTSETTINGSETAG;
        this.disableUploadOn404 = true;

        this.endpoint = baseUrl + iKey;
        this.queryParam = "?os=" + partA.osName + "&osVer=" + partA.osVer+ "&deviceClass=" + partA.deviceExt.getDeviceClass() + "&deviceId=" + partA.deviceExt.getLocalId();
    }

    /*
     * Parses the settings returned by OneSettings
     */
    @Override
    public void ParseSettings(JSONObject resultJson)
    {
        try
        {
            // apply settings specific overrides
            if (resultJson != null && resultJson.has("settings"))
            {
                JSONObject jsonSettings = (JSONObject) resultJson.get("settings");
                Iterator<String> keys = jsonSettings.keys();
                while (keys.hasNext())
                {
                    String key = keys.next();
                    String value = jsonSettings.getString(key);

                    if (key.split(":").length != 4)
                    {
                        logger.error(TAG, "Bad Settings Format");
                    }

                    // Cleanse the input
                    value = value.replaceAll(" ", "");
                    value = value.replaceAll("_", "");
                    value = value.toUpperCase();
                    key = key.toUpperCase();

                    SettingsStore.updateHostSetting(key, value);
                }
            }
            else
            {
                logger.info(TAG, "Json result did not contain a \"settings\" field!");
            }
        }
        catch (Exception e)
        {
            logger.error(TAG, "An exception occurred while parsing settings");
        }
    }
}
