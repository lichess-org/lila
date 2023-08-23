import { type PV } from 'zerofish';

export function linesWithin(move: string, lines: PV[], bias = 0, threshold = 50) {
  const zeroScore = lines.find(line => line.moves[0] === move)?.score ?? Number.NaN;
  return lines.filter(fish => Math.abs(fish.score - bias - zeroScore) < threshold && fish.moves.length);
}

export function randomSprinkle(move: string, lines: PV[]) {
  lines = linesWithin(move, lines, 0, 20);
  if (!lines.length) return move;
  return lines[Math.floor(Math.random() * lines.length)].moves[0] ?? move;
}

export function occurs(chance: number) {
  return Math.random() < chance;
}
