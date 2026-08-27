package com.smartbatch360.api.order;

import com.smartbatch360.api.client.Client;
import com.smartbatch360.api.client.ClientRepository;
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

import java.util.List;

@Service
@Transactional
public class SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final ClientRepository clientRepository;
    private final SiteRepository siteRepository;
    private final RecipeRepository recipeRepository;

    public SalesOrderService(SalesOrderRepository salesOrderRepository, ClientRepository clientRepository,
                              SiteRepository siteRepository, RecipeRepository recipeRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.clientRepository = clientRepository;
        this.siteRepository = siteRepository;
        this.recipeRepository = recipeRepository;
    }

    @Transactional(readOnly = true)
    public List<SalesOrderResponse> findAll() {
        return salesOrderRepository.findAll().stream()
                .map(SalesOrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SalesOrderResponse findById(Long id) {
        return SalesOrderResponse.from(getOrThrow(id));
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
        order.setQuantityM3(request.quantityM3());
        order.setStatus(OrderStatus.UNFULFILLED);
        return SalesOrderResponse.from(salesOrderRepository.save(order));
    }

    public void delete(Long id) {
        salesOrderRepository.delete(getOrThrow(id));
    }

    SalesOrder getOrThrow(Long id) {
        return salesOrderRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forId("Order", id));
    }
}
