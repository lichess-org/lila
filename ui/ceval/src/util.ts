import { CevalTechnology } from './types';
import { ExternalWorkerOpts } from './worker';

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

export const pow2floor = (n: number) => {
  let pow2 = 1;
  while (pow2 * 2 <= n) pow2 *= 2;
  return pow2;
};

export const sharedWasmMemory = (initial: number, maximum: number): WebAssembly.Memory =>
  new WebAssembly.Memory({ shared: true, initial, maximum });

export function sendableSharedWasmMemory(initial: number, maximum: number): WebAssembly.Memory | undefined {
  // Atomics
  if (typeof Atomics !== 'object') return;

  // SharedArrayBuffer
  if (typeof SharedArrayBuffer !== 'function') return;

  // Shared memory
  const mem = sharedWasmMemory(initial, maximum);
  if (!(mem.buffer instanceof SharedArrayBuffer)) return;

  // Structured cloning
  try {
    window.postMessage(mem.buffer, '*');
  } catch (e) {
    return undefined;
  }
  return mem;
}

export function defaultDepth(technology: CevalTechnology, threads: number, multiPv: number): number {
  const extraDepth = Math.min(Math.max(threads - multiPv, 0), 6);
  switch (technology) {
    case 'asmjs':
      return 18;
    case 'wasm':
      return 20;
    case 'external':
      return 24 + extraDepth;
    default:
      return 22 + extraDepth;
  }
}

export function engineName(technology: CevalTechnology, externalOpts: ExternalWorkerOpts | null): string {
  switch (technology) {
    case 'external':
      return externalOpts!.name;
    case 'wasm':
    case 'asmjs':
      return 'Stockfish 10+';
    case 'hce':
      return 'Stockfish 11+';
    case 'nnue':
      return 'Stockfish 14+';
  }
}
