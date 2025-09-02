import { describe, expect, test } from 'vitest';
import { autolink } from '../src/mod.autolink';

const hostname = location.hostname;
const linked = (text: string) => `<a href="${text}">${hostname}${text}</a>`;

describe('autolinks', () => {
  test('dont match external hosts', () => {
    expect(autolink('https://otherhost/12345678')).toBe('https://otherhost/12345678');
  });

  test('inside parentheses]', () => {
    expect(autolink(`(https://${hostname}/12345678)`)).toBe(`(${linked('/12345678')})`);
  });
  test('after colon', () => {
    expect(autolink(`see:https://${hostname}/inbox`)).toBe(`see:${linked('/inbox')}`);
  });

  test('dont match after equals', () => {
    expect(autolink(`url=https://${hostname}/inbox`)).toBe(`url=https://${hostname}/inbox`);
  });

  test('dont match in quotes', () => {
    expect(autolink(`"https://${hostname}/inbox"`)).toBe(`"https://${hostname}/inbox"`);
  });

  test('dont match when preceded by a word char', () => {
    expect(autolink(`foohttps://${hostname}/inbox`)).toBe(`foohttps://${hostname}/inbox`);
  });

  test('bare path inside of string', () => {
    expect(autolink(`boo /inbox`)).toBe(`boo ${linked('/inbox')}`);
  });

  test('hostname without scheme in parens', () => {
    expect(autolink(`foo bar (${hostname}/inbox)`)).toBe(`foo bar (${linked('/inbox')})`);
  });

  test('game id path, 8 chars', () => {
    expect(autolink(`https://${hostname}/12345678`)).toBe(linked('/12345678'));
  });

  test('game id path, 12 chars', () => {
    expect(autolink(`https://${hostname}/12345678abcd`)).toBe(linked('/12345678abcd'));
  });

  test('preceded by comma', () => {
    expect(autolink(`,https://${hostname}/inbox`)).toBe(`,${linked('/inbox')}`);
  });

  test('preceded by semicolon', () => {
    expect(autolink(`;https://${hostname}/inbox`)).toBe(`;${linked('/inbox')}`);
  });

  test('multi games already linked', () => {
    expect(
      autolink(`
<a href="localhost/1xeQrVqS/black">localhost/1xeQrVqS/black</a>
<a href="/iCuMS2K7">/iCuMS2K7</a>
/12345678
<a href="localhost/insights/Chess_Athlete_30/acpl/blur/variant:antichess">localhost/insights/Chess_Athlete_30/acpl/blur/variant:antichess</a>
`),
    ).toBe(`
<a href="localhost/1xeQrVqS/black">localhost/1xeQrVqS/black</a>
<a href="/iCuMS2K7">/iCuMS2K7</a>
${linked('/12345678')}
<a href="localhost/insights/Chess_Athlete_30/acpl/blur/variant:antichess">localhost/insights/Chess_Athlete_30/acpl/blur/variant:antichess</a>
`);
  });
  test('grab bag', () => {
    expect(autolink(`foo /12345678#anchor bar`)).toBe(`foo ${linked('/12345678#anchor')} bar`);
    expect(autolink(`http://${hostname}/inbox/nope /${hostname}/inbox/nope`)).toBe(
      `http://${hostname}/inbox/nope /${hostname}/inbox/nope`,
    );
    expect(autolink('(//inbox/ nope)')).toBe('(//inbox/ nope)');
  });
  test('path params', () => {
    expect(autolink(`${hostname}/inbox?param=value`)).toBe(linked('/inbox?param=value'));
    expect(autolink(`https://${hostname}/forum/blah#anchor`)).toBe(linked('/forum/blah#anchor'));
    expect(autolink(`(/inbox/extra/path?x=true&y=false)`)).toBe(
      `(${linked('/inbox/extra/path?x=true&y=false')})`,
    );
  });
});
