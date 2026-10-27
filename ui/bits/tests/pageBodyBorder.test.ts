import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { test } from 'node:test';
import { fileURLToPath } from 'node:url';

const cssDir = join(dirname(fileURLToPath(import.meta.url)), '../css');

const read = (path: string) => readFileSync(join(cssDir, path), 'utf8');

test('markdown preview border stays on editor previews', () => {
  const page = read('_page.scss');
  const markup = read('ublog/_markup.scss');
  const preview = read('_markdown-preview.scss');

  assert.doesNotMatch(page, /&:not\(\.expand-text\)/);
  assert.doesNotMatch(markup, /&:not\(\.expand-text\)/);
  assert.doesNotMatch(page, /border:\s*\$border/);
  assert.doesNotMatch(markup, /border:\s*\$border/);
  assert.match(
    preview,
    /\.markdown-textarea \.preview,\r?\n\.markdown-toastui \.preview \{\r?\n {2}border: \$border;/,
  );
});
