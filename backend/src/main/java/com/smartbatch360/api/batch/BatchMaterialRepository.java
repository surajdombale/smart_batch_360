package com.smartbatch360.api.batch;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Backs Material Consumption (com.smartbatch360.api.materialconsumption) -
 * the rows it aggregates by material name and day/week/month. Batch is joined
 * because every row's cycleDateTime is needed for the date filter and the period
 * bucketing; the query projects the five fields the aggregation reads rather than
 * returning entities, since it can cover the plant's whole history.
 */
public interface BatchMaterialRepository extends JpaRepository<BatchMaterial, Long> {

    @Query("SELECT new com.smartbatch360.api.batch.MaterialConsumptionRow("
            + "bm.materialName, bm.unit, bm.target, bm.achieved, b.cycleDateTime) "
            + "FROM BatchMaterial bm JOIN bm.batch b "
            + "WHERE (:from IS NULL OR b.cycleDateTime >= :from) "
            + "AND (:to IS NULL OR b.cycleDateTime < :to) "
            + "AND (:materialName IS NULL OR LOWER(bm.materialName) LIKE LOWER(CONCAT('%', :materialName, '%')))")
    List<MaterialConsumptionRow> findForConsumption(@Param("from") Instant from, @Param("to") Instant to,
                                                     @Param("materialName") String materialName);
}
