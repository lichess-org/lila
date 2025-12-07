import {
  CevalState,
  type Work,
  type CevalEngine,
  type BrowserEngineInfo,
  type EngineNotifier,
} from '../types';
import { Protocol } from '../protocol';
import { sharedWasmMemory } from '../util';
import type StockfishWeb from '@lichess-org/stockfish-web';
import { bigFileStorage } from '@/bigFileStorage';

export class StockfishWebEngine implements CevalEngine {
  failed: Error;
  protocol: Protocol;
  module?: StockfishWeb;

  constructor(
    readonly info: BrowserEngineInfo,
    readonly status?: EngineNotifier | undefined,
    readonly variantMap?: (v: VariantKey) => string,
  ) {
    this.protocol = new Protocol(variantMap);
    this.boot().catch(e => {
      this.failed = e;
      this.status?.({ error: String(e) });
    });
  }

  getInfo(): BrowserEngineInfo {
    return this.info;
  }

  async boot(): Promise<void> {
    const [root, js] = [this.info.assets.root, this.info.assets.js];
    const scriptUrl = site.asset.url(`${root}/${js}`, { documentOrigin: true });
    const makeModule = await import(scriptUrl);
    const module: StockfishWeb = await makeModule.default({
      wasmMemory: sharedWasmMemory(this.info.minMem!),
      locateFile: (file: string) => site.asset.url(`${root}/${file}`),
      mainScriptUrlOrBlob: scriptUrl,
    });
    if (this.info.tech === 'NNUE') {
      if (this.info.variants?.length === 1) {
        const model = this.info.variants[0].toLowerCase(); // set variant first for fairy stockfish
        module.uci(`setoption name UCI_Variant value ${model === 'threecheck' ? '3check' : model}`);
      }
      module.onError = this.makeErrorHandler(module);
      const nnueFilenames: string[] = this.info.assets.nnue ?? [];
      if (!nnueFilenames.length)
        for (let i = 0; ; i++) {
          const nnueFilename = module.getRecommendedNnue(i);
          if (!nnueFilename || nnueFilenames.includes(nnueFilename)) break;
          nnueFilenames.push(nnueFilename);
        }
      await Promise.all(
        nnueFilenames.map(async (name, index) => {
          module.setNnueBuffer(
            await bigFileStorage().get(site.asset.url(`lifat/nnue/${name}`), (bytes, total) =>
              this.status?.({ download: { bytes, total } }),
            ),
            index,
          );
        }),
      );
    }
    module.listen = (data: string) => this.protocol.received(data);
    this.protocol.connected(cmd => module.uci(cmd));
    this.module = module;
  }

  makeErrorHandler(module: StockfishWeb) {
    return (msg: string): void => {
      if (msg.startsWith('BAD_NNUE')) {
        const index = Math.max(0, Number(msg.slice(9)));
        const nnueFilename = this.info.assets.nnue ?? module.getRecommendedNnue(index);
        // if we got this from bigFileStorage, we should remove it. but wait for async ops to finish first
        setTimeout(() => {
          console.warn(`Corrupt NNUE file, removing ${nnueFilename} from OPFS/IDB`);
          bigFileStorage().delete(site.asset.url(`lifat/nnue/${nnueFilename}`));
        }, 2000);
      } else this.status?.({ error: msg });
    };
  }

  getState(): CevalState {
    return this.failed
      ? CevalState.Failed
      : !this.module
        ? CevalState.Loading
        : this.protocol.isComputing()
          ? CevalState.Computing
          : CevalState.Idle;
  }

  start = (work?: Work): void => this.protocol.compute(work);
  stop = (): void => this.protocol.compute(undefined);
  engineName = (): string | undefined => this.protocol.engineName;
  destroy = (): void => {
    this.module?.uci('quit');
    this.module = undefined;
  };
}
