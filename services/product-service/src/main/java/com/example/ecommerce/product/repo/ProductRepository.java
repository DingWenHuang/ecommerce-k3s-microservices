package com.example.ecommerce.product.repo;

import com.example.ecommerce.product.domain.ProductEntity;
import com.example.ecommerce.product.domain.ProductType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商品 Repository：
 * - reserveStockAtomic：用「單句 SQL」原子扣庫存，避免超賣
 */
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    /**
     * 原子扣庫存（最關鍵展示點）
     * UPDATE product SET stock = stock - :qty WHERE id=:id AND stock >= :qty
     * - 回傳值：受影響筆數（1=成功；0=庫存不足或商品不存在）
     */
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE product
            SET stock = stock - :qty
            WHERE id = :id
              AND stock >= :qty
            """, nativeQuery = true)
    int reserveStockAtomic(@Param("id") long id, @Param("qty") int qty);

    /**
     * 補貨（原子加庫存）
     */
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE product
            SET stock = stock + :qty
            WHERE id = :id
            """, nativeQuery = true)
    int restockAtomic(@Param("id") long id, @Param("qty") int qty);

    List<ProductEntity> findByProductTypeOrderByIdAsc(ProductType productType);
}
