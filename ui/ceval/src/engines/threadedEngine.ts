import { Protocol } from '../protocol';
import { Work, CevalEngine, CevalState, BrowserEngineInfo, EngineNotifier } from '../types';
import { sharedWasmMemory } from '../util';
import { Cache } from '../cache';
import { LegacyBot } from './legacyBot';

interface WasmModule {
  (opts: {
    wasmBinary?: ArrayBuffer;
    locateFile(path: string): string;
    wasmMemory: WebAssembly.Memory;
    printErr(msg: string): void;
    onError(err: Error): void;
  }): Promise<Stockfish>;
}

interface Stockfish {
  addMessageListener(cb: (msg: string) => void): void;
  removeMessageListener(cb: (msg: string) => void): void;
  postMessage(msg: string): void;
}

declare global {
  interface Window {
    Stockfish?: WasmModule;
    StockfishMv?: WasmModule;
  }
}

export class ThreadedEngine extends LegacyBot implements CevalEngine {
  failed: Error;
  protocol: Protocol;
  loaded = () => {};
  isLoaded = new Promise<void>(resolve => {
    this.loaded = resolve;
  });
  moduleProxy?: { postMessage: (msg: string) => void; listen: (cb: (msg: string) => void) => void };

  constructor(
    readonly info: BrowserEngineInfo,
    readonly status?: EngineNotifier,
    readonly variantMap?: (v: string) => string,
  ) {
    super(info);
    if (!this.info.isBot) this.protocol = new Protocol(this.variantMap);
    this.boot().catch(this.onError);
  }

  get module() {
    return this.moduleProxy;
  }
  load(): Promise<void> {
    return this.isLoaded;
  }

  onError = (err: Error) => {
    console.error(err);
    this.failed = err;
    this.status?.({ error: String(err) });
  };

  getInfo() {
    return this.info;
  }

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

  private async boot() {
    const [root, js, wasm, version] = [
        this.info.assets.root,
        this.info.assets.js,
        this.info.assets.wasm,
        this.info.assets.version,
      ],
      wasmPath = `${root}/${wasm}`;

    let wasmBinary: ArrayBuffer | undefined;
    if (wasm) {
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
          req.open('GET', lichess.asset.url(wasmPath, { version }), true);
          req.responseType = 'arraybuffer';
          req.onerror = event => reject(event);
          req.onprogress = event => this.status?.({ download: { bytes: event.loaded, total: event.total } });
          req.onload = _ => {
            this.status?.();
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
    await lichess.asset.loadIife(`${root}/${js}`, { version });
    const sf = await window[this.info.id === '__sf11mv' ? 'StockfishMv' : 'Stockfish']!({
      wasmBinary,
      printErr: (msg: string) => this.onError(new Error(msg)),
      onError: this.onError,
      locateFile: (path: string) =>
        lichess.asset.url(`${root}/${path}`, { version, sameDomain: path.endsWith('.worker.js') }),
      wasmMemory: sharedWasmMemory(this.info.minMem!),
    });
    if (!this.info.isBot) {
      sf.addMessageListener(data => this.protocol.received(data));
      this.protocol.connected(msg => sf.postMessage(msg));
    } else {
      let oldListener: (msg: string) => void;
      this.moduleProxy = {
        postMessage: (msg: string) => sf.postMessage(msg),
        listen: (cb: (msg: string) => void) => {
          if (oldListener) sf.removeMessageListener(oldListener);
          sf.addMessageListener((oldListener = cb));
        },
      };
    }
    this.loaded();
    return sf;
  }

  async start(work: Work) {
    this.protocol.compute(work);
  }

  stop() {
    this.protocol.compute(undefined);
  }

  destroy() {
    this.module?.postMessage('quit');
    this.moduleProxy = undefined;
  }
}
