import { Work, CevalEngine, CevalState, BrowserEngineInfo, EngineNotifier } from '../types';
import { Protocol } from '../protocol';
import { objectStorage, ObjectStorage } from 'common/objectStorage';
import { sharedWasmMemory } from '../util';
import { LegacyBot } from './legacyBot';
import type StockfishWeb from 'lila-stockfish-web';

export class StockfishWebEngine extends LegacyBot implements CevalEngine {
  failed: Error;
  protocol: Protocol;
  sfweb?: StockfishWeb;
  setLoaded = () => {};
  load = new Promise<void>(resolve => (this.setLoaded = resolve));
  store?: ObjectStorage<Uint8Array>;

  constructor(
    readonly info: BrowserEngineInfo,
    readonly status?: EngineNotifier,
    readonly variantMap?: (v: VariantKey) => string,
  ) {
    super(info);
    this.protocol = new Protocol(variantMap);
    this.boot().catch(e => {
      console.error(e);
      this.failed = e;
      this.status?.({ error: String(e) });
    });
  }
  get module() {
    return {
      uci: (x: string) => this.sfweb?.uci(x),
      listen: (x: (y: string) => void) => {
        if (this.sfweb) this.sfweb.listen = x;
      },
    };
  }

  getInfo() {
    return this.info;
  }

  async boot() {
    const [version, root, js] = [this.info.assets.version, this.info.assets.root, this.info.assets.js];
    const makeModule = await import(site.asset.url(`${root}/${js}`, { version, documentOrigin: true }));
    const module: StockfishWeb = await new Promise((resolve, reject) => {
      makeModule
        .default({
          wasmMemory: sharedWasmMemory(this.info.minMem!),
          onError: (msg: string) => reject(new Error(msg)),
          locateFile: (file: string) => site.asset.url(`${root}/${file}`, { version }),
        })
        .then(resolve)
        .catch(reject);
    });
    if (this.info.tech === 'NNUE') {
      if (this.info.variants?.length === 1) {
        const model = this.info.variants[0].toLowerCase(); // set variant first for fairy stockfish
        module.uci(`setoption name UCI_Variant value ${model === 'threecheck' ? '3check' : model}`);
      }
      this.store = await objectStorage<Uint8Array>({ store: 'nnue' }).catch(() => undefined);
      module.onError = this.makeErrorHandler(module);
      const nnueFilenames: string[] = this.info.assets.nnue ?? [];
      if (!nnueFilenames.length)
        for (let i = 0; ; i++) {
          const nnueFilename = module.getRecommendedNnue(i);
          if (!nnueFilename || nnueFilenames.includes(nnueFilename)) break;
          nnueFilenames.push(nnueFilename);
        }
      (await this.getModels(nnueFilenames)).forEach((nnueBuffer, i) => module.setNnueBuffer(nnueBuffer!, i));
    }
    module.listen = (data: string) => this.protocol.received(data);
    this.protocol.connected(cmd => module.uci(cmd));
    this.sfweb = module;
    this.setLoaded();
  }

  getModels(nnueFilenames: string[]): Promise<(Uint8Array | undefined)[]> {
    return Promise.all(
      nnueFilenames.map(async nnueFilename => {
        const storedBuffer = await this.store?.get(nnueFilename).catch(() => undefined);

        if (storedBuffer && storedBuffer.byteLength > 128 * 1024) return storedBuffer;
        const req = new XMLHttpRequest();

        req.open('get', site.asset.url(`lifat/nnue/${nnueFilename}`, { noVersion: true }), true);
        req.responseType = 'arraybuffer';
        req.onprogress = e => this.status?.({ download: { bytes: e.loaded, total: e.total } });

        const nnueBuffer = await new Promise<Uint8Array>((resolve, reject) => {
          req.onerror = () => reject(new Error(`NNUE download failed: ${req.status}`));
          req.onload = () => {
            if (req.status / 100 === 2) resolve(new Uint8Array(req.response));
            else reject(new Error(`NNUE download failed: ${req.status}`));
          };
          req.send();
        });
        this.status?.();
        this.store?.put(nnueFilename, nnueBuffer!).catch(() => console.warn('IDB store failed'));
        return nnueBuffer;
      }),
    );
  }

  makeErrorHandler(module: StockfishWeb) {
    return (msg: string) => {
      site.log(this.info.assets.js, msg);
      if (msg.startsWith('BAD_NNUE') && this.store) {
        // if we got this from IDB, we must remove it. but wait for getModels::store.put to finish first
        const index = Math.max(0, Number(msg.slice(9)));
        const nnueFilename = this.info.assets.nnue ?? module.getRecommendedNnue(index);
        setTimeout(() => {
          console.warn(`Corrupt NNUE file, removing ${nnueFilename} from IDB`);
          this.store!.remove(nnueFilename);
        }, 2000);
      } else this.status?.({ error: msg });
    };
  }

  getState() {
    return this.failed
      ? CevalState.Failed
      : !this.sfweb
      ? CevalState.Loading
      : this.protocol.isComputing()
      ? CevalState.Computing
      : CevalState.Idle;
  }

  start = (work?: Work) => this.protocol.compute(work);
  stop = () => this.protocol.compute(undefined);
  engineName = () => this.protocol.engineName;
  destroy = () => {
    this.sfweb?.uci('quit');
    this.sfweb = undefined;
  };
}
