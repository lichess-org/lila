import { CevalWorker, CevalState } from './worker';
import { Protocol } from '../protocol';
import { Redraw, Work } from '../types';
import { Cache } from '../cache';

export interface VariantWasmOpts {
  baseUrl: string;
  version: string;
  nnueProgress?: (mb: number) => void;
  wasmMemory: WebAssembly.Memory;
  cache?: Cache;
}

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
    StockfishMv?: WasmModule;
    Stockfish?: WasmModule; // hopefully this will be unused, but hang on to it
  }
}

export class VariantWasmWorker implements CevalWorker {
  private failed = false;
  private protocol = new Protocol();
  private sf: Promise<void>;

  constructor(
    private opts: VariantWasmOpts,
    private redraw: Redraw,
  ) {}

  getState() {
    return !this.sf
      ? CevalState.Initial
      : this.failed
      ? CevalState.Failed
      : !this.getProtocol().engineName
      ? CevalState.Loading
      : this.getProtocol().isComputing()
      ? CevalState.Computing
      : CevalState.Idle;
  }

  private getProtocol(): Protocol {
    return this.protocol;
  }

  private async boot(): Promise<Stockfish> {
    const version = this.opts.version;
    const cache = this.opts.cache;
    const useNnue = this.opts.nnueProgress !== undefined;

    // Fetch WASM file ourselves, for caching and progress indication.
    let wasmBinary: ArrayBuffer | undefined;
    if (cache) {
      const wasmPath = this.opts.baseUrl + 'stockfish.wasm';
      try {
        const [found, data] = await cache.get(wasmPath, version);
        if (found) wasmBinary = data;
      } catch (e) {
        console.log('ceval: idb cache load failed:', e);
      }
      if (!wasmBinary) {
        wasmBinary = await new Promise((resolve, reject) => {
          const req = new XMLHttpRequest();
          req.open('GET', lichess.assetUrl(wasmPath, { version }), true);
          req.responseType = 'arraybuffer';
          req.onerror = event => reject(event);
          req.onprogress = event => this.opts.nnueProgress?.(event.loaded);
          req.onload = _ => {
            this.opts.nnueProgress?.(0);
            resolve(req.response);
          };
          req.send();
        });
      }
      try {
        await cache.set(wasmPath, version, wasmBinary);
      } catch (e) {
        console.log('ceval: idb cache store failed:', e);
      }
    }

    // Load Emscripten module.
    await lichess.loadIife(this.opts.baseUrl + 'stockfish.js', { version });
    const sf = await window[useNnue ? 'Stockfish' : 'StockfishMv']!({
      wasmBinary,
      locateFile: (path: string) =>
        lichess.assetUrl(this.opts.baseUrl + path, { version, sameDomain: path.endsWith('.worker.js') }),
      wasmMemory: this.opts.wasmMemory,
    });

    const protocol = this.getProtocol();
    sf.addMessageListener(protocol.received.bind(protocol));
    protocol.connected(msg => sf.postMessage(msg));
    return sf;
  }

  start(work: Work) {
    this.getProtocol().compute(work);

    if (!this.sf) {
      this.sf = this.boot().then(
        () => {},
        err => {
          console.error(err);
          this.failed = true;
          this.redraw();
        },
      );
    }
  }

  stop() {
    this.getProtocol().compute(undefined);
  }

  engineName() {
    return this.getProtocol().engineName;
  }

  destroy() {
    // Terminated instances to not get freed reliably
    // (https://github.com/lichess-org/lila/issues/7334). So instead of
    // destroying, just stop instances and keep them around for reuse.
    this.stop();
  }
}
