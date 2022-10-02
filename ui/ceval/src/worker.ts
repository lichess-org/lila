import { Work } from './types';
import { Protocol } from './protocol';
import { Cache } from './cache';
import { readStream, CancellableStream } from 'common/stream';

interface WasmStockfishModule {
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
    Stockfish?: WasmStockfishModule;
    StockfishMv?: WasmStockfishModule;
  }
}

export abstract class AbstractWorker<T> {
  protected bootSuccess?: boolean;
  // undefined == not yet initialized, true == initialized, false == failed
  // if boot is async, then the subclass is responsible for setting bootSuccess.

  constructor(readonly opts: T) {}

  start(work: Work): void {
    if (!this.bootSuccess) {
      this.boot();
      if (this.boot.constructor.name !== 'AsyncFunction') this.bootSuccess = true;
    }
    this.getProtocol().compute(work);
  }

  stop(): void {
    this.getProtocol().compute(undefined);
  }

  isLoaded(): boolean {
    return this.bootSuccess === true;
  }

  initFailed(): boolean {
    return this.bootSuccess === false;
  }

  isComputing(): boolean {
    return this.getProtocol().isComputing();
  }

  engineName(): string | undefined {
    return this.getProtocol().engineName;
  }

  protected abstract getProtocol(): Protocol;
  protected abstract boot(): void;
  abstract destroy(): void;
}

export interface WebWorkerOpts {
  url: string;
}

export class WebWorker extends AbstractWorker<WebWorkerOpts> {
  private protocol = new Protocol();
  private worker: Worker | undefined;

  protected getProtocol(): Protocol {
    return this.protocol;
  }

  protected boot() {
    this.worker = new Worker(lichess.assetUrl(this.opts.url, { sameDomain: true }));
    this.worker.addEventListener('message', e => this.protocol.received(e.data), true);
    this.protocol.connected(cmd => this.worker?.postMessage(cmd));
  }

  destroy() {
    this.worker?.terminate();
  }
}

export interface ThreadedWasmWorkerOpts {
  baseUrl: string;
  module: 'Stockfish' | 'StockfishMv';
  version: string;
  downloadProgress?: (mb: number) => void;
  wasmMemory: WebAssembly.Memory;
  cache?: Cache;
}

export class ThreadedWasmWorker extends AbstractWorker<ThreadedWasmWorkerOpts> {
  private static protocols = { Stockfish: new Protocol(), StockfishMv: new Protocol() };
  private static sf: { Stockfish?: Stockfish; StockfishMv?: Stockfish } = {};

  protected getProtocol(): Protocol {
    return ThreadedWasmWorker.protocols[this.opts.module];
  }

  protected async boot() {
    if (!ThreadedWasmWorker.sf[this.opts.module]) {
      const version = this.opts.version;
      const cache = this.opts.cache;

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
      try {
        // Load Emscripten module.
        await lichess.loadScript(this.opts.baseUrl + 'stockfish.js', { version });
        const sf = await window[this.opts.module]!({
          wasmBinary,
          locateFile: (path: string) =>
            lichess.assetUrl(this.opts.baseUrl + path, { version, sameDomain: path.endsWith('.worker.js') }),
          wasmMemory: this.opts.wasmMemory,
        });

        const protocol = this.getProtocol();
        sf.addMessageListener(protocol.received.bind(protocol));
        protocol.connected(msg => sf.postMessage(msg));
        ThreadedWasmWorker.sf[this.opts.module] = sf;
        this.bootSuccess = true;
      } catch (err) {
        console.error(err);
        this.bootSuccess = false;
      }
    }
  }

  destroy() {
    // Terminated instances to not get freed reliably
    // (https://github.com/lichess-org/lila/issues/7334). So instead of
    // destroying, just stop instances and keep them around for reuse.
    this.getProtocol().compute(undefined);
  }
}

export interface ExternalEngine {
  id: string;
  name: string;
  variants: string;
  maxThreads: number;
  maxHash: number;
  clientSecret: string;
  officialStockfish?: boolean;
  endpoint: string;
}

export class ExternalWorker extends AbstractWorker<ExternalEngine> {
  private protocol = new Protocol();
  private session = Math.random().toString(36).slice(2, 12);

  protected getProtocol() {
    return this.protocol;
  }

  stream: CancellableStream;

  boot() {
    const url = new URL(`${this.opts.endpoint}/api/external-engine/${this.opts.id}/analyse`);
    fetch(url.href, {
      method: 'post',
      cache: 'default',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'omit',
      body: JSON.stringify({
        clientSecret: this.opts.clientSecret,
        work: {
          sessionId: this.session,
          // TODO
          threads: this.opts.maxThreads,
          hash: this.opts.maxHash,
          maxDepth: 99,
          multiPv: 1,
          variant: 'standard',
          initialFen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
          moves: [],
        },
      }),
    })
      .then(res => {
        // no need to call protocol.connected, ever
        // but need to set bootSuccess
        this.bootSuccess = true;
        this.stream = readStream((line: string) => {
          console.log(line);
          this.protocol.received(line);
        })(res);
        // ws.onclose = () => {
        //   this.protocol.disconnected();
        //   if (this.ws) setTimeout(() => this.boot(), 10_000);
        // };
      })
      .catch(err => {
        console.error(err);
        this.bootSuccess = false;
      });
  }

  destroy() {
    this.stream.cancel();
  }
}
