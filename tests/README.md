## Assumptions

1. database seeded with data from [lila-db-seed](https://github.com/lichess-org/lila-db-seed)
1. `./ui/build` run with `--debug` flag

### Run tests

```bash
npx playwright test

npx playwright test --ui
```

### Interactively create tests

```bash
npx playwright codegen
```
