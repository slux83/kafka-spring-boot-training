package fr.slux.kafka.ws.productsmicroservice.service;

import fr.slux.kafka.ws.api.ProductCreatedEvent;
import fr.slux.kafka.ws.productsmicroservice.rest.CreateProductRestModel;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProductServiceImpl implements ProductService {
    private static final Logger LOG = LoggerFactory.getLogger(ProductServiceImpl.class);

    private KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;

    public ProductServiceImpl(KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public String createProduct(CreateProductRestModel model) throws Exception {
        String productId = UUID.randomUUID().toString();
        // TODO Eventually store this into a Database, before publishing an event to Kafka

        ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent(productId, model.getTitle(), model.getPrice(), model.getQuantity());
        String topicName = "product-created-events-topic";

        // This will NOT wait for the ACK from kafka
        /*
        CompletableFuture<SendResult<String, ProductCreatedEvent>> kafkaRespFuture = this.kafkaTemplate.send(topicName, productId, productCreatedEvent);
        kafkaRespFuture.whenComplete((res, ex) -> {
            if (ex != null)
                LOG.error("Error while pushing data to kafka: {}", ex, ex);
            else
                LOG.info("Product ID {} was successfully sent to kafka: {}", productId, res.getRecordMetadata());
        });

        //kafkaRespFuture.join(); // this will make it synchronous. Not the best way to do so
         */

        // This is another way to get the call being synchronous
        //SendResult<String, ProductCreatedEvent> karfkaResult = this.kafkaTemplate.send(topicName, productId, productCreatedEvent).get();

        // Unique message ID is important to have a idempotent consumer. You can either add it as msg key or in the metadata of the msg.
        ProducerRecord<String, ProductCreatedEvent> record = new ProducerRecord<>(
                topicName,
                productId,
                productCreatedEvent
        );
        record.headers().add("msg-id", UUID.randomUUID().toString().getBytes());
        SendResult<String, ProductCreatedEvent> karfkaResult = this.kafkaTemplate.send(record).get();

        LOG.info("Returning from createProduct with id {}. Partition={} TopicName={}, Offset={}",
                productId,
                karfkaResult.getRecordMetadata().partition(),
                karfkaResult.getRecordMetadata().topic(),
                karfkaResult.getRecordMetadata().offset());


        return productId;
    }
}
