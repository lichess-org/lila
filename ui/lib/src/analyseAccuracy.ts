import { effectiveDivision } from './gameDivision';
import type { TreeNodeBase } from './tree/types';

export interface ColorAccuracy {
  white?: number;
  black?: number;
}

export interface Division {
  middle?: number;
  end?: number;
}

export type GamePhase = 'opening' | 'middlegame' | 'endgame';

export interface PhaseAccuracies {
  opening?: ColorAccuracy;
  middlegame?: ColorAccuracy;
  endgame?: ColorAccuracy;
}

const squeeze = (n: number, min: number, max: number): number => Math.min(max, Math.max(min, n));

/** Mirrors chess.eval.Eval.Cp.initial (centipawns at game start for accuracy). */
const CP_INITIAL = 15;

const ceiledCp = (cp: number): number => squeeze(cp, -1000, 1000);

/** Win percent on a 0..100 scale, matching chess.eval.WinPercent.fromCentiPawns. */
export const winPercentFromCp = (cp: number): number => {
  const ceiled = ceiledCp(cp);
  const raw = 2 / (1 + Math.exp(-0.00368208 * ceiled)) - 1;
  return squeeze(50 + 50 * raw, 0, 100);
};

/** Mirrors lila.tree.Eval.forceAsCp. */
export const evalToCp = (ev: EvalScore): number | undefined => {
  if (ev.cp !== undefined) return ev.cp;
  if (ev.mate !== undefined) {
    const m = ev.mate;
    return m < 0 ? -2147483648 - m : 2147483647 - m;
  }
  return undefined;
};

/** Per-move accuracy from WinPercent values, matching AccuracyPercent.fromWinPercents. */
export const moveAccuracy = (before: number, after: number): number => {
  if (after >= before) return 100;
  const winDiff = before - after;
  const raw = 103.1668100711649 * Math.exp(-0.04354415386753951 * winDiff) + -3.166924740191411;
  return squeeze(raw + 1, 0, 100);
};

const standardDeviation = (values: number[]): number | undefined => {
  if (values.length === 0) return undefined;
  const mean = values.reduce((a, b) => a + b, 0) / values.length;
  const variance = values.reduce((a, v) => a + (v - mean) ** 2, 0) / values.length;
  return Math.sqrt(variance);
};

const weightedMean = (items: { value: number; weight: number }[]): number | undefined => {
  if (items.length === 0) return undefined;
  const totalWeight = items.reduce((a, i) => a + i.weight, 0);
  if (totalWeight === 0) return undefined;
  return items.reduce((a, i) => a + i.value * i.weight, 0) / totalWeight;
};

const harmonicMean = (values: number[]): number | undefined => {
  if (values.length === 0) return undefined;
  let sum = 0;
  for (const v of values) {
    if (v === 0) return undefined;
    sum += 1 / v;
  }
  if (sum === 0) return undefined;
  return values.length / sum;
};

export const startColorFromGame = (game: { startedAtTurn?: number }): Color => {
  const ply = game.startedAtTurn ?? 0;
  return ply % 2 === 0 ? 'white' : 'black';
};

const toIntPercent = (n: number): number => Math.round(n);

/**
 * Game accuracy for both colors from a list of centipawn evals after each move.
 * Mirrors AccuracyPercent.gameAccuracy(startColor, cps).
 */
export const gameAccuracy = (
  startColor: Color,
  cps: readonly (number | null | undefined)[],
): ColorAccuracy | undefined => {
  const allWinPercents: (number | undefined)[] = [CP_INITIAL, ...cps].map(cp =>
    cp === null || cp === undefined ? undefined : winPercentFromCp(cp),
  );

  const windowSize = squeeze(Math.floor(cps.length / 10), 2, 8);
  const maxFill = Math.min(windowSize, allWinPercents.length);
  const fillCount = Math.max(0, maxFill - 2);
  const prefixWindows = Array.from({ length: fillCount }, () => allWinPercents.slice(0, windowSize));
  const slidingWindows: (number | undefined)[][] = [];
  for (let i = 0; i <= allWinPercents.length - windowSize; i++) {
    slidingWindows.push(allWinPercents.slice(i, i + windowSize));
  }
  const windows = [...prefixWindows, ...slidingWindows];

  const weights = windows.map(window => {
    if (window.some(wp => wp === undefined)) return undefined;
    const std = standardDeviation(window as number[]);
    return std === undefined ? undefined : squeeze(std, 0.5, 12);
  });

  const pairs = allWinPercents.slice(0, -1).map((prev, i) => ({
    prev,
    next: allWinPercents[i + 1],
    weight: weights[i],
    index: i,
  }));

  const weightedAccuracies: { color: Color; accuracy: number; weight: number }[] = [];
  for (const { prev, next, weight, index } of pairs) {
    if (prev === undefined || next === undefined || weight === undefined) continue;
    const isWhiteMove = (index % 2 === 0) === (startColor === 'white');
    const color: Color = isWhiteMove ? 'white' : 'black';
    const before = color === 'white' ? prev : next;
    const after = color === 'white' ? next : prev;
    weightedAccuracies.push({ color, accuracy: moveAccuracy(before, after), weight });
  }

  const colorAccuracy = (color: Color): number | undefined => {
    const forColor = weightedAccuracies.filter(w => w.color === color);
    if (forColor.length === 0) return undefined;
    const weighted = weightedMean(forColor.map(w => ({ value: w.accuracy, weight: w.weight })));
    const harmonic = harmonicMean(forColor.map(w => w.accuracy));
    if (weighted === undefined || harmonic === undefined) return undefined;
    return (weighted + harmonic) / 2;
  };

  const white = colorAccuracy('white');
  const black = colorAccuracy('black');
  if (white === undefined || black === undefined) return undefined;
  return { white: toIntPercent(white), black: toIntPercent(black) };
};

export const cpsFromMainline = (mainline: TreeNodeBase[], upToPly: number, variantKey?: string): number[] => {
  const cps: number[] = [];
  for (const node of mainline.slice(1)) {
    if (node.ply > upToPly) break;
    if (!node.eval) break;
    const cp = nodeEvalToCp(node, variantKey);
    if (cp === undefined) break;
    cps.push(cp);
  }
  return cps;
};

/** Extract centipawn eval from a tree node, mirroring acpl chart logic. */
export const nodeEvalToCp = (node: TreeNodeBase, variantKey?: string): number | undefined => {
  if (!node.eval) return undefined;
  const isWhite = (node.ply & 1) === 1;
  let cp: number | undefined = 0;
  if (node.eval.mate !== undefined) cp = node.eval.mate > 0 ? Infinity : -Infinity;
  else if (node.san?.includes('#')) cp = isWhite ? Infinity : -Infinity;
  if (cp !== undefined && variantKey === 'antichess' && node.san?.includes('#')) cp = -cp;
  else if (node.eval.cp !== undefined) cp = node.eval.cp;
  if (!Number.isFinite(cp)) return evalToCp(node.eval);
  return cp;
};

export const rollingAccuracy = (
  mainline: TreeNodeBase[],
  upToPly: number,
  game: { startedAtTurn?: number; variant?: { key: string } },
): ColorAccuracy | undefined =>
  gameAccuracy(startColorFromGame(game), cpsFromMainline(mainline, upToPly, game.variant?.key));

/** Precompute rolling accuracy for each ACPL chart data point (indexed by chart dataIndex). */
export const rollingAccuracyByChartIndex = (
  mainline: TreeNodeBase[],
  game: { startedAtTurn?: number; variant?: { key: string } },
): (ColorAccuracy | undefined)[] => {
  const nodes = mainline.slice(1);
  return nodes.map(node => rollingAccuracy(mainline, node.ply, game));
};

/** Mirrors lila.insight.Phase.of. */
export const phaseOf = (division: Division | undefined, ply: number): GamePhase => {
  if (!division?.middle) return 'opening';
  if (ply < division.middle) return 'opening';
  if (!division.end) return 'middlegame';
  if (ply < division.end) return 'middlegame';
  return 'endgame';
};

export const hasPhaseDivision = (division: Division | undefined): boolean =>
  division?.middle !== undefined && division.middle > 1;

const moveColorAtIndex = (moveIndex: number, gameStartColor: Color): Color =>
  (moveIndex % 2 === 0) === (gameStartColor === 'white') ? 'white' : 'black';

/** Accuracy for one game phase up to a given ply, using moves in that phase only. */
export const phaseAccuracy = (
  mainline: TreeNodeBase[],
  upToPly: number,
  game: { startedAtTurn?: number; variant?: { key: string }; division?: Division },
  phase: GamePhase,
): ColorAccuracy | undefined => {
  const division = effectiveDivision(game.division, mainline, game.variant?.key);
  if (!hasPhaseDivision(division)) return undefined;

  const nodes = mainline.slice(1);
  const phaseNodes: TreeNodeBase[] = [];
  const phaseMoveIndexes: number[] = [];

  nodes.forEach((node, moveIndex) => {
    if (node.ply > upToPly || !node.eval) return;
    if (phaseOf(division, node.ply) !== phase) return;
    phaseNodes.push(node);
    phaseMoveIndexes.push(moveIndex);
  });

  if (phaseNodes.length < 2) return undefined;

  const cps: number[] = [];
  for (const node of phaseNodes) {
    const cp = nodeEvalToCp(node, game.variant?.key);
    if (cp === undefined) return undefined;
    cps.push(cp);
  }

  const sliceStartColor = moveColorAtIndex(phaseMoveIndexes[0], startColorFromGame(game));
  return gameAccuracy(sliceStartColor, cps);
};

export const phaseAccuracies = (
  mainline: TreeNodeBase[],
  upToPly: number,
  game: { startedAtTurn?: number; variant?: { key: string }; division?: Division },
): PhaseAccuracies | undefined => {
  const { phases, hasAny } = phaseAccuraciesDisplay(mainline, upToPly, game);
  if (!hasAny) return undefined;

  const result: PhaseAccuracies = {};
  for (const phase of ['opening', 'middlegame', 'endgame'] as GamePhase[]) {
    const accuracy = phases[phase];
    if (accuracy.white !== undefined || accuracy.black !== undefined) result[phase] = accuracy;
  }
  return Object.keys(result).length ? result : undefined;
};

export type PhaseAccuraciesDisplay = Record<GamePhase, ColorAccuracy>;

export interface PhaseAccuraciesView {
  phases: PhaseAccuraciesDisplay;
  /** True when at least one phase has a computed accuracy value. */
  hasAny: boolean;
  /** True when any cell is empty or analysis is still in progress. */
  showHint: boolean;
}

const emptyPhaseAccuracies = (): PhaseAccuraciesDisplay => ({
  opening: {},
  middlegame: {},
  endgame: {},
});

export const formatPhaseAccuracy = (value: number | undefined): string =>
  value !== undefined ? `${value}%` : '–';

/** Always returns all three phases for UI; missing values render as –. */
export const phaseAccuraciesDisplay = (
  mainline: TreeNodeBase[],
  upToPly: number,
  game: { startedAtTurn?: number; variant?: { key: string }; division?: Division },
  analysisPartial = false,
): PhaseAccuraciesView => {
  const phases = emptyPhaseAccuracies();
  let hasAny = false;
  let showHint = analysisPartial;

  const division = effectiveDivision(game.division, mainline, game.variant?.key);
  if (!hasPhaseDivision(division)) return { phases, hasAny, showHint: true };

  const gameWithDivision = { ...game, division };

  for (const phase of ['opening', 'middlegame', 'endgame'] as GamePhase[]) {
    const accuracy = phaseAccuracy(mainline, upToPly, gameWithDivision, phase);
    if (accuracy) {
      phases[phase] = accuracy;
      if (accuracy.white !== undefined || accuracy.black !== undefined) hasAny = true;
      if (accuracy.white === undefined || accuracy.black === undefined) showHint = true;
    } else {
      showHint = true;
    }
  }

  return { phases, hasAny, showHint };
};
