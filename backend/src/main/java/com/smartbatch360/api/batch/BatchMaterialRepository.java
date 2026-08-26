package com.smartbatch360.api.batch;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Backs Material Consumption (com.smartbatch360.api.materialconsumption) -
 * the raw rows it aggregates by material name and day/week/month. Batch is
 * join-fetched since every row's Batch.cycleDateTime is needed for the date
 * filter and period bucketing, avoiding an N+1 per row.
 */
public interface BatchMaterialRepository extends JpaRepository<BatchMaterial, Long> {

    @Query("SELECT bm FROM BatchMaterial bm JOIN FETCH bm.batch b "
            + "WHERE (:from IS NULL OR b.cycleDateTime >= :from) "
            + "AND (:to IS NULL OR b.cycleDateTime < :to) "
            + "AND (:materialName IS NULL OR LOWER(bm.materialName) LIKE LOWER(CONCAT('%', :materialName, '%')))")
    List<BatchMaterial> findForConsumption(@Param("from") Instant from, @Param("to") Instant to,
                                            @Param("materialName") String materialName);
}
