-- Seed accounts
INSERT INTO account (id, account_number, owner_name, account_type, balance, currency, status, created_at)
VALUES
  (1, 'ACC-001-CHK', 'Alice Johnson',  'CHECKING', 5000.00, 'USD', 'ACTIVE', NOW()),
  (2, 'ACC-002-SAV', 'Alice Johnson',  'SAVINGS',  12000.00,'USD', 'ACTIVE', NOW()),
  (3, 'ACC-003-CHK', 'Bob Smith',      'CHECKING', 3200.00, 'USD', 'ACTIVE', NOW()),
  (4, 'ACC-004-SAV', 'Bob Smith',      'SAVINGS',  8750.50, 'USD', 'ACTIVE', NOW()),
  (5, 'ACC-005-CHK', 'Carol Williams', 'CHECKING', 250.00,  'USD', 'ACTIVE', NOW());
