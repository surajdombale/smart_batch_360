package com.smartbatch360.api.batch;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.math.BigDecimal;
import java.util.List;

public interface BatchRepository extends JpaRepository<Batch, Long>, JpaSpecificationExecutor<Batch> {

    boolean existsByBatchNumberIgnoreCase(String batchNumber);

    boolean existsByBatchNumberIgnoreCaseAndIdNot(String batchNumber, Long id);

    boolean existsByRecipeId(Long recipeId);

    boolean existsByClientId(Long clientId);

    boolean existsBySiteId(Long siteId);

    boolean existsByVehicleId(Long vehicleId);

    boolean existsByDriverId(Long driverId);

    boolean existsByOrderId(Long orderId);

    /**
     * Total kg already produced against an order. COALESCE so an order with no
     * batches yet reports 0 rather than null.
     */
    @Query("SELECT COALESCE(SUM(b.producedQuantity), 0) FROM Batch b WHERE b.order.id = :orderId")
    BigDecimal sumProducedQuantityForOrder(@Param("orderId") Long orderId);

    /**
     * The same sum for every order at once. The order list needs one number per
     * order, and asking per order cost one query each - 2,002 queries for 2,002
     * orders. Orders with no batches yet are simply absent; the caller treats a
     * missing entry as zero.
     */
    @Query("SELECT new com.smartbatch360.api.batch.OrderProducedQuantity("
            + "b.order.id, COALESCE(SUM(b.producedQuantity), 0)) "
            + "FROM Batch b WHERE b.order IS NOT NULL GROUP BY b.order.id")
    List<OrderProducedQuantity> sumProducedQuantityByOrder();
}
