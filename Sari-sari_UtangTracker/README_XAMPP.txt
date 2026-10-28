============================================================
Sari-sari Utang Tracker - XAMPP / MySQL Setup
============================================================

1. Start XAMPP Control Panel
   - Start Apache
   - Start MySQL

2. Import the database:
   - Open browser → http://localhost/phpmyadmin
   - Click "Import"
   - Choose utang_tracker.sql
   - Click "Go"

3. Open this project in NetBeans:
   - File → Open Project → select the Sari-sari_UtangTracker folder
   - Clean and Build
   - Run (F6)

db.properties is already set for default XAMPP (root / empty password).

DEMO ACCOUNTS:
  Owner    : owner   / owner123
  Staff    : staff   / staff123
  Customer : maria   / maria123   (PIN: 1234)
  Customer : juan    / juan123    (PIN: 1234)
  Customer : liza    / liza123    (PIN: 1234)
  Customer : pedro   / pedro123   (PIN: 1234)
  Customer : pending / pending123 (PIN: 1234)
