Sari-sari Utang Tracker
Sari-sari Utang Tracker System - NetBeans project

HOW TO OPEN IN NETBEANS
1. Extract this zip completely.
2. In NetBeans: File → Open Project (do NOT use Import).
3. Select the Sari-sari_UtangTracker folder.
4. If asked to regenerate project files, click Yes.
5. Press Run (F6).

FEATURES
- Customers can self-register.
- After registration a popup tells them to present a valid ID for approval.
- New accounts start as Pending (cannot log in until approved).
- Owner can approve from Customer Accounts page or Pending Approvals page.
- Quantity + automatic total calculation.
- Receipt shows quantity, unit price, and total.
- Daily report with today's totals.
- Stronger password rules (8–12 characters, capital, number, special).
- Update customer contact (Owner).
- Owner can also change a customer's username in Update Customer (4-20 letters/numbers/dots/
  underscores, must be unique). The customer then signs in with the new username.

SECURITY UPDATES
- Forgot-password (recovery PIN) works for CUSTOMER accounts only. Staff and owner
  accounts cannot be reset by PIN and have no default PIN.
- Recovery PINs are now stored hashed, not in plain text. Older databases are
  upgraded automatically the first time the app opens them.
- 5 wrong PIN tries lock the PIN reset for that account for 15 minutes.
- Owner-only actions (approve customers, create/deactivate staff, update customer
  info) are now enforced in the service layer, not just hidden in the UI.
- Pending customers now see "waiting for the owner's approval" when they try to log in.
- Creating or deactivating staff is now written to the audit log.
- Owner > "Reset User PIN": enter the customer's contact number and the new 4-digit PIN
  the customer chose (typed twice). It is no longer reset to a fixed 1234.

DEMO ACCOUNTS (already approved)
Owner     owner / owner123
Staff     staff / staff123
Customer  maria / maria123
Also: juan / juan123, liza / liza123, pedro / pedro123
Demo customers have recovery PIN 1234. Remove or change demo accounts before real use.

Database is created in your user folder: UtangTracker/utang.db

RUNNING THE SMOKE TEST (optional)
Compile src and test/, then run utangtracker.SmokeTest with
-Dutang.home=<temp folder> so your real database is not touched.

RESPONSIVE UI (updated)
-----------------------
Windows now size themselves to ~80-88% of the current screen (never larger than the original design size).
They remain resizable and have lowered minimum sizes so they work on smaller displays (laptops, projectors, etc.).
Main content is wrapped in scroll panes so nothing is cut off if the window is made smaller.
Tables auto-resize columns and fill available space.

RESET PIN SCREEN (new)
----------------------
Login > "Forgot password?" now opens ResetPinFrame, styled like MoneyTrackerSystem (rounded card, header strip, pill fields, flat colored buttons). New classes: ResetPinFrame, RoundedPanel, RoundedTextField, RoundedPasswordField. GuiShots also renders 06-reset-pin.png.
