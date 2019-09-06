package controllers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class EdgeSerializer extends StdSerializer<Edge> {

    public EdgeSerializer() {
        this(null);
    }

    public EdgeSerializer(Class<Edge> t) {
        super(t);
    }

    @Override
    public void serialize(
            Edge edge, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {

        jgen.writeStartObject();
        jgen.writeArrayFieldStart("source");
        jgen.writeNumber(edge.getFromLongitude());
        jgen.writeNumber(edge.getFromLatitude());
        jgen.writeEndArray();
        jgen.writeArrayFieldStart("target");
        jgen.writeNumber(edge.getToLongitude());
        jgen.writeNumber(edge.getToLatitude());
        jgen.writeEndArray();
        jgen.writeEndObject();
    }
}
