package com.smartbatch360.api.batch;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BatchRepository extends JpaRepository<Batch, Long>, JpaSpecificationExecutor<Batch> {

    boolean existsByBatchNumberIgnoreCase(String batchNumber);

    /**
     * The batch the plant is on, if any. The plant runs one batch from start to
     * finish before loading the next, so this is what Production checks before
     * letting another be started.
     */
    Optional<Batch> findFirstByStatusIn(List<BatchStatus> statuses);

    /** Every batch in these states, oldest first - Production's running batch. */
    List<Batch> findByStatusInOrderByCycleDateTimeAsc(List<BatchStatus> statuses);

    /** The PLC knows a batch by its number, not by our id. */
    Optional<Batch> findByBatchNumberIgnoreCase(String batchNumber);

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
     * What an order's finished batches delivered: a completed batch at what it
     * was planned to deliver, a stopped one at what it weighed before it was
     * stopped. Running batches have delivered nothing yet.
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN b.status = com.smartbatch360.api.batch.BatchStatus.COMPLETED "
            + "THEN b.targetQuantity ELSE b.producedQuantity END), 0) FROM Batch b WHERE b.order.id = :orderId "
            + "AND b.status IN (com.smartbatch360.api.batch.BatchStatus.COMPLETED, "
            + "com.smartbatch360.api.batch.BatchStatus.STOPPED)")
    BigDecimal sumDeliveredForOrder(@Param("orderId") Long orderId);

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

    /** The dashboard's headline figures for batches made in [from, to). */
    @Query("SELECT new com.smartbatch360.api.batch.BatchTotals("
            + "COUNT(b), COALESCE(SUM(b.producedQuantity), 0), COUNT(DISTINCT b.vehicle.id)) "
            + "FROM Batch b WHERE b.cycleDateTime >= :from AND b.cycleDateTime < :to")
    BatchTotals totalsBetween(@Param("from") Instant from, @Param("to") Instant to);

    /**
     * When each batch in [from, to) was made, for the dashboard's trend. The
     * bucketing into plant hours or days happens in Java through ReportingZone:
     * done in SQL it would follow the database session's zone instead.
     */
    @Query("SELECT b.cycleDateTime FROM Batch b WHERE b.cycleDateTime >= :from AND b.cycleDateTime < :to")
    List<Instant> findCycleDateTimesBetween(@Param("from") Instant from, @Param("to") Instant to);

    /** The newest batches made in [from, to), as many as the page asks for. */
    @Query("SELECT new com.smartbatch360.api.batch.RecentBatchRow("
            + "b.id, b.batchNumber, r.name, v.vehicleNumber, b.producedQuantity, b.cycleDateTime, b.status) "
            + "FROM Batch b JOIN b.recipe r JOIN b.vehicle v "
            + "WHERE b.cycleDateTime >= :from AND b.cycleDateTime < :to "
            + "ORDER BY b.cycleDateTime DESC, b.id DESC")
    List<RecentBatchRow> findRecentBetween(@Param("from") Instant from, @Param("to") Instant to, Pageable pageable);
}
