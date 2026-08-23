package com.smartbatch360.api.batch;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BatchRepository extends JpaRepository<Batch, Long>, JpaSpecificationExecutor<Batch> {

    boolean existsByBatchNumberIgnoreCase(String batchNumber);

    boolean existsByBatchNumberIgnoreCaseAndIdNot(String batchNumber, Long id);

    boolean existsByRecipeId(Long recipeId);

    boolean existsByClientId(Long clientId);

    boolean existsBySiteId(Long siteId);

    boolean existsByVehicleId(Long vehicleId);

    boolean existsByDriverId(Long driverId);
}
