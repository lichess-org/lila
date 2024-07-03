import * as co from 'chessops';
import { defined } from 'common';
import { SearchResult, Line } from 'zerofish';

export function zeroWithScores({ zero, fish }: { zero?: SearchResult; fish?: SearchResult }): Line[] {
  const lines: Line[] = [];
  const fishPvs = fish ? structuredClone(fish.pvs) : [];
  for (const pv of zero?.pvs ?? []) {
    const mv = pv.moves[0];
    if (!mv) continue;
    const fishIndex = fish?.pvs.find(x => x.moves[0] === mv);
    lines.push(fish?.pvs.find(x => x.moves[0] === mv) ?? pv);
  }
  return lines;
}

export function zeroUnscored({
  zero,
  fish,
}: {
  zero?: SearchResult;
  fish?: SearchResult;
}): { move: Uci; multipv: number }[] {
  return (
    zero?.pvs
      .filter(x => !(fish?.pvs ?? []).find(y => y.moves[0] === x.moves[0]))
      .map((pv, i) => ({
        move: pv.moves[0],
        multipv: i + 1,
      })) ?? []
  );
}

export function intersect({ zero, fish }: { zero?: SearchResult; fish?: SearchResult }): Line[] {
  return zero?.pvs.map(x => fish?.pvs.find(y => y.moves[0] === x.moves[0])).filter(defined) ?? [];
}
