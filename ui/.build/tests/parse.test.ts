import { describe, expect, test } from 'vitest';
import { minifyHtml } from '../src/parse';

describe('minify', () => {
  test('string', () => {
    expect(minifyHtml('a       b')).toBe('a b');
    expect(minifyHtml('tab\ttab')).toBe('tab tab');
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
    const minified = minifyHtml(html);
    expect(minified).toBe('<html><head><title>Test</title></head><body><h1>Test</h1></body></html>');
  });
});
