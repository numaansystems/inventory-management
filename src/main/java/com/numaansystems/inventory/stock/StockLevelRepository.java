package com.numaansystems.inventory.stock;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface StockLevelRepository extends JpaRepository<StockLevel, Long> {

    /** Loads the stock level holding a row lock until the transaction ends, serialising concurrent movements. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StockLevel s where s.productId = :productId")
    Optional<StockLevel> findForUpdate(Long productId);

    @Query(value = "select s from StockLevel s join fetch s.product",
            countQuery = "select count(s) from StockLevel s")
    Page<StockLevel> findAllWithProduct(Pageable pageable);
}
