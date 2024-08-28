package fr.slux.kafka.ws.productsmicroservice.rest;

import fr.slux.kafka.ws.productsmicroservice.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class ProductController {
    private static final Logger LOG = LoggerFactory.getLogger(ProductController.class);

    private ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/products")
    public ResponseEntity<?> createProduct(@RequestBody CreateProductRestModel model) {
        String productId = null;
        try {
            productId = this.productService.createProduct(model);
        } catch (Exception e) {
            LOG.warn("Error while calling /products", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage(LocalDateTime.now(), "while calling /products -> " + e.getMessage(),
                            "Exception Class: " + e.getClass()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(productId);
    }
}
