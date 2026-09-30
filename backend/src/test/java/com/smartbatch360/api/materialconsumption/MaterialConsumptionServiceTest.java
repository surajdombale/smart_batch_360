package com.smartbatch360.api.materialconsumption;

import com.smartbatch360.api.batch.Batch;
import com.smartbatch360.api.batch.BatchMaterial;
import com.smartbatch360.api.batch.BatchMaterialRepository;
import com.smartbatch360.api.batch.BatchStatus;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import com.smartbatch360.api.common.ReportingZone;
import com.smartbatch360.api.order.SalesOrderRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real (in-memory H2) aggregation test - a mocked repository can't
 * meaningfully verify the day/week/month bucketing and target/achieved
 * summing actually happen correctly, same reasoning as
 * BatchSpecificationsTest.
 */
@DataJpaTest
@Import({MaterialConsumptionService.class, MaterialConsumptionServiceTest.UtcZone.class})
class MaterialConsumptionServiceTest {

    /**
     * These tests seed instants directly and expect days to line up with them,
     * so they read days in UTC rather than in whatever zone the machine is set
     * to. The plant-local behaviour has tests of its own below.
     */
    @TestConfiguration
    static class UtcZone {
        @Bean
        ReportingZone reportingZone() {
            return ReportingZone.of(ZoneOffset.UTC);
        }
    }

    @Autowired
    private BatchMaterialRepository batchMaterialRepository;

    @Autowired
    private MaterialConsumptionService service;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private EntityManager entityManager;

    /**
     * End to end in the zone a plant actually runs in: a batch made at 01:30 on
     * a night shift must be reported under that day, not the previous one. With
     * UTC bucketing it landed in the day before, and a filter on the day it was
     * made returned nothing.
     */
    @Test
    void bucketsANightShiftBatchUnderThePlantsDay() {
        Client client = client("Client A");
        entityManager.persist(client);
        Site site = site("Kharadi", client);
        entityManager.persist(site);
        Driver driver = driver("Ganesh More");
        entityManager.persist(driver);
        Vehicle vehicle = vehicle("MH12PQ0001", driver);
        entityManager.persist(vehicle);
        Recipe recipe = recipe("M25");
        entityManager.persist(recipe);

        // 2026-09-25T20:00Z is 01:30 on the 26th at the plant.
        persistBatchAt("NS1", client, site, vehicle, driver, recipe,
                Instant.parse("2026-09-25T20:00:00Z"), "Cement", "50.00", "50.00");
        // 2026-09-25T10:00Z is 15:30 on the 25th - the same day either way.
        persistBatchAt("NS2", client, site, vehicle, driver, recipe,
                Instant.parse("2026-09-25T10:00:00Z"), "Cement", "50.00", "50.00");
        entityManager.flush();

        MaterialConsumptionService plantService = new MaterialConsumptionService(
                batchMaterialRepository, salesOrderRepository, ReportingZone.of(ZoneId.of("Asia/Kolkata")));

        List<MaterialConsumptionResponse> byDay = plantService.search(
                new MaterialConsumptionSearchCriteria(null, null, null, MaterialConsumptionGroupBy.DAY));

        assertThat(byDay).extracting(MaterialConsumptionResponse::period)
                .containsExactly("2026-09-25", "2026-09-26");

        // And asking for just the 26th finds the night-shift batch on its own.
        List<MaterialConsumptionResponse> justTheSixth = plantService.search(
                new MaterialConsumptionSearchCriteria(null, LocalDate.of(2026, 9, 26),
                        LocalDate.of(2026, 9, 26), MaterialConsumptionGroupBy.DAY));

        assertThat(justTheSixth).singleElement()
                .satisfies(row -> {
                    assertThat(row.period()).isEqualTo("2026-09-26");
                    assertThat(row.totalAchieved()).isEqualByComparingTo("50.00");
                });
    }

    /** As persistBatch, but at an exact instant rather than a date. */
    private void persistBatchAt(String number, Client client, Site site, Vehicle vehicle, Driver driver,
                                 Recipe recipe, Instant cycleDateTime, String materialName,
                                 String target, String achieved) {
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
        batch.setCycleDateTime(cycleDateTime);
        BatchMaterial material = new BatchMaterial();
        material.setBatch(batch);
        material.setMaterialName(materialName);
        material.setTarget(new BigDecimal(target));
        material.setSetpoint(new BigDecimal(target));
        material.setAchieved(new BigDecimal(achieved));
        material.setUnit("kg");
        batch.getMaterials().add(material);
        entityManager.persist(batch);
    }

    private void seed() {
        Client client = client("Client A");
        entityManager.persist(client);
        Site site = site("Kharadi", client);
        entityManager.persist(site);
        Driver driver = driver("Ganesh More");
        entityManager.persist(driver);
        Vehicle vehicle = vehicle("MH12PQ0001", driver);
        entityManager.persist(vehicle);
        Recipe recipe = recipe("M25");
        entityManager.persist(recipe);

        // Two batches same day (2026-08-01): should sum into one Cement row.
        persistBatch("B1", client, site, vehicle, driver, recipe,
                LocalDate.of(2026, 8, 1), "Cement", "960.00", "955.00");
        persistBatch("B2", client, site, vehicle, driver, recipe,
                LocalDate.of(2026, 8, 1), "Cement", "960.00", "965.00");
        // Different day, same month: separate DAY row, same MONTH row.
        persistBatch("B3", client, site, vehicle, driver, recipe,
                LocalDate.of(2026, 8, 15), "Cement", "500.00", "500.00");
        // Different material entirely, filtered out by materialName below.
        persistBatch("B4", client, site, vehicle, driver, recipe,
                LocalDate.of(2026, 8, 1), "Water", "540.00", "540.00");
        entityManager.flush();
    }

    @Test
    void sumsTargetAndAchievedForTheSameMaterialAndDay() {
        seed();
        List<MaterialConsumptionResponse> result = service.search(
                new MaterialConsumptionSearchCriteria("Cement", null, null, MaterialConsumptionGroupBy.DAY));

        MaterialConsumptionResponse aug1 = result.stream()
                .filter(r -> r.period().equals("2026-08-01")).findFirst().orElseThrow();
        assertThat(aug1.totalTarget()).isEqualByComparingTo("1920.00");
        assertThat(aug1.totalAchieved()).isEqualByComparingTo("1920.00");
        assertThat(aug1.variance()).isEqualByComparingTo("0.00");
        assertThat(aug1.batchCount()).isEqualTo(2);
    }

    @Test
    void varianceIsAchievedMinusTarget() {
        seed();
        List<MaterialConsumptionResponse> result = service.search(
                new MaterialConsumptionSearchCriteria("Cement", null, null, MaterialConsumptionGroupBy.MONTH));

        // Aug totals: target 960+960+500=2420, achieved 955+965+500=2420 -> variance 0
        // but B1 is under (-5) and B2 is over (+5), net cancels - verify the net figure.
        MaterialConsumptionResponse aug = result.get(0);
        assertThat(aug.totalTarget()).isEqualByComparingTo("2420.00");
        assertThat(aug.totalAchieved()).isEqualByComparingTo("2420.00");
        assertThat(aug.variance()).isEqualByComparingTo("0.00");
    }

    @Test
    void groupsByDayVsMonthDifferently() {
        seed();
        List<MaterialConsumptionResponse> byDay = service.search(
                new MaterialConsumptionSearchCriteria("Cement", null, null, MaterialConsumptionGroupBy.DAY));
        List<MaterialConsumptionResponse> byMonth = service.search(
                new MaterialConsumptionSearchCriteria("Cement", null, null, MaterialConsumptionGroupBy.MONTH));

        assertThat(byDay).extracting(MaterialConsumptionResponse::period)
                .containsExactly("2026-08-01", "2026-08-15");
        assertThat(byMonth).extracting(MaterialConsumptionResponse::period)
                .containsExactly("2026-08");
    }

    @Test
    void materialNameFilterIsCaseInsensitivePartialMatch() {
        seed();
        List<MaterialConsumptionResponse> result = service.search(
                new MaterialConsumptionSearchCriteria("cem", null, null, MaterialConsumptionGroupBy.DAY));

        assertThat(result).allMatch(r -> r.materialName().equals("Cement"));
    }

    @Test
    void dateRangeExcludesRowsOutsideIt() {
        seed();
        List<MaterialConsumptionResponse> result = service.search(new MaterialConsumptionSearchCriteria(
                "Cement", LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 20), MaterialConsumptionGroupBy.DAY));

        assertThat(result).extracting(MaterialConsumptionResponse::period).containsExactly("2026-08-15");
    }

    @Test
    void noFiltersReturnsEveryMaterial() {
        seed();
        List<MaterialConsumptionResponse> result = service.search(
                new MaterialConsumptionSearchCriteria(null, null, null, MaterialConsumptionGroupBy.DAY));

        assertThat(result).extracting(MaterialConsumptionResponse::materialName)
                .contains("Cement", "Water");
    }

    /**
     * The plant has pre-kilogram history: one water line in the real database
     * still carries "L" from before the switch to kilograms. When rows in the
     * same group disagree, the reported unit used to be whichever row the
     * database returned first, so the same period could read "L" on one request
     * and "kg" on the next. It now reports the unit most rows agree on.
     */
    @Test
    void reportsTheUnitMostRowsAgreeOnWhenTheyDisagree() {
        Client client = client("Client A");
        entityManager.persist(client);
        Site site = site("Kharadi", client);
        entityManager.persist(site);
        Driver driver = driver("Ganesh More");
        entityManager.persist(driver);
        Vehicle vehicle = vehicle("MH12PQ0001", driver);
        entityManager.persist(vehicle);
        Recipe recipe = recipe("M25");
        entityManager.persist(recipe);

        LocalDate day = LocalDate.of(2026, 9, 20);
        persistBatch("250901", client, site, vehicle, driver, recipe, day, "Water", "10", "10", "L");
        persistBatch("250902", client, site, vehicle, driver, recipe, day, "Water", "10", "10", "kg");
        persistBatch("250903", client, site, vehicle, driver, recipe, day, "Water", "10", "10", "kg");
        entityManager.flush();

        List<MaterialConsumptionResponse> result = service.search(
                new MaterialConsumptionSearchCriteria(null, null, null, MaterialConsumptionGroupBy.DAY));

        assertThat(result).singleElement()
                .satisfies(row -> {
                    assertThat(row.unit()).isEqualTo("kg");
                    // The totals must still count every row, mislabelled or not.
                    assertThat(row.totalTarget()).isEqualByComparingTo("30");
                    assertThat(row.batchCount()).isEqualTo(3);
                });
    }

    private void persistBatch(String number, Client client, Site site, Vehicle vehicle, Driver driver,
                               Recipe recipe, LocalDate date, String materialName, String target, String achieved) {
        persistBatch(number, client, site, vehicle, driver, recipe, date, materialName, target, achieved, "kg");
    }

    private void persistBatch(String number, Client client, Site site, Vehicle vehicle, Driver driver,
                               Recipe recipe, LocalDate date, String materialName, String target, String achieved,
                               String unit) {
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
        material.setMaterialName(materialName);
        material.setTarget(new BigDecimal(target));
        material.setSetpoint(new BigDecimal(target));
        material.setAchieved(new BigDecimal(achieved));
        material.setUnit(unit);
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

    private Driver driver(String name) {
        Driver d = new Driver();
        d.setName(name);
        d.setPhone("9000000001");
        d.setLicenseNo("MH12 2019 000001");
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
        r.setTotalBatchQuantityKg(new BigDecimal("3.00"));
        r.setStatus(RecipeStatus.ACTIVE);
        return r;
    }
}
