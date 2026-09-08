package com.elrecetariodeshir.backend.recipe;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "recipe_ingredient")
public class RecipeIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(nullable = false)
    private int position;

    @Column(name = "ingredient_name", length = 300)
    private String ingredientName;

    @Column(precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "quantity_max", precision = 12, scale = 3)
    private BigDecimal quantityMax;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RecipeIngredientUnit unit;

    @Column(length = 500)
    private String notes;

    @Column(name = "text", nullable = false, length = 1000)
    private String displayText;

    protected RecipeIngredient() {
    }

    public RecipeIngredient(int position, String displayText) {
        this.position = position;
        this.displayText = displayText;
    }

    public RecipeIngredient(
            int position,
            String ingredientName,
            BigDecimal quantity,
            BigDecimal quantityMax,
            RecipeIngredientUnit unit,
            String notes,
            String displayText) {
        this.position = position;
        this.ingredientName = ingredientName;
        this.quantity = quantity;
        this.quantityMax = quantityMax;
        this.unit = unit;
        this.notes = notes;
        this.displayText = displayText;
    }

    public Long getId() {
        return id;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getQuantityMax() {
        return quantityMax;
    }

    public void setQuantityMax(BigDecimal quantityMax) {
        this.quantityMax = quantityMax;
    }

    public RecipeIngredientUnit getUnit() {
        return unit;
    }

    public void setUnit(RecipeIngredientUnit unit) {
        this.unit = unit;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public String getText() {
        return displayText;
    }

    public void setText(String text) {
        this.displayText = text;
    }
}
