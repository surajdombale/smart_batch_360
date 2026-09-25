package com.smartbatch360.api.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    /**
     * Every order with the three names the list shows. The associations are
     * lazy, and the list reads customer, site and recipe names, so a plain
     * findAll() fetched each one on demand - one query per distinct row.
     */
    @Query("SELECT o FROM SalesOrder o "
            + "JOIN FETCH o.client JOIN FETCH o.site JOIN FETCH o.recipe ORDER BY o.id")
    List<SalesOrder> findAllForList();

    boolean existsByClientId(Long clientId);

    boolean existsBySiteId(Long siteId);

    boolean existsByRecipeId(Long recipeId);
}
