package com.smartbatch360.api.batch;

import com.smartbatch360.api.batch.dto.BatchMaterialRequest;
import com.smartbatch360.api.batch.dto.BatchRequest;
import com.smartbatch360.api.batch.dto.BatchResponse;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchServiceTest {

    @Mock
    private BatchRepository batchRepository;
    @Mock
    private RecipeRepository recipeRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private SiteRepository siteRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private DriverRepository driverRepository;

    private BatchService service() {
        return new BatchService(batchRepository, recipeRepository, clientRepository, siteRepository,
                vehicleRepository, driverRepository);
    }

    private BatchRequest sampleRequest() {
        return new BatchRequest("250201", 1L, 2L, 3L, 4L, 5L,
                new BigDecimal("3.00"), BigDecimal.ZERO, null, 1, "Day",
                BatchStatus.PENDING, EquipmentStatus.STOPPED, EquipmentStatus.STOPPED,
                EquipmentStatus.STOPPED, EquipmentStatus.STOPPED, EquipmentStatus.STOPPED,
                List.of(new BatchMaterialRequest("OPC S3 Cement", new BigDecimal("960.00"),
                        new BigDecimal("960.00"), BigDecimal.ZERO, "kg")));
    }

    private void stubReferences() {
        Recipe recipe = new Recipe();
        recipe.setName("M25");
        Client client = new Client();
        client.setName("SmartBatch Solutions");
        Site site = new Site();
        site.setName("Kharadi");
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleNumber("MH12PQ3457");
        Driver driver = new Driver();
        driver.setName("Ganesh More");

        when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
        when(clientRepository.findById(2L)).thenReturn(Optional.of(client));
        when(siteRepository.findById(3L)).thenReturn(Optional.of(site));
        when(vehicleRepository.findById(4L)).thenReturn(Optional.of(vehicle));
        when(driverRepository.findById(5L)).thenReturn(Optional.of(driver));
    }

    @Test
    void createsBatchWithMaterials() {
        stubReferences();
        when(batchRepository.existsByBatchNumberIgnoreCase("250201")).thenReturn(false);
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));

        BatchResponse response = service().create(sampleRequest());

        assertThat(response.batchNumber()).isEqualTo("250201");
        assertThat(response.recipeName()).isEqualTo("M25");
        assertThat(response.clientName()).isEqualTo("SmartBatch Solutions");
        assertThat(response.remainingQuantity()).isEqualByComparingTo("3.00");
        assertThat(response.materials()).hasSize(1);
    }

    @Test
    void createRejectsDuplicateBatchNumber() {
        when(batchRepository.existsByBatchNumberIgnoreCase("250201")).thenReturn(true);

        assertThatThrownBy(() -> service().create(sampleRequest()))
                .isInstanceOf(DuplicateResourceException.class);

        verify(batchRepository, never()).save(any());
    }

    @Test
    void createRejectsUnknownRecipe() {
        when(batchRepository.existsByBatchNumberIgnoreCase("250201")).thenReturn(false);
        when(recipeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().create(sampleRequest()))
                .isInstanceOf(NotFoundException.class);
    }

    /** BatchResponse.from() dereferences every relation, so control-endpoint tests need a fully-linked batch. */
    private Batch batchWithRelations(BatchStatus status) {
        Batch batch = new Batch();
        batch.setStatus(status);
        batch.setTargetQuantity(new BigDecimal("3.00"));
        Recipe recipe = new Recipe();
        recipe.setName("M25");
        batch.setRecipe(recipe);
        Client client = new Client();
        client.setName("SmartBatch Solutions");
        batch.setClient(client);
        Site site = new Site();
        site.setName("Kharadi");
        batch.setSite(site);
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleNumber("MH12PQ3457");
        batch.setVehicle(vehicle);
        Driver driver = new Driver();
        driver.setName("Ganesh More");
        batch.setDriver(driver);
        return batch;
    }

    @Test
    void startMovesToInProgress() {
        Batch batch = batchWithRelations(BatchStatus.PENDING);
        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));

        BatchResponse response = service().start(1L);

        assertThat(response.status()).isEqualTo(BatchStatus.IN_PROGRESS);
    }

    @Test
    void emergencyStopStopsAllEquipment() {
        Batch batch = batchWithRelations(BatchStatus.IN_PROGRESS);
        batch.setMixerStatus(EquipmentStatus.RUNNING);
        batch.setConveyorStatus(EquipmentStatus.RUNNING);
        batch.setWaterValveStatus(EquipmentStatus.RUNNING);
        batch.setCementScrewStatus(EquipmentStatus.RUNNING);
        batch.setCompressorStatus(EquipmentStatus.RUNNING);
        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));

        BatchResponse response = service().emergencyStop(1L);

        assertThat(response.status()).isEqualTo(BatchStatus.STOPPED);
        assertThat(response.mixerStatus()).isEqualTo(EquipmentStatus.STOPPED);
        assertThat(response.conveyorStatus()).isEqualTo(EquipmentStatus.STOPPED);
        assertThat(response.waterValveStatus()).isEqualTo(EquipmentStatus.STOPPED);
        assertThat(response.cementScrewStatus()).isEqualTo(EquipmentStatus.STOPPED);
        assertThat(response.compressorStatus()).isEqualTo(EquipmentStatus.STOPPED);
    }

    @Test
    void remainingQuantityNeverGoesNegative() {
        stubReferences();
        when(batchRepository.existsByBatchNumberIgnoreCase("250201")).thenReturn(false);
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));

        BatchRequest overProduced = new BatchRequest("250201", 1L, 2L, 3L, 4L, 5L,
                new BigDecimal("3.00"), new BigDecimal("3.50"), null, 1, "Day",
                BatchStatus.IN_PROGRESS, EquipmentStatus.RUNNING, EquipmentStatus.RUNNING,
                EquipmentStatus.RUNNING, EquipmentStatus.RUNNING, EquipmentStatus.RUNNING,
                List.of(new BatchMaterialRequest("Cement", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, "kg")));

        BatchResponse response = service().create(overProduced);

        assertThat(response.remainingQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
