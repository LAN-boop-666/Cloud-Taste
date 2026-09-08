USE cloud_taste_loadtest;
SET NAMES utf8mb4;
SET SESSION cte_max_recursion_depth = 200000;
DELETE FROM order_detail WHERE id >= 2000001;
DELETE FROM orders WHERE id >= 1000001;
DELETE FROM shopping_cart WHERE id >= 30001;
DELETE FROM address_book WHERE id >= 10001;
DELETE FROM user WHERE id >= 10001;

INSERT INTO user (id, openid, name, phone, sex, id_number, avatar, create_time)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 1000
)
SELECT
    10000 + n,
    CONCAT('loadtest-openid-', LPAD(n, 4, '0')),
    CONCAT('loadtest-user-', LPAD(n, 4, '0')),
    CONCAT('139', LPAD(n, 8, '0')),
    IF(MOD(n, 2) = 0, '1', '0'),
    NULL,
    NULL,
    NOW() - INTERVAL MOD(n, 365) DAY
FROM seq;

INSERT INTO address_book (id, user_id, consignee, sex, phone, province_code, province_name,
                          city_code, city_name, district_code, district_name, detail, label, is_default)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 1000
)
SELECT
    10000 + n,
    10000 + n,
    CONCAT('loadtest-receiver-', LPAD(n, 4, '0')),
    IF(MOD(n, 2) = 0, '1', '0'),
    CONCAT('139', LPAD(n, 8, '0')),
    '110000',
    'Beijing',
    '110100',
    'Beijing',
    '110101',
    'Dongcheng',
    CONCAT('loadtest-address-', LPAD(n, 4, '0')),
    'home',
    1
FROM seq;

INSERT INTO shopping_cart (id, name, image, user_id, dish_id, setmeal_id, dish_flavor, number, amount, create_time)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 1000
)
SELECT
    30000 + n,
    CONCAT('loadtest-dish-', 46 + MOD(n, 24)),
    NULL,
    10000 + n,
    46 + MOD(n, 24),
    NULL,
    NULL,
    1 + MOD(n, 3),
    4.00 + MOD(n, 20),
    NOW()
FROM seq;

INSERT INTO orders
(id, number, status, user_id, address_book_id, order_time, checkout_time, pay_method, pay_status,
 amount, remark, phone, address, user_name, consignee, cancel_reason, rejection_reason, cancel_time,
 estimated_delivery_time, delivery_status, delivery_time, pack_amount, tableware_number, tableware_status)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 50000
)
SELECT
    1000000 + n,
    CONCAT('LOADTEST-', LPAD(n, 6, '0')),
    CASE MOD(n, 10)
        WHEN 0 THEN 1
        WHEN 1 THEN 2
        WHEN 2 THEN 3
        WHEN 3 THEN 4
        WHEN 4 THEN 5
        WHEN 5 THEN 5
        WHEN 6 THEN 5
        WHEN 7 THEN 6
        WHEN 8 THEN 7
        ELSE 5
    END,
    10000 + MOD(n - 1, 1000) + 1,
    10000 + MOD(n - 1, 1000) + 1,
    DATE_SUB(NOW(), INTERVAL MOD(n, 180) DAY) - INTERVAL MOD(n, 86400) SECOND,
    CASE WHEN MOD(n, 10) IN (0, 7) THEN NULL
         ELSE DATE_SUB(NOW(), INTERVAL MOD(n, 180) DAY) END,
    1,
    CASE WHEN MOD(n, 10) IN (0, 7) THEN 0 ELSE 1 END,
    10.00 + MOD(n, 200),
    'LOADTEST',
    CONCAT('139', LPAD(MOD(n - 1, 1000) + 1, 8, '0')),
    CONCAT('loadtest-address-', LPAD(MOD(n - 1, 1000) + 1, 4, '0')),
    CONCAT('loadtest-user-', LPAD(MOD(n - 1, 1000) + 1, 4, '0')),
    CONCAT('loadtest-receiver-', LPAD(MOD(n - 1, 1000) + 1, 4, '0')),
    CASE WHEN MOD(n, 10) IN (6, 7) THEN 'LOADTEST_CANCELLED' ELSE NULL END,
    CASE WHEN MOD(n, 10) = 7 THEN 'LOADTEST_REJECTED' ELSE NULL END,
    CASE WHEN MOD(n, 10) IN (6, 7) THEN DATE_SUB(NOW(), INTERVAL MOD(n, 180) DAY) ELSE NULL END,
    NULL,
    1,
    NULL,
    NULL,
    1,
    1
FROM seq;

INSERT INTO order_detail
(id, name, image, order_id, dish_id, setmeal_id, dish_flavor, number, amount)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 150000
)
SELECT
    2000000 + n,
    CONCAT('loadtest-dish-', 46 + MOD(n, 24)),
    NULL,
    1000001 + FLOOR((n - 1) / 3),
    46 + MOD(n, 24),
    NULL,
    NULL,
    1 + MOD(n, 3),
    4.00 + MOD(n, 20)
FROM seq;

SELECT 'LOADTEST_DATA_READY' AS result;
