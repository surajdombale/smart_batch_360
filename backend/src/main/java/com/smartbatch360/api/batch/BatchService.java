package com.smartbatch360.api.batch;

import com.smartbatch360.api.batch.dto.BatchMaterialRequest;
import com.smartbatch360.api.batch.dto.BatchPageResponse;
import com.smartbatch360.api.batch.dto.BatchRequest;
import com.smartbatch360.api.batch.dto.BatchResponse;
import com.smartbatch360.api.batch.dto.BatchSearchCriteria;
import com.smartbatch360.api.client.Client;
import com.smartbatch360.api.client.ClientRepository;
import com.smartbatch360.api.common.DuplicateResourceException;
import com.smartbatch360.api.common.NotFoundException;
import com.smartbatch360.api.driver.Driver;
import com.smartbatch360.api.driver.DriverRepository;
import com.smartbatch360.api.recipe.Recipe;
import com.smartbatch360.api.recipe.RecipeRepository;
import com.smartbatch360.api.site.Site;
import com.smartbatch360.api.site.SiteRepository;
import com.smartbatch360.api.vehicle.Vehicle;
import com.smartbatch360.api.vehicle.VehicleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Batch controls (start/pause/resume/stop/emergencyStop) are deliberately
 * permissive about which prior status they accept - this is a manual/
 * simulated state, not a rigorously enforced PLC-driven state machine
 * (docs/06_SCOPE_AND_ROADMAP.md). The desktop UI enables/disables the
 * relevant buttons based on current status for a sane operator experience,
 * but the API itself does not reject an out-of-order transition.
 */
@Service
@Transactional
public class BatchService {

    private final BatchRepository batchRepository;
    private final RecipeRepository recipeRepository;
    private final ClientRepository clientRepository;
    private final SiteRepository siteRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    public BatchService(BatchRepository batchRepository, RecipeRepository recipeRepository,
                         ClientRepository clientRepository, SiteRepository siteRepository,
                         VehicleRepository vehicleRepository, DriverRepository driverRepository) {
        this.batchRepository = batchRepository;
        this.recipeRepository = recipeRepository;
        this.clientRepository = clientRepository;
        this.siteRepository = siteRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
    }

    @Transactional(readOnly = true)
    public List<BatchResponse> findAll() {
        return batchRepository.findAll().stream()
                .map(BatchResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BatchResponse findById(Long id) {
        return BatchResponse.from(getOrThrow(id));
    }

    /** Batch Reports: filtered, paginated, sortable history search (docs/02_UI_REFERENCE.md). */
    @Transactional(readOnly = true)
    public BatchPageResponse search(BatchSearchCriteria criteria, Pageable pageable) {
        Page<Batch> page = batchRepository.findAll(BatchSpecifications.matching(criteria), pageable);
        return BatchPageResponse.from(page);
    }

    public BatchResponse create(BatchRequest request) {
        String batchNumber = request.batchNumber().trim();
        if (batchRepository.existsByBatchNumberIgnoreCase(batchNumber)) {
            throw new DuplicateResourceException("A batch with number '" + batchNumber + "' already exists.");
        }
        Batch batch = new Batch();
        applyRequest(batch, request, batchNumber);
        return BatchResponse.from(batchRepository.save(batch));
    }

    public BatchResponse update(Long id, BatchRequest request) {
        Batch batch = getOrThrow(id);
        String batchNumber = request.batchNumber().trim();
        if (batchRepository.existsByBatchNumberIgnoreCaseAndIdNot(batchNumber, id)) {
            throw new DuplicateResourceException("A batch with number '" + batchNumber + "' already exists.");
        }
        applyRequest(batch, request, batchNumber);
        return BatchResponse.from(batchRepository.save(batch));
    }

    public void delete(Long id) {
        Batch batch = getOrThrow(id);
        batchRepository.delete(batch);
    }

    public BatchResponse start(Long id) {
        return applyStatus(id, BatchStatus.IN_PROGRESS);
    }

    public BatchResponse pause(Long id) {
        return applyStatus(id, BatchStatus.PAUSED);
    }

    public BatchResponse resume(Long id) {
        return applyStatus(id, BatchStatus.IN_PROGRESS);
    }

    public BatchResponse stop(Long id) {
        return applyStatus(id, BatchStatus.STOPPED);
    }

    /** Also drops every equipment status to STOPPED, unlike a plain stop() - this is the "kill everything now" control. */
    public BatchResponse emergencyStop(Long id) {
        Batch batch = getOrThrow(id);
        batch.setStatus(BatchStatus.STOPPED);
        batch.setMixerStatus(EquipmentStatus.STOPPED);
        batch.setConveyorStatus(EquipmentStatus.STOPPED);
        batch.setWaterValveStatus(EquipmentStatus.STOPPED);
        batch.setCementScrewStatus(EquipmentStatus.STOPPED);
        batch.setCompressorStatus(EquipmentStatus.STOPPED);
        return BatchResponse.from(batchRepository.save(batch));
    }

    public BatchResponse complete(Long id) {
        return applyStatus(id, BatchStatus.COMPLETED);
    }

    private BatchResponse applyStatus(Long id, BatchStatus status) {
        Batch batch = getOrThrow(id);
        batch.setStatus(status);
        return BatchResponse.from(batchRepository.save(batch));
    }

    Batch getOrThrow(Long id) {
        return batchRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forId("Batch", id));
    }

    private void applyRequest(Batch batch, BatchRequest request, String batchNumber) {
        Recipe recipe = recipeRepository.findById(request.recipeId())
                .orElseThrow(() -> NotFoundException.forId("Recipe", request.recipeId()));
        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> NotFoundException.forId("Client", request.clientId()));
        Site site = siteRepository.findById(request.siteId())
                .orElseThrow(() -> NotFoundException.forId("Site", request.siteId()));
        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> NotFoundException.forId("Vehicle", request.vehicleId()));
        Driver driver = driverRepository.findById(request.driverId())
                .orElseThrow(() -> NotFoundException.forId("Driver", request.driverId()));

        batch.setBatchNumber(batchNumber);
        batch.setRecipe(recipe);
        batch.setClient(client);
        batch.setSite(site);
        batch.setVehicle(vehicle);
        batch.setDriver(driver);
        batch.setTargetQuantity(request.targetQuantity());
        batch.setProducedQuantity(request.producedQuantity());
        batch.setCycleDateTime(request.cycleDateTime() != null ? request.cycleDateTime() : Instant.now());
        batch.setCycleNumber(request.cycleNumber());
        batch.setShift(request.shift() != null && !request.shift().isBlank() ? request.shift().trim() : null);
        batch.setStatus(request.status());
        batch.setMixerStatus(request.mixerStatus());
        batch.setConveyorStatus(request.conveyorStatus());
        batch.setWaterValveStatus(request.waterValveStatus());
        batch.setCementScrewStatus(request.cementScrewStatus());
        batch.setCompressorStatus(request.compressorStatus());

        batch.getMaterials().clear();
        int order = 0;
        for (BatchMaterialRequest materialRequest : request.materials()) {
            BatchMaterial material = new BatchMaterial();
            material.setBatch(batch);
            material.setMaterialName(materialRequest.materialName().trim());
            material.setTarget(materialRequest.target());
            material.setSetpoint(materialRequest.setpoint());
            material.setAchieved(materialRequest.achieved());
            material.setUnit(materialRequest.unit().trim());
            material.setDisplayOrder(order++);
            batch.getMaterials().add(material);
        }
    }
}
