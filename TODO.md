# TODO

Planned improvements after the kilt-graphics → Swing port. The port itself keeps the original 2024 behavior on purpose.

## README
- [ ] Retake the screenshots with the Swing UI
- [ ] Fix the broken image links (Markdown image syntax wrapped around HTML `<img>` tags)
- [ ] Update the setup/run instructions (no longer needs the course's VSCode/GitHub Desktop setup)

## UI
- [x] Show error messages instead of crashing or silently doing nothing:
  - empty or default name on Friends (currently throws `IllegalArgumentException`)
  - duplicate names (currently allowed)
  - more than 10 people (currently crashes later, when adding a transaction)
  - non-number amount on Expenses (currently `NumberFormatException`)
  - zero/negative amount, or same person on both sides
  - a name that isn't in the group (currently nothing happens)
- [x] Pick people from dropdowns instead of typing names
- [x] Use labels or real placeholder text instead of pre-filled text boxes ("Paid by", "Value", ...)
- [x] Keep each screen instead of recreating it on every click, so Expenses keeps its state
- [x] Show a history list of transactions, including the description
- [x] Show the group as a list instead of `[Me, Harold, ...]`
- [x] Show "Everyone is settled up" when there's nothing to pay
- [x] Use the system look and feel; tidy up layout and colors
- [x] Remove debug output (`System.out.println`, `matrixPrint`)
- [x] Add `application` plugin to `build.gradle` so `gradle run` works; add a Gradle wrapper

## Calculations
- [x] Store money as whole cents (`long`) or `BigDecimal` instead of `double`, so small floating-point
      leftovers (e.g. 0.1 + 0.2 - 0.3) can't show up as `$0.00` payments
- [x] Clearer variable names in `cashFlowMin()` (`vMax`/`vMin` → `receiver`/`payer`)
- [x] Make `cashFlowMin()` find the true minimum number of payments (see below)
- [ ] Consider dropping the `LIMIT x LIMIT` matrix: settling up only needs each person's net balance.
      Keep the matrix only if the app should also show who owes whom directly.

### Why the greedy approach isn't always optimal
`cashFlowMin()` repeatedly matches the person owed the most with the person who owes the most.
That always settles everyone in at most n - 1 payments, but not always in the fewest.

Example: balances +6, +5, -5, -3, -3.
- Greedy: 6↔5 (pay 5), 5↔3 (pay 3), 2↔3 (pay 2), 1↔1 (pay 1) = **4 payments**
- Optimal: {+5, -5} settles on its own (1 payment), {+6, -3, -3} in 2 = **3 payments**

The real minimum is `n - (most groups the people can be split into where each group sums to 0)`.
Finding that is NP-hard in general, but the group is capped at 10 people, so an exact search is cheap:
- Bitmask DP over subsets of people with a nonzero balance: `dp[mask]` = most zero-sum groups that can
  be formed from `mask`. About 2^10 · 10 ≈ 10,000 steps.
- Then settle each zero-sum group with the existing greedy (k - 1 payments for k people).
- Keep the greedy as a fallback if the group limit is ever raised far beyond 10 (e.g. above ~20).
