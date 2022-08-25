/**
 * Copyright Microsoft Corporation 2014
 * All Rights Reserved
 */

package com.microsoft.cll.android;

import android.util.Base64;
import com.microsoft.bond.*;

import java.io.IOException;
import java.util.Stack;

/**
 * Created by vsabella on 12/13/13.
 */
public class JsonProtocol extends com.microsoft.bond.ProtocolWriter
{
    private static final char ESCAPE_CHAR = '\\';
    private static final String NUMERIC_ESCAPE_STRING = "\\u";
    private static final Stack<BondDataType> keyTypes = new Stack<BondDataType>();
    private static final Stack<BondDataType> valueTypes = new Stack<BondDataType>();
    private static final Stack<Boolean> inContainerStack = new Stack<Boolean>();

    private static final char[] HEX_CHARACTERS =
    { '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private final StringBuilder stringBuilder;

    private final Stack<Boolean> containerIsTyped = new Stack<Boolean>();
    private boolean inContainer;
    private boolean isKey;

    public JsonProtocol(
            final StringBuilder stringBuilder)
    {
        this.stringBuilder = stringBuilder;
    }

    @Override
    public void writeVersion() throws IOException
    {
    }

    @Override
    public void writeBegin()
    {
        // this.stringBuilder.append('{');
    }

    @Override
    public void writeEnd()
    {
        // this.stringBuilder.append('}');
    }

    @Override
    public void writeStructBegin(
            final com.microsoft.bond.BondSerializable metadata,
            final boolean isBase)
    {
        if (!isBase)
        {
            this.stringBuilder.append('{');
        }

        inContainerStack.push(false);
    }

    @Override
    public void writeStructEnd(final boolean isBase)
    {
        if (!isBase)
        {
            this.removeLastComma();
            this.stringBuilder.append('}');

            // Don't add comma to root struct
            if(inContainerStack.size() > 1) {
                this.appendComma();
            }
        }

        inContainerStack.pop();
    }

    @Override
    public void writeFieldBegin(final com.microsoft.bond.BondDataType type,
            final int id,
            final com.microsoft.bond.BondSerializable metadata)
            throws java.io.IOException
    {
        final Metadata bondMetadata = metadata instanceof Metadata ? (Metadata) metadata
                : null;

        if (bondMetadata != null)
        {
            this.writeJsonFieldName(bondMetadata.getName());
        }
    }

    @Override
    public void writeFieldEnd()
    {
        this.appendComma();
    }

    @Override
    public void writeFieldOmitted(BondDataType type, int id, BondSerializable metadata) throws IOException {
    }

    @Override
    public void writeContainerBegin(final int i, final BondDataType bondDataType)
            throws IOException
    {
        this.stringBuilder.append('[');
        this.containerIsTyped.push(Boolean.TRUE);

        inContainerStack.push(true);
    }

    @Override
    public void writeContainerBegin(final int i, final BondDataType keyType,
            final BondDataType valueType) throws IOException
    {
        this.stringBuilder.append('{');
        this.containerIsTyped.push(Boolean.FALSE);
        this.inContainer = true;
        this.isKey = true;

        keyTypes.push(keyType);
        valueTypes.push(valueType);
        inContainerStack.push(true);
    }

    @Override
    public void writeContainerEnd() throws IOException
    {
        this.removeLastComma();
        this.stringBuilder.append(this.containerIsTyped.pop() ? ']' : '}');
        this.inContainer = false;
        this.isKey = false;

        keyTypes.pop();
        inContainerStack.pop();
    }

    @Override
    public void writeBool(final boolean b) throws IOException
    {
        this.stringBuilder.append(b);
        this.appendInContainer();
    }

    @Override
    public void writeString(final String s) throws IOException
    {
        if (inContainerStack.peek() && !keyTypes.empty() && keyTypes.peek() == BondDataType.BT_STRING)
        {
            if (this.isKey)
            {
                // Handle the case where we are in a container and need to print a string as a field
                this.writeJsonFieldName(s);
            } else if(!this.isKey){
                // Handle the case where we are in a container and we want to print the value
                actuallyWriteString(s);
            }

            // Only if both key and value types are string should we alternate how we write.
            if(valueTypes.peek() == BondDataType.BT_STRING) {
                this.isKey = !this.isKey;
            }
        }
        else {
            actuallyWriteString(s);
        }

    }

    private void actuallyWriteString(String s) {

        if (s == null)
        {
            // null isn't a string technically
            this.appendEscaped("null"); //$NON-NLS-1$
            this.appendInContainer();
        }
        else
        {
            this.stringBuilder.append('"');
            this.appendEscaped(s);
            this.stringBuilder.append('"');
            this.appendInContainer();

        }
    }

    @Override
    public void writeWString(final String s) throws IOException
    {
        this.writeString(s);
    }

    @Override
    public void writeFloat(final float v) throws IOException
    {
        this.stringBuilder.append(v);
        this.appendInContainer();
    }

    @Override
    public void writeDouble(final double v) throws IOException
    {
        this.stringBuilder.append(v);
        this.appendInContainer();
    }

    @Override
    public void writeBlob(final BondBlob bondBlob) throws IOException
    {
        this.stringBuilder.append(Base64.encode(bondBlob.getBuffer(), Base64.DEFAULT));
        this.appendInContainer();
    }

    @Override
    public void writeUInt8(final byte b) throws IOException
    {
        this.stringBuilder.append(b);
        this.appendInContainer();
    }

    @Override
    public void writeUInt16(final short i) throws IOException
    {
        this.stringBuilder.append(i);
        this.appendInContainer();
    }

    @Override
    public void writeUInt32(final int i) throws IOException
    {
        this.stringBuilder.append(i);
        this.appendInContainer();
    }

    @Override
    public void writeUInt64(final long l) throws IOException
    {
        this.stringBuilder.append(l);
        this.appendInContainer();
    }

    @Override
    public void writeInt8(final byte b) throws IOException
    {
        this.stringBuilder.append(b);
        this.appendInContainer();
    }

    @Override
    public void writeInt16(final short i) throws IOException
    {
        this.stringBuilder.append(i);
        this.appendInContainer();
    }

    @Override
    public void writeInt32(final int i) throws IOException
    {
        this.stringBuilder.append(i);
        this.appendInContainer();
    }

    @Override
    public void writeInt64(final long l) throws IOException
    {
        this.stringBuilder.append(l);
        this.appendInContainer();
    }

    @Override
    public boolean hasCapability(ProtocolCapability capability) {
        if(capability == ProtocolCapability.CAN_OMIT_FIELDS) {
            return true;
        }

        return super.hasCapability(capability);
    }

    @Override
    public String toString()
    {
        return this.stringBuilder.toString();
    }

    private void appendInContainer()
    {
        if (this.inContainer)
        {
            this.appendComma();
        }
    }

    private void appendComma()
    {
        if ((this.stringBuilder.length() > 0)
                && (this.stringBuilder.charAt(this.stringBuilder.length() - 1) != ','))
        {
            this.stringBuilder.append(',');
        }
    }

    private void removeLastComma()
    {
        if ((this.stringBuilder.length() > 0)
                && (this.stringBuilder.charAt(this.stringBuilder.length() - 1) == ','))
        {
            this.stringBuilder.deleteCharAt(this.stringBuilder.length() - 1);
        }
    }

    private void writeJsonFieldName(final String fieldName)
    {
        this.stringBuilder.append("\""); //$NON-NLS-1$
        this.appendEscaped(fieldName);
        this.stringBuilder.append("\":"); //$NON-NLS-1$
    }

    private void appendEscaped(final String value)
    {
        int parseIndex = this.stringBuilder.length();
        this.stringBuilder.append(value);
        int length = this.stringBuilder.length();

        while (parseIndex < length)
        {
            final char current = this.stringBuilder.charAt(parseIndex);
            switch (current)
            {
            // Escape back slash
            case '\\':
                this.stringBuilder.insert(parseIndex,
                        JsonProtocol.ESCAPE_CHAR);
                parseIndex += 2;
                length++;
                break;

            // Replace special control characters.
            case '\n':
                this.stringBuilder.insert(parseIndex++,
                        JsonProtocol.ESCAPE_CHAR);
                this.stringBuilder.setCharAt(parseIndex++, 'n');
                length++;
                break;

            case '\r':
                this.stringBuilder.insert(parseIndex++,
                        JsonProtocol.ESCAPE_CHAR);
                this.stringBuilder.setCharAt(parseIndex++, 'r');
                length++;
                break;
            case '\t':
                    this.stringBuilder.insert(parseIndex++,
                            JsonProtocol.ESCAPE_CHAR);
                    this.stringBuilder.setCharAt(parseIndex++, 't');
                    length++;
                    break;
            case'\"':
                this.stringBuilder.insert(parseIndex++,
                        JsonProtocol.ESCAPE_CHAR);
                this.stringBuilder.setCharAt(parseIndex++, '"');
                length++;
                break;
            default:
                if (Character.isISOControl(current))
                {
                    // Escape control characters
                    this.stringBuilder.insert(parseIndex++,
                            JsonProtocol.NUMERIC_ESCAPE_STRING);

                    this.stringBuilder
                            .setCharAt(
                                    parseIndex++,
                                    JsonProtocol.HEX_CHARACTERS[(current >> 12) & 0xF]);
                    this.stringBuilder
                            .insert(parseIndex++,
                                    JsonProtocol.HEX_CHARACTERS[(current >> 8) & 0xF]);
                    this.stringBuilder
                            .insert(parseIndex++,
                                    JsonProtocol.HEX_CHARACTERS[(current >> 4) & 0xF]);
                    this.stringBuilder.insert(parseIndex++,
                            JsonProtocol.HEX_CHARACTERS[current & 0xF]);

                    length += 5;
                }
                else
                {
                    // Regular Characters
                    parseIndex++;
                }
                break;
            }
        }
    }
}
