// mod.autolink.test.ts (node:test)

import { describe, test, before } from 'node:test';
import assert from 'node:assert/strict';

let autolink!: (s: string) => string;
let hostname!: string;

const ensureLocation = () => {
  if (!('location' in globalThis) || !globalThis.location) {
    Object.defineProperty(globalThis, 'location', {
      value: { hostname: 'localhost' },
      configurable: true,
      writable: true,
    });
  }
};

const linked = (text: string) => `<a href="${text}">${hostname}${text}</a>`;

before(async () => {
  ensureLocation();
  hostname = globalThis.location.hostname;
  ({ autolink } = await import('../src/mod.autolink'));
});

describe('autolinks', () => {
  test('dont match external hosts', () => {
    assert.strictEqual(autolink('https://otherhost/12345678'), 'https://otherhost/12345678');
  });

  test('inside parentheses]', () => {
    assert.strictEqual(autolink(`(https://${hostname}/12345678)`), `(${linked('/12345678')})`);
  });

  test('after colon', () => {
    assert.strictEqual(autolink(`see:https://${hostname}/inbox`), `see:${linked('/inbox')}`);
  });

  test('dont match after equals', () => {
    assert.strictEqual(autolink(`url=https://${hostname}/inbox`), `url=https://${hostname}/inbox`);
  });

  test('dont match in quotes', () => {
    assert.strictEqual(autolink(`"https://${hostname}/inbox"`), `"https://${hostname}/inbox"`);
  });

  test('dont match when preceded by a word char', () => {
    assert.strictEqual(autolink(`foohttps://${hostname}/inbox`), `foohttps://${hostname}/inbox`);
  });

  test('bare path inside of string', () => {
    assert.strictEqual(autolink(`boo /inbox`), `boo ${linked('/inbox')}`);
  });

  test('hostname without scheme in parens', () => {
    assert.strictEqual(autolink(`foo bar (${hostname}/inbox)`), `foo bar (${linked('/inbox')})`);
  });

  test('game id path, 8 chars', () => {
    assert.strictEqual(autolink(`https://${hostname}/12345678`), linked('/12345678'));
  });

  test('game id path, 12 chars', () => {
    assert.strictEqual(autolink(`https://${hostname}/12345678abcd`), linked('/12345678abcd'));
  });

  test('preceded by comma', () => {
    assert.strictEqual(autolink(`,https://${hostname}/inbox`), `,${linked('/inbox')}`);
  });

  test('preceded by semicolon', () => {
    assert.strictEqual(autolink(`;https://${hostname}/inbox`), `;${linked('/inbox')}`);
  });

  test('multi games already linked', () => {
    assert.strictEqual(
      autolink(`
<a href="localhost/1xeQrVqS/black">localhost/1xeQrVqS/black</a>
<a href="/iCuMS2K7">/iCuMS2K7</a>
/12345678
<a href="localhost/insights/Chess_Athlete_30/acpl/blur/variant:antichess">localhost/insights/Chess_Athlete_30/acpl/blur/variant:antichess</a>
`),
      `
<a href="localhost/1xeQrVqS/black">localhost/1xeQrVqS/black</a>
<a href="/iCuMS2K7">/iCuMS2K7</a>
${linked('/12345678')}
<a href="localhost/insights/Chess_Athlete_30/acpl/blur/variant:antichess">localhost/insights/Chess_Athlete_30/acpl/blur/variant:antichess</a>
`,
    );
  });

  test('grab bag', () => {
    assert.strictEqual(autolink(`foo /12345678#anchor bar`), `foo ${linked('/12345678#anchor')} bar`);
    assert.strictEqual(
      autolink(`http://${hostname}/inbox/nope /${hostname}/inbox/nope`),
      `http://${hostname}/inbox/nope /${hostname}/inbox/nope`,
    );
    assert.strictEqual(autolink('(//inbox/ nope)'), '(//inbox/ nope)');
  });

  test('path params', () => {
    assert.strictEqual(autolink(`${hostname}/inbox?param=value`), linked('/inbox?param=value'));
    assert.strictEqual(autolink(`https://${hostname}/forum/blah#anchor`), linked('/forum/blah#anchor'));
    assert.strictEqual(
      autolink(`(/inbox/extra/path?x=true&y=false)`),
      `(${linked('/inbox/extra/path?x=true&y=false')})`,
    );
  });
});
