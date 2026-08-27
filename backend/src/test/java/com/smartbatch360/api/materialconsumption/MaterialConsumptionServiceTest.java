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
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real (in-memory H2) aggregation test - a mocked repository can't
 * meaningfully verify the day/week/month bucketing and target/achieved
 * summing actually happen correctly, same reasoning as
 * BatchSpecificationsTest.
 */
@DataJpaTest
@Import(MaterialConsumptionService.class)
class MaterialConsumptionServiceTest {

    @Autowired
    private BatchMaterialRepository batchMaterialRepository;

    @Autowired
    private MaterialConsumptionService service;

    @Autowired
    private EntityManager entityManager;

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

    private void persistBatch(String number, Client client, Site site, Vehicle vehicle, Driver driver,
                               Recipe recipe, LocalDate date, String materialName, String target, String achieved) {
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
        r.setTotalBatchQuantityM3(new BigDecimal("3.00"));
        r.setStatus(RecipeStatus.ACTIVE);
        return r;
    }
}
