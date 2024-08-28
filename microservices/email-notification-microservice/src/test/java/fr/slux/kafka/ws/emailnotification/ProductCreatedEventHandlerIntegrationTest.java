package fr.slux.kafka.ws.emailnotification;

import fr.slux.kafka.ws.api.ProductCreatedEvent;
import fr.slux.kafka.ws.emailnotification.dao.ProcessedEventEntity;
import fr.slux.kafka.ws.emailnotification.dao.ProcessedEventRepository;
import fr.slux.kafka.ws.emailnotification.handler.ProductCreatedEventHandler;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DirtiesContext // spring context it was modified during the execution. So, it gets rebuilt an every test
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test") // looks for application-test.properties
@EmbeddedKafka
@SpringBootTest(properties = "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}")
public class ProductCreatedEventHandlerIntegrationTest {

    @MockBean
    private ProcessedEventRepository processedEventRepository;

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    // Keep the original object but can be spied by mockito and arg captors
    @SpyBean
    private ProductCreatedEventHandler handler;

    @Test
    public void testProductCreatedEventHandler_OnProductCreated_HandlesEvent() throws Exception {
        // Arrange
        ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent();
        productCreatedEvent.setPrice(new BigDecimal(40));
        productCreatedEvent.setProductId(UUID.randomUUID().toString());
        productCreatedEvent.setQuantity(2);
        productCreatedEvent.setTitle("Test Product");

        String msgId = UUID.randomUUID().toString();
        String msgKey = productCreatedEvent.getProductId();
        ProducerRecord<String, Object> record = new ProducerRecord<>(
                "product-created-events-topic",
                msgKey,
                productCreatedEvent);
        record.headers().add("msg-id", msgId.getBytes());
        record.headers().add(KafkaHeaders.RECEIVED_KEY, msgKey.getBytes());

        // teach the mocks
        ProcessedEventEntity processedEventEntity = new ProcessedEventEntity();
        when(this.processedEventRepository.findByMsgId(anyString())).thenReturn(processedEventEntity);
        when(this.processedEventRepository.save(any(ProcessedEventEntity.class))).thenReturn(null);

        // Optional but useful
        String responseBody = """
                {"key": "value"}
                """;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> responseEntity = new ResponseEntity<>(responseBody, headers, HttpStatus.OK);
        when(this.restTemplate.exchange(
                any(String.class),
                any(HttpMethod.class),
                isNull(),
                eq(String.class)
        )).thenReturn(responseEntity);

        // Act
        this.kafkaTemplate.send(record).get();

        // Assert
        ArgumentCaptor<String> messageIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ProductCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ProductCreatedEvent.class);

        verify(this.handler,
                timeout(5000).times(1))
                .handle(
                        eventCaptor.capture(),
                        messageIdCaptor.capture(),
                        messageKeyCaptor.capture());

        assertEquals(msgId, messageIdCaptor.getValue());
        assertEquals(msgKey, messageKeyCaptor.getValue());
        assertEquals(productCreatedEvent.getProductId(), eventCaptor.getValue().getProductId());
    }
}
