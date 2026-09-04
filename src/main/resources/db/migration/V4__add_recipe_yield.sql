ALTER TABLE recipe
    ADD COLUMN yield_quantity DECIMAL(12,3) NULL,
    ADD COLUMN yield_min DECIMAL(12,3) NULL,
    ADD COLUMN yield_max DECIMAL(12,3) NULL,
    ADD COLUMN yield_unit VARCHAR(20) NULL,
    ADD COLUMN yield_display VARCHAR(160) NULL,
    ADD CONSTRAINT chk_recipe_yield_quantity
        CHECK (yield_quantity IS NULL OR yield_quantity >= 0),
    ADD CONSTRAINT chk_recipe_yield_min
        CHECK (yield_min IS NULL OR yield_min >= 0),
    ADD CONSTRAINT chk_recipe_yield_max
        CHECK (yield_max IS NULL OR yield_max >= 0),
    ADD CONSTRAINT chk_recipe_yield_range
        CHECK (
            yield_min IS NULL
            OR yield_max IS NULL
            OR yield_min <= yield_max
        ),
    ADD CONSTRAINT chk_recipe_yield_unit
        CHECK (
            yield_unit IS NULL
            OR yield_unit IN (
                'SERVING',
                'UNIT',
                'GRAM',
                'KILOGRAM',
                'MILLILITER',
                'LITER',
                'RECIPE',
                'OTHER'
            )
        );
