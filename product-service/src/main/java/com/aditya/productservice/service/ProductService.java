package com.aditya.productservice.service;

import com.aditya.common.dto.ProductResponse;
import com.aditya.productservice.entity.Product;
import com.aditya.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    @Cacheable(value = "products", key = "#p0")
    public ProductResponse getProductById(Long id) {

        System.out.println("Fetching product from DB");

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .stock(product.getStock())
                .build();
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // if you don't provide parameter spring automatically uses all the parameters.
    @CacheEvict(value = "products", key = "#p0")
    public ProductResponse updateProduct(Long id, Product productRequest) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setName(productRequest.getName());
        product.setPrice(productRequest.getPrice());
        product.setStock(productRequest.getStock());

        productRepository.save(product);

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .stock(product.getStock())
                .build();
    }
}
