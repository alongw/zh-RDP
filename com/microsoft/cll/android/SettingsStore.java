package com.microsoft.cll.android;

import com.microsoft.telemetry.Base;
import com.microsoft.cll.android.EventEnums.Latency;
import com.microsoft.cll.android.EventEnums.Persistence;
import com.microsoft.cll.android.EventEnums.Sensitivity;

import java.util.HashMap;
import java.util.EnumSet;

/**
 * This is a static class for managing the values we get back from OneSettings
 */
public class SettingsStore
{
    private static HashMap<String, String> hostEventSettings = new HashMap<String, String>();
    protected static HashMap<Settings, Object> cllSettings = new HashMap<Settings, Object>();
    private static UpdateListener updateListener;

    public enum Settings
    {
        SYNCREFRESHINTERVAL,
        QUEUEDRAININTERVAL,
        SNAPSHOTSCHEDULEINTERVAL,
        MAXEVENTSIZEINBYTES,
        MAXEVENTSPERPOST,
        SAMPLERATE,
        MAXFILESSPACE,
        UPLOADENABLED,
        PERSISTENCE,
        LATENCY,
        HTTPTIMEOUTINTERVAL,
        THREADSTOUSEWITHEXECUTOR,
        MAXCORRELATIONVECTORLENGTH,
        MAXCRITICALCANADDATTEMPTS,
        MAXRETRYPERIOD,
        BASERETRYPERIOD,
        CONSTANTFORRETRYPERIOD,
        NORMALEVENTMEMORYQUEUESIZE,
        CLLSETTINGSURL,
        HOSTSETTINGSETAG,
        CLLSETTINGSETAG,
        VORTEXPRODURL,
        MAXREALTIMETHREADS
    }

    static
    {
        cllSettings.put(Settings.SYNCREFRESHINTERVAL, 30 * 60);       // Interval in seconds that we sync settings
        cllSettings.put(Settings.QUEUEDRAININTERVAL, 120);            // Interval in seconds that we empty the queue
        cllSettings.put(Settings.SNAPSHOTSCHEDULEINTERVAL, 15 * 60);  // Interval in seconds that we send the snapshot
        cllSettings.put(Settings.MAXEVENTSIZEINBYTES, 65536);         // Limit of post size in bytes
        cllSettings.put(Settings.MAXEVENTSPERPOST, 500);              // Max events supported per post
        cllSettings.put(Settings.MAXFILESSPACE, 10 * 1024 * 1024);    // This is the maximum amount of storage space we will use for files
        cllSettings.put(Settings.UPLOADENABLED, true);                // Master control to turn off event upload in case of emergency
        cllSettings.put(Settings.HTTPTIMEOUTINTERVAL, 60000);
        cllSettings.put(Settings.THREADSTOUSEWITHEXECUTOR, 3);
        cllSettings.put(Settings.MAXCORRELATIONVECTORLENGTH, 63);
        cllSettings.put(Settings.MAXCRITICALCANADDATTEMPTS, 5);
        cllSettings.put(Settings.MAXRETRYPERIOD, 180);
        cllSettings.put(Settings.BASERETRYPERIOD, 2);
        cllSettings.put(Settings.CONSTANTFORRETRYPERIOD, 5);
        cllSettings.put(Settings.NORMALEVENTMEMORYQUEUESIZE, 50);
        cllSettings.put(Settings.CLLSETTINGSURL, "https://settings.data.microsoft.com/settings/v2.0/androidLL/app");
        cllSettings.put(Settings.HOSTSETTINGSETAG, "");
        cllSettings.put(Settings.CLLSETTINGSETAG, "");
        cllSettings.put(Settings.VORTEXPRODURL, "https://vortex.data.microsoft.com/collect/v1");
        cllSettings.put(Settings.MAXREALTIMETHREADS, 200);
    }

    protected static int getCllSettingsAsInt(Settings setting)
    {
        return Integer.parseInt(cllSettings.get(setting).toString());
    }

    protected static long getCllSettingsAsLong(Settings setting)
    {
        return Long.parseLong(cllSettings.get(setting).toString());
    }

    protected static boolean getCllSettingsAsBoolean(Settings setting)
    {
        return Boolean.parseBoolean(cllSettings.get(setting).toString());
    }

    protected static String getCllSettingsAsString(Settings setting)
    {
        return cllSettings.get(setting).toString();
    }

    public static void setUpdateListener(UpdateListener updateListener)
    {
        SettingsStore.updateListener = updateListener;
    }

    public static void updateHostSetting(String settingName, String settingValue)
    {
        // Only perform the update action if the setting's value isn't present or has changed
        if(hostEventSettings.get(settingName) == null || !hostEventSettings.get(settingName).equals(settingValue))
        {
            hostEventSettings.put(settingName, settingValue);
            if(updateListener != null)
            {
                updateListener.OnHostSettingUpdate(settingName, settingValue);
            }
        }
    }

    public static void updateCllSetting(SettingsStore.Settings settingName, String settingValue)
    {
        // Only perform the update action if the setting's value has changed
        if(cllSettings.get(settingName) == null || !cllSettings.get(settingName).equals(settingValue))
        {
            SettingsStore.cllSettings.put(settingName, settingValue);
            if(updateListener != null)
            {
                updateListener.OnCllSettingUpdate(settingName.toString(), settingValue);
            }
        }
    }

    public static Latency getLatencyForEvent(Base base, Latency passedInValue)
    {
        String settingName = "LATENCY";

        // try reading from the cloud
        String valueString = getSettingFromCloud(base, settingName);
        if (valueString != null)
        {
            return Latency.FromString(valueString);
        }

        // use the passed in value in it is specified
        if (passedInValue != null && passedInValue != Latency.LatencyUnspecified)
        {
            return passedInValue;
        }

        // try reading from the event schema
        valueString = getSettingFromSchema(base, settingName);
        if (valueString != null)
        {
            return Latency.FromString(valueString);
        }

        // try reading the global cloud default
        valueString = getSettingFromCloudDefaults(settingName);
        if (valueString != null)
        {
            return Latency.FromString(valueString);
        }

        // otherwise return the default value
        return Latency.LatencyNormal;
    }

    public static Persistence getPersistenceForEvent(Base base, Persistence passedInValue)
    {
        String settingName = "PERSISTENCE";

        // try reading from the cloud
        String valueString = getSettingFromCloud(base, settingName);
        if (valueString != null)
        {
            return Persistence.FromString(valueString);
        }

        // use the passed in value in it is specified
        if (passedInValue != null && passedInValue != Persistence.PersistenceUnspecified)
        {
            return passedInValue;
        }

        // try reading from the event schema
        valueString = getSettingFromSchema(base, settingName);
        if (valueString != null)
        {
            return Persistence.FromString(valueString);
        }

        // try reading the global cloud default
        valueString = getSettingFromCloudDefaults(settingName);
        if (valueString != null)
        {
            return Persistence.FromString(valueString);
        }

        // otherwise return the default value
        return Persistence.PersistenceNormal;
    }

    public static EnumSet<Sensitivity> getSensitivityForEvent(Base base, EnumSet<Sensitivity> passedInValue)
    {
        String settingName = "SENSITIVITY";

        // try reading from the cloud
        String valueString = getSettingFromCloud(base, settingName);
        if (valueString != null)
        {
            return Sensitivity.FromString(valueString);
        }

        // use the passed in value in it is specified
        if (passedInValue != null && !passedInValue.contains(Sensitivity.SensitivityUnspecified))
        {
            return passedInValue;
        }

        // try reading from the event schema
        valueString = getSettingFromSchema(base, settingName);
        if (valueString != null)
        {
            return Sensitivity.FromString(valueString);
        }

        // try reading the global cloud default
        valueString = getSettingFromCloudDefaults(settingName);
        if (valueString != null)
        {
            return Sensitivity.FromString(valueString);
        }

        // otherwise return the default value
        return EnumSet.of(Sensitivity.SensitivityNone);
    }

    public static double getSampleRateForEvent(Base base, double passedInValue)
    {
        String settingName = "SAMPLERATE";

        // try reading from the cloud
        String valueString = getSettingFromCloud(base, settingName);
        if (valueString != null)
        {
            return EventEnums.SampleRateFromString(valueString);
        }

        // use the passed in value in it is specified
        if (passedInValue >= -EventEnums.SampleRate_Epsilon)
        {
            return passedInValue;
        }

        // try reading from the event schema
        valueString = getSettingFromSchema(base, settingName);
        if (valueString != null)
        {
            return EventEnums.SampleRateFromString(valueString);
        }

        // try reading the global cloud default
        valueString = getSettingFromCloudDefaults(settingName);
        if (valueString != null)
        {
            return EventEnums.SampleRateFromString(valueString);
        }

        // otherwise return the default value
        return EventEnums.SampleRate_NoSampling;
    }

    /**
     * Checks for cloud setting
     *
     * @param base
     * @param settingName
     * @return
     */
    private static String getSettingFromCloud(Base base, String settingName)
    {
        String qualifiedEventName = base.QualifiedName.toUpperCase();

        String namespace;
        String eventName;

        // Handle case where we have bad event names that don't contain a .
        if (qualifiedEventName.lastIndexOf(".") == -1)
        {
            namespace = "";
            eventName = qualifiedEventName;
        }
        else
        {
            namespace = qualifiedEventName.substring(0, qualifiedEventName.lastIndexOf("."));
            eventName = qualifiedEventName.substring(qualifiedEventName.lastIndexOf(".") + 1);
        }

        if (SettingsStore.hostEventSettings.containsKey(namespace + ":" + eventName + "::" + settingName))
        {
            return SettingsStore.hostEventSettings.get(namespace + ":" + eventName + "::" + settingName);
        }
        else if (SettingsStore.hostEventSettings.containsKey(":" + eventName + "::" + settingName))
        {
            return SettingsStore.hostEventSettings.get(":" + eventName + "::" + settingName);
        }
        else if (SettingsStore.hostEventSettings.containsKey(namespace + ":::" + settingName))
        {
            return SettingsStore.hostEventSettings.get(namespace + ":::" + settingName);
        }
        else if (SettingsStore.hostEventSettings.containsKey(":::" + settingName))
        {
            return SettingsStore.hostEventSettings.get(":::" + settingName);
        }

        return null;
    }

    /**
     * Retrieves the setting value from the schema if present
     *
     * @param base        The schema
     * @param settingName The setting to retrieve
     * @return The value of the setting if present, otherwise null
     */
    private static String getSettingFromSchema(Base base, String settingName)
    {
        return base.Attributes.get(settingName);
    }

    /**
     * Get global default setting value if available.
     * @param settingName
     * @return
     */
    private static String getSettingFromCloudDefaults(String settingName)
    {
        Object settingObject = SettingsStore.cllSettings.get(settingName);

        if (settingObject != null)
        {
            return settingObject.toString();
        }

        return null;
    }

    public interface UpdateListener
    {
        void OnHostSettingUpdate(String settingName, String SettingValue);
        void OnCllSettingUpdate(String settingName, String SettingValue);
    }
}
