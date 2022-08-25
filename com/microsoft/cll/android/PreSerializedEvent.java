package com.microsoft.cll.android;

import com.microsoft.telemetry.Base;
import com.microsoft.telemetry.Data;
import com.microsoft.telemetry.Domain;

// this class also uses the conflicting following types, so be careful
// import Microsoft.Telemetry.Base;
// import Microsoft.Telemetry.Data;
// import Microsoft.Telemetry.Domain;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.HashMap;

public class PreSerializedEvent extends Data
{
    public String serializedData;

    private static final String TAG = "AndroidCll-PreSerializedEvent";

    public PreSerializedEvent(String eventName, String eventData, String partBName, Map<String, String> attributes)
    {
        this.serializedData = eventData;
        Data baseData = ((Data)(Base)this);
        baseData.setBaseData(new Domain());
        ((Domain) baseData.getBaseData()).QualifiedName = partBName;

        this.QualifiedName = eventName;

        if (attributes != null)
        {
            this.Attributes.putAll(attributes);
        }
    }

    @Override
    public void serialize(Writer writer) throws IOException
    {
        writer.write(serializedData);
    }

    public static PreSerializedEvent createFromDynamicEvent(String eventName, String eventData)
    {
        return new PreSerializedEvent(eventName, eventData, "", null);
    }

    public static PreSerializedEvent createFromStaticEvent(ILogger logger, Microsoft.Telemetry.Base event)
    {
        String eventName = getPartCName(event);
        String partBName = getPartBName(logger, event);

        Map<String, String> attributes = getAttributes(event);

        if (!partBName.isEmpty())
        {
            event.setBaseType(partBName);
        }

        String eventData = serializeEvent(logger, event);

        return new PreSerializedEvent(eventName, eventData, partBName, attributes);
    }

    private static String getPartCName(Microsoft.Telemetry.Base event)
    {
        String partCName = event.getSchema().getStructs().get(0).getMetadata().getQualified_name();
        return partCName;
    }

    private static String getPartBName(ILogger logger, Microsoft.Telemetry.Base event)
    {
        String partBName = "";
        try
        {
            partBName = ((Microsoft.Telemetry.Domain) ((Microsoft.Telemetry.Data) event).getBaseData()).getSchema().getStructs().get(0).getMetadata().getQualified_name();
        }
        catch (ClassCastException e)
        {
            logger.info(TAG, "This event doesn't extend data");
        }

        return partBName;
    }

    private static Map<String, String> getAttributes(Microsoft.Telemetry.Base event)
    {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.putAll(event.getSchema().getStructs().get(0).getMetadata().getAttributes());
        return attributes;
    }

    private static String serializeEvent(ILogger logger, Microsoft.Telemetry.Base event)
    {
        BondJsonSerializer bondJsonSerializer = new BondJsonSerializer(logger);
        return bondJsonSerializer.serialize(event);
    }
}
