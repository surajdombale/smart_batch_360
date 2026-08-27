package com.smartbatch360.api.batch;

import com.smartbatch360.api.batch.dto.BatchSearchCriteria;
import com.smartbatch360.api.client.Client;
import com.smartbatch360.api.client.ClientStatus;
import com.smartbatch360.api.driver.Driver;
import com.smartbatch360.api.driver.DriverStatus;
import com.smartbatch360.api.recipe.Recipe;
import com.smartbatch360.api.recipe.RecipeStatus;
import com.smartbatch360.api.site.Site;
import com.smartbatch360.api.site.SiteStatus;
import com.smartbatch360.api.vehicle.Vehicle;
import com.smartbatch360.api.vehicle.VehicleStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real (in-memory H2) query test for Batch Reports filtering - a mocked
 * BatchRepository can't meaningfully verify a Specification actually
 * narrows results correctly, so this exercises it against a real JPA layer
 * instead, same approach as SiteRepositoryTest.
 */
@DataJpaTest
class BatchSpecificationsTest {

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private Long clientAId;
    private Long clientBId;

    private void seed() {
        Client clientA = client("Client A");
        Client clientB = client("Client B");
        entityManager.persist(clientA);
        entityManager.persist(clientB);
        clientAId = clientA.getId();
        clientBId = clientB.getId();

        Site site = site("Kharadi", clientA);
        entityManager.persist(site);
        Driver driver = driver("Ganesh More", "MH12 2019 000001");
        entityManager.persist(driver);
        Vehicle vehicle = vehicle("MH12PQ0001", driver);
        entityManager.persist(vehicle);
        Recipe recipe = recipe("M25");
        entityManager.persist(recipe);

        persistBatch("250100", clientA, site, vehicle, driver, recipe, LocalDate.of(2026, 8, 1));
        persistBatch("250200", clientA, site, vehicle, driver, recipe, LocalDate.of(2026, 8, 15));
        persistBatch("250300", clientB, site, vehicle, driver, recipe, LocalDate.of(2026, 8, 20));
        entityManager.flush();
    }

    @Test
    void filtersByBatchNumberRange() {
        seed();
        BatchSearchCriteria criteria = new BatchSearchCriteria("250150", "250250", null, null, null, null, null, null, null);

        Page<Batch> result = batchRepository.findAll(BatchSpecifications.matching(criteria), Pageable.unpaged());

        assertThat(result.getContent()).extracting(Batch::getBatchNumber).containsExactly("250200");
    }

    @Test
    void filtersByDateRange() {
        seed();
        BatchSearchCriteria criteria = new BatchSearchCriteria(null, null,
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 16), null, null, null, null, null);

        Page<Batch> result = batchRepository.findAll(BatchSpecifications.matching(criteria), Pageable.unpaged());

        assertThat(result.getContent()).extracting(Batch::getBatchNumber).containsExactly("250200");
    }

    @Test
    void filtersByClient() {
        seed();
        BatchSearchCriteria criteria = new BatchSearchCriteria(null, null, null, null, clientBId, null, null, null, null);

        Page<Batch> result = batchRepository.findAll(BatchSpecifications.matching(criteria), Pageable.unpaged());

        assertThat(result.getContent()).extracting(Batch::getBatchNumber).containsExactly("250300");
    }

    @Test
    void noFiltersReturnsEverything() {
        seed();
        BatchSearchCriteria criteria = new BatchSearchCriteria(null, null, null, null, null, null, null, null, null);

        Page<Batch> result = batchRepository.findAll(BatchSpecifications.matching(criteria), Pageable.unpaged());

        assertThat(result.getContent()).hasSize(3);
    }

    private void persistBatch(String number, Client client, Site site, Vehicle vehicle, Driver driver,
                               Recipe recipe, LocalDate date) {
        Batch batch = new Batch();
        batch.setBatchNumber(number);
        batch.setClient(client);
        batch.setSite(site);
        batch.setVehicle(vehicle);
        batch.setDriver(driver);
        batch.setRecipe(recipe);
        batch.setTargetQuantity(new BigDecimal("3.00"));
        batch.setProducedQuantity(BigDecimal.ZERO);
        batch.setStatus(BatchStatus.PENDING);
        batch.setCycleDateTime(date.atStartOfDay(ZoneOffset.UTC).toInstant().plusSeconds(3600));
        BatchMaterial material = new BatchMaterial();
        material.setBatch(batch);
        material.setMaterialName("Cement");
        material.setTarget(BigDecimal.TEN);
        material.setSetpoint(BigDecimal.TEN);
        material.setAchieved(BigDecimal.ZERO);
        material.setUnit("kg");
        batch.getMaterials().add(material);
        entityManager.persist(batch);
    }

    private Client client(String name) {
        Client c = new Client();
        c.setName(name);
        c.setContactPerson("Contact");
        c.setPhone("9000000000");
        c.setStatus(ClientStatus.ACTIVE);
        return c;
    }

    private Site site(String name, Client client) {
        Site s = new Site();
        s.setName(name);
        s.setClient(client);
        s.setLocation("Pune");
        s.setStatus(SiteStatus.ACTIVE);
        return s;
    }

    private Driver driver(String name, String licenseNo) {
        Driver d = new Driver();
        d.setName(name);
        d.setPhone("9000000001");
        d.setLicenseNo(licenseNo);
        d.setStatus(DriverStatus.ACTIVE);
        return d;
    }

    private Vehicle vehicle(String number, Driver driver) {
        Vehicle v = new Vehicle();
        v.setVehicleNumber(number);
        v.setDriver(driver);
        v.setCapacityCubicMeters(new BigDecimal("6.00"));
        v.setStatus(VehicleStatus.AVAILABLE);
        return v;
    }

    private Recipe recipe(String name) {
        Recipe r = new Recipe();
        r.setName(name);
        r.setTotalBatchQuantityM3(new BigDecimal("3.00"));
        r.setStatus(RecipeStatus.ACTIVE);
        return r;
    }
}
