package com.smartbatch360.api.batch;

import com.smartbatch360.api.batch.dto.BatchResponse;
import com.smartbatch360.api.batch.dto.ProductionPlanRequest;
import com.smartbatch360.api.batch.dto.ProductionPlanResponse;
import com.smartbatch360.api.batch.dto.StartProductionRequest;
import com.smartbatch360.api.common.InvalidRequestException;
import com.smartbatch360.api.common.NotFoundException;
import com.smartbatch360.api.order.SalesOrder;
import com.smartbatch360.api.order.SalesOrderRepository;
import com.smartbatch360.api.vehicle.Vehicle;
import com.smartbatch360.api.vehicle.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * The Production screen: pick an order, pick a vehicle, say how many cubic
 * metres, and start.
 *
 * Everything the batch needs beyond that comes from what was picked - the
 * customer, site and recipe from the order, the driver from the vehicle - so
 * the screen cannot produce a batch that disagrees with the order it is
 * fulfilling.
 */
@Service
@Transactional
public class ProductionService {

    private final SalesOrderRepository salesOrderRepository;
    private final VehicleRepository vehicleRepository;
    private final BatchRepository batchRepository;
    private final BatchPlanner batchPlanner;
    private final MixerCapacity mixerCapacity;
    private final ReportingZoneBatchNumber batchNumbering;

    public ProductionService(SalesOrderRepository salesOrderRepository, VehicleRepository vehicleRepository,
                             BatchRepository batchRepository, BatchPlanner batchPlanner,
                             MixerCapacity mixerCapacity, ReportingZoneBatchNumber batchNumbering) {
        this.salesOrderRepository = salesOrderRepository;
        this.vehicleRepository = vehicleRepository;
        this.batchRepository = batchRepository;
        this.batchPlanner = batchPlanner;
        this.mixerCapacity = mixerCapacity;
        this.batchNumbering = batchNumbering;
    }

    /** What the screen shows while the operator types a batch size. Changes nothing. */
    @Transactional(readOnly = true)
    public ProductionPlanResponse plan(ProductionPlanRequest request) {
        SalesOrder order = order(request.orderId());
        BatchPlan plan = batchPlanner.plan(order.getRecipe(), request.batchSizeM3());

        return ProductionPlanResponse.of(
                order.getId(),
                order.getClient().getName(),
                order.getSite().getName(),
                order.getSite().getId(),
                order.getRecipe().getName(),
                order.getRecipe().getId(),
                order.getQuantityKg(),
                remainingOn(order),
                mixerCapacity.capacityM3(),
                plan);
    }

    /**
     * Creates the batch the plan describes and puts it in progress. The
     * materials carry the planned per-cycle setpoint; what each cycle actually
     * weighs arrives afterwards from the PLC.
     */
    public BatchResponse start(StartProductionRequest request) {
        SalesOrder order = order(request.orderId());
        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> NotFoundException.forId("Vehicle", request.vehicleId()));

        BatchPlan plan = batchPlanner.plan(order.getRecipe(), request.batchSizeM3());
        String batchNumber = resolveBatchNumber(request.batchNumber());

        Batch batch = new Batch();
        batch.setBatchNumber(batchNumber);
        batch.setOrder(order);
        batch.setClient(order.getClient());
        batch.setSite(order.getSite());
        batch.setRecipe(order.getRecipe());
        batch.setVehicle(vehicle);
        batch.setDriver(vehicle.getDriver());
        batch.setTargetQuantity(plan.totalKg());
        batch.setProducedQuantity(BigDecimal.ZERO);
        batch.setCycleDateTime(Instant.now());
        batch.setCycleNumber(plan.cycles());
        batch.setShift(request.shift() == null || request.shift().isBlank() ? null : request.shift().trim());
        batch.setStatus(BatchStatus.IN_PROGRESS);

        int order_ = 0;
        for (BatchPlan.MaterialSetpoint setpoint : plan.materials()) {
            BatchMaterial material = new BatchMaterial();
            material.setBatch(batch);
            material.setMaterialName(setpoint.materialName());
            // Target is what the whole load needs; setpoint is what one cycle is
            // set to. The report prints both rows above the cycles.
            material.setTarget(setpoint.totalKg());
            material.setSetpoint(setpoint.perCycleKg());
            material.setAchieved(BigDecimal.ZERO);
            material.setUnit("kg");
            material.setDisplayOrder(order_++);
            batch.getMaterials().add(material);
        }

        return BatchResponse.from(batchRepository.save(batch));
    }

    private SalesOrder order(Long orderId) {
        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> NotFoundException.forId("Order", orderId));
        if (order.getRecipe() == null) {
            throw new InvalidRequestException("Order #" + orderId + " has no recipe, so nothing can be produced "
                    + "against it.");
        }
        return order;
    }

    /** Ordered minus produced, never negative - the same rule the Orders screen shows. */
    private BigDecimal remainingOn(SalesOrder order) {
        BigDecimal produced = batchRepository.sumProducedQuantityForOrder(order.getId());
        BigDecimal remaining = order.getQuantityKg().subtract(produced);
        return remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining;
    }

    private String resolveBatchNumber(String requested) {
        if (requested != null && !requested.isBlank()) {
            String trimmed = requested.trim();
            if (batchRepository.existsByBatchNumberIgnoreCase(trimmed)) {
                throw new InvalidRequestException("A batch numbered '" + trimmed + "' already exists.");
            }
            return trimmed;
        }
        return batchNumbering.next();
    }

    /** Date-based batch numbering, kept separate so the scheme is one thing to change. */
    @Service
    public static class ReportingZoneBatchNumber {

        private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyMMdd");

        private final BatchRepository batchRepository;
        private final com.smartbatch360.api.common.ReportingZone reportingZone;

        public ReportingZoneBatchNumber(BatchRepository batchRepository,
                                        com.smartbatch360.api.common.ReportingZone reportingZone) {
            this.batchRepository = batchRepository;
            this.reportingZone = reportingZone;
        }

        /**
         * yyMMdd plus a counter for the day, in the plant's own dates - so the
         * number reads as the day the plant made it, not a UTC day that rolls
         * over mid-shift.
         */
        public String next() {
            String prefix = DAY.format(reportingZone.dayOf(Instant.now()));
            for (int sequence = 1; sequence <= 999; sequence++) {
                String candidate = prefix + String.format("%03d", sequence);
                if (!batchRepository.existsByBatchNumberIgnoreCase(candidate)) {
                    return candidate;
                }
            }
            throw new InvalidRequestException("999 batches have already been numbered today. Enter a batch number "
                    + "by hand.");
        }
    }
}
