-- ============================================================
-- Utang Tracker - Organized Schema
-- Supports: Customer Accounts, Activity Logs, Paid Utang,
--           Reports, Audit Log, Pending Approvals
-- ============================================================

DROP DATABASE IF EXISTS `igpitopol_utang_v2`;
CREATE DATABASE `igpitopol_utang_v2`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE `igpitopol_utang_v2`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- 1. USERS (Customer Accounts + Staff + Owner + Pending)
-- ------------------------------------------------------------
CREATE TABLE `users` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `first_name` VARCHAR(100) NOT NULL,
  `last_name` VARCHAR(100) NOT NULL,
  `username` VARCHAR(50) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `salt` VARCHAR(255) NOT NULL,
  `contact` VARCHAR(20) DEFAULT NULL,
  `role` ENUM('OWNER','STAFF','CUSTOMER') NOT NULL,
  `active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '0 = Pending Approval',
  `recovery_pin` VARCHAR(255) DEFAULT NULL,
  `pin_attempts` INT(11) NOT NULL DEFAULT 0,
  `pin_locked_until` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_role_active` (`role`, `active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ------------------------------------------------------------
-- 2. TRANSACTIONS (Activity Logs + Paid Utang source)
-- ------------------------------------------------------------
CREATE TABLE `transactions` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `customer_id` INT(11) NOT NULL,
  `staff_id` INT(11) DEFAULT NULL,
  `item_description` VARCHAR(255) NOT NULL,
  `quantity` DECIMAL(10,2) NOT NULL DEFAULT 1.00,
  `unit_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `amount` DECIMAL(10,2) NOT NULL,
  `txn_type` ENUM('UTANG','PAYMENT') NOT NULL,
  `note` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `voided` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_customer` (`customer_id`),
  KEY `idx_staff` (`staff_id`),
  KEY `idx_type_date` (`txn_type`, `created_at`),
  KEY `idx_voided` (`voided`),
  CONSTRAINT `fk_txn_customer` FOREIGN KEY (`customer_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_txn_staff` FOREIGN KEY (`staff_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ------------------------------------------------------------
-- 3. AUDIT LOG
-- ------------------------------------------------------------
CREATE TABLE `audit_log` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `actor_id` INT(11) DEFAULT NULL,
  `actor_name` VARCHAR(100) DEFAULT NULL,
  `action` VARCHAR(50) NOT NULL,
  `target` VARCHAR(50) DEFAULT NULL,
  `detail` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_actor` (`actor_id`),
  KEY `idx_action` (`action`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ------------------------------------------------------------
-- VIEWS — one per menu section (makes queries organized)
-- ------------------------------------------------------------

-- Customer Accounts (active customers with current balance)
CREATE OR REPLACE VIEW `vw_customer_accounts` AS
SELECT
  u.id AS account_id,
  u.first_name,
  u.last_name,
  u.contact,
  u.username,
  u.active,
  COALESCE(SUM(CASE WHEN t.txn_type = 'UTANG' AND t.voided = 0 THEN t.amount ELSE 0 END), 0)
    - COALESCE(SUM(CASE WHEN t.txn_type = 'PAYMENT' AND t.voided = 0 THEN t.amount ELSE 0 END), 0)
    AS current_balance,
  CASE WHEN u.active = 1 THEN 'Active' ELSE 'Pending' END AS status
FROM users u
LEFT JOIN transactions t ON t.customer_id = u.id
WHERE u.role = 'CUSTOMER'
GROUP BY u.id;

-- Activity Logs (all non-voided transactions)
CREATE OR REPLACE VIEW `vw_activity_logs` AS
SELECT
  t.id,
  t.created_at,
  u.id AS account_id,
  u.first_name,
  u.last_name,
  CONCAT(s.first_name, ' ', s.last_name) AS staff,
  t.item_description,
  t.amount,
  t.txn_type AS activity,
  t.note
FROM transactions t
JOIN users u ON u.id = t.customer_id
LEFT JOIN users s ON s.id = t.staff_id
WHERE t.voided = 0
ORDER BY t.created_at DESC;

-- Paid Utang (payments only)
CREATE OR REPLACE VIEW `vw_paid_utang` AS
SELECT
  t.id,
  t.created_at AS date_paid,
  u.id AS account_id,
  u.first_name,
  u.last_name,
  CONCAT(s.first_name, ' ', s.last_name) AS staff,
  t.amount AS amount_paid,
  t.note
FROM transactions t
JOIN users u ON u.id = t.customer_id
LEFT JOIN users s ON s.id = t.staff_id
WHERE t.txn_type = 'PAYMENT' AND t.voided = 0
ORDER BY t.created_at DESC;

-- Pending Approvals
CREATE OR REPLACE VIEW `vw_pending_approvals` AS
SELECT
  id AS account_id,
  first_name,
  last_name,
  contact,
  username,
  created_at
FROM users
WHERE role = 'CUSTOMER' AND active = 0
ORDER BY created_at DESC;

-- Audit Log (clean view)
CREATE OR REPLACE VIEW `vw_audit_log` AS
SELECT
  id,
  created_at AS `when`,
  actor_name AS who,
  action,
  target,
  detail
FROM audit_log
ORDER BY created_at DESC;

-- Reports: Outstanding balances
CREATE OR REPLACE VIEW `vw_outstanding_balances` AS
SELECT
  u.id AS account_id,
  u.first_name,
  u.last_name,
  COALESCE(SUM(CASE WHEN t.txn_type = 'UTANG' AND t.voided = 0 THEN t.amount ELSE 0 END), 0)
    - COALESCE(SUM(CASE WHEN t.txn_type = 'PAYMENT' AND t.voided = 0 THEN t.amount ELSE 0 END), 0)
    AS current_balance
FROM users u
LEFT JOIN transactions t ON t.customer_id = u.id
WHERE u.role = 'CUSTOMER' AND u.active = 1
GROUP BY u.id
HAVING current_balance > 0.009
ORDER BY current_balance DESC;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- DEMO DATA (same as original)
-- ============================================================

INSERT INTO `users` (`id`, `first_name`, `last_name`, `username`, `password_hash`, `salt`, `contact`, `role`, `active`, `recovery_pin`, `pin_attempts`, `pin_locked_until`, `created_at`) VALUES
(1, 'Rosa', 'Bautista', 'owner', 'b80509bd722eca5ebd3debaa64246a41947943c1fe590963c78172d41b2e3dee', 'a1b2c3d4e5f6789012345678abcdef01', '09170000001', 'OWNER', 1, NULL, 0, NULL, '2026-10-01 08:00:00'),
(2, 'Ana', 'Reyes', 'staff', '3c5afed1af2b9204358076067ac5bbca71f9975ba8ee132e921f2ac7d64a0136', 'b2c3d4e5f6789012345678abcdef0123', '09170000002', 'STAFF', 1, NULL, 0, NULL, '2026-10-01 08:00:00'),
(3, 'Maria', 'Santos', 'maria', '78a8f7ccdcba76fa3a883541192fe5ae0fe378954ecac6a3b2ca2a6a6a917520', 'c3d4e5f6789012345678abcdef012345', '09171234567', 'CUSTOMER', 1, 'aabbccddeeff00112233445566778899$4baa8cef76705d1310de7ab6bf3304fc6ef2c1231a85929c3cad4cae1ade552b', 0, NULL, '2026-10-01 08:00:00'),
(4, 'Juan', 'Dela Cruz', 'juan', '26a0c376db335b94951c1cc40acdee1a6acc4df9ef503f391046021ef274ddaf', 'd4e5f6789012345678abcdef01234567', '09181234567', 'CUSTOMER', 1, 'aabbccddeeff00112233445566778899$4baa8cef76705d1310de7ab6bf3304fc6ef2c1231a85929c3cad4cae1ade552b', 0, NULL, '2026-10-01 08:00:00'),
(5, 'Liza', 'Garcia', 'liza', '3ceb4a159636ebec53f9842c63e266273d1e96da54c364b5dbb5d4e80500a562', 'e5f6789012345678abcdef0123456789', '09201234567', 'CUSTOMER', 1, 'aabbccddeeff00112233445566778899$4baa8cef76705d1310de7ab6bf3304fc6ef2c1231a85929c3cad4cae1ade552b', 0, NULL, '2026-10-01 08:00:00'),
(6, 'Pedro', 'Ramos', 'pedro', 'f247aece39f856670cac4d595d65fed7715adac6740d1fb0439b4a4419846ddd', 'f6789012345678abcdef0123456789ab', '09190001111', 'CUSTOMER', 1, 'aabbccddeeff00112233445566778899$4baa8cef76705d1310de7ab6bf3304fc6ef2c1231a85929c3cad4cae1ade552b', 0, NULL, '2026-10-01 08:00:00'),
(7, 'Rico', 'Pending', 'pending', 'af854c3b3bce78aa95cb68d67b746ef23eff96e343c559d48b60ab7be8c272a7', '11223344556677889900aabbccddeeff', '09191112222', 'CUSTOMER', 0, 'aabbccddeeff00112233445566778899$4baa8cef76705d1310de7ab6bf3304fc6ef2c1231a85929c3cad4cae1ade552b', 0, NULL, '2026-10-07 10:30:00');

INSERT INTO `transactions` (`id`, `customer_id`, `staff_id`, `item_description`, `quantity`, `unit_price`, `amount`, `txn_type`, `note`, `created_at`, `voided`) VALUES
(1, 3, 2, '1 pack noodles', 1.00, 15.00, 15.00, 'UTANG', '', '2026-10-05 14:30:00', 0),
(2, 3, 2, '1 canned sardines', 1.00, 30.00, 30.00, 'UTANG', '', '2026-10-06 09:15:00', 0),
(3, 4, 2, '1 kg rice', 1.00, 50.00, 50.00, 'UTANG', '', '2026-10-02 16:05:00', 0),
(4, 4, 2, 'Cash payment', 1.00, 20.00, 20.00, 'PAYMENT', 'Partial payment', '2026-10-06 18:40:00', 0),
(5, 5, 2, '3-in-1 coffee', 1.00, 25.00, 25.00, 'UTANG', 'Customer asked to double-check spelling of name', '2026-10-07 08:10:00', 0),
(6, 6, 2, 'Cooking oil 350ml', 1.00, 42.00, 42.00, 'UTANG', '', '2026-10-01 11:20:00', 0),
(7, 6, 1, 'Cash payment', 1.00, 42.00, 42.00, 'PAYMENT', 'Paid in full', '2026-10-04 17:05:00', 0),
(8, 3, 2, 'Soft drinks 1.5L', 1.00, 35.00, 35.00, 'UTANG', 'Today sample for Reports', '2026-10-07 16:45:00', 0);

INSERT INTO `audit_log` (`id`, `actor_id`, `actor_name`, `action`, `target`, `detail`, `created_at`) VALUES
(1, 1, 'Rosa Bautista', 'SYSTEM_SEED', 'users', 'Demo accounts created', '2026-10-01 08:00:00'),
(2, 2, 'Ana Reyes', 'UTANG', 'maria', '1 pack noodles - 15.00', '2026-10-05 14:30:00'),
(3, 2, 'Ana Reyes', 'UTANG', 'maria', '1 canned sardines - 30.00', '2026-10-06 09:15:00'),
(4, 1, 'Rosa Bautista', 'CREATE', 'pending', 'Customer Rico Pending registered (pending approval)', '2026-10-07 10:30:00');

ALTER TABLE `users` AUTO_INCREMENT = 8;
ALTER TABLE `transactions` AUTO_INCREMENT = 9;
ALTER TABLE `audit_log` AUTO_INCREMENT = 5;

-- ============================================================
-- DONE
-- Import this file in phpMyAdmin after starting XAMPP MySQL.
-- ============================================================
