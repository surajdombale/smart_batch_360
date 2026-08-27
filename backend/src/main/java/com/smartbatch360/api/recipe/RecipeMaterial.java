package com.smartbatch360.api.recipe;

import com.smartbatch360.api.material.Material;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * One line of a Recipe's material list: which Material, and how much of it
 * (in that material's own unit).
 *
 * As of 2026-08-27 this references a Material record rather than repeating the
 * material's name and unit as free text - those now live on Material alone, so
 * they cannot drift between recipes.
 */
@Entity
@Table(name = "recipe_material")
public class RecipeMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @ManyToOne(optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "quantity", nullable = false, precision = 8, scale = 2)
    private BigDecimal quantity;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    /** This line's contribution to the recipe's total batch quantity, in m3. */
    public BigDecimal toCubicMetres() {
        return material.toCubicMetres(quantity);
    }

    public Long getId() {
        return id;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
