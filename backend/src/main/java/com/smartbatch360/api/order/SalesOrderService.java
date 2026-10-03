package com.smartbatch360.api.order;

import com.smartbatch360.api.client.Client;
import com.smartbatch360.api.batch.BatchRepository;
import com.smartbatch360.api.batch.OrderProducedQuantity;
import com.smartbatch360.api.client.ClientRepository;
import com.smartbatch360.api.common.ConflictException;
import com.smartbatch360.api.common.InvalidRequestException;
import com.smartbatch360.api.common.NotFoundException;
import com.smartbatch360.api.order.dto.SalesOrderRequest;
import com.smartbatch360.api.order.dto.SalesOrderResponse;
import com.smartbatch360.api.recipe.Recipe;
import com.smartbatch360.api.recipe.RecipeRepository;
import com.smartbatch360.api.site.Site;
import com.smartbatch360.api.site.SiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final ClientRepository clientRepository;
    private final SiteRepository siteRepository;
    private final RecipeRepository recipeRepository;
    private final BatchRepository batchRepository;

    public SalesOrderService(SalesOrderRepository salesOrderRepository, ClientRepository clientRepository,
                              SiteRepository siteRepository, RecipeRepository recipeRepository,
                              BatchRepository batchRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.clientRepository = clientRepository;
        this.siteRepository = siteRepository;
        this.recipeRepository = recipeRepository;
        this.batchRepository = batchRepository;
    }

    /** Wraps an order with how much has actually been produced against it. */
    private SalesOrderResponse withFulfilment(SalesOrder order) {
        return SalesOrderResponse.from(order, batchRepository.sumProducedQuantityForOrder(order.getId()));
    }

    /**
     * The whole order list. Fulfilment is summed for every order in one grouped
     * query rather than per order: the per-order sum is fine for a single order,
     * but on the list it cost one query each, so the screen got slower with
     * every order the plant took.
     */
    @Transactional(readOnly = true)
    public List<SalesOrderResponse> findAll() {
        Map<Long, BigDecimal> producedByOrder = batchRepository.sumProducedQuantityByOrder().stream()
                .collect(Collectors.toMap(OrderProducedQuantity::orderId,
                        OrderProducedQuantity::producedQuantity));
        return salesOrderRepository.findAllForList().stream()
                .map(order -> SalesOrderResponse.from(order,
                        producedByOrder.getOrDefault(order.getId(), BigDecimal.ZERO)))
                .toList();
    }

    @Transactional(readOnly = true)
    public SalesOrderResponse findById(Long id) {
        return withFulfilment(getOrThrow(id));
    }

    public SalesOrderResponse create(SalesOrderRequest request) {
        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> NotFoundException.forId("Client", request.clientId()));
        Site site = siteRepository.findById(request.siteId())
                .orElseThrow(() -> NotFoundException.forId("Site", request.siteId()));
        Recipe recipe = recipeRepository.findById(request.recipeId())
                .orElseThrow(() -> NotFoundException.forId("Recipe", request.recipeId()));

        // A site belongs to exactly one client; ordering to someone else's site
        // would produce an order nobody can actually fulfil.
        if (!site.getClient().getId().equals(client.getId())) {
            throw new InvalidRequestException("Site '" + site.getName() + "' does not belong to customer '"
                    + client.getName() + "'.");
        }

        SalesOrder order = new SalesOrder();
        order.setClient(client);
        order.setSite(site);
        order.setRecipe(recipe);
        order.setQuantityKg(request.quantityKg());
        order.setStatus(OrderStatus.UNFULFILLED);
        return SalesOrderResponse.from(salesOrderRepository.save(order));
    }

    /**
     * Changes an order that has already been placed. Added 2026-10-03 at the
     * user's request ("add edit button" on the Orders screen).
     *
     * An order with batches recorded against it may only have its quantity
     * changed. The batch-to-order link requires the batch's recipe, customer and
     * site to match the order's, so moving a fulfilled order to a different
     * recipe or customer would silently invalidate every batch already produced
     * against it - and the produced figures with them.
     */
    public SalesOrderResponse update(Long id, SalesOrderRequest request) {
        SalesOrder order = getOrThrow(id);

        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> NotFoundException.forId("Client", request.clientId()));
        Site site = siteRepository.findById(request.siteId())
                .orElseThrow(() -> NotFoundException.forId("Site", request.siteId()));
        Recipe recipe = recipeRepository.findById(request.recipeId())
                .orElseThrow(() -> NotFoundException.forId("Recipe", request.recipeId()));

        if (!Objects.equals(site.getClient().getId(), client.getId())) {
            throw new InvalidRequestException("Site '" + site.getName() + "' does not belong to customer '"
                    + client.getName() + "'.");
        }

        if (batchRepository.existsByOrderId(id)) {
            String changed = whatChanged(order, client, site, recipe);
            if (changed != null) {
                throw new InvalidRequestException("Order #" + id + " has production batches recorded against it, so "
                        + "its " + changed + " cannot be changed. Only the quantity can.");
            }
        }

        order.setClient(client);
        order.setSite(site);
        order.setRecipe(recipe);
        order.setQuantityKg(request.quantityKg());
        return withFulfilment(salesOrderRepository.save(order));
    }

    /** Names the first of customer/site/recipe that this request would change, or null. */
    private String whatChanged(SalesOrder order, Client client, Site site, Recipe recipe) {
        if (!Objects.equals(order.getClient().getId(), client.getId())) {
            return "customer";
        }
        if (!Objects.equals(order.getSite().getId(), site.getId())) {
            return "site";
        }
        if (!Objects.equals(order.getRecipe().getId(), recipe.getId())) {
            return "recipe";
        }
        return null;
    }

    /** UNFULFILLED -> IN_PROGRESS: production has started against the order. */
    public SalesOrderResponse start(Long id) {
        return transition(id, OrderStatus.IN_PROGRESS);
    }

    /** IN_PROGRESS -> FULFILLED: fully delivered. Terminal. */
    public SalesOrderResponse fulfil(Long id) {
        return transition(id, OrderStatus.FULFILLED);
    }

    /** UNFULFILLED or IN_PROGRESS -> CANCELLED. Terminal. */
    public SalesOrderResponse cancel(Long id) {
        return transition(id, OrderStatus.CANCELLED);
    }

    /**
     * Applies a status change, refusing anything the lifecycle doesn't allow.
     * Enforced rather than permissive - see OrderStatus's note on why this
     * differs from Batch's controls.
     */
    private SalesOrderResponse transition(Long id, OrderStatus target) {
        SalesOrder order = getOrThrow(id);
        OrderStatus current = order.getStatus();
        if (current == target) {
            throw new InvalidRequestException("Order #" + id + " is already " + target + ".");
        }
        if (!current.canTransitionTo(target)) {
            throw new InvalidRequestException("Order #" + id + " is " + current
                    + (current.isTerminal() ? ", which is final, so it" : ", so it")
                    + " cannot be moved to " + target + ".");
        }
        order.setStatus(target);
        return withFulfilment(salesOrderRepository.save(order));
    }

    public void delete(Long id) {
        SalesOrder order = getOrThrow(id);
        // An order that production has already started against is history, not
        // a mistake to be erased - cancel it instead.
        if (order.getStatus() == OrderStatus.IN_PROGRESS) {
            throw new ConflictException("Order #" + id + " is in progress and cannot be deleted. Cancel it instead.");
        }
        // Batches record what was actually produced; deleting the order they
        // point at would orphan that history.
        if (batchRepository.existsByOrderId(id)) {
            throw new ConflictException("Order #" + id
                    + " has production batches recorded against it and cannot be deleted.");
        }
        salesOrderRepository.delete(order);
    }

    SalesOrder getOrThrow(Long id) {
        return salesOrderRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forId("Order", id));
    }
}
