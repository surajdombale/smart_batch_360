package com.smartbatch360.api.order;

import com.smartbatch360.api.client.Client;
import com.smartbatch360.api.recipe.Recipe;
import com.smartbatch360.api.site.Site;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A sales order: a client wants some quantity of a given recipe, delivered to
 * one of their sites. Added 2026-08-27.
 *
 * Named SalesOrder rather than Order because ORDER is a SQL reserved word -
 * the table is sales_order for the same reason.
 */
@Entity
@Table(name = "sales_order")
public class SalesOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(name = "quantity_m3", nullable = false, precision = 12, scale = 4)
    private BigDecimal quantityM3;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = OrderStatus.UNFULFILLED;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public BigDecimal getQuantityM3() {
        return quantityM3;
    }

    public void setQuantityM3(BigDecimal quantityM3) {
        this.quantityM3 = quantityM3;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
