import { cpWinningChances } from 'lib/ceval/winningChances';
import { clamp, stddev, harmonicMean, weightedMean } from 'lib/algo';

// modules/analyse/.../AccuracyPercent.scala

type Accuracy = { percent: number; acpl: number };

export function accuracy(nodes: Tree.Node[]): { [c in Color]: Accuracy } {
  const evals = [...nodes.map(n => n.eval)];
  const winProbs = evals.map(evalToWinProb);
  const windowSize = clamp(Math.floor(nodes.length / 10), { min: 2, max: 8 });
  const weights = [
    ...Array.from({ length: windowSize - 2 }, () => winProbs.slice(0, windowSize)),
    ...Array.from({ length: nodes.length }, (_, i) => winProbs.slice(i, i + windowSize)),
  ].map(segment => clamp(stddev(segment), { min: 0.005, max: 0.12 }));

  const meanInputs: [Percentage, number][][] = [[], []];
  const cpls: number[][] = [[], []];

  for (let i = 0; i < nodes.length; i++) {
    const turn = (nodes[0].ply + i) & 1;
    const sign = turn === 0 ? 1 : -1;
    meanInputs[turn].push([accuracyPercentage(sign * (winProbs[i] - winProbs[i + 1])), weights[i]]);
    cpls[turn].push(sign * (forceCp(evals[i]) - forceCp(evals[i + 1])));
  }
  return {
    white: { percent: balanceMeans(meanInputs[0]), acpl: averageCpl(cpls[0]) },
    black: { percent: balanceMeans(meanInputs[1]), acpl: averageCpl(cpls[1]) },
  };
}

function accuracyPercentage(probabilityLoss: number): Percentage {
  const scale = 103.1668100711649;
  const offset = 1 - (scale % 100);
  const decayRate = 4.354415386753951;

  return Math.round(clamp(scale * Math.exp(-decayRate * probabilityLoss) + offset, { min: 0, max: 100 }));
}

function balanceMeans(ofColor: [Percentage, number][]) {
  const valid = ofColor.filter(x => x.every(isFinite));
  return Math.round((weightedMean(valid) + harmonicMean(valid.map(([v]) => v))) / 2);
}

function averageCpl(ofColor: number[]) {
  const valid = ofColor.filter(isFinite);
  return Math.round(valid.reduce((sum, val) => sum + Math.max(0, val), 0) / valid.length);
}

function evalToWinProb(ev: EvalScore | undefined) {
  return !ev ? NaN : (cpWinningChances(ev.cp ?? ev.mate! * 1000) + 1) / 2; // [0,1]
}

function forceCp(ev: EvalScore | undefined) {
  return clamp(ev?.cp ?? (ev?.mate === undefined ? NaN : ev.mate * 1000), { min: -1000, max: 1000 });
}
