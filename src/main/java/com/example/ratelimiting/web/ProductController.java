package com.example.ratelimiting.web;

import com.example.ratelimiting.config.RateLimitingProperties;
import com.example.ratelimiting.ratelimit.annotation.RateLimitBypass;
import com.example.ratelimiting.ratelimit.annotation.RateLimited;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@RateLimited(requestsPerMinute = 100, burstCapacity = 20, dimension = RateLimitingProperties.Dimension.USER)
public class ProductController {
    private final List<Product> products = new ArrayList<>(List.of(
            new Product("prod-123", "Product A", 29.99),
            new Product("prod-456", "Product B", 49.99)
    ));

    @GetMapping
    public List<Product> getProducts() {
        return List.copyOf(products);
    }

    @PostMapping
    @RateLimited(requestsPerMinute = 10, burstCapacity = 5, dimension = RateLimitingProperties.Dimension.USER)
    public Product createProduct(@Valid @RequestBody CreateProductRequest req) {
        Product p = new Product("prod-" + UUID.randomUUID(), req.name(), req.price());
        products.add(p);
        return p;
    }

    @GetMapping("/admin")
    @RateLimitBypass
    @PreAuthorize("hasRole('ADMIN')")
    public List<Product> getAdminProducts() {
        return List.copyOf(products);
    }

    public record Product(String id, String name, double price) {
    }

    public record CreateProductRequest(@NotBlank String name, double price) {
    }
}

