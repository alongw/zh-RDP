//------------------------------------------------------------------------------
// <auto-generated>
//     This code was generated by a tool.
// 
//     Tool     : bondc, Version=3.0.1, Build=bond-git.retail.directory
//     Template : Microsoft.Bond.Rules.dll#Java.tt
//     File     : Microsoft/Telemetry/Extensions/app.java
//
//     Changes to this file may cause incorrect behavior and will be lost when
//     the code is regenerated.
// </auto-generated>
//------------------------------------------------------------------------------
package Microsoft.Telemetry.Extensions;


// [Description("Describes the properties of the running application. This extension could be populated by a client app or a web app.")]
/**
* app
*/
@SuppressWarnings("all")
public class app extends Microsoft.Telemetry.Extension {
    // TODO: implement
    public com.microsoft.bond.BondSerializable clone() {return null;}

    //
    // Fields
    //

    // 10: Optional string expId
    private String expId;

    /**
     * @return current value of expId property
     */
    public final String getExpId() {
        return this.expId;
    }

    /**
     * @param value new value of expId property
     */
    public final void setExpId(String value) {
        this.expId = value;
    }

    /**
     * Schema metadata
     */
    public static class Schema {
        public static final com.microsoft.bond.SchemaDef schemaDef;
        public static final com.microsoft.bond.Metadata metadata;
        private static final com.microsoft.bond.Metadata expId_metadata;

        static {
            metadata = new com.microsoft.bond.Metadata();
            metadata.setName("app");
            metadata.setQualified_name("Microsoft.Telemetry.Extensions.app");

            metadata.getAttributes().put("Description", "Describes the properties of the running application. This extension could be populated by a client app or a web app.");

            // expId
            expId_metadata = new com.microsoft.bond.Metadata();
            expId_metadata.setName("expId");
            expId_metadata.getAttributes().put("Description", "Comma delimited list of experiment ids for experiments installed on the Application. Format is <NamespaceIdentifier>:<ExperimentId> for example, m:12345.");

            schemaDef = new com.microsoft.bond.SchemaDef();
            schemaDef.setRoot(getTypeDef(schemaDef));
        }

        public static com.microsoft.bond.TypeDef getTypeDef(com.microsoft.bond.SchemaDef schema)
        {
            com.microsoft.bond.TypeDef type = new com.microsoft.bond.TypeDef();
            type.setId(com.microsoft.bond.BondDataType.BT_STRUCT);
            type.setStruct_def(getStructDef(schema));
            return type;
        }

        private static short getStructDef(com.microsoft.bond.SchemaDef schema)
        {
            short pos;

            for(pos = 0; pos < schema.getStructs().size(); pos++)
            {
                if (schema.getStructs().get(pos).getMetadata() == metadata)
                {
                    return pos;
                }
            }

            com.microsoft.bond.StructDef structDef = new com.microsoft.bond.StructDef();
            schema.getStructs().add(structDef);

            structDef.setMetadata(metadata);
            structDef.setBase_def(Microsoft.Telemetry.Extension.Schema.getTypeDef(schema));

            com.microsoft.bond.FieldDef field;

            field = new com.microsoft.bond.FieldDef();
            field.setId((short)10);
            field.setMetadata(expId_metadata);
            field.getType().setId(com.microsoft.bond.BondDataType.BT_STRING);
            structDef.getFields().add(field);

            return pos;
        }
    }

    /*
    * @see com.microsoft.bond.BondMirror#getField()
    */
    public Object getField(com.microsoft.bond.FieldDef fieldDef) {
        switch (fieldDef.getId()) {
            case (short)10:
                return this.expId;
            default:
                return null;
        }
    }


    /*
    * @see com.microsoft.bond.BondMirror#setField()
    */
    public void setField(com.microsoft.bond.FieldDef fieldDef, Object value) {
        switch (fieldDef.getId()) {
            case (short)10:
                this.expId = (String)value;
                break;
        }
    }


    /*
    * @see com.microsoft.bond.BondMirror#createInstance()
    */
    public com.microsoft.bond.BondMirror createInstance(com.microsoft.bond.StructDef structDef) {
        return null;
    }

    /*
     * @see com.microsoft.bond.BondMirror#getSchema()
     */
    public com.microsoft.bond.SchemaDef getSchema()
    {
        return getRuntimeSchema();
    }

    /**
     * Static method returning {@link SchemaDef} instance.
     */
    public static com.microsoft.bond.SchemaDef getRuntimeSchema()
    {
        return Schema.schemaDef;
    }


    // Constructor
    public app() {
        
    }

    /*
     * @see com.microsoft.bond.BondSerializable#reset()
     */
    public void reset() {
        reset("app", "Microsoft.Telemetry.Extensions.app");
    }

    protected void reset(String name, String qualifiedName) {
        super.reset(name, qualifiedName);
        this.expId = "";
    }

    /*
     * @see com.microsoft.bond.BondSerializable#unmarshal()
     */
    public void unmarshal(java.io.InputStream input) throws java.io.IOException {
        com.microsoft.bond.internal.Marshaler.unmarshal(input, this);
    }

    /*
     * @see com.microsoft.bond.BondSerializable#unmarshal()
     */
    public void unmarshal(java.io.InputStream input, com.microsoft.bond.BondSerializable schema) throws java.io.IOException {
        com.microsoft.bond.internal.Marshaler.unmarshal(input, (com.microsoft.bond.SchemaDef)schema, this);
    }

    /*
     * @see com.microsoft.bond.BondSerializable#read()
     */
    public void read(com.microsoft.bond.ProtocolReader reader) throws java.io.IOException {
        reader.readBegin();
        readNested(reader);
        reader.readEnd();
    }

    /*
     * Called to read a struct that is contained inside another struct.
     */
    public void readNested(com.microsoft.bond.ProtocolReader reader) throws java.io.IOException {
        if (!reader.hasCapability(com.microsoft.bond.ProtocolCapability.TAGGED)) {
            readUntagged(reader, false);
        } else if (readTagged(reader, false)) {
            com.microsoft.bond.internal.ReadHelper.skipPartialStruct(reader);
        }
    }

    /*
     * @see com.microsoft.bond.BondSerializable#read()
     */
    public void read(com.microsoft.bond.ProtocolReader reader, com.microsoft.bond.BondSerializable schema) throws java.io.IOException {
        // read(com.microsoft.bond.internal.ProtocolHelper.createReader(reader, schema));
    }

    protected void readUntagged(com.microsoft.bond.ProtocolReader reader, boolean isBase) throws java.io.IOException {
        boolean canOmitFields = reader.hasCapability(com.microsoft.bond.ProtocolCapability.CAN_OMIT_FIELDS);

        reader.readStructBegin(isBase);
        super.readUntagged(reader, true);

        if (!canOmitFields || !reader.readFieldOmitted()) {
            this.expId = reader.readString();
        }
        reader.readStructEnd();
    } // ReadUntagged()


    protected boolean readTagged(com.microsoft.bond.ProtocolReader reader, boolean isBase) throws java.io.IOException {
        boolean isPartial;
        reader.readStructBegin(isBase);

        if (!super.readTagged(reader, true))
        {
            return false;
        }

        while (true) {
            com.microsoft.bond.ProtocolReader.FieldTag fieldTag = reader.readFieldBegin();

            if (fieldTag.type == com.microsoft.bond.BondDataType.BT_STOP
             || fieldTag.type == com.microsoft.bond.BondDataType.BT_STOP_BASE) {
                isPartial = (fieldTag.type == com.microsoft.bond.BondDataType.BT_STOP_BASE);
                break;
            }

            switch (fieldTag.id) {
                case 10:
                    this.expId = com.microsoft.bond.internal.ReadHelper.readString(reader, fieldTag.type);
                    break;
                default:
                    reader.skip(fieldTag.type);
                    break;
            }

            reader.readFieldEnd();
        }

        reader.readStructEnd();

        return isPartial;
    }


    /*
     * @see com.microsoft.bond.BondSerializable#marshal()
     */
    public void marshal(com.microsoft.bond.ProtocolWriter writer) throws java.io.IOException {
        com.microsoft.bond.internal.Marshaler.marshal(this, writer);
    }

    /*
     * @see com.microsoft.bond.BondSerializable#write()
     */
    public void write(com.microsoft.bond.ProtocolWriter writer) throws java.io.IOException {
        writer.writeBegin();
        com.microsoft.bond.ProtocolWriter firstPassWriter;
        if ((firstPassWriter = writer.getFirstPassWriter()) != null)
        {
            writeNested(firstPassWriter, false);
            writeNested(writer, false);
        }
        else
        {
          writeNested(writer, false);
        }
        writer.writeEnd();
    }

    public void writeNested(com.microsoft.bond.ProtocolWriter writer, boolean isBase) throws java.io.IOException {
        boolean canOmitFields = writer.hasCapability(com.microsoft.bond.ProtocolCapability.CAN_OMIT_FIELDS);
        writer.writeStructBegin(Schema.metadata, isBase);
        super.writeNested(writer, true);

        if (!canOmitFields || (expId != Schema.expId_metadata.getDefault_value().getString_value())) {
            writer.writeFieldBegin(com.microsoft.bond.BondDataType.BT_STRING, 10, Schema.expId_metadata);
            writer.writeString(expId);
            writer.writeFieldEnd();
        } else {
            writer.writeFieldOmitted(com.microsoft.bond.BondDataType.BT_STRING, 10, Schema.expId_metadata);
        }

        writer.writeStructEnd(isBase);
    } // writeNested


    public boolean memberwiseCompare(Object obj) {
        if (obj == null) {
            return false;
        }

        app that = (app)obj;

        return memberwiseCompareQuick(that) && memberwiseCompareDeep(that);
    }

    protected boolean memberwiseCompareQuick(app that) {
        boolean equals = true;
        equals = equals && super.memberwiseCompareQuick(that);
        equals = equals && ((this.expId == null) == (that.expId == null));
        equals = equals && (this.expId == null ? true : (this.expId.length() == that.expId.length()));
        return equals;
    } // memberwiseCompareQuick

    protected boolean memberwiseCompareDeep(app that) {
        boolean equals = true;
        equals = equals && super.memberwiseCompareDeep(that);
        equals = equals && (this.expId == null ? true : this.expId.equals(that.expId));
        return equals;
    } // memberwiseCompareDeep

}; // class app
