package io.surisoft.capi.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.surisoft.capi.schema.CapiEvent;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CapiEventSerializer implements Serializer<CapiEvent> {
    private static final Logger log = LoggerFactory.getLogger(CapiEventSerializer.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public byte[] serialize(String topic, CapiEvent data) {
        try {
            if (data == null){
                log.warn("Null received at serializing");
                return null;
            }
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new SerializationException("Error when serializing MessageDto to byte[]");
        }
    }
}