import { isAndroid, isIOS, isIPad } from 'common/device';
import { pow2floor, sharedWasmMemory } from './util';
import { ExternalEngine } from './worker';

export type CevalTechnology = 'asmjs' | 'wasm' | 'hce' | 'nnue' | 'external';

export interface CevalPlatform {
  technology: CevalTechnology;
  growableSharedMem: boolean;
  supportsNnue: boolean;
  maxThreads: number;
  maxWasmPages: (minPages: number) => number;
  maxHashSize: () => number;
}

// select external > nnue > hce > wasm > asmjs
export function detectPlatform(
  officialStockfish: boolean,
  enableNnue: boolean,
  externalEngine?: ExternalEngine,
): CevalPlatform {
  let technology: CevalTechnology = 'asmjs',
    growableSharedMem = false,
    supportsNnue = false;

  if (externalEngine) technology = 'external';
  else if (
    typeof WebAssembly === 'object' &&
    typeof WebAssembly.validate === 'function' &&
    WebAssembly.validate(Uint8Array.from([0, 97, 115, 109, 1, 0, 0, 0]))
  ) {
    technology = 'wasm'; // WebAssembly 1.0
    const sharedMem = sendableSharedWasmMemory(1, 2);
    if (sharedMem?.buffer) {
      technology = 'hce';
      try {
        sharedMem.grow(1);
        growableSharedMem = true;
      } catch (e) {
        // memory growth not supported
      }
      // i32x4.dot_i16x8_s, i32x4.trunc_sat_f64x2_u_zero
      const sourceWithSimd = Uint8Array.from([0, 97, 115, 109, 1, 0, 0, 0, 1, 12, 2, 96, 2, 123, 123, 1, 123, 96, 1, 123, 1, 123, 3, 3, 2, 0, 1, 7, 9, 2, 1, 97, 0, 0, 1, 98, 0, 1, 10, 19, 2, 9, 0, 32, 0, 32, 1, 253, 186, 1, 11, 7, 0, 32, 0, 253, 253, 1, 11]); // prettier-ignore
      supportsNnue = WebAssembly.validate(sourceWithSimd);
      if (supportsNnue && officialStockfish && enableNnue) technology = 'nnue';
    }
  }

  const maxThreads = externalEngine
    ? externalEngine.maxThreads
    : technology == 'nnue' || technology == 'hce'
    ? Math.min(
        Math.max((navigator.hardwareConcurrency || 1) - 1, 1),
        growableSharedMem ? 32 : officialStockfish ? 2 : 1,
      )
    : 1;

  // the numbers returned by maxHashMB seem small, but who knows if wasm stockfish performance even
  // scales like native stockfish with increasing hash. prefer smaller, non-crashing values
  // steer the high performance crowd towards external engine as it gets better
  const maxHashMB = (): number => {
    let maxHash = 256; // this is conservative but safe, mostly desktop firefox / mac safari users here
    if (navigator.deviceMemory) maxHash = pow2floor(navigator.deviceMemory * 128); // chrome/edge/opera
    else if (isAndroid()) maxHash = 64; // budget androids are easy to crash @ 128
    else if (isIPad()) maxHash = 64; // iPadOS safari pretends to be desktop but acts more like iphone
    else if (isIOS()) maxHash = 32;
    return maxHash;
  };

  return {
    technology,
    growableSharedMem,
    supportsNnue,
    maxThreads,
    maxWasmPages: (minPages: number): number => {
      if (!growableSharedMem) return minPages;
      let maxPages = 32768; // hopefully desktop browser, 2 GB max shared
      if (isAndroid()) maxPages = 8192; // 512 MB max shared
      else if (isIPad()) maxPages = 8192; // 512 MB max shared
      else if (isIOS()) maxPages = 4096; // 256 MB max shared
      return Math.max(minPages, maxPages);
    },
    maxHashSize: () => (technology == 'external' ? externalEngine?.maxHash || 16 : maxHashMB()),
  };
}

function sendableSharedWasmMemory(initial: number, maximum: number): WebAssembly.Memory | undefined {
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
