import { Work } from '../types';
import { CevalWorker, CevalState } from './worker';
import { Protocol } from '../protocol';
import { objectStorage } from 'common/objectStorage';
import type StockfishWeb from 'stockfish-web';

const version = 'sf1600';

export class WasmWorker implements CevalWorker {
  private failed = false;
  private protocol = new Protocol({ reasonableDepthLimit: 99 });
  private module: StockfishWeb;

  constructor(
    readonly wasmMemory: WebAssembly.Memory,
    readonly nnueProgress?: (download?: { bytes: number; total: number }) => void,
  ) {
    this.boot().catch(e => {
      console.error(e);
      this.failed = true;
    });
  }

  async boot() {
    const makeStockfish = await import(lichess.assetUrl('npm/stockfish-web/stockfishWeb.js', { version }));

    const module = await makeStockfish.default({
      wasmMemory: this.wasmMemory,
      locateFile: (name: string) =>
        lichess.assetUrl(`npm/stockfish-web/${name}`, { version, sameDomain: name.endsWith('.worker.js') }),
    });

    const nnueFilename = module.getRecommendedNnue();
    const nnueVersion = nnueFilename.slice(3, 9);
    const nnueStore = await objectStorage<Uint8Array>({ store: 'nnue' }).catch(() => undefined);

    module.errorHandler = (msg: string) => {
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

    if (this.nnueProgress && nnueStore) {
      let nnueBuffer = await nnueStore.get(nnueVersion).catch(() => undefined);

      if (!nnueBuffer || nnueBuffer.byteLength < 1024 * 1024) {
        const req = new XMLHttpRequest();

        req.open('get', lichess.assetUrl(`lifat/nnue/${nnueFilename}`, { version: nnueVersion }), true);
        req.responseType = 'arraybuffer';
        req.onprogress = e => this.nnueProgress!({ bytes: e.loaded, total: e.total });

        nnueBuffer = await new Promise((resolve, reject) => {
          req.onerror = reject;
          req.onload = _ => resolve(new Uint8Array(req.response));
          req.send();
        });

        this.nnueProgress();
        nnueStore.put(nnueVersion, nnueBuffer!).catch(() => console.warn('IDB store failed'));
      }
      module.setNnueBuffer(nnueBuffer!);
    }
    module.addMessageListener((data: string) => this.protocol.received(data));
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
