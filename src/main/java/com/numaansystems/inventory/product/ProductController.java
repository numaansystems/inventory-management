package com.numaansystems.inventory.product;

import com.numaansystems.inventory.common.PageResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
class ProductController {

    private final ProductService service;

    ProductController(ProductService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse body = ProductResponse.from(service.create(request));
        return ResponseEntity.created(URI.create("/api/products/" + body.id())).body(body);
    }

    @GetMapping
    PageResponse<ProductResponse> list(
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 50, sort = "sku", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.of(service.list(active, pageable), ProductResponse::from);
    }

    @GetMapping("/{id}")
    ProductResponse get(@PathVariable long id) {
        return ProductResponse.from(service.get(id));
    }

    @PutMapping("/{id}")
    ProductResponse update(@PathVariable long id, @Valid @RequestBody ProductRequest request) {
        return ProductResponse.from(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable long id) {
        service.delete(id);
    }
}
