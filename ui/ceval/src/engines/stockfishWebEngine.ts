import { Work, CevalEngine, CevalState, BrowserEngineInfo } from '../types';
import { Protocol } from '../protocol';
import { objectStorage } from 'common/objectStorage';
import { sharedWasmMemory } from '../util';
import type StockfishWeb from 'lila-stockfish-web';

export class StockfishWebEngine implements CevalEngine {
  failed = false;
  protocol: Protocol;
  module: StockfishWeb;

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
  }

  async boot() {
    const [version, root, js] = [this.info.assets.version, this.info.assets.root, this.info.assets.js];
    const makeModule = await import(lichess.assetUrl(`${root}/${js}`, { version }));

    const module: StockfishWeb = await makeModule.default({
      wasmMemory: sharedWasmMemory(this.info.minMem!),
      locateFile: (name: string) =>
        lichess.assetUrl(`${root}/${name}`, { version, sameDomain: name.endsWith('.worker.js') }),
    });

    if (!this.info.id.endsWith('hce')) {
      const nnueStore = await objectStorage<Uint8Array>({ store: 'nnue' }).catch(() => undefined);
      const nnueFilename = module.getRecommendedNnue();

      module.onError = (msg: string) => {
        if (msg.startsWith('BAD_NNUE')) {
          // stockfish doesn't like our nnue file, let's remove it from IDB.
          // this will happen before nnueStore.put completes so let that finish before deletion.
          // otherwise, the resulting object store will best be described as undefined
          setTimeout(() => {
            console.warn(`Corrupt NNUE file, removing ${nnueFilename} from IDB`);
            nnueStore?.remove(nnueFilename);
          }, 2000);
        } else console.error(msg);
      };
      let nnueBuffer = await nnueStore?.get(nnueFilename).catch(() => undefined);

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
        nnueStore?.put(nnueFilename, nnueBuffer!).catch(() => console.warn('IDB store failed'));
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
