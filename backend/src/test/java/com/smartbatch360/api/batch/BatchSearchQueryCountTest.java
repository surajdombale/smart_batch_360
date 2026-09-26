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
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards hibernate.default_batch_fetch_size, which is a line of configuration
 * with nothing else holding it in place.
 *
 * A batch row shows six names and its list of materials, every one a lazy
 * association, so reading a page used to cost a query per row: 109 queries to
 * open a page of 20 against the real database, and 2,413 for the 2,000 rows an
 * export re-fetches. Batch fetching loads them 100 owners at a time instead.
 * If the setting is dropped, the query count starts tracking the row count
 * again and this test fails.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class BatchSearchQueryCountTest {

    @Autowired private BatchRepository batchRepository;
    @Autowired private EntityManager entityManager;

    private Client client;
    private Site site;
    private Recipe recipe;
    private Vehicle vehicle;
    private Driver driver;
    private int batchNumber = 250000;

    @Test
    void readingAPageCostsTheSameWhateverTheRowCount() {
        seedReferenceData();
        persistBatches(5);
        long forFive = queriesToReadPage(PageRequest.of(0, 50));

        persistBatches(45);
        long forFifty = queriesToReadPage(PageRequest.of(0, 50));

        assertThat(batchRepository.count()).isEqualTo(50);
        // Ten times the rows may cost one extra query - Spring Data skips the
        // count query when the first page is not full - but not ten times the
        // queries. Without batch fetching this reads 57 against 11.
        assertThat(forFifty).isLessThanOrEqualTo(forFive + 1);
    }

    /**
     * Reading the response fields is what triggers the lazy loads, so the count
     * is taken around the same work the controller does - mapping every row,
     * materials included.
     */
    private long queriesToReadPage(Pageable pageable) {
        Statistics statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();
        entityManager.clear();
        long before = statistics.getPrepareStatementCount();
        batchRepository.findAll(BatchSpecifications.matching(noFilters()), pageable)
                .getContent()
                .forEach(batch -> {
                    batch.getRecipe().getName();
                    batch.getClient().getName();
                    batch.getSite().getName();
                    batch.getVehicle().getVehicleNumber();
                    batch.getDriver().getName();
                    batch.getMaterials().size();
                });
        return statistics.getPrepareStatementCount() - before;
    }

    private BatchSearchCriteria noFilters() {
        return new BatchSearchCriteria(null, null, null, null, null, null, null, null, null);
    }

    private void seedReferenceData() {
        client = new Client();
        client.setName("Client A");
        client.setContactPerson("Contact");
        client.setPhone("9000000000");
        client.setStatus(ClientStatus.ACTIVE);
        entityManager.persist(client);

        site = new Site();
        site.setName("Kharadi");
        site.setClient(client);
        site.setLocation("Pune");
        site.setStatus(SiteStatus.ACTIVE);
        entityManager.persist(site);

        recipe = new Recipe();
        recipe.setName("M25");
        recipe.setTotalBatchQuantityKg(new BigDecimal("3.00"));
        recipe.setStatus(RecipeStatus.ACTIVE);
        entityManager.persist(recipe);

        driver = new Driver();
        driver.setName("Ganesh More");
        driver.setPhone("9000000001");
        driver.setLicenseNo("MH12 2019 000001");
        driver.setStatus(DriverStatus.ACTIVE);
        entityManager.persist(driver);

        vehicle = new Vehicle();
        vehicle.setVehicleNumber("MH12PQ0001");
        vehicle.setDriver(driver);
        vehicle.setCapacityCubicMeters(new BigDecimal("6.00"));
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        entityManager.persist(vehicle);
    }

    private void persistBatches(int count) {
        for (int i = 0; i < count; i++) {
            Batch batch = new Batch();
            batch.setBatchNumber(String.valueOf(batchNumber++));
            batch.setClient(client);
            batch.setSite(site);
            batch.setVehicle(vehicle);
            batch.setDriver(driver);
            batch.setRecipe(recipe);
            batch.setTargetQuantity(new BigDecimal("100.00"));
            batch.setProducedQuantity(new BigDecimal("99.00"));
            batch.setStatus(BatchStatus.COMPLETED);
            batch.setCycleDateTime(Instant.parse("2026-09-26T06:00:00Z").plusSeconds(i * 3600L));
            // Six materials, as a real mix has - the collection is the load that
            // cannot be fetch-joined away under pagination.
            for (int m = 0; m < 6; m++) {
                BatchMaterial material = new BatchMaterial();
                material.setBatch(batch);
                material.setMaterialName("Material " + m);
                material.setTarget(BigDecimal.TEN);
                material.setSetpoint(BigDecimal.TEN);
                material.setAchieved(BigDecimal.TEN);
                material.setUnit("kg");
                material.setDisplayOrder(m);
                batch.getMaterials().add(material);
            }
            entityManager.persist(batch);
        }
        entityManager.flush();
    }
}
