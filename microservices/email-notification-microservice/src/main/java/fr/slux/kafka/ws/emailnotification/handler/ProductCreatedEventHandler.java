package fr.slux.kafka.ws.emailnotification.handler;

import fr.slux.kafka.ws.api.ProductCreatedEvent;
import fr.slux.kafka.ws.emailnotification.dao.ProcessedEventEntity;
import fr.slux.kafka.ws.emailnotification.dao.ProcessedEventRepository;
import fr.slux.kafka.ws.emailnotification.error.NotRetryableException;
import fr.slux.kafka.ws.emailnotification.error.RetryableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

@Component
// @KafkaListener can also be applied to the method directly, but not a good way if your topic has multiple data types.
// Therefore, better to simply put it at class level and specialize the methods below with the expected data type
@KafkaListener(topics = "product-created-events-topic")
public class ProductCreatedEventHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ProductCreatedEventHandler.class);
    private AtomicInteger messagesConuter;
    private RestTemplate restTemplate;
    private ProcessedEventRepository processedEventRepository;

    public ProductCreatedEventHandler(RestTemplate restTemplate, ProcessedEventRepository processedEventRepository) {
        this.restTemplate = restTemplate;
        this.processedEventRepository = processedEventRepository;
        this.messagesConuter = new AtomicInteger();
    }

    @Transactional // if there's an exception, the TX will be rolled back
    @KafkaHandler
    public void handle(
            @Payload ProductCreatedEvent productCreatedEvent,
            @Header(value = "msg-id", required = true) String msgId, // Reading custom header entries (can be optional too)
            @Header(KafkaHeaders.RECEIVED_KEY) String msgKey) {

        // Check if the msg was already processed
        if (this.processedEventRepository.findByMsgId(msgId) != null) {
            LOG.info("Event already processed with msg-id {}. Nothing to do here...", msgId);
            return;
        }

        this.messagesConuter.incrementAndGet();

        // if a business logic exception marked as not-retryable is thrown, this message will not be processed again
        // and also it will be sent to the DLT topic even though it's a "valid" and "parsable" message.
        // You can imagine having a second set of microservices that try to recover from these errors later on
        // when the service you are trying to consume in your business logic is available again (if needed)
        //if (true)
        //    throw new NotRetryableException("An error took place but no need to consume this message again");

        LOG.info("Received event (total={}, msg-id={}): product-it={}, {}",
                this.messagesConuter.get(), msgId,
                productCreatedEvent.getProductId(), productCreatedEvent);

        // We simulate a call to an external microservice that fails so that the backoff will retry if
        // we throw the correct retryable exception

        //String theUrl = "http://localhost:9090/whatever"; // Invalid one
        String theUrl = "https://httpbin.org/get"; // Valid one
        try {
            ResponseEntity<String> resp = this.restTemplate.exchange(theUrl, HttpMethod.GET, null, String.class);
            if (resp.getStatusCode().value() == HttpStatus.OK.value()) {
                LOG.info("Received response from remote service: STATUS {}", resp.getStatusCode().value());
            }
        } catch (ResourceAccessException e) {   // E.g., server unavailable (connection refused)
            LOG.error(e.getMessage());
            throw new RetryableException(e);    // RETRY
        } catch (Exception e) { // Anything else like 500 HTTP or other logic issues like DB unavailable
            LOG.error("Generic exception: {}", e.getMessage());
            throw new NotRetryableException(e); // NOT RETRY
        }

        // Store the message ID to the DB
        try {
            this.processedEventRepository.save(new ProcessedEventEntity(msgId, productCreatedEvent.getProductId()));
        } catch (DataIntegrityViolationException e) {
            LOG.warn("msg ID {} already present in the database. Likely this event was already processed", msgId);
            throw new NotRetryableException(e);
        }
    }
}
