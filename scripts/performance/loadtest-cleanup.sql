USE cloud_taste_loadtest;

-- Only remove rows created by the load-test data script.
DELETE FROM order_detail WHERE id >= 2000001;
DELETE FROM orders WHERE id >= 1000001;
DELETE FROM shopping_cart WHERE id >= 30001;
DELETE FROM address_book WHERE id >= 10001;
DELETE FROM user WHERE id >= 10001;

SELECT 'LOADTEST_ROWS_REMOVED' AS result;
