---
name: tui-than-tai-prd-implementation
description: Implement and verify product requirements from the standardized Tui Than Tai PRD.
---

# Skill: Implement Tui Than Tai PRD

Use this skill when asked to implement product flows, UI screens, sync behavior, OCR behavior, or dashboard requirements for Tui Than Tai.

## Source of truth

Read `docs/PRD_FOR_CLAUDE_CODE.md` before changing code.

## Implementation priorities

1. App launches and builds.
2. Auth: register name + email, Google button/linking, auto-login after register, logout in Settings.
3. Language: Vietnamese/English with confirmation popup.
4. Home: balance, income green, expense red, current date strip, quick actions, recent transactions.
5. Add expense/income flows.
6. Transaction History with four filters.
7. Scan/OCR review behavior; unknown image leaves name blank.
8. Reports Dashboard with time filter and bar chart.
9. Accounts add/edit/delete.
10. Budgets add/edit/delete.
11. Expense categories and income categories add/edit/delete.
12. Recurring transactions add/edit/delete and auto-create rules.
13. Split bill calculations.
14. Automatic PC backend sync.

## Key product rules

- Expense is red and decreases balance.
- Income is green and increases balance.
- VND Vietnamese format: `1.250.000 ₫`.
- USD English format: `$1,250.50`.
- OCR must not invent product names.
- If scan cannot identify object/product/service, leave name blank.
- Local-first is mandatory.
- User does not manually sync.

## Verification

For every feature:

- Build must pass.
- State how you tested it.
- If not fully implemented, clearly mark as scaffold/deferred and keep UI safe.
