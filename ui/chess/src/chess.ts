import { uciChar } from './uciChar';

export * from './sanWriter';

export const fixCrazySan = (san: San): San => (san[0] === 'P' ? san.slice(1) : san);

export const destsToUcis = (destMap: Dests): Uci[] =>
  Array.from(destMap).reduce<Uci[]>((acc, [orig, dests]) => acc.concat(dests.map(dest => orig + dest)), []);

export const readDests = (lines?: string): Dests | null =>
  lines
    ? lines.split(' ').reduce<Dests>((dests, line) => {
        dests.set(
          uciChar[line[0]],
          line
            .slice(1)
            .split('')
            .map(c => uciChar[c]),
        );
        return dests;
      }, new Map())
    : null;

export const readDrops = (line?: string | null): Key[] | null =>
  line ? (line.match(/.{2}/g) as Key[]) || [] : null;
