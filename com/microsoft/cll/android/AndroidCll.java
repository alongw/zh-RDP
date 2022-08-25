package com.microsoft.cll.android;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import Microsoft.Telemetry.Base;

import com.microsoft.cll.android.EventEnums.Latency;
import com.microsoft.cll.android.EventEnums.Persistence;
import com.microsoft.cll.android.EventEnums.Sensitivity;

/**
 * The cll main class that should be called into via the client application.
 * The CLL must be initialized first with the <code>start()</code> method, which
 * starts the thread for collection of <code>IJsonSerializable</code> events, and then
 * <code>setEndpointUrl</code> must be called to set the url for the events to be sent to.
 */
public class AndroidCll implements ICll, SettingsStore.UpdateListener
{
    private final String TAG = "AndroidCll-AndroidCll";
    private final ILogger logger = AndroidLogger.getInstance();

    private final String sharedCllPreferencesName = "AndroidCllSettingsSharedPreferences";
    private final String sharedHostPreferencesName = "AndroidHostSettingsSharedPreferences";
    private final SharedPreferences cllPreferences;
    private final SharedPreferences hostPreferences;

    protected ISingletonCll cll;

    /**
     * Create a Cll for Android
     * @param iKey Your iKey
     * @param context The application context
     */
    public AndroidCll(String iKey, Context context)
    {
        CorrelationVector correlationVector = new CorrelationVector();
        String dataPath = context.getFilesDir().getPath();
        AndroidPartA partA = new AndroidPartA(AndroidLogger.getInstance(), iKey, context, correlationVector);

        cll = SingletonCll.getInstance(iKey, AndroidLogger.getInstance(), dataPath, partA, correlationVector);
        cllPreferences = context.getSharedPreferences(sharedCllPreferencesName, 0);
        hostPreferences = context.getSharedPreferences(sharedHostPreferencesName, 0);

        SettingsStore.setUpdateListener(this);
        setSettingsStoreValues();
    }

    /**
     * Test-constructor
     */
    protected AndroidCll()
    {
        cllPreferences = null;
        hostPreferences = null;
    }

    @Override
    public void start()
    {
        cll.start();
    }

    @Override
    public void stop()
    {
        cll.stop();
    }

    @Override
    public void pause()
    {
        cll.pause();
    }

    @Override
    public void resume()
    {
        cll.resume();
    }

    /**
     * Event logging methods
     */

    /**
     * Log simple event based on event schema defined in bond.
     *
     * @param event
     */
    public void log (Base event)
    {
        log(event, null);
    }

    /**
     * Log event with one or more user ticket ids (CLL will call your back before uploading these to get the actual user and device tickets).
     *
     * @param event
     * @param ids
     */
    public void log (Base event, List<String> ids)
    {
        log(event, Latency.LatencyUnspecified, Persistence.PersistenceUnspecified, EnumSet.of(Sensitivity.SensitivityUnspecified), EventEnums.SampleRate_Unspecified, ids);
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
    public void log (Base event, Latency latency, Persistence persistence, EnumSet<Sensitivity> sensitivity, double sampleRate, List<String> ids)
    {
        PreSerializedEvent preSerializedEvent = PreSerializedEvent.createFromStaticEvent(logger, event);
        cll.log(preSerializedEvent, latency, persistence, sensitivity, sampleRate, ids);
    }

    /* Log dynamic event with specifed Latency/Persistence/Sensitivity/SampleRate overrides and optionally user ticket ids
     * use the corresponding *Unspecified enum values to keep the value defined for the event in bond schema.
     *
     * @param eventName
     * @param eventData
     * @param latency
     * @param persistence
     * @param sensitivity
     * @param sampleRate
     * @param ids
     */
    public void log(String eventName, String eventData, Latency latency, Persistence persistence, EnumSet<Sensitivity> sensitivity, double sampleRate, List<String> ids)
    {
        if (!eventName.contains("."))
        {
            logger.error(TAG, "Event Name does not follow a valid format. Your event must have at least one . between two words. E.g. Microsoft.MyEvent");
            return;
        }

        PreSerializedEvent preSerializedEvent = PreSerializedEvent.createFromDynamicEvent(eventName, eventData);
        cll.log(preSerializedEvent, latency, persistence, sensitivity, sampleRate, ids);
    }

    /**
     * TEST ONLY INTERFACE
     * Log event with specified Latency/Persistence/Sensitivity/SampleRate overrides
     * use the corresponding *Unspecified enum values to keep the value defined for the event in bond schema.
     *
     * @param testEvent
     */
    public void logInternal(com.microsoft.telemetry.Base testEvent)
    {
        cll.log(testEvent, null, null, null, EventEnums.SampleRate_Unspecified, null);
    }

    /**
     * Etc
     */

    @Override
    public void setDebugVerbosity(Verbosity verbosity)
    {
        cll.setDebugVerbosity(verbosity);
    }

    @Override
    public void send()
    {
        cll.send();
    }

    @Override
    public void setEndpointUrl(String url)
    {
        cll.setEndpointUrl(url);
    }

    @Override
    public void useLegacyCS(boolean value)
    {
        cll.useLegacyCS(value);
    }

    @Override
    public void setExperimentId(String id)
    {
        cll.setExperimentId(id);
    }

    @Override
    public void synchronize()
    {
        cll.synchronize();
    }

    @Override
    public void SubscribeCllEvents(ICllEvents cllEvents)
    {
        cll.SubscribeCllEvents(cllEvents);
    }

    @Override
    public void setAppUserId(String userId)
    {
        cll.setAppUserId(userId);
    }

    @Override
    public String getAppUserId()
    {
        return cll.getAppUserId();
    }

    public CorrelationVector getCorrelationVector()
    {
        return ((SingletonCll)cll).correlationVector;
    }

    public void setXuidCallback(ITicketCallback callback)
    {
        cll.setXuidCallback(callback);
    }

    /**
     * SettingsStore.UpdateListener interface implementation
     */

    @Override
    public void OnHostSettingUpdate(String settingName, String settingValue)
    {
        SharedPreferences.Editor editor = hostPreferences.edit();
        editor.putString(settingName, settingValue);
        editor.apply();
    }

    @Override
    public void OnCllSettingUpdate(String settingName, String settingValue)
    {
        SharedPreferences.Editor editor = cllPreferences.edit();
        editor.putString(settingName, settingValue);
        editor.apply();
    }

    private void setSettingsStoreValues()
    {
        Map<String, String> settings = (Map<String, String>) cllPreferences.getAll();
        for (Map.Entry<String, String> setting : settings.entrySet())
        {
            // attempt to get the enum value for deprecated/removed string may fail
            SettingsStore.Settings settingKey;

            try
            {
                settingKey = SettingsStore.Settings.valueOf(setting.getKey());
            }
            catch (Exception e)
            {
                // remove the failing setting so that we don't throw every time
                SharedPreferences.Editor editor = cllPreferences.edit();
                editor.remove(setting.getKey());
                editor.apply();
                continue;
            }

            SettingsStore.updateCllSetting(settingKey, setting.getValue());
        }

        settings = (Map<String, String>) hostPreferences.getAll();
        for (Map.Entry<String, String> setting : settings.entrySet())
        {
            SettingsStore.updateHostSetting(setting.getKey(), setting.getValue());
        }
    }
}