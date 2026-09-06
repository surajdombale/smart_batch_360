package com.smartbatch360.api.batch;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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
     * Total m3 already produced against an order. COALESCE so an order with no
     * batches yet reports 0 rather than null.
     */
    @Query("SELECT COALESCE(SUM(b.producedQuantity), 0) FROM Batch b WHERE b.order.id = :orderId")
    BigDecimal sumProducedQuantityForOrder(@Param("orderId") Long orderId);
}
