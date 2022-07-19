export function isEvalBetter(a: Tree.ClientEval, b: Tree.ClientEval): boolean {
  return a.depth > b.depth || (a.depth === b.depth && a.nodes > b.nodes);
}

export function renderEval(e: number): string {
  e = Math.max(Math.min(Math.round(e / 10) / 10, 99), -99);
  return (e > 0 ? '+' : '') + e.toFixed(1);
}

// making the evalbar a bit more flat, because sente's getting +1.6 right off the bat, which doesn't look good
export function cubicRegressionEval(x: number): number {
  return 1 + 0.4505495 * x + 1.284439 * Math.pow(10, -16) * Math.pow(x, 2) + 0.5494505 * Math.pow(x, 3);
}
