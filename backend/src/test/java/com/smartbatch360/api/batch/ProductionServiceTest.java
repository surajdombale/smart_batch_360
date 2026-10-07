package com.smartbatch360.api.batch;

import com.smartbatch360.api.batch.dto.BatchResponse;
import com.smartbatch360.api.batch.dto.ProductionPlanRequest;
import com.smartbatch360.api.batch.dto.ProductionPlanResponse;
import com.smartbatch360.api.batch.dto.StartProductionRequest;
import com.smartbatch360.api.client.Client;
import com.smartbatch360.api.client.ClientStatus;
import com.smartbatch360.api.common.InvalidRequestException;
import com.smartbatch360.api.common.ReportingZone;
import com.smartbatch360.api.driver.Driver;
import com.smartbatch360.api.driver.DriverStatus;
import com.smartbatch360.api.material.Material;
import com.smartbatch360.api.order.OrderStatus;
import com.smartbatch360.api.order.SalesOrder;
import com.smartbatch360.api.order.SalesOrderRepository;
import com.smartbatch360.api.recipe.Recipe;
import com.smartbatch360.api.recipe.RecipeMaterial;
import com.smartbatch360.api.recipe.RecipeStatus;
import com.smartbatch360.api.site.Site;
import com.smartbatch360.api.site.SiteStatus;
import com.smartbatch360.api.vehicle.Vehicle;
import com.smartbatch360.api.vehicle.VehicleRepository;
import com.smartbatch360.api.vehicle.VehicleStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The Production screen's two steps: show what a load works out to, then start
 * it. Real H2, because what matters is the batch that ends up stored.
 */
@DataJpaTest
class ProductionServiceTest {

    @Autowired private SalesOrderRepository salesOrderRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private BatchRepository batchRepository;
    @Autowired private com.smartbatch360.api.header.HeaderRepository headerRepository;
    @Autowired private com.smartbatch360.api.driver.DriverRepository driverRepository;
    @Autowired private EntityManager entityManager;

    private ProductionService service;
    private SalesOrder order;
    private Vehicle vehicle;

    @BeforeEach
    void seed() {
        BatchPlanner planner = new BatchPlanner(ConcreteDensity.standard());
        // A 1 m3 mixer, as if Company Details said so.
        PlantSettings plantSettings = new PlantSettings(headerRepository, "1");
        service = new ProductionService(salesOrderRepository, vehicleRepository, driverRepository,
                batchRepository, planner,
                plantSettings,
                new ProductionService.ReportingZoneBatchNumber(batchRepository,
                        ReportingZone.of(ZoneOffset.UTC)));

        Client client = new Client();
        client.setName("Shree Cement");
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

        vehicle = new Vehicle();
        vehicle.setVehicleNumber("MH12PQ0001");
        vehicle.setDriver(driver);
        vehicle.setCapacityCubicMeters(new BigDecimal("6.00"));
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        entityManager.persist(vehicle);

        Material cement = new Material();
        cement.setName("Cement");
        entityManager.persist(cement);
        Material sand = new Material();
        sand.setName("Sand");
        entityManager.persist(sand);

        Recipe recipe = new Recipe();
        recipe.setName("M25");
        recipe.setStatus(RecipeStatus.ACTIVE);
        recipe.getMaterials().add(line(recipe, cement, "600"));
        recipe.getMaterials().add(line(recipe, sand, "600"));
        recipe.recalculateTotalBatchQuantity();
        entityManager.persist(recipe);

        order = new SalesOrder();
        order.setClient(client);
        order.setSite(site);
        order.setRecipe(recipe);
        order.setQuantityKg(new BigDecimal("10000.00"));
        order.setStatus(OrderStatus.UNFULFILLED);
        entityManager.persist(order);
        entityManager.flush();
    }

    private RecipeMaterial line(Recipe recipe, Material material, String quantity) {
        RecipeMaterial line = new RecipeMaterial();
        line.setRecipe(recipe);
        line.setMaterial(material);
        line.setQuantity(new BigDecimal(quantity));
        return line;
    }

    @Test
    void theScreenShowsTheOrdersOwnDetailsAndTheCalculatedFigures() {
        ProductionPlanResponse plan = service.plan(new ProductionPlanRequest(order.getId(), new BigDecimal("2")));

        assertThat(plan.clientName()).isEqualTo("Shree Cement");
        assertThat(plan.siteName()).isEqualTo("Kharadi");
        assertThat(plan.recipeName()).isEqualTo("M25");
        assertThat(plan.cycles()).isEqualTo(2);
        assertThat(plan.perCycleM3()).isEqualByComparingTo("1");
        assertThat(plan.totalKg()).isEqualByComparingTo("4800.00");
        assertThat(plan.materials()).extracting(ProductionPlanResponse.MaterialSetpointResponse::materialName)
                .containsExactly("Cement", "Sand");
        assertThat(plan.materials().get(0).perCycleKg()).isEqualByComparingTo("1200.00");
    }

    /** Nothing has been produced yet, so the whole order is still outstanding. */
    @Test
    void theScreenShowsWhatIsStillOutstandingOnTheOrder() {
        ProductionPlanResponse plan = service.plan(new ProductionPlanRequest(order.getId(), new BigDecimal("1")));

        assertThat(plan.orderQuantityKg()).isEqualByComparingTo("10000.00");
        assertThat(plan.orderRemainingKg()).isEqualByComparingTo("10000.00");
    }

    @Test
    void startingProductionCreatesTheBatchThePlanDescribes() {
        BatchResponse batch = service.start(new StartProductionRequest(
                order.getId(), vehicle.getId(), new BigDecimal("2"), null, "Day"));

        assertThat(batch.status()).isEqualTo(BatchStatus.IN_PROGRESS);
        assertThat(batch.orderId()).isEqualTo(order.getId());
        // Taken from the order and the vehicle, never from the screen.
        assertThat(batch.clientName()).isEqualTo("Shree Cement");
        assertThat(batch.siteName()).isEqualTo("Kharadi");
        assertThat(batch.recipeName()).isEqualTo("M25");
        assertThat(batch.driverName()).isEqualTo("Ganesh More");
        assertThat(batch.targetQuantity()).isEqualByComparingTo("4800.00");
        assertThat(batch.cycleNumber()).isEqualTo(2);
    }

    /**
     * The report prints a target row and a setpoint row above the cycles: the
     * target is what the whole load needs, the setpoint what one cycle is set to.
     */
    @Test
    void theMaterialsCarryBothTheLoadsTargetAndThePerCycleSetpoint() {
        BatchResponse batch = service.start(new StartProductionRequest(
                order.getId(), vehicle.getId(), new BigDecimal("2"), null, null));

        assertThat(batch.materials()).hasSize(2);
        assertThat(batch.materials().get(0).materialName()).isEqualTo("Cement");
        assertThat(batch.materials().get(0).target()).isEqualByComparingTo("2400.00");
        assertThat(batch.materials().get(0).setpoint()).isEqualByComparingTo("1200.00");
        assertThat(batch.materials().get(0).achieved()).isEqualByComparingTo("0");
    }

    /**
     * The report prints the load in cubic metres. It is kept as the operator
     * sized it, because working it back from the kilograms would go through a
     * density and a mixer capacity that can both be changed afterwards.
     */
    @Test
    void theBatchKeepsTheSizeItWasPlannedAt() {
        BatchResponse batch = service.start(new StartProductionRequest(
                order.getId(), vehicle.getId(), new BigDecimal("2"), null, null));

        assertThat(batch.batchSizeM3()).isEqualByComparingTo("2");
        assertThat(batch.perCycleM3()).isEqualByComparingTo("1");
        assertThat(batch.moistureEnabled()).isFalse();
    }

    @Test
    void moistureCorrectionIsRecordedWhenTheLoadRunsWithIt() {
        BatchResponse batch = service.start(new StartProductionRequest(
                order.getId(), vehicle.getId(), new BigDecimal("1"), null, null, true));

        assertThat(batch.moistureEnabled()).isTrue();
    }

    @Test
    void aBatchNumberIsGeneratedWhenNoneIsGiven() {
        BatchResponse first = service.start(new StartProductionRequest(
                order.getId(), vehicle.getId(), new BigDecimal("1"), null, null));
        // One batch at a time, so the first has to finish before the next loads.
        batchRepository.findById(first.id()).orElseThrow().setStatus(BatchStatus.COMPLETED);
        entityManager.flush();
        BatchResponse second = service.start(new StartProductionRequest(
                order.getId(), vehicle.getId(), new BigDecimal("1"), null, null));

        assertThat(first.batchNumber()).hasSize(9).endsWith("001");
        assertThat(second.batchNumber()).endsWith("002");
    }

    @Test
    void aBatchNumberCanBeGivenByHand() {
        BatchResponse batch = service.start(new StartProductionRequest(
                order.getId(), vehicle.getId(), new BigDecimal("1"), "PLANT-7", null));

        assertThat(batch.batchNumber()).isEqualTo("PLANT-7");
    }

    /**
     * The plant runs one batch from start to finish before loading the next
     * (sheet, 07-Oct-2026), so a second start while one is still going is
     * refused and the running batch is named.
     */
    @Test
    void asecondBatchCannotBeStartedWhileOneIsRunning() {
        service.start(new StartProductionRequest(order.getId(), vehicle.getId(), new BigDecimal("1"), null, null));

        assertThatThrownBy(() -> service.start(new StartProductionRequest(
                order.getId(), vehicle.getId(), new BigDecimal("1"), null, null)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("is still IN_PROGRESS")
                .hasMessageContaining("one batch at a time");
    }

    /** Finishing the running batch frees the plant for the next load. */
    @Test
    void anotherBatchCanBeStartedOnceTheRunningOneIsDone() {
        BatchResponse first = service.start(new StartProductionRequest(
                order.getId(), vehicle.getId(), new BigDecimal("1"), null, null));
        Batch running = batchRepository.findById(first.id()).orElseThrow();
        running.setStatus(BatchStatus.COMPLETED);
        entityManager.flush();

        BatchResponse second = service.start(new StartProductionRequest(
                order.getId(), vehicle.getId(), new BigDecimal("1"), null, null));

        assertThat(second.status()).isEqualTo(BatchStatus.IN_PROGRESS);
    }

    /** The driver is chosen on the screen now, rather than always the vehicle's own. */
    @Test
    void theChosenDriverIsUsedRatherThanTheVehiclesOwn() {
        Driver reliefDriver = new Driver();
        reliefDriver.setName("Relief Driver");
        reliefDriver.setPhone("9000000002");
        reliefDriver.setLicenseNo("MH12 2020 000002");
        reliefDriver.setStatus(DriverStatus.ACTIVE);
        entityManager.persist(reliefDriver);
        entityManager.flush();

        BatchResponse batch = service.start(new StartProductionRequest(
                order.getId(), vehicle.getId(), new BigDecimal("1"), null, null, null, reliefDriver.getId()));

        assertThat(batch.driverName()).isEqualTo("Relief Driver");
    }

    /** The recipe's own figure is shown next to the calculated setpoint. */
    @Test
    void thePlanCarriesTheRecipesTargetBesideTheSetpoint() {
        ProductionPlanResponse plan = service.plan(new ProductionPlanRequest(order.getId(), new BigDecimal("2")));

        ProductionPlanResponse.MaterialSetpointResponse cement = plan.materials().get(0);
        assertThat(cement.materialName()).isEqualTo("Cement");
        assertThat(cement.recipeQuantityKg()).isEqualByComparingTo("600");
        assertThat(cement.perCycleKg()).isEqualByComparingTo("1200.00");
    }
}
