package fr.slux.kafka.ws.productsmicroservice.service;

import fr.slux.kafka.ws.productsmicroservice.rest.CreateProductRestModel;

public interface ProductService {
    String createProduct(CreateProductRestModel model) throws Exception;
}
