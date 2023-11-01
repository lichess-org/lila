import { Protocol } from '../protocol';
import { Redraw, Work, CevalEngine, CevalState, BrowserEngineInfo } from '../types';
import { sharedWasmMemory } from '../util';
import { Cache } from '../cache';

interface WasmModule {
  (opts: {
    wasmBinary?: ArrayBuffer;
    locateFile(path: string): string;
    wasmMemory: WebAssembly.Memory;
  }): Promise<Stockfish>;
}

interface Stockfish {
  addMessageListener(cb: (msg: string) => void): void;
  postMessage(msg: string): void;
}

declare global {
  interface Window {
    Stockfish?: WasmModule;
    StockfishMv?: WasmModule;
  }
}

export class ThreadedEngine implements CevalEngine {
  failed: boolean;
  protocol: Protocol;

  constructor(
    readonly info: BrowserEngineInfo,
    readonly redraw: Redraw,
    readonly progress?: (download?: { bytes: number; total: number }) => void,
    readonly variantMap?: (v: string) => string,
  ) {}

  getState() {
    return !this.protocol
      ? CevalState.Initial
      : this.failed
      ? CevalState.Failed
      : !this.protocol.engineName
      ? CevalState.Loading
      : this.protocol.isComputing()
      ? CevalState.Computing
      : CevalState.Idle;
  }

  private async boot(): Promise<Stockfish> {
    const [root, js, wasm, version] = [
        this.info.assets.root,
        this.info.assets.js,
        this.info.assets.wasm,
        this.info.assets.version,
      ],
      wasmPath = `${root}/${wasm}`;

    let wasmBinary: ArrayBuffer | undefined;
    if (this.info.id === '__sf14nnue') {
      const cache = window.indexedDB && new Cache('ceval-wasm-cache');
      try {
        if (cache) {
          const [found, data] = await cache.get(wasmPath, version!);
          if (found) wasmBinary = data;
        }
      } catch (e) {
        console.log('ceval: idb cache load failed:', e);
      }
      if (!wasmBinary) {
        wasmBinary = await new Promise((resolve, reject) => {
          const req = new XMLHttpRequest();
          req.open('GET', lichess.assetUrl(wasmPath, { version }), true);
          req.responseType = 'arraybuffer';
          req.onerror = event => reject(event);
          req.onprogress = event => this.progress?.({ bytes: event.loaded, total: event.total });
          req.onload = _ => {
            this.progress?.();
            resolve(req.response);
          };
          req.send();
        });
      }
      try {
        await cache.set(wasmPath, version!, wasmBinary);
      } catch (e) {
        console.log('ceval: idb cache store failed:', e);
      }
    }

    // Load Emscripten module.
    await lichess.loadIife(`${root}/${js}`, { version });
    const sf = await window[this.info.id === '__sf11mv' ? 'StockfishMv' : 'Stockfish']!({
      wasmBinary,
      locateFile: (path: string) =>
        lichess.assetUrl(`${root}/${path}`, { version, sameDomain: path.endsWith('.worker.js') }),
      wasmMemory: sharedWasmMemory(this.info.minMem!),
    });

    sf.addMessageListener(data => this.protocol.received(data));
    this.protocol.connected(msg => sf.postMessage(msg));
    return sf;
  }

  async start(work: Work) {
    if (!this.protocol) {
      this.protocol = new Protocol(this.variantMap);
      this.boot().catch(err => {
        console.error(err);
        this.failed = true;
        this.redraw();
      });
    }
    this.protocol.compute(work);
  }

  stop() {
    this.protocol.compute(undefined);
  }

  destroy() {
    // Terminated instances to not get freed reliably
    // (https://github.com/lichess-org/lila/issues/7334). So instead of
    // destroying, just stop instances and keep them around for reuse.
    this.stop();
  }
}
