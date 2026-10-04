package com.numaansystems.inventory.stock;

import com.numaansystems.inventory.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class StockController {

    private final StockService service;

    StockController(StockService service) {
        this.service = service;
    }

    @GetMapping("/stock")
    PageResponse<StockLevelResponse> listLevels(
            @PageableDefault(size = 50, sort = "product.sku", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.of(service.listLevels(pageable), StockLevelResponse::from);
    }

    @GetMapping("/products/{productId}/stock")
    StockLevelResponse getLevel(@PathVariable long productId) {
        return StockLevelResponse.from(service.getLevel(productId));
    }

    @PostMapping("/products/{productId}/stock/movements")
    @ResponseStatus(HttpStatus.CREATED)
    StockMovementResponse record(@PathVariable long productId, @Valid @RequestBody StockMovementRequest request) {
        return StockMovementResponse.from(service.record(productId, request));
    }

    @GetMapping("/products/{productId}/stock/movements")
    PageResponse<StockMovementResponse> listMovements(
            @PathVariable long productId, @PageableDefault(size = 50) Pageable pageable) {
        return PageResponse.of(service.listMovements(productId, pageable), StockMovementResponse::from);
    }
}
