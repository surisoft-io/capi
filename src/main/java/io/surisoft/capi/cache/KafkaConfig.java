package io.surisoft.capi.cache;

import io.surisoft.capi.kafka.CapiEventSerializer;
import io.surisoft.capi.kafka.CapiInstance;
import io.surisoft.capi.kafka.CapiKafkaEvent;
import io.surisoft.capi.schema.CapiEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.*;

@Configuration
@ConditionalOnProperty(prefix = "capi.kafka", name = "enabled", havingValue = "true")
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Value("${capi.kafka.host}")
    private String capiKafkaHost;

    //@Value("${capi.kafka.topic}")
    //private String capiKafkaTopic;

    //@Value("${capi.kafka.group-instance}")
    //private String capiKafkaGroupInstance;

    //@Value("${capi.kafka.group-id}")
    //private String capiKafkaGroupId;

    @Value("${capi.kafka.ssl.enabled}")
    private boolean capiKafkaSslEnabled;

    @Value("${capi.kafka.ssl.keystore.location}")
    private String capiKafkaSslKeystoreLocation;

    @Value("${capi.kafka.ssl.keystore.password}")
    private String capiKafkaSslKeystorePassword;

    @Value("${capi.kafka.ssl.truststore.location}")
    private String capiKafkaSslTruststoreLocation;

    @Value("${capi.kafka.ssl.truststore.password}")
    private String capiKafkaSslTruststorePassword;

    @Bean
    public CapiInstance capiInstance() {
        return new CapiInstance(UUID.randomUUID().toString());
    }

    @Bean
    @ConditionalOnProperty(prefix = "capi.kafka", name = "enabled", havingValue = "true")
    public ProducerFactory<String, CapiEvent> producerFactory() {
        log.info("Configuring CAPI Kafka Producer");
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                capiKafkaHost);
        configProps.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class);
        configProps.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                CapiEventSerializer.class);
        if(capiKafkaSslEnabled) {
            configProps.put(
                    SECURITY_PROTOCOL_CONFIG,
                    "SSL");
            configProps.put(SSL_TRUSTSTORE_LOCATION_CONFIG,
                    capiKafkaSslTruststoreLocation);
            configProps.put(SSL_TRUSTSTORE_PASSWORD_CONFIG,
                    capiKafkaSslTruststorePassword);
            configProps.put(SSL_KEYSTORE_LOCATION_CONFIG,
                    capiKafkaSslKeystoreLocation);
            configProps.put(SSL_KEYSTORE_PASSWORD_CONFIG,
                    capiKafkaSslKeystorePassword);
            configProps.put(SSL_KEY_PASSWORD_CONFIG,
                    capiKafkaSslKeystorePassword);
        }
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, CapiEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}