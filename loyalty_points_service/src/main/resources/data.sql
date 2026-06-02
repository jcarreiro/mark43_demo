-- Test data for purchases table
INSERT INTO purchases (purchase_id, account_id, dollar_amount, timestamp)
VALUES
  ('order-122', 'account-100', 45.99, '2025-05-31 00:00:00'),
  ('order-123', 'account-100', 59.99, '2026-05-20 00:00:00'),
  ('order-124', 'account-100', 120.00, '2026-05-21 00:00:00'),
  ('order-125', 'account-101', 15.75, '2025-05-22 00:00:00'),
  ('order-126', 'account-101', 200.50, '2026-05-23 00:00:00');
