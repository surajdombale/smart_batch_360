package com.smartbatch360.api.batch;

import com.smartbatch360.api.client.Client;
import com.smartbatch360.api.client.ClientRepository;
import com.smartbatch360.api.common.InvalidRequestException;
import com.smartbatch360.api.common.NotFoundException;
import com.smartbatch360.api.batch.dto.BatchMaterialRequest;
import com.smartbatch360.api.batch.dto.BatchRequest;
import com.smartbatch360.api.driver.Driver;
import com.smartbatch360.api.driver.DriverRepository;
import com.smartbatch360.api.order.SalesOrder;
import com.smartbatch360.api.order.SalesOrderRepository;
import com.smartbatch360.api.recipe.Recipe;
import com.smartbatch360.api.recipe.RecipeRepository;
import com.smartbatch360.api.site.Site;
import com.smartbatch360.api.site.SiteRepository;
import com.smartbatch360.api.vehicle.Vehicle;
import com.smartbatch360.api.vehicle.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Batch -> Order linking (2026-09-05). The link is optional, but when it is
 * present the batch must actually belong to that order: a mismatched recipe,
 * customer or site would corrupt both the order's fulfilment figures and its
 * material consumption, which is derived from the order's recipe.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BatchOrderLinkTest {

    @Mock private BatchRepository batchRepository;
    @Mock private RecipeRepository recipeRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private SiteRepository siteRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private DriverRepository driverRepository;
    @Mock private SalesOrderRepository salesOrderRepository;

    private Recipe recipe;
    private Client client;
    private Site site;

    private BatchService service() {
        return new BatchService(batchRepository, recipeRepository, clientRepository, siteRepository,
                vehicleRepository, driverRepository, salesOrderRepository);
    }

    /** Ids aren't settable (generated), so stub identity through the repositories. */
    private <T> T withId(T entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return entity;
    }

    @BeforeEach
    void setUp() {
        recipe = withId(new Recipe(), 1L);
        recipe.setName("M20");
        client = withId(new Client(), 2L);
        client.setName("Client A");
        site = withId(new Site(), 3L);
        site.setName("Kharadi");
        site.setClient(client);

        Vehicle vehicle = withId(new Vehicle(), 4L);
        Driver driver = withId(new Driver(), 5L);

        when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
        when(clientRepository.findById(2L)).thenReturn(Optional.of(client));
        when(siteRepository.findById(3L)).thenReturn(Optional.of(site));
        when(vehicleRepository.findById(4L)).thenReturn(Optional.of(vehicle));
        when(driverRepository.findById(5L)).thenReturn(Optional.of(driver));
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private SalesOrder order(Recipe forRecipe, Client forClient, Site forSite) {
        SalesOrder order = withId(new SalesOrder(), 9L);
        order.setRecipe(forRecipe);
        order.setClient(forClient);
        order.setSite(forSite);
        order.setQuantityKg(new BigDecimal("25"));
        when(salesOrderRepository.findById(9L)).thenReturn(Optional.of(order));
        return order;
    }

    private BatchRequest request(Long orderId) {
        return new BatchRequest("250900", 1L, orderId, 2L, 3L, 4L, 5L,
                new BigDecimal("3.00"), BigDecimal.ZERO, null, 1, "Day",
                BatchStatus.PENDING, EquipmentStatus.STOPPED, EquipmentStatus.STOPPED,
                EquipmentStatus.STOPPED, EquipmentStatus.STOPPED, EquipmentStatus.STOPPED,
                List.of(new BatchMaterialRequest("Cement", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, "kg")));
    }

    @Test
    void batchWithoutAnOrderIsStillAllowed() {
        assertThat(service().create(request(null)).orderId()).isNull();
    }

    @Test
    void batchLinksToAMatchingOrder() {
        order(recipe, client, site);
        assertThat(service().create(request(9L)).orderId()).isEqualTo(9L);
    }

    @Test
    void rejectsOrderForADifferentRecipe() {
        Recipe other = withId(new Recipe(), 99L);
        other.setName("M25");
        order(other, client, site);

        assertThatThrownBy(() -> service().create(request(9L)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("M25");
        verify(batchRepository, never()).save(any());
    }

    @Test
    void rejectsOrderForADifferentCustomer() {
        Client other = withId(new Client(), 99L);
        other.setName("Other Co");
        order(recipe, other, site);

        assertThatThrownBy(() -> service().create(request(9L)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Other Co");
    }

    @Test
    void rejectsOrderForADifferentSite() {
        Site other = withId(new Site(), 99L);
        other.setName("Hinjewadi");
        order(recipe, client, other);

        assertThatThrownBy(() -> service().create(request(9L)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Hinjewadi");
    }

    @Test
    void rejectsUnknownOrder() {
        when(salesOrderRepository.findById(77L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().create(request(77L)))
                .isInstanceOf(NotFoundException.class);
    }
}
