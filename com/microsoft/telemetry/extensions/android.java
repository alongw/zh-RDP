/*
 * Generated from Microsoft.Telemetry.Extensions.bond (https://github.com/Microsoft/bond)
*/
package com.microsoft.telemetry.extensions;
import com.microsoft.telemetry.Extension;
import com.microsoft.telemetry.IJsonSerializable;
import com.microsoft.telemetry.JsonHelper;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Data contract class android.
 */
public class android extends Extension implements
    IJsonSerializable
{
    /**
     * Backing field for property LibVer.
     */
    private String libVer;
    
    /**
     * Backing field for property Tickets.
     */
    private List<String> tickets;
    
    /**
     * Initializes a new instance of the android class.
     */
    public android()
    {
        this.InitializeFields();
    }
    
    /**
     * Gets the LibVer property.
     */
    public String getLibVer() {
        return this.libVer;
    }
    
    /**
     * Sets the LibVer property.
     */
    public void setLibVer(String value) {
        this.libVer = value;
    }
    
    /**
     * Gets the Tickets property.
     */
    public List<String> getTickets() {
        if (this.tickets == null) {
            this.tickets = new ArrayList<String>();
        }
        return this.tickets;
    }
    
    /**
     * Sets the Tickets property.
     */
    public void setTickets(List<String> value) {
        this.tickets = value;
    }
    

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
    protected String serializeContent(Writer writer) throws IOException
    {
        String prefix = super.serializeContent(writer);
        if (!(this.libVer == null))
        {
            writer.write(prefix + "\"libVer\":");
            writer.write(JsonHelper.convert(this.libVer));
            prefix = ",";
        }
        
        if (!(this.tickets == null))
        {
            writer.write(prefix + "\"tickets\":");
            // I manually modified this to writeListString because the bond generator doesn't
            // understand how to write a list of strings
            JsonHelper.writeListString(writer, this.tickets);
            prefix = ",";
        }
        
        return prefix;
    }
    
    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {
        
    }
}
