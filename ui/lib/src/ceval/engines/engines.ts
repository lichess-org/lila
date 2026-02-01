import type { BrowserEngineInfo, ExternalEngineInfo, EngineInfo, CevalEngine } from '../types';
import type CevalCtrl from '../ctrl';
import { SimpleEngine } from './simpleEngine';
import { StockfishWebEngine } from './stockfishWebEngine';
import { ThreadedEngine } from './threadedEngine';
import { ExternalEngine } from './externalEngine';
import { storedStringProp, type StoredProp } from '@/storage';
import { isAndroid, isIos, isIPad, features as browserSupport } from '@/device';
import { xhrHeader } from '@/xhr';
import { lichessRules } from 'chessops/compat';
import { log } from '@/permalog';

export class Engines {
  private activeEngine: EngineInfo | undefined = undefined;
  localEngines: BrowserEngineInfo[];
  localEngineMap: Map<string, WithMake>;
  externalEngines: ExternalEngineInfo[];
  selectProp: StoredProp<string>;

  constructor(private ctrl: CevalCtrl) {
    this.localEngineMap = this.makeEngineMap();
    this.localEngines = [...this.localEngineMap.values()].map(e => e.info);
    this.externalEngines = this.ctrl.opts.externalEngines?.map(e => ({ tech: 'EXTERNAL', ...e })) ?? [];
    this.selectProp = storedStringProp('ceval.engine', this.localEngines[0].id);
  }

  status = (status: { download?: { bytes: number; total: number }; error?: string } = {}): void => {
    if (this.ctrl.available()) this.ctrl.download = status.download;
    if (status.error) {
      log(status.error);
      this.ctrl.engineFailed(status.error);
    }
    this.ctrl.opts.redraw();
  };

  makeEngineMap(): Map<string, WithMake> {
    type Hash = string;
    type Variant = [VariantKey, Hash];
    const variantMap = (v: VariantKey): string => (v === 'threeCheck' ? '3check' : v.toLowerCase());
    const makeVariant = ([key, nnue]: Variant): WithMake => ({
      info: {
        id: `__fsfnnue-${key === 'kingOfTheHill' ? 'koth' : variantMap(key)}`,
        name: 'Fairy Stockfish 14+ NNUE',
        short: 'FSF 14+',
        tech: 'NNUE',
        requires: ['sharedMem', 'simd', 'dynamicImportFromWorker'],
        variants: [key],
        cloudEval: true,
        assets: {
          root: 'npm/stockfish-web',
          nnue: [`${variantMap(key)}-${nnue}.nnue`],
          js: 'fsf_14.js',
        },
      },
      make: (e: BrowserEngineInfo) => new StockfishWebEngine(e, this.status, variantMap),
    });
    const variants: Variant[] = [
      ['antichess', 'dd3cbe53cd4e'],
      ['atomic', '2cf13ff256cc'],
      ['crazyhouse', '8ebf84784ad2'],
      ['horde', '28173ddccabe'],
      ['kingOfTheHill', '978b86d0e6a4'],
      ['threeCheck', 'cb5f517c228b'],
      ['racingKings', '636b95f085e3'],
    ];
    const browserEngines: WithMake[] = [
      {
        info: {
          id: '__sf_18_smallnet',
          name: 'Stockfish 18 NNUE · 15MB',
          short: 'SF 18 · 15MB',
          tech: 'NNUE',
          requires: ['sharedMem', 'simd', 'dynamicImportFromWorker'],
          minMem: 1536,
          cloudEval: true,
          assets: {
            root: 'npm/stockfish-web',
            nnue: ['nn-4ca89e4b3abf.nnue'],
            js: 'sf_18_smallnet.js',
          },
        },
        make: (e: BrowserEngineInfo) => new StockfishWebEngine(e, this.status),
      },
      {
        info: {
          id: '__sf_18',
          name: 'Stockfish 18 NNUE · 108MB',
          short: 'SF 18 · 108MB',
          tech: 'NNUE',
          requires: ['sharedMem', 'simd', 'dynamicImportFromWorker'],
          minMem: 2560,
          cloudEval: true,
          assets: {
            root: 'npm/stockfish-web',
            js: 'sf_18.js',
          },
        },
        make: (e: BrowserEngineInfo) => new StockfishWebEngine(e, this.status),
      },
      {
        info: {
          id: '__sf14nnue',
          name: 'Stockfish 14 NNUE',
          short: 'SF 14',
          tech: 'NNUE',
          obsoletedBy: 'dynamicImportFromWorker',
          requires: ['sharedMem', 'simd'],
          minMem: 2048,
          assets: {
            version: 'b6939d',
            root: 'npm/stockfish-nnue.wasm',
            js: 'stockfish.js',
            wasm: 'stockfish.wasm',
          },
        },
        make: (e: BrowserEngineInfo) => new ThreadedEngine(e, this.status),
      },
      ...variants.map(makeVariant),
      {
        info: {
          id: '__fsfhce',
          name: 'Fairy Stockfish 14+ HCE',
          short: 'FSF 14+',
          tech: 'HCE',
          requires: ['sharedMem', 'simd', 'dynamicImportFromWorker'],
          variants: variants.map(v => v[0]),
          assets: {
            root: 'npm/stockfish-web',
            js: 'fsf_14.js',
          },
        },
        make: (e: BrowserEngineInfo) =>
          new StockfishWebEngine(e, this.status, v => (v === 'threeCheck' ? '3check' : v.toLowerCase())),
      },
      {
        info: {
          id: '__sf11mv',
          name: 'Stockfish 11 Multi-Variant',
          short: 'SF 11 MV',
          tech: 'HCE',
          requires: ['sharedMem'],
          minThreads: 1,
          variants: variants.map(v => v[0]),
          assets: {
            version: 'a022fa',
            root: 'npm/stockfish-mv.wasm',
            js: 'stockfish.js',
            wasm: 'stockfish.wasm',
          },
        },
        make: (e: BrowserEngineInfo) =>
          new ThreadedEngine(e, undefined, (v: VariantKey) =>
            v === 'antichess' ? 'giveaway' : lichessRules(v),
          ),
      },
      {
        info: {
          id: '__sf11hce',
          name: 'Stockfish 11 HCE',
          short: 'SF 11',
          tech: 'HCE',
          requires: ['sharedMem'],
          minThreads: 1,
          assets: {
            version: 'a022fa',
            root: 'npm/stockfish.wasm',
            js: 'stockfish.js',
            wasm: 'stockfish.wasm',
          },
        },
        make: (e: BrowserEngineInfo) => new ThreadedEngine(e),
      },
      {
        info: {
          id: '__sfwasm',
          name: 'Stockfish WASM',
          short: 'Stockfish',
          tech: 'HCE',
          minThreads: 1,
          maxThreads: 1,
          requires: ['wasm'],
          obsoletedBy: 'sharedMem',
          assets: {
            version: 'a022fa',
            root: 'npm/stockfish.js',
            js: 'stockfish.wasm.js',
          },
        },
        make: (e: BrowserEngineInfo) => new SimpleEngine(e),
      },
      {
        info: {
          id: '__sfjs',
          name: 'Stockfish JS',
          short: 'Stockfish',
          tech: 'HCE',
          minThreads: 1,
          maxThreads: 1,
          requires: [],
          obsoletedBy: 'wasm',
          assets: {
            version: 'a022fa',
            root: 'npm/stockfish.js',
            js: 'stockfish.js',
          },
        },
        make: (e: BrowserEngineInfo) => new SimpleEngine(e),
      },
    ];
    return new Map<string, WithMake>(
      browserEngines
        .filter(
          e =>
            e.info.requires.every(req => browserSupport().includes(req)) &&
            !(e.info.obsoletedBy && browserSupport().includes(e.info.obsoletedBy)),
        )
        .map(e => [e.info.id, { info: withDefaults(e.info), make: e.make }]),
    );
  }

  get active(): EngineInfo | undefined {
    return this.activeEngine ?? this.activate();
  }

  activate(): EngineInfo | undefined {
    this.activeEngine = this.getEngine({ id: this.selectProp(), variant: this.ctrl.opts.variant.key });
    return this.activeEngine;
  }

  select(id: string): void {
    this.selectProp(id);
    this.activate();
  }

  get external(): ExternalEngineInfo | undefined {
    return this.active && this.isExternalEngineInfo(this.active) ? this.active : undefined;
  }

  get maxMovetime(): number {
    return this.external ? 30 * 1000 : Number.POSITIVE_INFINITY; // broker timeouts prevent long search
  }

  async deleteExternal(id: string): Promise<boolean> {
    if (this.externalEngines.every(e => e.id !== id)) return false;
    const r = await fetch(`/api/external-engine/${id}`, { method: 'DELETE', headers: xhrHeader });
    if (!r.ok) return false;
    this.externalEngines = this.externalEngines.filter(e => e.id !== id);
    this.activate();
    return true;
  }

  updateCevalCtrl(ctrl: CevalCtrl): void {
    this.ctrl = ctrl;
  }

  supporting(variant: VariantKey): EngineInfo[] {
    return [
      ...this.localEngines.filter(e => e.variants?.includes(variant)),
      ...this.externalEngines.filter(e => externalEngineSupports(e, variant)),
    ];
  }

  getEngine(selector?: { id?: string; variant?: VariantKey }): EngineInfo | undefined {
    const id = selector?.id || this.selectProp();
    const variant = selector?.variant || 'standard';
    return (
      this.externalEngines.find(e => e.id === id && externalEngineSupports(e, variant)) ??
      this.localEngines.find(e => e.id === id && e.variants?.includes(variant)) ??
      this.localEngines.find(e => e.variants?.includes(variant)) ??
      this.externalEngines.find(e => externalEngineSupports(e, variant))
    );
  }

  make(selector?: { id?: string; variant?: VariantKey }): CevalEngine {
    const e = (this.activeEngine = this.getEngine(selector));
    if (!e) throw Error(`Engine not found ${selector?.id ?? selector?.variant ?? this.selectProp()}`);

    return !this.isExternalEngineInfo(e)
      ? this.localEngineMap.get(e.id)!.make(e)
      : new ExternalEngine(e, this.status);
  }

  isExternalEngineInfo(e: EngineInfo): e is ExternalEngineInfo {
    return e.tech === 'EXTERNAL';
  }
}

function maxHashMB() {
  if (isAndroid())
    return 64; // budget androids are easy to crash @ 128
  else if (isIPad())
    return 64; // iPadOS safari pretends to be desktop but acts more like iphone
  else if (isIos()) return 32;
  return 512; // allocating 1024 often fails and offers little benefit over 512, or 16 for that matter
}
const maxHash = maxHashMB();

function externalEngineSupports(e: EngineInfo, v: VariantKey) {
  const names = [v.toLowerCase()];
  if (v === 'standard' || v === 'fromPosition' || v === 'chess960') names.push('chess');
  if (v === 'threeCheck') names.push('3check');
  if (v === 'antichess') names.push('giveaway');
  return (e.variants ?? []).filter(v => names.includes(v.toLowerCase())).length;
}

const withDefaults = (engine: BrowserEngineInfo): BrowserEngineInfo => ({
  variants: ['standard', 'chess960', 'fromPosition'],
  minMem: 1024,
  maxHash,
  minThreads: 2,
  maxThreads: 32,
  ...engine,
});

type WithMake = {
  info: BrowserEngineInfo;
  make: (e: BrowserEngineInfo) => CevalEngine;
};
