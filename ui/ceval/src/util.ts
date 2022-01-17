export function isEvalBetter(a: Tree.ClientEval, b: Tree.ClientEval): boolean {
  return a.depth > b.depth || (a.depth === b.depth && a.nodes > b.nodes);
}

export function renderEval(e: number): string {
  e = Math.max(Math.min(Math.round(e / 10) / 10, 99), -99);
  return (e > 0 ? '+' : '') + e.toFixed(1);
}

export function sanIrreversible(variant: VariantKey, san: string): boolean {
  if (san.startsWith('O-O')) return true;
  if (variant === 'crazyhouse') return false;
  if (san.includes('x')) return true; // capture
  if (san.toLowerCase() === san) return true; // pawn move
  return variant === 'threeCheck' && san.includes('+');
}
