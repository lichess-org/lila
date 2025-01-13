import { describe, expect, test } from 'vitest';
import { trimAndConsolidateWhitespace } from '../src/parse';

describe('minify', () => {
  test('string', () => {
    expect(trimAndConsolidateWhitespace('a       b')).toBe('a b');
    expect(trimAndConsolidateWhitespace('tab\ttab')).toBe('tab tab');
  });
  test('html', () => {
    const html = `
      <html>
      <head>
          <title>Test</title>
      </head>
      <body>
          <h1>Test</h1>
      </body>
      </html>
    `;
    const minified = trimAndConsolidateWhitespace(html);
    expect(minified).toBe('<html> <head> <title>Test</title> </head> <body> <h1>Test</h1> </body> </html>');
  });
});
