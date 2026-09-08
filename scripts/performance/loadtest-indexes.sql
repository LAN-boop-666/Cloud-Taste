USE cloud_taste_loadtest;

-- These indexes are intentionally applied only to the isolated load-test database.
-- Validate them with EXPLAIN before considering any production schema change.
-- The order, order_detail, and shopping_cart indexes are now part of
-- database/cloud_taste.sql and are created when the isolated schema loads.
CREATE INDEX idx_address_book_user_id ON address_book (user_id);
CREATE INDEX idx_dish_category_status ON dish (category_id, status);
CREATE INDEX idx_setmeal_category_status ON setmeal (category_id, status);
CREATE INDEX idx_setmeal_dish_dish_id ON setmeal_dish (dish_id);

SELECT 'LOADTEST_INDEXES_READY' AS result;
