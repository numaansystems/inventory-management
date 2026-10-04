package com.numaansystems.inventory.stock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    boolean existsByProductId(Long productId);

    Page<StockMovement> findByProductIdOrderByOccurredAtDescIdDesc(Long productId, Pageable pageable);
}
