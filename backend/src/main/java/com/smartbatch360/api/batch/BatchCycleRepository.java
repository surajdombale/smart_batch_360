package com.smartbatch360.api.batch;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BatchCycleRepository extends JpaRepository<BatchCycle, Long> {

    Optional<BatchCycle> findByBatchIdAndCycleNumber(Long batchId, Integer cycleNumber);

    /**
     * Every cycle of a batch in order, with its materials - the batch report
     * reads the whole grid at once, so this fetches the collection rather than
     * leaving it to be loaded a cycle at a time.
     */
    @Query("SELECT DISTINCT c FROM BatchCycle c LEFT JOIN FETCH c.materials "
            + "WHERE c.batch.id = :batchId ORDER BY c.cycleNumber")
    List<BatchCycle> findForReport(Long batchId);
}
