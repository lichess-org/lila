import { BrowserEngineInfo, ExternalEngineInfo, EngineInfo, CevalEngine, Requires } from '../types';
import CevalCtrl from '../ctrl';
import { SimpleEngine } from './simpleEngine';
import { StockfishWebEngine } from './stockfishWebEngine';
import { ThreadedEngine } from './threadedEngine';
import { ExternalEngine } from './externalEngine';
import { storedStringProp, StoredProp } from 'common/storage';
import { isAndroid, isIOS, isIPad, getFirefoxMajorVersion, features, Feature } from 'common/device';
import { xhrHeader } from 'common/xhr';
import { lichessRules } from 'chessops/compat';

export class Engines {
  private _active: EngineInfo | undefined = undefined;
  localEngines: BrowserEngineInfo[];
  localEngineMap: Map<string, WithMake>;
  externalEngines: ExternalEngineInfo[];
  selectProp: StoredProp<string>;
  browserSupport: Requires[] = features().slice();

  constructor(private ctrl: CevalCtrl) {
    if (
      ((getFirefoxMajorVersion() ?? 114) > 113 && !('brave' in navigator)) ||
      site.storage.get('ceval.lsfw.forceEnable') === 'true'
    ) {
      this.browserSupport.push('allowLsfw'); // lsfw is https://github.com/lichess-org/lila-stockfish-web
    }
    this.localEngineMap = this.makeEngineMap();
    this.localEngines = [...this.localEngineMap.values()].map(e => e.info);
    this.externalEngines = this.ctrl.opts.externalEngines?.map(e => ({ tech: 'EXTERNAL', ...e })) ?? [];
    this.selectProp = storedStringProp('ceval.engine', this.localEngines[0].id);
  }

  status = (status: { download?: { bytes: number; total: number }; error?: string } = {}): void => {
    if (this.ctrl.enabled()) this.ctrl.download = status.download;
    if (status.error) this.ctrl.engineFailed(status.error);
    this.ctrl.opts.redraw();
  };

  makeEngineMap(): Map<string, WithMake> {
    type Hash = string;
    type Variant = [VariantKey, Hash];
    const variantMap = (v: VariantKey): string => (v === 'threeCheck' ? '3check' : v.toLowerCase());
    const makeVariant = ([key, nnue]: Variant): WithMake => ({
      info: {
        id: `__fsfnnue-${key == 'kingOfTheHill' ? 'koth' : variantMap(key)}`,
        name: 'Fairy Stockfish 14+ NNUE',
        short: 'FSF 14+',
        tech: 'NNUE',
        requires: ['simd', 'allowLsfw'],
        variants: [key],
        assets: {
          version: 'sfw004',
          root: 'npm/lila-stockfish-web',
          nnue: [`${variantMap(key)}-${nnue}.nnue`],
          js: 'fsf14.js',
        },
      },
      make: (e: BrowserEngineInfo) => new StockfishWebEngine(e, this.status, variantMap),
    });
    const variants: Variant[] = [
      ['antichess', '689c016df8e0'],
      ['atomic', '2cf13ff256cc'],
      ['crazyhouse', '8ebf84784ad2'],
      ['horde', '28173ddccabe'],
      ['kingOfTheHill', '978b86d0e6a4'],
      ['threeCheck', '313cc226a173'],
      ['racingKings', '636b95f085e3'],
    ];
    return new Map<string, WithMake>(
      [
        {
          info: {
            id: '__sf16nnue7',
            name: 'Stockfish 16 NNUE · 7MB',
            short: 'SF 16 · 7MB',
            tech: 'NNUE',
            requires: ['simd', 'allowLsfw'],
            minMem: 1536,
            assets: {
              version: 'sfw004',
              root: 'npm/lila-stockfish-web',
              js: 'sf16-7.js',
            },
          },
          make: (e: BrowserEngineInfo) => new StockfishWebEngine(e, this.status),
        },
        {
          info: {
            id: '__sf16nnue40',
            name: 'Stockfish 16 NNUE · 40MB',
            short: 'SF 16 · 40MB',
            tech: 'NNUE',
            requires: ['simd', 'allowLsfw'],
            minMem: 2048,
            assets: {
              version: 'sfw004',
              root: 'npm/lila-stockfish-web',
              js: 'sf16-40.js',
            },
          },
          make: (e: BrowserEngineInfo) => new StockfishWebEngine(e, this.status),
        },
        {
          info: {
            id: '__sf161nnue70',
            name: 'Stockfish 16.1 NNUE · 70MB',
            short: 'SF 16.1 · 70MB',
            tech: 'NNUE',
            requires: ['simd', 'allowLsfw'],
            minMem: 2560,
            assets: {
              version: 'sfw004',
              root: 'npm/lila-stockfish-web',
              js: 'sf161-70.js',
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
            requires: ['simd'],
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
            requires: ['simd', 'allowLsfw'],
            variants: variants.map(v => v[0]),
            assets: {
              version: 'sfw004',
              root: 'npm/lila-stockfish-web',
              js: 'fsf14.js',
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
      ]
        .filter(
          e =>
            e.info.requires.every((req: Requires) => this.browserSupport.includes(req)) &&
            !(e.info.obsoletedBy && this.browserSupport.includes(e.info.obsoletedBy as Feature)),
        )
        .map(e => [e.info.id, { info: withDefaults(e.info as BrowserEngineInfo), make: e.make }]),
    );
  }

  get active(): EngineInfo | undefined {
    return this._active ?? this.activate();
  }

  activate(): EngineInfo | undefined {
    this._active = this.getEngine({ id: this.selectProp(), variant: this.ctrl.opts.variant.key });
    return this._active;
  }

  select(id: string): void {
    this.selectProp(id);
    this.activate();
  }

  get external(): EngineInfo | undefined {
    return this.active && 'endpoint' in this.active ? this.active : undefined;
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
    const e = (this._active = this.getEngine(selector));
    if (!e) throw Error(`Engine not found ${selector?.id ?? selector?.variant ?? this.selectProp()}}`);

    return e.tech !== 'EXTERNAL'
      ? this.localEngineMap.get(e.id)!.make(e as BrowserEngineInfo)
      : new ExternalEngine(e as ExternalEngineInfo, this.status);
  }
}

function maxHashMB() {
  if (isAndroid()) return 64; // budget androids are easy to crash @ 128
  else if (isIPad()) return 64; // iPadOS safari pretends to be desktop but acts more like iphone
  else if (isIOS()) return 32;
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
