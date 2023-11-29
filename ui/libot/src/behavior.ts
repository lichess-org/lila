import { type Score } from 'zerofish';
import * as Chops from 'chessops';

export function linesWithin(move: string, lines: Score[], bias = 0, threshold = 50) {
  const zeroScore = lines.find(line => line.moves[0] === move)?.score ?? Number.NaN;
  return lines.filter(fish => Math.abs(fish.score - bias - zeroScore) < threshold && fish.moves.length);
}

export function randomSprinkle(move: string, lines: Score[]) {
  lines = linesWithin(move, lines, 0, 20);
  if (!lines.length) return move;
  return lines[Math.floor(Math.random() * lines.length)].moves[0] ?? move;
}

export function occurs(chance: number) {
  return Math.random() < chance;
}

export function scores(lines: Score[][], move?: string) {
  const matches: Score[] = [];
  if (!move) return matches;
  for (const history of lines) {
    for (const line of history) {
      if (line.moves[0] === move) matches.push(line);
    }
  }
  return matches;
}

export function deepScores(lines: Score[][], move?: string) {
  const matches: Score[] = [];
  if (!move) return matches;
  let deepest = 0;
  for (const history of lines) {
    for (const line of history) {
      if (line.moves[0] !== move || line.depth < deepest) continue;
      if (line.depth > deepest) matches.length = 0;
      matches.push(line);
      deepest = line.depth;
    }
  }
  return matches;
}

export function shallowScores(lines: Score[][], move?: string) {
  const matches: Score[] = [];
  if (!move) return matches;
  let shallowest = 99;
  for (const history of lines) {
    for (const line of history) {
      if (line.moves[0] !== move || line.depth > shallowest) continue;
      if (line.depth > shallowest) matches.length = 0;
      matches.push(line);
      shallowest = line.depth;
    }
  }
  return matches;
}

export function byDestruction(lines: Score[][], fen: string) {
  const chess = Chops.Chess.fromSetup(Chops.fen.parseFen(fen).unwrap()).unwrap();
  const before = weigh(Chops.Material.fromBoard(chess.board));
  const aggression: [number, Score][] = [];
  for (const history of lines) {
    for (const pv of history) {
      const pvChess = chess.clone();
      for (const move of pv.moves) pvChess.play(Chops.parseUci(move)!);
      const destruction = (before - weigh(Chops.Material.fromBoard(pvChess.board))) / pv.moves.length;
      if (destruction > 0) aggression.push([destruction, pv]);
    }
  }
  return aggression;
}

const prices: { [role in Chops.Role]?: number } = {
  pawn: 1,
  knight: 2.8,
  bishop: 3,
  rook: 5,
  queen: 9,
};

function weigh(material: Chops.Material) {
  let score = 0;
  for (const [role, price] of Object.entries(prices) as [Chops.Role, number][]) {
    score += price * material.count(role);
  }
  return score;
}
