package com.smartbatch360.api.order;

import com.smartbatch360.api.batch.Batch;
import com.smartbatch360.api.batch.BatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * An order's status following its production.
 *
 * The Start and Fulfil buttons left the Orders screen on 2026-10-03, so nothing
 * moved an order at all: one could be produced in full and still read
 * UNFULFILLED. Asked for on 28-Oct-2026: starting production puts an order in
 * progress, and finishing the batch that covers it fulfils it.
 *
 * Every move goes through OrderStatus.canTransitionTo, so the lifecycle stays
 * as enforced as it was when people drove it by hand.
 */
@Service
@Transactional
public class OrderProgress {

    private final BatchRepository batchRepository;

    public OrderProgress(BatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    /** A batch has been started against the order: UNFULFILLED becomes IN_PROGRESS. */
    public void productionStarted(SalesOrder order) {
        if (order != null && order.getStatus() == OrderStatus.UNFULFILLED) {
            order.setStatus(OrderStatus.IN_PROGRESS);
        }
    }

    /**
     * A batch against the order has completed. The order is FULFILLED once its
     * finished batches cover the ordered quantity.
     *
     * A completed batch counts at what it was planned to deliver, not at what
     * the scales read. Weighing lands a few kilograms either side of the plan,
     * so judged by weight alone half the orders produced in full would stay
     * open for a shortfall nobody would send another truck for.
     */
    public void batchCompleted(Batch batch) {
        SalesOrder order = batch.getOrder();
        if (order == null) {
            return;
        }
        // An order produced against before its status followed production
        // has to pass through IN_PROGRESS on the way, as the lifecycle requires.
        productionStarted(order);

        BigDecimal delivered = batchRepository.sumDeliveredForOrder(order.getId());
        if (delivered.compareTo(order.getQuantityKg()) >= 0
                && order.getStatus().canTransitionTo(OrderStatus.FULFILLED)) {
            order.setStatus(OrderStatus.FULFILLED);
        }
    }
}
