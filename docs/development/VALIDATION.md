# Maintenance validation

The maintenance baseline is published commit `6b64b36` (v4.2.1). The unfinished feature batch is preserved separately at `archive/roadmap-before-readability`, commit `2120842`.

Completed local checks:

- 111 unit tests, zero failures, including two independent protocol-v1 fixture cases.
- Full native solo input regression: passed (4m21s).
- Full two-process co-op native scenarios: passed (39s).
- `test packageGame`: passed.
- Java format check and Black check: passed.
- Java reference documentation generation: passed; legacy missing-comment warnings remain.
- Relative links in the maintenance handbook and contribution guide: checked.
- All 12 existing Python tools retain identical Python ASTs after formatting.
- 114 existing Java files retain equivalent token streams after accounting for import order, legal modifier order, comments and literal wrapping. Intentional structural changes are in `Game`, `CoopWorld` and `CoopProtocol`, with a new `MainMenuScreen`.

These checks establish refactoring compatibility within their coverage. They do not resolve the archived branch's balance failure, establish subjective game quality, or constitute a complete audit of every native resource path. Cross-platform results are recorded by GitHub Actions for the maintenance commit.
