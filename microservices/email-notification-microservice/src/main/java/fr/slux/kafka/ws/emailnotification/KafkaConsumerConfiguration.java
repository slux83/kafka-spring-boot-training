package fr.slux.kafka.ws.emailnotification;

import fr.slux.kafka.ws.emailnotification.error.NotRetryableException;
import fr.slux.kafka.ws.emailnotification.error.RetryableException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.web.client.HttpServerErrorException;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfiguration {
    @Autowired
    private Environment env;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();

        // Another way to get application property values
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, env.getProperty("spring.kafka.consumer.bootstrap-servers"));
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Wrapper for deserialization to avoid loops in case of wrong JSON format
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class); // Use this when everything is OK

        config.put(JsonDeserializer.TRUSTED_PACKAGES, env.getProperty("spring.kafka.consumer.properties.spring.json.trusted.packages"));
        config.put(ConsumerConfig.GROUP_ID_CONFIG, env.getProperty("spring.kafka.consumer.group-id"));

        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, env.getProperty("spring.kafka.consumer.auto-offset-reset"));
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, env.getProperty("spring.kafka.consumer.bootstrap-servers"));
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory, KafkaTemplate<String, Object> kafkaTemplate) {

        // Any error message that can't be understood they are sent to the <origin-topic-name>.DLT topic
        DefaultErrorHandler defaultErrorHandler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate),
                new FixedBackOff(5000, 3)); // Needed for the retryable exception case to retry N times every M millisec

        // We add this to exclude the list of exceptions that should not be retried in case of error by the kafka listener
        // Better to keep one custom and catch exception programmatically (see ProductCreatedEventHandler class)
        defaultErrorHandler.addNotRetryableExceptions(NotRetryableException.class);
        defaultErrorHandler.addRetryableExceptions(RetryableException.class);

        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // This enables the sending of all badly formatted messages to the DLT topic
        factory.setCommonErrorHandler(defaultErrorHandler);

        return factory;
    }
}
