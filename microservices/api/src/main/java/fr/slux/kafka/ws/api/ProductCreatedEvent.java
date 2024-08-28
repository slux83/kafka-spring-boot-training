package fr.slux.kafka.ws.api;

import java.math.BigDecimal;

/**
 * This is the class serialized into a kafka message
 */
public class ProductCreatedEvent {
    private String productId;
    private String title;
    private BigDecimal price;
    private Integer quantity;

    public ProductCreatedEvent(String productId, String title, BigDecimal price, Integer quantity) {
        this.productId = productId;
        this.title = title;
        this.price = price;
        this.quantity = quantity;
    }

    public ProductCreatedEvent() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    @Override
    public String toString() {
        return "ProductCreatedEvent{" +
                "title='" + title + '\'' +
                ", price=" + price +
                ", quantity=" + quantity +
                ", productId='" + productId + '\'' +
                '}';
    }
}
