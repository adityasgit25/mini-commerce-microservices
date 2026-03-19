package com.aditya.productservice.controller;

import com.aditya.common.dto.ProductResponse;
import com.aditya.productservice.entity.Product;
import com.aditya.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        return productService.createProduct(product);
    }

    @GetMapping("/{id}")
    public ProductResponse getProduct(@PathVariable("id") Long id) {
        return productService.getProductById(id);
    }

    @GetMapping
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(
            @PathVariable("id") Long id,
            @RequestBody Product productRequest
    ) {
        return productService.updateProduct(id, productRequest);
    }
}
