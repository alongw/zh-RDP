package com.microsoft.cll.android;

import com.microsoft.bond.BondSerializable;
import com.microsoft.bond.ProtocolWriter;

import java.io.IOException;

/**
 * Created by jmorman on 3/9/2015.
 */
public class BondJsonSerializer {
    private final ProtocolWriter writer;
    private final StringBuilder resultString;
    private final ILogger logger;
    private final String TAG = "AndroidCll-EventSerializer";

    public BondJsonSerializer(ILogger logger) {
        this.resultString   = new StringBuilder();
        this.writer         = new JsonProtocol(resultString);
        this.logger         = logger;
    }

    /*
    Serializes the event to json
     */
    public synchronized String serialize(BondSerializable event) {
        try {
            event.write(this.writer);
        } catch (IOException e) {
            logger.error(TAG, "IOException when serializing");
        }

        String serialized = this.writer.toString();
        this.resultString.setLength(0);
        return serialized;
    }
}
