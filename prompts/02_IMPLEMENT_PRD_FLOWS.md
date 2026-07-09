# Prompt 02 - Implement PRD flows

Use `docs/PRD_FOR_CLAUDE_CODE.md` as the product source of truth.

Your task: compare current Android app behavior against the PRD and implement missing or incorrect flows in priority order.

Priority order:

1. App launches on OPPO A31.
2. Home screen with balance, income, expense, date strip, quick actions, recent transactions.
3. Add expense and add income flows.
4. Transaction History with four filters.
5. Settings account/language/logout behavior.
6. Scan/Review behavior, including unknown image leaves name blank.
7. Reports Dashboard with time filter and bar chart.
8. Accounts add/edit/delete.
9. Budgets add/edit/delete.
10. Expense and income categories add/edit/delete.
11. Recurring transactions.
12. Split bill.
13. Automatic backend sync integration.

Before coding:

- Read existing source files.
- Identify which items already exist.
- Produce a gap table: PRD item, existing status, files to change, risk.
- Then implement in small increments.

Verification:

- After each increment, build or run targeted tests.
- Provide changed files and command output.
- If a feature is too large, scaffold it with clear TODOs and no broken UI.
