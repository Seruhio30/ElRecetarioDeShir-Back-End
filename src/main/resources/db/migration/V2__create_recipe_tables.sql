CREATE TABLE recipe (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slug VARCHAR(160) NOT NULL,
    name VARCHAR(200) NOT NULL,
    cuisine VARCHAR(120) NULL,
    category VARCHAR(30) NOT NULL,
    country_code CHAR(2) NOT NULL,
    type VARCHAR(30) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    time INT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    published_at DATETIME(6) NULL,
    archived_at DATETIME(6) NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_recipe_slug UNIQUE (slug),

    CONSTRAINT chk_recipe_category
        CHECK (category IN ('NATIONAL', 'INTERNATIONAL')),

    CONSTRAINT chk_recipe_type
        CHECK (type IN (
            'MAIN_COURSE',
            'SIDE_DISH',
            'SAUCE',
            'BASE',
            'DESSERT',
            'DRINK'
        )),

    CONSTRAINT chk_recipe_difficulty
        CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),

    CONSTRAINT chk_recipe_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),

    CONSTRAINT chk_recipe_time
        CHECK (time IS NULL OR time >= 0),

    CONSTRAINT chk_recipe_country_code
        CHECK (CHAR_LENGTH(country_code) = 2)
);

CREATE TABLE recipe_ingredient (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipe_id BIGINT NOT NULL,
    position INT NOT NULL,
    text VARCHAR(1000) NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT uk_recipe_ingredient_position
        UNIQUE (recipe_id, position),

    CONSTRAINT chk_recipe_ingredient_position
        CHECK (position >= 0),

    CONSTRAINT fk_recipe_ingredient_recipe
        FOREIGN KEY (recipe_id)
        REFERENCES recipe (id)
        ON DELETE CASCADE
);

CREATE TABLE recipe_step (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipe_id BIGINT NOT NULL,
    position INT NOT NULL,
    instruction TEXT NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT uk_recipe_step_position
        UNIQUE (recipe_id, position),

    CONSTRAINT chk_recipe_step_position
        CHECK (position >= 0),

    CONSTRAINT fk_recipe_step_recipe
        FOREIGN KEY (recipe_id)
        REFERENCES recipe (id)
        ON DELETE CASCADE
);

CREATE TABLE recipe_image (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipe_id BIGINT NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255) NULL,
    media_type VARCHAR(120) NOT NULL,
    alt_text VARCHAR(500) NULL,
    position INT NOT NULL,
    primary_image BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT uk_recipe_image_position
        UNIQUE (recipe_id, position),

    CONSTRAINT chk_recipe_image_position
        CHECK (position >= 0),

    CONSTRAINT fk_recipe_image_recipe
        FOREIGN KEY (recipe_id)
        REFERENCES recipe (id)
        ON DELETE CASCADE
);
