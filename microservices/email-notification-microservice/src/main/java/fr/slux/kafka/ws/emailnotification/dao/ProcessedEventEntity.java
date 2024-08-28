package fr.slux.kafka.ws.emailnotification.dao;

import jakarta.persistence.*;

@Entity
@Table(name = "processed-events")
public class ProcessedEventEntity {
    @Id
    @GeneratedValue()
    private long id;

    @Column(nullable = false, unique = true)
    private String msgId;

    @Column(nullable = false)
    private String productId;

    public ProcessedEventEntity() {
    }

    public ProcessedEventEntity(String msgId, String productId) {
        this.msgId = msgId;
        this.productId = productId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }
}
