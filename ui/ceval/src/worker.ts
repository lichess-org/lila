import { Cache } from './cache';
import { Protocol } from './protocol';
import { Config, Work } from './types';

interface WasmEngineModule {
  (opts: {
    wasmBinary?: ArrayBuffer;
    locateFile(path: string): string;
    wasmMemory: WebAssembly.Memory;
  }): Promise<Engine>;
}

interface Engine {
  addMessageListener(cb: (msg: string) => void): void;
  postMessage(msg: string): void;
}

declare global {
  interface Window {
    YaneuraOu_K_P?: WasmEngineModule;
    Fairy?: WasmEngineModule;
  }
}

export abstract class AbstractWorker<T> {
  private booted: boolean;

  constructor(readonly opts: T) {}

  start(work: Work): void {
    if (!this.booted) {
      this.boot({ threads: work.threads, hashSize: work.hashSize });
      this.booted = true;
    }
    this.getProtocol().compute(work);
  }

  stop(): void {
    this.getProtocol().compute(undefined);
  }

  isComputing(): boolean {
    return this.getProtocol().isComputing();
  }

  protected abstract getProtocol(): Protocol;
  protected abstract boot(cfg: Config): void;
  abstract destroy(): void;
}

export interface ThreadedWasmWorkerOpts {
  baseName: string;
  baseUrl: string;
  module: 'YaneuraOu_K_P' | 'Stockfish';
  version: string;
  wasmMemory: WebAssembly.Memory;
  downloadProgress?: (mb: number) => void;
  cache?: Cache;
}

export class ThreadedWasmWorker extends AbstractWorker<ThreadedWasmWorkerOpts> {
  private static protocols = { YaneuraOu_K_P: new Protocol(), Stockfish: new Protocol() };
  private static engine: { YaneuraOu_K_P?: Engine; Stockfish?: Engine } = {};

  protected getProtocol(): Protocol {
    return ThreadedWasmWorker.protocols[this.opts.module];
  }

  protected async boot(cfg: Config) {
    if (!ThreadedWasmWorker.engine[this.opts.module]) {
      const version = this.opts.version;
      const cache = this.opts.cache;

      // Fetch WASM file ourselves, for caching and progress indication.
      let wasmBinary: ArrayBuffer | undefined;
      if (cache) {
        const wasmPath = this.opts.baseUrl + this.opts.baseName + '.wasm';
        if (cache) {
          try {
            const [found, data] = await cache.get(wasmPath, version);
            if (found) wasmBinary = data;
          } catch (e) {
            console.log('ceval: idb cache load failed:', e);
          }
        }

        if (!wasmBinary) {
          wasmBinary = await new Promise((resolve, reject) => {
            const req = new XMLHttpRequest();
            req.open('GET', window.lishogi.assetUrl(wasmPath, { version }), true);
            req.responseType = 'arraybuffer';
            req.onerror = event => reject(event);
            req.onprogress = event => this.opts.downloadProgress?.(event.loaded);
            req.onload = _ => {
              this.opts.downloadProgress?.(0);
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
      await window.lishogi.loadScript(this.opts.baseUrl + this.opts.baseName + '.js', { version });
      const engine = await window[this.opts.module]!({
        wasmBinary,
        locateFile: (path: string) =>
          window.lishogi.assetUrl(this.opts.baseUrl + path, {
            version: version,
            sameDomain: path.endsWith('.worker.js'),
          }),
        wasmMemory: this.opts.wasmMemory,
      });

      const protocol = this.getProtocol();
      protocol.config = cfg;
      engine.addMessageListener(protocol.received.bind(protocol));
      protocol.connected(msg => {
        console.info(`Engine <- ${msg}`);
        engine.postMessage(msg);
      });
      ThreadedWasmWorker.engine[this.opts.module] = engine;
    }
  }

  destroy() {
    // Terminated instances to not get freed reliably
    // (https://github.com/lichess-org/lila/issues/7334). So instead of
    // destroying, just stop instances and keep them around for reuse.
    this.getProtocol().compute(undefined);
  }
}
