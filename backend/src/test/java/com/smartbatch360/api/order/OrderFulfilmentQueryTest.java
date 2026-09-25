package com.smartbatch360.api.order;

import com.smartbatch360.api.batch.Batch;
import com.smartbatch360.api.batch.BatchMaterial;
import com.smartbatch360.api.batch.BatchRepository;
import com.smartbatch360.api.batch.BatchStatus;
import com.smartbatch360.api.batch.OrderProducedQuantity;
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
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The two queries behind the order list. Both exist for the same reason: the
 * list used to grow a query per row - one to sum each order's batches, plus one
 * per lazily loaded customer, site and recipe name - so opening Orders got
 * slower with every order the plant took. Real H2 rather than mocks, because
 * what is being asserted is the SQL the list actually runs.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(SalesOrderService.class)
class OrderFulfilmentQueryTest {

    @Autowired private BatchRepository batchRepository;
    @Autowired private SalesOrderRepository salesOrderRepository;
    @Autowired private SalesOrderService salesOrderService;
    @Autowired private EntityManager entityManager;

    @Test
    void sumsEveryOrdersProductionInOneQuery() {
        Fixture f = seed();

        Map<Long, BigDecimal> produced = batchRepository.sumProducedQuantityByOrder().stream()
                .collect(Collectors.toMap(OrderProducedQuantity::orderId,
                        OrderProducedQuantity::producedQuantity));

        // Two batches on the first order are added together; the second order's
        // single batch stands alone.
        assertThat(produced.get(f.firstOrderId)).isEqualByComparingTo("7.50");
        assertThat(produced.get(f.secondOrderId)).isEqualByComparingTo("2.00");
    }

    /**
     * An order nobody has produced against yet is absent rather than zero - the
     * service treats a missing entry as zero, so this pins down which of the two
     * it is.
     */
    @Test
    void leavesOutOrdersWithNoBatchesYet() {
        Fixture f = seed();

        assertThat(batchRepository.sumProducedQuantityByOrder())
                .extracting(OrderProducedQuantity::orderId)
                .doesNotContain(f.untouchedOrderId);
    }

    /** A batch not raised against any order must not become its own group. */
    @Test
    void ignoresBatchesThatBelongToNoOrder() {
        seed();

        assertThat(batchRepository.sumProducedQuantityByOrder())
                .extracting(OrderProducedQuantity::orderId)
                .doesNotContainNull();
    }

    /**
     * The fetch join, asserted where it can actually fail: reading a name inside
     * an open transaction would lazily load it and pass either way, so this
     * checks the associations came back already initialised.
     */
    @Test
    void loadsTheListWithTheNamesItShows() {
        seed();
        entityManager.clear(); // so nothing can come from the first-level cache

        var orders = salesOrderRepository.findAllForList();

        assertThat(orders).hasSize(3);
        assertThat(orders).allSatisfy(order -> {
            assertThat(Hibernate.isInitialized(order.getClient())).isTrue();
            assertThat(Hibernate.isInitialized(order.getSite())).isTrue();
            assertThat(Hibernate.isInitialized(order.getRecipe())).isTrue();
            assertThat(order.getClient().getName()).isNotBlank();
            assertThat(order.getSite().getName()).isNotBlank();
            assertThat(order.getRecipe().getName()).isNotBlank();
        });
    }

    /**
     * The whole point of the change: the list must cost the same number of
     * queries whatever the plant's order history looks like. Before, 3 orders
     * cost 4 queries and 30 cost 31 - it grew one per order for ever.
     */
    @Test
    void costsTheSameNumberOfQueriesHoweverManyOrdersThereAre() {
        Fixture f = seed();

        long forThree = queriesToListOrders();
        for (int i = 0; i < 27; i++) {
            order(f.client, f.site, f.recipe, "5.00");
        }
        entityManager.flush();
        long forThirty = queriesToListOrders();

        assertThat(salesOrderRepository.count()).isEqualTo(30);
        assertThat(forThirty).isEqualTo(forThree);
    }

    /** Runs the order list and reports how many SQL statements it took. */
    private long queriesToListOrders() {
        Statistics statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();
        entityManager.clear();
        long before = statistics.getQueryExecutionCount() + statistics.getPrepareStatementCount();
        salesOrderService.findAll();
        return statistics.getQueryExecutionCount() + statistics.getPrepareStatementCount() - before;
    }

    private record Fixture(Long firstOrderId, Long secondOrderId, Long untouchedOrderId,
                            Client client, Site site, Recipe recipe) {
    }

    private Fixture seed() {
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

        Recipe recipe = new Recipe();
        recipe.setName("M25");
        recipe.setTotalBatchQuantityKg(new BigDecimal("3.00"));
        recipe.setStatus(RecipeStatus.ACTIVE);
        entityManager.persist(recipe);

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

        SalesOrder first = order(client, site, recipe, "10.00");
        SalesOrder second = order(client, site, recipe, "20.00");
        SalesOrder untouched = order(client, site, recipe, "30.00");

        persistBatch("250100", client, site, recipe, vehicle, driver, first, "5.00");
        persistBatch("250200", client, site, recipe, vehicle, driver, first, "2.50");
        persistBatch("250300", client, site, recipe, vehicle, driver, second, "2.00");
        persistBatch("250400", client, site, recipe, vehicle, driver, null, "9.00");
        entityManager.flush();

        return new Fixture(first.getId(), second.getId(), untouched.getId(), client, site, recipe);
    }

    private SalesOrder order(Client client, Site site, Recipe recipe, String quantityKg) {
        SalesOrder order = new SalesOrder();
        order.setClient(client);
        order.setSite(site);
        order.setRecipe(recipe);
        order.setQuantityKg(new BigDecimal(quantityKg));
        order.setStatus(OrderStatus.UNFULFILLED);
        entityManager.persist(order);
        return order;
    }

    private void persistBatch(String number, Client client, Site site, Recipe recipe, Vehicle vehicle,
                               Driver driver, SalesOrder order, String producedQuantity) {
        Batch batch = new Batch();
        batch.setBatchNumber(number);
        batch.setClient(client);
        batch.setSite(site);
        batch.setRecipe(recipe);
        batch.setVehicle(vehicle);
        batch.setDriver(driver);
        batch.setOrder(order);
        batch.setTargetQuantity(new BigDecimal("10.00"));
        batch.setProducedQuantity(new BigDecimal(producedQuantity));
        batch.setStatus(BatchStatus.COMPLETED);
        batch.setCycleDateTime(Instant.parse("2026-09-25T06:00:00Z"));
        BatchMaterial material = new BatchMaterial();
        material.setBatch(batch);
        material.setMaterialName("Cement");
        material.setTarget(BigDecimal.TEN);
        material.setSetpoint(BigDecimal.TEN);
        material.setAchieved(BigDecimal.TEN);
        material.setUnit("kg");
        batch.getMaterials().add(material);
        entityManager.persist(batch);
    }
}
