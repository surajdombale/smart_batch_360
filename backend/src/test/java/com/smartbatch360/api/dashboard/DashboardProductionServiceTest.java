package com.smartbatch360.api.dashboard;

import com.smartbatch360.api.batch.Batch;
import com.smartbatch360.api.batch.BatchRepository;
import com.smartbatch360.api.batch.BatchStatus;
import com.smartbatch360.api.batch.ConcreteDensity;
import com.smartbatch360.api.client.Client;
import com.smartbatch360.api.client.ClientStatus;
import com.smartbatch360.api.common.ReportingZone;
import com.smartbatch360.api.dashboard.DashboardProductionResponse.RecentBatch;
import com.smartbatch360.api.dashboard.DashboardProductionResponse.TrendPoint;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pinned to a real plant zone, five and a half hours ahead of UTC, because that
 * is where a day boundary in the wrong zone shows: a batch at 01:30 on the night
 * shift is the previous day in UTC.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class DashboardProductionServiceTest {

    private static final ZoneId PLANT = ZoneId.of("Asia/Kolkata");

    /** 10:20 at the plant on 27-Oct-2026. */
    private static final Instant NOW = plant(2026, 10, 27, 10, 20);

    @Autowired private BatchRepository batchRepository;
    @Autowired private EntityManager entityManager;

    private DashboardProductionService service;
    private Client client;
    private Site site;
    private Recipe recipe;
    private Driver driver;
    private Vehicle truckOne;
    private Vehicle truckTwo;
    private int batchNumber = 1;

    @BeforeEach
    void setUp() {
        service = new DashboardProductionService(batchRepository, ReportingZone.of(PLANT), ConcreteDensity.standard());
        seedReferenceData();
    }

    @Test
    void todayCountsTheNightShiftButNotLastNight() {
        batch(plant(2026, 10, 26, 23, 30), truckOne, "2400", BatchStatus.COMPLETED);
        batch(plant(2026, 10, 27, 1, 30), truckOne, "2400", BatchStatus.COMPLETED);
        batch(plant(2026, 10, 27, 9, 5), truckTwo, "3600", BatchStatus.IN_PROGRESS);
        batch(plant(2026, 10, 27, 9, 40), truckOne, "4800", BatchStatus.COMPLETED);

        DashboardProductionResponse today = service.getProduction(DashboardPeriod.TODAY, NOW);

        assertThat(today.firstDay()).isEqualTo(LocalDate.of(2026, 10, 27));
        assertThat(today.current().batches()).isEqualTo(3);
        // 10,800 kg at 2400 kg per m3, the in-progress batch counted as far as it has got.
        assertThat(today.current().producedM3()).isEqualByComparingTo("4.50");
        assertThat(today.current().activeVehicles()).isEqualTo(2);
    }

    @Test
    void todaySoFarIsComparedWithYesterdayUpToTheSameTime() {
        batch(plant(2026, 10, 26, 8, 0), truckOne, "2400", BatchStatus.COMPLETED);
        batch(plant(2026, 10, 26, 23, 30), truckTwo, "2400", BatchStatus.COMPLETED);
        batch(plant(2026, 10, 27, 9, 0), truckOne, "2400", BatchStatus.COMPLETED);

        DashboardProductionResponse today = service.getProduction(DashboardPeriod.TODAY, NOW);

        // Yesterday's 23:30 batch has no counterpart yet today, so it is not held against it.
        assertThat(today.previous().batches()).isEqualTo(1);
        assertThat(today.previous().producedM3()).isEqualByComparingTo("1.00");
    }

    @Test
    void yesterdayIsAWholeDayAgainstTheWholeDayBefore() {
        batch(plant(2026, 10, 25, 22, 0), truckOne, "2400", BatchStatus.COMPLETED);
        batch(plant(2026, 10, 26, 8, 0), truckOne, "2400", BatchStatus.COMPLETED);
        batch(plant(2026, 10, 26, 23, 30), truckTwo, "2400", BatchStatus.COMPLETED);
        batch(plant(2026, 10, 27, 9, 0), truckOne, "2400", BatchStatus.COMPLETED);

        DashboardProductionResponse yesterday = service.getProduction(DashboardPeriod.YESTERDAY, NOW);

        assertThat(yesterday.firstDay()).isEqualTo(LocalDate.of(2026, 10, 26));
        assertThat(yesterday.current().batches()).isEqualTo(2);
        assertThat(yesterday.previous().batches()).isEqualTo(1);
        assertThat(yesterday.trend()).hasSize(24);
    }

    @Test
    void todaysTrendRunsByPlantHourAndStopsAtTheCurrentOne() {
        batch(plant(2026, 10, 27, 1, 30), truckOne, "2400", BatchStatus.COMPLETED);
        batch(plant(2026, 10, 27, 9, 5), truckTwo, "2400", BatchStatus.COMPLETED);
        batch(plant(2026, 10, 27, 9, 40), truckOne, "2400", BatchStatus.COMPLETED);

        DashboardProductionResponse today = service.getProduction(DashboardPeriod.TODAY, NOW);

        // 00:00 to 10:00 - the hour now under way is in, the ones still to come are not.
        assertThat(today.trend()).hasSize(11);
        assertThat(today.trend().get(0).start()).isEqualTo(LocalDateTime.of(2026, 10, 27, 0, 0));
        assertThat(today.trend().get(1).batches()).isEqualTo(1);
        assertThat(today.trend().get(9).batches()).isEqualTo(2);
        assertThat(today.trend().get(10).batches()).isZero();
    }

    @Test
    void aWeekIsChartedByDayAndComparedWithTheWeekBefore() {
        batch(plant(2026, 10, 20, 9, 0), truckOne, "2400", BatchStatus.COMPLETED);
        batch(plant(2026, 10, 21, 0, 15), truckOne, "2400", BatchStatus.COMPLETED);
        batch(plant(2026, 10, 27, 9, 0), truckTwo, "2400", BatchStatus.COMPLETED);

        DashboardProductionResponse week = service.getProduction(DashboardPeriod.LAST_7_DAYS, NOW);

        assertThat(week.trend()).extracting(TrendPoint::start).containsExactly(
                day(21), day(22), day(23), day(24), day(25), day(26), day(27));
        assertThat(week.trend()).extracting(TrendPoint::batches).containsExactly(1L, 0L, 0L, 0L, 0L, 0L, 1L);
        assertThat(week.current().batches()).isEqualTo(2);
        assertThat(week.previous().batches()).isEqualTo(1);
    }

    @Test
    void recentBatchesAreTheNewestFiveWithTheirNames() {
        for (int minute = 0; minute < 7; minute++) {
            batch(plant(2026, 10, 27, 8, minute), truckOne, "2400", BatchStatus.COMPLETED);
        }
        batch(plant(2026, 10, 27, 9, 30), truckTwo, "3600", BatchStatus.IN_PROGRESS);

        DashboardProductionResponse today = service.getProduction(DashboardPeriod.TODAY, NOW);

        assertThat(today.recentBatches()).hasSize(DashboardProductionService.RECENT_BATCHES);
        RecentBatch newest = today.recentBatches().get(0);
        assertThat(newest.batchNumber()).isEqualTo("8");
        assertThat(newest.recipeName()).isEqualTo("M25");
        assertThat(newest.vehicleNumber()).isEqualTo("MH12AB0002");
        assertThat(newest.producedM3()).isEqualByComparingTo("1.50");
        assertThat(newest.status()).isEqualTo(BatchStatus.IN_PROGRESS);
        assertThat(today.recentBatches()).extracting(RecentBatch::batchNumber)
                .containsExactly("8", "7", "6", "5", "4");
    }

    @Test
    void anEmptyPeriodIsZeroRatherThanMissing() {
        DashboardProductionResponse today = service.getProduction(DashboardPeriod.TODAY, NOW);

        assertThat(today.current().batches()).isZero();
        assertThat(today.current().producedM3()).isEqualByComparingTo("0");
        assertThat(today.current().activeVehicles()).isZero();
        assertThat(today.recentBatches()).isEmpty();
        assertThat(today.trend()).extracting(TrendPoint::batches).containsOnly(0L);
    }

    /** It refreshes every five seconds, so its cost must not grow with the plant's history. */
    @Test
    void costsTheSameQueriesWhateverTheVolume() {
        for (int minute = 0; minute < 2; minute++) {
            batch(plant(2026, 10, 27, 8, minute), truckOne, "2400", BatchStatus.COMPLETED);
        }
        long forTwo = queriesFor(DashboardPeriod.LAST_30_DAYS);

        for (int minute = 2; minute < 40; minute++) {
            batch(plant(2026, 10, 27, 8, minute), minute % 2 == 0 ? truckOne : truckTwo, "2400",
                    BatchStatus.COMPLETED);
        }
        long forForty = queriesFor(DashboardPeriod.LAST_30_DAYS);

        assertThat(forForty).isEqualTo(forTwo).isEqualTo(4);
    }

    private long queriesFor(DashboardPeriod period) {
        Statistics statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();
        entityManager.clear();
        long before = statistics.getPrepareStatementCount();
        service.getProduction(period, NOW);
        return statistics.getPrepareStatementCount() - before;
    }

    private static Instant plant(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute).atZone(PLANT).toInstant();
    }

    private static LocalDateTime day(int dayOfOctober) {
        return LocalDate.of(2026, 10, dayOfOctober).atStartOfDay();
    }

    private void batch(Instant at, Vehicle vehicle, String producedKg, BatchStatus status) {
        Batch batch = new Batch();
        batch.setBatchNumber(String.valueOf(batchNumber++));
        batch.setClient(client);
        batch.setSite(site);
        batch.setVehicle(vehicle);
        batch.setDriver(driver);
        batch.setRecipe(recipe);
        batch.setTargetQuantity(new BigDecimal(producedKg));
        batch.setProducedQuantity(new BigDecimal(producedKg));
        batch.setStatus(status);
        batch.setCycleDateTime(at);
        entityManager.persist(batch);
        entityManager.flush();
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
        recipe.setTotalBatchQuantityKg(new BigDecimal("2400.00"));
        recipe.setStatus(RecipeStatus.ACTIVE);
        entityManager.persist(recipe);

        driver = new Driver();
        driver.setName("Ganesh More");
        driver.setPhone("9000000001");
        driver.setLicenseNo("MH12 2019 000001");
        driver.setStatus(DriverStatus.ACTIVE);
        entityManager.persist(driver);

        truckOne = vehicle("MH12AB0001");
        truckTwo = vehicle("MH12AB0002");
    }

    private Vehicle vehicle(String number) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleNumber(number);
        vehicle.setDriver(driver);
        vehicle.setCapacityCubicMeters(new BigDecimal("6.00"));
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        entityManager.persist(vehicle);
        return vehicle;
    }
}
