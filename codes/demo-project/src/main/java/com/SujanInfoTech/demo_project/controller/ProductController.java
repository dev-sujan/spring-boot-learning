package com.SujanInfoTech.demo_project.controller;

import com.SujanInfoTech.demo_project.model.Product;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/products")
public class ProductController {
    private List<Product> products = new ArrayList<>();

    private static AtomicLong idGenerator = new AtomicLong(0);

    public ProductController() {
        // initialize some products
        products.add(new Product(idGenerator.incrementAndGet(), "Red Pen", "This is red pen", 20.00, "Pen"));
        products.add(new Product(idGenerator.incrementAndGet(), "Black Pen", "This is red pen", 10.00, "Pen"));
        products.add(new Product(idGenerator.incrementAndGet(), "Blue Pen", "This is red pen", 15.00, "Pen"));
        products.add(new Product(idGenerator.incrementAndGet(), "Book", "This is a story book", 399.00, "Book"));
    }

    private Product findProductById(Long id) {
        return products.stream()
                .filter(product -> product.getId().equals(id))
                .findFirst()
                .orElseThrow(()->new RuntimeException("Product not found with id " + id));
    }

    // GET /products -> Get all products
    @GetMapping
    public List<Product> getAllProducts() {
        return products;
    }

    // Get /products -> Get specific product by ID
    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {
        // Todo:
        return findProductById(id);
    }

    // POST /products -> Create a new product
    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        product.setId(idGenerator.incrementAndGet());
        products.add(product);
        return product;
    }

    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable Long id, @RequestBody Product product) {
        // Todo:
        Product existingProduct = findProductById(id);
        existingProduct.setName(product.getName());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setCategory(product.getCategory());
        existingProduct.setDescription(product.getDescription());
        return existingProduct;
    }

    @DeleteMapping("/{id}")
    public String deleteProduct(@PathVariable Long id) {
        // Todo:
        Product productToDelete = this.getProductById(id);
        products.remove(productToDelete);
        return "Product with id " + id + " has been deleted";
    }
}
