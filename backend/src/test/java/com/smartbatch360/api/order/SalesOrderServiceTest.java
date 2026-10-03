package com.smartbatch360.api.order;

import com.smartbatch360.api.client.Client;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Order lifecycle transitions (added 2026-08-28). Unlike Batch's deliberately
 * permissive controls, these are enforced - an order's status is a business
 * record, so illegal moves are rejected rather than quietly accepted.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SalesOrderServiceTest {

    @Mock private SalesOrderRepository salesOrderRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private SiteRepository siteRepository;
    @Mock private RecipeRepository recipeRepository;
    @Mock private com.smartbatch360.api.batch.BatchRepository batchRepository;

    private SalesOrderService service() {
        return new SalesOrderService(salesOrderRepository, clientRepository, siteRepository, recipeRepository,
                batchRepository);
    }

    private SalesOrder orderWith(OrderStatus status) {
        Client client = new Client();
        client.setName("Client A");
        Site site = new Site();
        site.setName("Kharadi");
        site.setClient(client);
        Recipe recipe = new Recipe();
        recipe.setName("M20");
        recipe.setTotalBatchQuantityKg(new BigDecimal("0.2072"));

        setId(client, 1L);
        setId(site, 2L);
        setId(recipe, 3L);

        SalesOrder order = new SalesOrder();
        order.setClient(client);
        order.setSite(site);
        order.setRecipe(recipe);
        order.setQuantityKg(new BigDecimal("10"));
        order.setStatus(status);

        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(salesOrderRepository.save(any(SalesOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(batchRepository.sumProducedQuantityForOrder(any())).thenReturn(BigDecimal.ZERO);
        return order;
    }

    /**
     * Editing a placed order, added with the Orders screen's Edit button.
     * Everything is changeable while nothing has been produced against it.
     */
    @Test
    void updateChangesAnOrderThatHasNoBatchesYet() {
        SalesOrder order = orderWith(OrderStatus.UNFULFILLED);
        when(batchRepository.existsByOrderId(1L)).thenReturn(false);
        stubReferences();

        SalesOrderResponse response = service().update(1L,
                new SalesOrderRequest(7L, 8L, 9L, new BigDecimal("250.00")));

        assertThat(response.quantityKg()).isEqualByComparingTo("250.00");
        assertThat(order.getRecipe().getName()).isEqualTo("M30");
    }

    /**
     * The guard that matters: a batch may only name an order whose recipe,
     * customer and site match its own, so changing those on an order that has
     * already been produced against would invalidate the batches and the
     * produced figures with them.
     */
    @Test
    void updateRefusesToChangeTheRecipeOnceBatchesExist() {
        SalesOrder order = orderWith(OrderStatus.IN_PROGRESS);
        when(batchRepository.existsByOrderId(1L)).thenReturn(true);
        // Same customer and site, different recipe - so the guard must name the recipe.
        when(clientRepository.findById(1L)).thenReturn(Optional.of(order.getClient()));
        when(siteRepository.findById(2L)).thenReturn(Optional.of(order.getSite()));
        Recipe otherRecipe = new Recipe();
        otherRecipe.setName("M30");
        setId(otherRecipe, 9L);
        when(recipeRepository.findById(9L)).thenReturn(Optional.of(otherRecipe));

        assertThatThrownBy(() -> service().update(1L,
                new SalesOrderRequest(1L, 2L, 9L, new BigDecimal("250.00"))))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("production batches recorded against it")
                .hasMessageContaining("recipe");
    }

    /** The quantity may still be corrected on an order already in production. */
    @Test
    void updateAllowsTheQuantityToChangeOnceBatchesExist() {
        SalesOrder order = orderWith(OrderStatus.IN_PROGRESS);
        when(batchRepository.existsByOrderId(1L)).thenReturn(true);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(order.getClient()));
        when(siteRepository.findById(2L)).thenReturn(Optional.of(order.getSite()));
        when(recipeRepository.findById(3L)).thenReturn(Optional.of(order.getRecipe()));

        SalesOrderResponse response = service().update(1L,
                new SalesOrderRequest(1L, 2L, 3L, new BigDecimal("77.00")));

        assertThat(response.quantityKg()).isEqualByComparingTo("77.00");
    }

    /** A site still has to belong to the customer it is ordered for. */
    @Test
    void updateRefusesASiteThatBelongsToAnotherCustomer() {
        orderWith(OrderStatus.UNFULFILLED);
        when(batchRepository.existsByOrderId(1L)).thenReturn(false);
        Client otherClient = new Client();
        setId(otherClient, 99L);
        otherClient.setName("Someone Else");
        Site otherSite = new Site();
        setId(otherSite, 8L);
        otherSite.setName("Wrong Site");
        otherSite.setClient(otherClient);
        Client client = new Client();
        setId(client, 7L);
        client.setName("Client A");
        when(clientRepository.findById(7L)).thenReturn(Optional.of(client));
        when(siteRepository.findById(8L)).thenReturn(Optional.of(otherSite));
        when(recipeRepository.findById(9L)).thenReturn(Optional.of(new Recipe()));

        assertThatThrownBy(() -> service().update(1L,
                new SalesOrderRequest(7L, 8L, 9L, new BigDecimal("250.00"))))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("does not belong to customer");
    }

    /** Ids are generated, so there is no setter - the tests need them set anyway. */
    private static void setId(Object entity, long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }

    /** References the update tests point at: a different customer, site and recipe. */
    private void stubReferences() {
        Client client = new Client();
        setId(client, 7L);
        client.setName("Client B");
        Site site = new Site();
        setId(site, 8L);
        site.setName("Hinjewadi");
        site.setClient(client);
        Recipe recipe = new Recipe();
        setId(recipe, 9L);
        recipe.setName("M30");
        recipe.setTotalBatchQuantityKg(new BigDecimal("1000"));
        when(clientRepository.findById(7L)).thenReturn(Optional.of(client));
        when(siteRepository.findById(8L)).thenReturn(Optional.of(site));
        when(recipeRepository.findById(9L)).thenReturn(Optional.of(recipe));
    }

    @Test
    void startMovesUnfulfilledToInProgress() {
        orderWith(OrderStatus.UNFULFILLED);
        SalesOrderResponse response = service().start(1L);
        assertThat(response.status()).isEqualTo(OrderStatus.IN_PROGRESS);
    }

    @Test
    void fulfilMovesInProgressToFulfilled() {
        orderWith(OrderStatus.IN_PROGRESS);
        assertThat(service().fulfil(1L).status()).isEqualTo(OrderStatus.FULFILLED);
    }

    @Test
    void cancelIsAllowedFromUnfulfilledAndInProgress() {
        orderWith(OrderStatus.UNFULFILLED);
        assertThat(service().cancel(1L).status()).isEqualTo(OrderStatus.CANCELLED);

        orderWith(OrderStatus.IN_PROGRESS);
        assertThat(service().cancel(1L).status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cannotFulfilAnOrderThatHasNotStarted() {
        orderWith(OrderStatus.UNFULFILLED);
        assertThatThrownBy(() -> service().fulfil(1L))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("UNFULFILLED");
        verify(salesOrderRepository, never()).save(any());
    }

    @Test
    void terminalStatesCannotBeLeft() {
        orderWith(OrderStatus.FULFILLED);
        assertThatThrownBy(() -> service().start(1L))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("final");

        orderWith(OrderStatus.CANCELLED);
        assertThatThrownBy(() -> service().fulfil(1L))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("final");
    }

    @Test
    void repeatingTheCurrentStatusIsRejected() {
        orderWith(OrderStatus.IN_PROGRESS);
        assertThatThrownBy(() -> service().start(1L))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("already");
    }

    @Test
    void inProgressOrderCannotBeDeleted() {
        orderWith(OrderStatus.IN_PROGRESS);
        assertThatThrownBy(() -> service().delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cancel it instead");
        verify(salesOrderRepository, never()).delete(any());
    }

    @Test
    void unfulfilledOrderCanStillBeDeleted() {
        SalesOrder order = orderWith(OrderStatus.UNFULFILLED);
        service().delete(1L);
        verify(salesOrderRepository).delete(order);
    }

    @Test
    void transitionOnMissingOrderIsNotFound() {
        when(salesOrderRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().start(99L)).isInstanceOf(NotFoundException.class);
    }
}
