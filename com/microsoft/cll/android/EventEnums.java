package com.microsoft.cll.android;

//
// EventEnums.java
// Defines the enumerations used to log the events.
//
// Copyright © 2016 Microsoft. All rights reserved.

import java.util.EnumSet;
import java.util.Iterator;

public final class EventEnums
{
    private EventEnums()
    {
        // This is a static const/enum holding class and it should never be instantiated
        throw new AssertionError();
    }

    // Event Flags
    // See https://osgwiki.com/wiki/CommonSchema/flags

    // Event Latency (Normal or Realtime) controls whether events has to be uploaded immediately or
    // could wait to be batched with other events.
    // See https://osgwiki.com/wiki/Common_Schema_Event_Latency
    static public enum Latency
    {
        LatencyUnspecified  (0),
        LatencyNormal       (0x0100),
        LatencyRealtime     (0x0200);

        final public int id;

        Latency(int id)
        {
            this.id = id;
        }

        static Latency FromString(String s)
        {
            if (s == "REALTIME")
            {
                return LatencyRealtime;
            }

            return LatencyNormal;
        }
    }

    // Event Persistence (Normal or Critical) controls which events to delete first if we are out of space.
    // See https://osgwiki.com/wiki/Common_Schema_Event_Persistence
    static public enum Persistence
    {
        PersistenceUnspecified  (0),
        PersistenceNormal       (0x01),
        PersistenceCritical     (0x02);

        final public int id;

        Persistence(int id)
        {
            this.id = id;
        }

        static Persistence FromString(String s)
        {
            if (s == "CRITICAL")
            {
                return PersistenceCritical;
            }

            return PersistenceNormal;
        }
    };

    // Event Sensitivity controls how sensitive the event is. None means it goes to the XPert and normal
    // CLL journals, Flag means it goes to UserSensitive journal, Hash means the PII info ñ user id, device id,
    // etc in the event needs to be hashed and there should be a separate epoch and seqNum and we should never
    // batch these with low-sensitivity, Drop means similar measures as Hash except that the Part A PII fields
    // would be dropped, not hashed.
    // See https://osgwiki.com/wiki/Common_Schema_Event_Sensitivity
    static public enum Sensitivity
    {
        // this value is not part of the Common Schema spec but it is necessary
        // for the callers to signal "use the default sensitivity from metadata or settings"
        // if nether sources specifies sensitivity it will eventually default to None
        SensitivityUnspecified  (0x000001),

        SensitivityNone         (0x000000),
        SensitivityMark         (0x080000),
        SensitivityHash         (0x100000),
        SensitivityDrop         (0x200000);

        final public int id;

        Sensitivity(int id)
        {
            this.id = id;
        }

        static EnumSet<Sensitivity> FromString(String s)
        {
            EnumSet<Sensitivity> sensitivity = EnumSet.noneOf(Sensitivity.class);

            if (s != null)
            {
                if (s.contains("MARK") || s.toUpperCase().contains("USERSENSITIVE"))
                {
                    sensitivity.add(SensitivityMark);
                }

                if (s.contains("DROP"))
                {
                    sensitivity.add(SensitivityDrop);
                }

                if (s.contains("HASH"))
                {
                    sensitivity.add(SensitivityHash);
                }
            }

            return sensitivity;
        }
    };

    // Sample Rate (0 % -100 %) determins the percentage of devices randomly sampled in for this event collection
    // based on their device.localId. The smallest sampleRate step 0.001 (%).
    // SampleRate_Epsilon is used as an accuracy limit for floating point number comparisions.
    static public final double SampleRate_NoSampling = 100.0;
    static public final double SampleRate_10_percent = 10.0;
    static public final double SampleRate_0_percent = 0.0;
    static public final double SampleRate_Unspecified = -1.0;
    static public final double SampleRate_Epsilon = 0.00001;

    static double SampleRateFromString(String s)
    {
        double d = SampleRate_NoSampling;

        try
        {
            d = Double.parseDouble(s);
        }
        catch (NumberFormatException nfe)
        {
        }

        return d;
    }
}
