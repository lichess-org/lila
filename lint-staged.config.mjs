// lint-staged.config.mjs gives behavior for the lint-staged tool linked in package.json.
//
// lint-staged is invoked a git pre-commit hook for users that have run `pnpm add-hooks`.
// The pre-commit script itself is at bin/git-hooks/pre-commit.
//
// Running `pnpm add-hooks` just once will save future headaches patching pushes that fail CI checks.

const args = files => files.map(f => `'${f.replaceAll("'", "'\\''")}'`).join(' ');

export default {
  '*.{json,ts,mts,js,mjs,scss}': files => {
    const code = files.filter(f => /\.(?:ts|mts|js|mjs)$/.test(f));
    const scss = files.filter(f => f.endsWith('.scss'));

    return [
      code.length && `oxlint --type-aware ${args(code)}`,
      scss.length && `stylelint ${args(scss)}`,
      `oxfmt ${args(files)}`,
    ].filter(Boolean);
  },
};
