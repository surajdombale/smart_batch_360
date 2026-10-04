package com.smartbatch360.api.batch;

import com.smartbatch360.api.batch.dto.BatchCycleRequest;
import com.smartbatch360.api.batch.dto.BatchCycleResponse;
import com.smartbatch360.api.client.Client;
import com.smartbatch360.api.client.ClientStatus;
import com.smartbatch360.api.common.InvalidRequestException;
import com.smartbatch360.api.common.NotFoundException;
import com.smartbatch360.api.driver.Driver;
import com.smartbatch360.api.driver.DriverStatus;
import com.smartbatch360.api.recipe.Recipe;
import com.smartbatch360.api.recipe.RecipeStatus;
import com.smartbatch360.api.site.Site;
import com.smartbatch360.api.site.SiteStatus;
import com.smartbatch360.api.vehicle.Vehicle;
import com.smartbatch360.api.vehicle.VehicleStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Recording what the PLC reports. Real H2 rather than mocks, because the rules
 * being checked - one record per cycle, one row per material in it - are
 * enforced by the database as well as the service, and a mock would prove
 * neither.
 */
@DataJpaTest
@Import(BatchCycleService.class)
class BatchCycleServiceTest {

    @Autowired private BatchCycleService service;
    @Autowired private BatchCycleRepository batchCycleRepository;
    @Autowired private EntityManager entityManager;

    private Batch batch;

    @BeforeEach
    void seedBatch() {
        Client client = new Client();
        client.setName("Client A");
        client.setContactPerson("Contact");
        client.setPhone("9000000000");
        client.setStatus(ClientStatus.ACTIVE);
        entityManager.persist(client);

        Site site = new Site();
        site.setName("Kharadi");
        site.setClient(client);
        site.setLocation("Pune");
        site.setStatus(SiteStatus.ACTIVE);
        entityManager.persist(site);

        Driver driver = new Driver();
        driver.setName("Ganesh More");
        driver.setPhone("9000000001");
        driver.setLicenseNo("MH12 2019 000001");
        driver.setStatus(DriverStatus.ACTIVE);
        entityManager.persist(driver);

        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleNumber("MH12PQ0001");
        vehicle.setDriver(driver);
        vehicle.setCapacityCubicMeters(new BigDecimal("6.00"));
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        entityManager.persist(vehicle);

        Recipe recipe = new Recipe();
        recipe.setName("M25");
        recipe.setTotalBatchQuantityKg(new BigDecimal("1200.00"));
        recipe.setStatus(RecipeStatus.ACTIVE);
        entityManager.persist(recipe);

        batch = new Batch();
        batch.setBatchNumber("250401");
        batch.setClient(client);
        batch.setSite(site);
        batch.setVehicle(vehicle);
        batch.setDriver(driver);
        batch.setRecipe(recipe);
        batch.setTargetQuantity(new BigDecimal("100.00"));
        batch.setProducedQuantity(BigDecimal.ZERO);
        batch.setStatus(BatchStatus.IN_PROGRESS);
        batch.setCycleDateTime(Instant.parse("2026-10-04T12:30:00Z"));
        entityManager.persist(batch);
        entityManager.flush();
    }

    private BatchCycleRequest cycle(int number, String cement, String water) {
        return new BatchCycleRequest("250401", number, Instant.parse("2026-10-04T12:31:00Z"),
                List.of(new BatchCycleRequest.MaterialAchieved("Cement", new BigDecimal(cement)),
                        new BatchCycleRequest.MaterialAchieved("Water", new BigDecimal(water))));
    }

    @Test
    void recordsACycleWithEveryMaterialItReports() {
        BatchCycleResponse response = service.record(cycle(1, "101.00", "151.00"));

        assertThat(response.cycleNumber()).isEqualTo(1);
        assertThat(response.batchNumber()).isEqualTo("250401");
        assertThat(response.materials()).extracting(m -> m.materialName())
                .containsExactly("Cement", "Water");
        // The report's row total.
        assertThat(response.totalAchieved()).isEqualByComparingTo("252.00");
    }

    /**
     * The one that matters for an integration: a dropped acknowledgement makes
     * a PLC send the same cycle again. Counting it twice would overstate what
     * the plant produced, so a resend corrects the cycle instead.
     */
    @Test
    void resendingACycleCorrectsItRatherThanDoublingIt() {
        service.record(cycle(1, "101.00", "151.00"));
        service.record(cycle(1, "102.00", "150.00"));
        entityManager.flush();

        List<BatchCycleResponse> cycles = service.findForBatch(batch.getId());

        assertThat(cycles).hasSize(1);
        assertThat(cycles.get(0).materials()).extracting(m -> m.achieved())
                .containsExactly(new BigDecimal("102.00"), new BigDecimal("150.00"));
        assertThat(service.totalAchieved(batch.getId())).isEqualByComparingTo("252.00");
    }

    @Test
    void cyclesComeBackInOrderWithTheirTotals() {
        service.record(cycle(2, "102.00", "150.00"));
        service.record(cycle(1, "101.00", "151.00"));
        service.record(cycle(3, "100.00", "149.00"));
        entityManager.flush();

        List<BatchCycleResponse> cycles = service.findForBatch(batch.getId());

        assertThat(cycles).extracting(BatchCycleResponse::cycleNumber).containsExactly(1, 2, 3);
        // The report's "Achieved total" - 303 of cement and 450 of water.
        assertThat(service.totalAchieved(batch.getId())).isEqualByComparingTo("753.00");
    }

    @Test
    void aCycleForAnUnknownBatchIsRefused() {
        BatchCycleRequest unknown = new BatchCycleRequest("NOPE", 1, Instant.now(),
                List.of(new BatchCycleRequest.MaterialAchieved("Cement", new BigDecimal("1.00"))));

        assertThatThrownBy(() -> service.record(unknown))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("No batch numbered 'NOPE'");
    }

    @Test
    void thesameMaterialTwiceInOneCycleIsRefused() {
        BatchCycleRequest duplicated = new BatchCycleRequest("250401", 1, Instant.now(),
                List.of(new BatchCycleRequest.MaterialAchieved("Cement", new BigDecimal("1.00")),
                        new BatchCycleRequest.MaterialAchieved("cement", new BigDecimal("2.00"))));

        assertThatThrownBy(() -> service.record(duplicated))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("reported more than once");
    }

    /** The PLC knows the number, not our id, and need not match its case. */
    @Test
    void theBatchNumberIsMatchedWithoutCase() {
        service.record(new BatchCycleRequest("250401", 1, Instant.now(),
                List.of(new BatchCycleRequest.MaterialAchieved("Cement", new BigDecimal("1.00")))));

        assertThat(batchCycleRepository.findByBatchIdAndCycleNumber(batch.getId(), 1)).isPresent();
    }
}
