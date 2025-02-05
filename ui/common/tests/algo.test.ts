import { describe, expect, test } from 'vitest';
import { reduceWhitespace } from '../src/algo';

describe('minify', () => {
  test('string', () => {
    expect(reduceWhitespace('a       b')).toBe('a b');
    expect(reduceWhitespace('tab\ttab')).toBe('tab tab');
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
    const minified = reduceWhitespace(html);
    expect(minified).toBe('<html> <head> <title>Test</title> </head> <body> <h1>Test</h1> </body> </html>');
  });
});
