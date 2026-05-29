package com.javarush.productservice.controller;

import com.javarush.productservice.model.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
public class ProductController {

    private static final List<Product> PRODUCTS = List.of(
            new Product(1L, "Keyboard", 49.90),
            new Product(2L, "Mouse", 19.90),
            new Product(3L, "Monitor", 219.00)
    );

    @Value("${server.port}")
    private int port;

    @GetMapping
    public Map<String, Object> getAll(@RequestHeader(value = "X-Request-Source", required = false) String source) {
        return Map.of(
                "servedByPort", port,
                "requestSource", source == null ? "direct" : source,
                "products", PRODUCTS
        );
    }

    @GetMapping("/{id}")
    public Product getOne(@PathVariable Long id) {
        return PRODUCTS.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
    }
}
