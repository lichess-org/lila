/* kind like $.data, except simpler */

const makeKey = (key: string) => `lishogi-${key}`;

export const get = <T = any>(owner: Element, key: string): T | undefined =>
  (owner as any)[makeKey(key)];

export const set = <T = any>(owner: Element, key: string, value: T): void => {
  (owner as any)[makeKey(key)] = value;
};
