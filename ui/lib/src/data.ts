/* kind like $.data, except simpler */

const makeKey = (key: string) => `lichess-${key}`;

// should be
// export const get = <A>(owner: Element, key: string): A | undefined => (owner as any)[makeKey(key)];
export const get = (owner: Element, key: string): any => (owner as any)[makeKey(key)];

export const set = (owner: Element, key: string, value: any): void => ((owner as any)[makeKey(key)] = value);
