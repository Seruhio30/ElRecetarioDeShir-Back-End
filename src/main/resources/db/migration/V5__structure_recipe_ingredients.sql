ALTER TABLE recipe_ingredient
    ADD COLUMN ingredient_name VARCHAR(300) NULL AFTER position,
    ADD COLUMN quantity DECIMAL(12, 3) NULL AFTER ingredient_name,
    ADD COLUMN quantity_max DECIMAL(12, 3) NULL AFTER quantity,
    ADD COLUMN unit VARCHAR(20) NULL AFTER quantity_max,
    ADD COLUMN notes VARCHAR(500) NULL AFTER unit;

ALTER TABLE recipe_ingredient
    ADD CONSTRAINT chk_recipe_ingredient_quantity
        CHECK (quantity IS NULL OR quantity >= 0),
    ADD CONSTRAINT chk_recipe_ingredient_quantity_max
        CHECK (quantity_max IS NULL OR quantity_max >= 0),
    ADD CONSTRAINT chk_recipe_ingredient_quantity_range
        CHECK (
            quantity IS NULL
            OR quantity_max IS NULL
            OR quantity_max >= quantity
        ),
    ADD CONSTRAINT chk_recipe_ingredient_unit
        CHECK (
            unit IS NULL
            OR unit IN (
                'GRAM',
                'KILOGRAM',
                'MILLILITER',
                'LITER',
                'UNIT',
                'CUP',
                'TABLESPOON',
                'TEASPOON',
                'PACKAGE',
                'PORTION',
                'RECIPE',
                'BRANCH',
                'STALK',
                'OTHER'
            )
        );
