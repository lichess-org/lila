import { Work, CevalEngine, CevalState, BrowserEngineInfo, EngineNotifier } from '../types';
import { Protocol } from '../protocol';
import { objectStorage } from 'common/objectStorage';
import { sharedWasmMemory } from '../util';
import type StockfishWeb from 'lila-stockfish-web';

export class StockfishWebEngine implements CevalEngine {
  failed: Error;
  protocol: Protocol;
  module?: StockfishWeb;

  constructor(
    readonly info: BrowserEngineInfo,
    readonly status?: EngineNotifier,
    readonly variantMap?: (v: VariantKey) => string,
  ) {
    this.protocol = new Protocol(variantMap);
    this.boot().catch(e => {
      console.error(e);
      this.failed = e;
      this.status?.({ error: String(e) });
    });
  }

  getInfo() {
    return this.info;
  }

  async boot() {
    const [version, root, js] = [this.info.assets.version, this.info.assets.root, this.info.assets.js];
    const makeModule = await import(site.asset.url(`${root}/${js}`, { version }));

    const module: StockfishWeb = await new Promise((resolve, reject) => {
      makeModule
        .default({
          wasmMemory: sharedWasmMemory(this.info.minMem!),
          onError: (msg: string) => reject(new Error(msg)),
          locateFile: (name: string) =>
            site.asset.url(`${root}/${name}`, { version, sameDomain: name.endsWith('.worker.js') }),
        })
        .then(resolve)
        .catch(reject);
    });
    if (this.info.tech === 'NNUE') {
      const nnueStore = await objectStorage<Uint8Array>({ store: 'nnue' }).catch(() => undefined);
      const nnueFilename = this.info.assets.nnue ?? module.getRecommendedNnue();

      module.onError = (msg: string) => {
        if (msg.startsWith('BAD_NNUE')) {
          // stockfish doesn't like our nnue file, let's remove it from IDB.
          // this will happen before nnueStore.put completes so let that finish before deletion.
          // otherwise, the resulting object store will best be described as undefined
          setTimeout(() => {
            console.warn(`Corrupt NNUE file, removing ${nnueFilename} from IDB`);
            nnueStore?.remove(nnueFilename);
          }, 2000);
        } else {
          console.error(msg);
          this.status?.({ error: msg });
        }
      };
      let nnueBuffer = await nnueStore?.get(nnueFilename).catch(() => undefined);

      if (!nnueBuffer || nnueBuffer.byteLength < 128 * 1024) {
        const req = new XMLHttpRequest();

        req.open('get', site.asset.url(`lifat/nnue/${nnueFilename}`, { noVersion: true }), true);
        req.responseType = 'arraybuffer';
        req.onprogress = e => this.status?.({ download: { bytes: e.loaded, total: e.total } });

        nnueBuffer = await new Promise((resolve, reject) => {
          req.onerror = () => reject(new Error(`NNUE download failed: ${req.status}`));
          req.onload = () => {
            if (req.status / 100 === 2) resolve(new Uint8Array(req.response));
            else reject(new Error(`NNUE download failed: ${req.status}`));
          };
          req.send();
        });
        this.status?.();
        nnueStore?.put(nnueFilename, nnueBuffer!).catch(() => console.warn('IDB store failed'));
      }
      if (this.info.variants?.length === 1) {
        const variant = this.info.variants[0].toLowerCase();
        module.postMessage(
          `setoption name UCI_Variant value ${variant === 'threecheck' ? '3check' : variant}`,
        );
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
  destroy = () => {
    this.module?.postMessage('quit');
    this.module = undefined;
  };
}
