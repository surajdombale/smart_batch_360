package com.smartbatch360.api.batch;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchRepository extends JpaRepository<Batch, Long> {

    boolean existsByBatchNumberIgnoreCase(String batchNumber);

    boolean existsByBatchNumberIgnoreCaseAndIdNot(String batchNumber, Long id);

    boolean existsByRecipeId(Long recipeId);

    boolean existsByClientId(Long clientId);

    boolean existsBySiteId(Long siteId);

    boolean existsByVehicleId(Long vehicleId);

    boolean existsByDriverId(Long driverId);
}
