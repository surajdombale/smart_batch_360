package com.smartbatch360.api.order;

import com.smartbatch360.api.client.Client;
import com.smartbatch360.api.client.ClientRepository;
import com.smartbatch360.api.common.ConflictException;
import com.smartbatch360.api.common.InvalidRequestException;
import com.smartbatch360.api.common.NotFoundException;
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

    private SalesOrderService service() {
        return new SalesOrderService(salesOrderRepository, clientRepository, siteRepository, recipeRepository);
    }

    private SalesOrder orderWith(OrderStatus status) {
        Client client = new Client();
        client.setName("Client A");
        Site site = new Site();
        site.setName("Kharadi");
        site.setClient(client);
        Recipe recipe = new Recipe();
        recipe.setName("M20");
        recipe.setTotalBatchQuantityM3(new BigDecimal("0.2072"));

        SalesOrder order = new SalesOrder();
        order.setClient(client);
        order.setSite(site);
        order.setRecipe(recipe);
        order.setQuantityM3(new BigDecimal("10"));
        order.setStatus(status);

        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(salesOrderRepository.save(any(SalesOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        return order;
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
