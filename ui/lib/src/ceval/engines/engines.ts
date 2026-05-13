import { lichessRules } from 'chessops/compat';

import type { BrowserEngineInfo, ExternalEngineInfo, EngineInfo, EngineTrust, CevalEngine } from '@/ceval';
import { isAndroid, isIos, isIPad, features as browserSupport } from '@/device';
import { log } from '@/permalog';
import { storedStringProp, type StoredProp } from '@/storage';
import { xhrHeader } from '@/xhr';

import type { CevalCtrl } from '../ctrl';
import { ExternalEngine } from './externalEngine';
import { SimpleEngine } from './simpleEngine';
import { StockfishWebEngine } from './stockfishWebEngine';
import { ThreadedEngine } from './threadedEngine';

export class Engines {
  private activeEngine: EngineInfo | undefined = undefined;
  localEngineMap: Map<string, WithMake>;
  externalEngines: ExternalEngineInfo[];
  selectProp: StoredProp<string>;

  constructor(private readonly ctrl: CevalCtrl) {
    this.localEngineMap = this.makeEngineMap();
    this.externalEngines = this.ctrl.opts.externalEngines?.map(e => ({ tech: 'EXTERNAL', ...e })) ?? [];
    this.selectProp = storedStringProp(
      'ceval.selected-engine',
      this.localEngineMap.keys().next().value ?? '',
    );
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
    const variants: Variant[] = [
      ['antichess', 'dd3cbe53cd4e'],
      ['atomic', '2cf13ff256cc'],
      ['crazyhouse', '8ebf84784ad2'],
      ['horde', '28173ddccabe'],
      ['kingOfTheHill', '978b86d0e6a4'],
      ['threeCheck', 'cb5f517c228b'],
      ['racingKings', '636b95f085e3'],
    ];
    const relaxedSimdPair = (base: WithMake): [WithMake, WithMake] => [
      {
        ...base,
        info: {
          ...base.info,
          id: `${base.info.id}_relaxed-simd`,
          requires: [...base.info.requires, 'relaxedSimd'],
          assets: { ...base.info.assets, js: base.info.assets.js?.replace('.js', '_relaxed-simd.js') },
        },
      },
      { ...base, info: { ...base.info, obsoletedBy: 'relaxedSimd' } },
    ];
    const browserEngines: WithMake[] = [
      ...relaxedSimdPair({
        info: {
          id: '__sf_18_smallnet',
          name: 'Stockfish 18 · 15MB sscg13/threat-small',
          short: 'SF 18 · 15MB',
          tech: 'NNUE',
          requires: ['sharedMem', 'simd', 'dynamicImportFromWorker'],
          minMem: 1536,
          capabilities: ['cloudEval', 'puzzleReport'],
          assets: {
            root: 'npm/stockfish-web',
            nnue: ['nn-4ca89e4b3abf.nnue'],
            js: 'sf_18_smallnet.js',
          },
        },
        make: (e: BrowserEngineInfo) => new StockfishWebEngine(e, this.status),
      }),
      ...relaxedSimdPair({
        info: {
          id: '__sf_dev',
          name: 'Stockfish 18+ dev-20260213-77d46ff6 · 88MB SFNNv12',
          short: 'SF dev · 88MB',
          tech: 'NNUE',
          requires: ['sharedMem', 'simd', 'dynamicImportFromWorker'],
          minMem: 2560,
          capabilities: ['cloudEval', 'staticAnalysis', 'puzzleReport'],
          assets: {
            root: 'npm/stockfish-web',
            js: 'sf_dev.js',
          },
        },
        make: (e: BrowserEngineInfo) => new StockfishWebEngine(e, this.status),
      }),
      ...relaxedSimdPair({
        info: {
          id: '__sf_18',
          name: 'Stockfish 18 · 108MB SFNNv10',
          short: 'SF 18 · 108MB',
          tech: 'NNUE',
          requires: ['sharedMem', 'simd', 'dynamicImportFromWorker'],
          minMem: 2560,
          assets: {
            root: 'npm/stockfish-web',
            js: 'sf_18.js',
          },
        },
        make: (e: BrowserEngineInfo) => new StockfishWebEngine(e, this.status),
      }),
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
      ...variants.map(
        ([key, nnue]: Variant): WithMake => ({
          info: {
            id: `__fsfnnue-${key === 'kingOfTheHill' ? 'koth' : variantMap(key)}`,
            name: 'Fairy Stockfish 14+ NNUE',
            short: 'FSF 14+',
            tech: 'NNUE',
            requires: ['sharedMem', 'simd', 'dynamicImportFromWorker'],
            variants: [key],
            capabilities: ['cloudEval', 'staticAnalysis'],
            assets: {
              root: 'npm/stockfish-web',
              nnue: [`${variantMap(key)}-${nnue}.nnue`],
              js: 'fsf14.js',
            },
          },
          make: (e: BrowserEngineInfo) => new StockfishWebEngine(e, this.status, variantMap),
        }),
      ),
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

  getEngine(selector?: {
    id?: string;
    variant?: VariantKey;
    capability?: EngineTrust;
  }): EngineInfo | undefined {
    const id = selector?.id || this.ctrl.storedEngine();
    const variant = selector?.variant || 'standard';
    const localEngines = [...this.localEngineMap.values()]
      .filter(e => !selector?.capability || e.info.capabilities?.includes(selector.capability))
      .map(e => e.info);
    return (
      this.externalEngines.find(e => e.id === id && externalEngineSupports(e, variant)) ??
      localEngines.find(e => e.id === id && e.variants?.includes(variant)) ??
      localEngines.find(e => e.variants?.includes(variant)) ??
      this.externalEngines.find(e => externalEngineSupports(e, variant))
    );
  }

  getExact(id: string): EngineInfo | undefined {
    return this.externalEngines.find(e => e.id === id) ?? this.localEngineMap.get(id)?.info;
  }

  current(id?: string): EngineInfo | undefined {
    if (!this.activeEngine || (id && id !== this.activeEngine.id)) {
      this.activeEngine = this.getEngine({ id, variant: this.ctrl.opts.variant.key });
    }
    return this.activeEngine;
  }

  external(): ExternalEngineInfo | undefined {
    const e = this.current();
    return e?.tech === 'EXTERNAL' ? e : undefined;
  }

  maxMovetime(): number {
    return this.external() ? 30 * 1000 : Number.POSITIVE_INFINITY; // broker timeouts prevent long search
  }

  async deleteExternal(id: string): Promise<boolean> {
    if (this.externalEngines.every(e => e.id !== id)) return false;
    const r = await fetch(`/api/external-engine/${id}`, { method: 'DELETE', headers: xhrHeader });
    if (!r.ok) return false;
    this.externalEngines = this.externalEngines.filter(e => e.id !== id);
    this.current();
    return true;
  }

  supporting(variant: VariantKey, capability?: EngineTrust): EngineInfo[] {
    const localEngines = [...this.localEngineMap.values()].map(e => e.info);
    return [
      ...localEngines.filter(
        e => e.variants?.includes(variant) && (!capability || e.capabilities?.includes(capability)),
      ),
      ...this.externalEngines.filter(e => externalEngineSupports(e, variant)),
    ];
  }

  makeEngine(selector?: { id?: string; variant?: VariantKey }): CevalEngine {
    const e = (this.activeEngine = this.getEngine(selector));
    if (!e) throw Error(`Engine not found ${selector?.id ?? selector?.variant ?? this.ctrl.storedEngine()}}`);

    return this.isExternalEngineInfo(e)
      ? new ExternalEngine(e, this.status)
      : this.localEngineMap.get(e.id)!.make(e);
  }

  isExternalEngineInfo(e: EngineInfo | undefined): e is ExternalEngineInfo {
    return e?.tech === 'EXTERNAL';
  }

  get defaultId(): string {
    return this.localEngineMap.values().next().value!.info.id;
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
