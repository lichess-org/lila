import { piotr } from "./piotr";

export const initialFen: Fen =
  "lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w KQkq - 0 1";

export function fixCrazySan(san: San): San {
  return san;
}

export type Dests = Map<Key, Key[]>;

export function readDests(lines?: string): Dests | null {
  if (typeof lines === "undefined") return null;
  const dests = new Map();
  if (lines)
    for (const line of lines.split(" ")) {
      dests.set(
        piotr[line[0]],
        line
          .slice(1)
          .split("")
          .map((c) => piotr[c])
      );
    }
  return dests;
}

export function readDrops(line?: string | null): Key[] | null {
  if (typeof line === "undefined" || line === null) return null;
  return (line.match(/.{2}/g) as Key[]) || [];
}

export const altCastles = {
  e1a1: "e1c1",
  e1h1: "e1g1",
  e8a8: "e8c8",
  e8h8: "e8g8",
};
