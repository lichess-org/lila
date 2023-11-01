import { Work, CevalEngine, CevalState, BrowserEngineInfo } from '../types';
import { Protocol } from '../protocol';
import { objectStorage } from 'common/objectStorage';
import { sharedWasmMemory } from '../util';
import type StockfishWeb from 'lila-stockfish-web';

export class StockfishWebEngine implements CevalEngine {
  failed = false;
  protocol: Protocol;
  module: StockfishWeb;
  wasmMemory: WebAssembly.Memory;

  constructor(
    readonly info: BrowserEngineInfo,
    readonly nnue?: (download?: { bytes: number; total: number }) => void,
    readonly variantMap?: (v: string) => string,
  ) {
    this.protocol = new Protocol(variantMap);
    this.boot().catch(e => {
      console.error(e);
      this.failed = true;
    });
    this.wasmMemory = sharedWasmMemory(info.minMem!);
  }

  async boot() {
    const [version, root, js] = [this.info.assets.version, this.info.assets.root, this.info.assets.js];
    const makeModule = await import(lichess.assetUrl(`${root}/${js}`, { version }));

    const module: StockfishWeb = await makeModule.default({
      wasmMemory: this.wasmMemory,
      locateFile: (name: string) =>
        lichess.assetUrl(`${root}/${name}`, {
          version,
          sameDomain: name.endsWith('.worker.js'),
        }),
    });

    if (!this.info.id.endsWith('hce')) {
      const nnueStore = await objectStorage<Uint8Array>({ store: 'nnue' }).catch(() => undefined);
      const nnueFilename = module.getRecommendedNnue();
      const nnueVersion = nnueFilename.slice(3, 9);

      module.onError = (msg: string) => {
        if (msg.startsWith('BAD_NNUE')) {
          // stockfish doesn't like our nnue file, let's remove it from IDB.
          // this will happen before nnueStore.put completes so let that finish before deletion.
          // otherwise, the resulting object store will best be described as undefined
          setTimeout(() => {
            console.warn(`Corrupt NNUE file, removing ${nnueVersion} from IDB`);
            nnueStore?.remove(nnueVersion);
          }, 2000);
        } else console.error(msg);
      };
      let nnueBuffer = await nnueStore?.get(nnueVersion).catch(() => undefined);

      if (!nnueBuffer || nnueBuffer.byteLength < 1024 * 1024) {
        const req = new XMLHttpRequest();

        req.open('get', lichess.assetUrl(`lifat/nnue/${nnueFilename}`, { noVersion: true }), true);
        req.responseType = 'arraybuffer';
        req.onprogress = e => this.nnue?.({ bytes: e.loaded, total: e.total });

        nnueBuffer = await new Promise((resolve, reject) => {
          req.onerror = reject;
          req.onload = _ => resolve(new Uint8Array(req.response));
          req.send();
        });

        this.nnue?.();
        nnueStore?.put(nnueVersion, nnueBuffer!).catch(() => console.warn('IDB store failed'));
      }
      module.setNnueBuffer(nnueBuffer!);
    }
    module.listen = (data: string) => this.protocol.received(data);
    this.protocol.connected(cmd => module.postMessage(cmd));
    this.module = module;
  }

  getState() {
    return this.failed
      ? CevalState.Failed
      : !this.module
      ? CevalState.Loading
      : this.protocol.isComputing()
      ? CevalState.Computing
      : CevalState.Idle;
  }

  start = (work?: Work) => this.protocol.compute(work);
  stop = () => this.protocol.compute(undefined);
  engineName = () => this.protocol.engineName;
  destroy = () => this.stop();
}
