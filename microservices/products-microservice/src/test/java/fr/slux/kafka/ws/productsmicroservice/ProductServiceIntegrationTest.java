package fr.slux.kafka.ws.productsmicroservice;

import fr.slux.kafka.ws.api.ProductCreatedEvent;
import fr.slux.kafka.ws.productsmicroservice.rest.CreateProductRestModel;
import fr.slux.kafka.ws.productsmicroservice.service.ProductService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@DirtiesContext // spring context it was modified during the execution. So, it gets rebuilt an every test
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test") // looks for application-test.properties
@EmbeddedKafka(partitions = 3, count = 3, controlledShutdown = true)
@SpringBootTest(properties = "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}")
public class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private Environment env;

    private KafkaMessageListenerContainer<String, ProductCreatedEvent> kafkaListener;

    private BlockingQueue<ConsumerRecord<String, ProductCreatedEvent>> records;

    @BeforeAll
    void setUp() {
        DefaultKafkaConsumerFactory<String, Object> consumerFactory = new DefaultKafkaConsumerFactory<>(getConsumerProperties());
        ContainerProperties containerProperties = new ContainerProperties(env.getProperty("product-created-events-topic-name"));
        this.kafkaListener = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        this.records = new LinkedBlockingQueue<>();

        // Each consumed kafka message record will be stored in the queue so that it's easy to assert its content in the tests
        // below is the same as this this.kafkaListener.setupMessageListener((MessageListener<String, ProductCreatedEvent>) data -> records.add(data));
        this.kafkaListener.setupMessageListener((MessageListener<String, ProductCreatedEvent>) records::add);
        this.kafkaListener.start();
        ContainerTestUtils.waitForAssignment(this.kafkaListener, embeddedKafkaBroker.getPartitionsPerTopic());
    }

    @AfterAll
    void tierDown() {
        this.kafkaListener.stop();
    }

    @Test
    void testCreateProduct_whenGivenValidProductDetails_successfulSendsKafkaMessage() throws Exception {
        // Arrange
        String title = "iPhone 12";
        BigDecimal price = new BigDecimal(899.99);
        Integer quantity = 1;

        CreateProductRestModel createProductRestModel = new CreateProductRestModel();
        createProductRestModel.setTitle(title);
        createProductRestModel.setPrice(price);
        createProductRestModel.setQuantity(quantity);

        // Act
        this.productService.createProduct(createProductRestModel);

        // Assert (check if the Kafka message was actually sent)
        ConsumerRecord<String, ProductCreatedEvent> record = this.records.poll(3000, TimeUnit.MILLISECONDS);
        Assertions.assertNotNull(record);
        Assertions.assertNotNull(record.key());
        ProductCreatedEvent msg = record.value();
        Assertions.assertEquals(title, msg.getTitle());
        Assertions.assertEquals(price, msg.getPrice());
        Assertions.assertEquals(quantity, msg.getQuantity());
    }

    private Map<String, Object> getConsumerProperties() {
        return Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.embeddedKafkaBroker.getBrokersAsString(),
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class,
                ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class,
                ConsumerConfig.GROUP_ID_CONFIG, env.getProperty("spring.kafka.consumer.group-id"),// in the application-test.property
                JsonDeserializer.TRUSTED_PACKAGES, env.getProperty("spring.kafka.consumer.properties.spring.json.trusted.packages"),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, env.getProperty("spring.kafka.consumer.auto-offset-reset"));
    }
}
