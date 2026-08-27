package com.smartbatch360.api.order;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    boolean existsByClientId(Long clientId);

    boolean existsBySiteId(Long siteId);

    boolean existsByRecipeId(Long recipeId);
}
