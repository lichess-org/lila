import { lichessRules } from 'chessops/compat';

import type { BrowserEngineInfo, ExternalEngineInfo, EngineInfo, EngineTrust, CevalEngine } from '@/ceval';
import { isAndroid, isIos, isIPad, features as browserSupport } from '@/device';
import { log } from '@/permalog';
import { xhrHeader } from '@/xhr';

import type { CevalCtrl } from '../ctrl';
import { ExternalEngine } from './externalEngine';
import { SimpleEngine } from './simpleEngine';
import { StockfishWebEngine } from './stockfishWebEngine';
import { ThreadedEngine } from './threadedEngine';

interface WithMake {
  info: BrowserEngineInfo;
  make: (e: BrowserEngineInfo) => CevalEngine;
}

export class Engines {
  private activeEngine: EngineInfo | undefined = undefined;
  localEngineMap: Map<string, WithMake>;
  externalEngines: ExternalEngineInfo[];

  constructor(private readonly ctrl: CevalCtrl) {
    type Variant = { key: VariantKey; nnue: string };
    const variants: Variant[] = [
      { key: 'antichess', nnue: 'antichess-dd3cbe53cd4e' },
      { key: 'atomic', nnue: 'atomic-2cf13ff256cc' },
      { key: 'crazyhouse', nnue: 'crazyhouse-8ebf84784ad2' },
      { key: 'horde', nnue: 'horde-28173ddccabe' },
      { key: 'kingOfTheHill', nnue: 'kingofthehill-978b86d0e6a4' },
      { key: 'threeCheck', nnue: '3check-cb5f517c228b' },
      { key: 'racingKings', nnue: 'racingkings-636b95f085e3' },
    ];
    const relaxedSimdPair = (base: WithMake): [WithMake, WithMake] => [
      {
        ...base,
        info: {
          ...base.info,
          requires: [...base.info.requires, 'relaxedSimd'],
          assets: { ...base.info.assets, js: base.info.assets.js?.replace('.js', '_relaxed-simd.js') },
        },
      },
      { ...base, info: { ...base.info, obsoletedBy: 'relaxedSimd' } },
    ];
    // list engines in decreasing order of strength
    const browserEngines: WithMake[] = [
      ...relaxedSimdPair({
        info: {
          id: '__sf_dev',
          name: 'Stockfish 18 dev · 85MB',
          short: 'SF 18 dev 85MB',
          url: 'https://github.com/lichess-org/stockfish-web#sf_dev-stockfish-dev-20260609-415ff793',
          tech: 'NNUE',
          requires: ['sharedMem', 'simd', 'dynamicImportFromWorker'],
          minMem: 2560,
          capabilities: ['cloudEval', 'staticAnalysis', 'puzzleReport'],
          assets: {
            root: 'npm/stockfish-web',
            js: 'sf_dev.js',
          },
        },
        make: (e: BrowserEngineInfo) => new StockfishWebEngine(e, this.statusCallback),
      }),
      ...relaxedSimdPair({
        info: {
          id: '__sf_18',
          name: 'Stockfish 18 · 108MB',
          short: 'SF 18 108MB',
          url: 'https://github.com/lichess-org/stockfish-web#sf_18-stockfish-18',
          tech: 'NNUE',
          requires: ['sharedMem', 'simd', 'dynamicImportFromWorker'],
          minMem: 2560,
          capabilities: ['cloudEval', 'puzzleReport'],
          assets: {
            root: 'npm/stockfish-web',
            js: 'sf_18.js',
          },
        },
        make: (e: BrowserEngineInfo) => new StockfishWebEngine(e, this.statusCallback),
      }),
      ...relaxedSimdPair({
        info: {
          id: '__sf_18_smallnet',
          name: 'Stockfish 18 · 15MB',
          short: 'SF 18 15MB',
          url: 'https://github.com/lichess-org/stockfish-web#sf_18_smallnet-stockfish-18-with-sscg13threat-small',
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
        make: (e: BrowserEngineInfo) => new StockfishWebEngine(e, this.statusCallback),
      }),
      {
        info: {
          id: '__sf14nnue',
          name: 'Stockfish 14 NNUE',
          short: 'SF 14',
          url: 'https://github.com/lichess-org/stockfish-nnue.wasm',
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
        make: (e: BrowserEngineInfo) => new ThreadedEngine(e, this.statusCallback),
      },
      ...variants.map(
        ({ key, nnue }: Variant): WithMake => ({
          info: {
            id: `__fsfnnue-${key}`,
            name: 'Fairy Stockfish 14+ NNUE',
            short: 'FSF 14+',
            url: 'https://github.com/lichess-org/stockfish-web#fsf_14-fairy-stockfish-14',
            tech: 'NNUE',
            requires: ['sharedMem', 'simd', 'dynamicImportFromWorker'],
            variants: [key],
            capabilities: ['cloudEval', 'staticAnalysis'],
            assets: {
              root: 'npm/stockfish-web',
              nnue: [`${nnue}.nnue`],
              js: 'fsf_14.js',
            },
          },
          make: (e: BrowserEngineInfo) => new StockfishWebEngine(e, this.statusCallback),
        }),
      ),
      {
        info: {
          id: '__fsfhce',
          name: 'Fairy Stockfish 14+ HCE',
          short: 'FSF 14+',
          url: 'https://github.com/lichess-org/stockfish-web#fsf_14-fairy-stockfish-14',
          tech: 'HCE',
          requires: ['sharedMem', 'simd', 'dynamicImportFromWorker'],
          variants: variants.map(v => v.key),
          assets: {
            root: 'npm/stockfish-web',
            js: 'fsf_14.js',
          },
        },
        make: (e: BrowserEngineInfo) => new StockfishWebEngine(e, this.statusCallback),
      },
      {
        info: {
          id: '__sf11mv',
          name: 'Stockfish 11 Multi-Variant',
          short: 'SF 11 MV',
          tech: 'HCE',
          requires: ['sharedMem'],
          minThreads: 1,
          variants: variants.map(v => v.key),
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
          url: 'https://github.com/lichess-org/stockfish.wasm',
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
          url: 'https://github.com/lichess-org/stockfish.js',
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
          url: 'https://github.com/lichess-org/stockfish.js',
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
    this.localEngineMap = new Map<string, WithMake>(
      browserEngines
        .filter(
          e =>
            e.info.requires.every(req => browserSupport().includes(req)) &&
            !(e.info.obsoletedBy && browserSupport().includes(e.info.obsoletedBy)),
        )
        .map(e => [e.info.id, { info: withDefaults(e.info), make: e.make }]),
    );
    this.externalEngines =
      this.ctrl.opts.externalEngines?.map(e => ({
        tech: 'EXTERNAL',
        maxMovetime: 30 * 1000, // broker timeouts prevent long search
        ...e,
      })) ?? [];
  }

  getEngine(selector?: {
    id?: string;
    variant?: VariantKey;
    capability?: EngineTrust;
  }): EngineInfo | undefined {
    const id = selector?.id ?? this.activeEngine?.id;
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

  active(): EngineInfo {
    this.activeEngine ??= this.getEngine({ variant: this.ctrl.opts.variant.key });
    return this.activeEngine!;
  }

  setActive(id: string): EngineInfo | undefined {
    if (!this.activeEngine || id !== this.activeEngine.id) {
      this.activeEngine = this.getEngine({ id, variant: this.ctrl.opts.variant.key });
    }
    return this.activeEngine;
  }

  get defaultId(): string {
    return this.localEngineMap.values().next().value!.info.id;
  }

  get external(): ExternalEngineInfo | undefined {
    return this.activeEngine?.tech === 'EXTERNAL' ? this.activeEngine : undefined;
  }

  async deleteExternal(id: string): Promise<boolean> {
    if (this.externalEngines.every(e => e.id !== id)) return false;
    const r = await fetch(`/api/external-engine/${id}`, { method: 'DELETE', headers: xhrHeader });
    if (!r.ok) return false;
    this.externalEngines = this.externalEngines.filter(e => e.id !== id);
    this.active();
    return true;
  }

  supporting(
    variant: VariantKey,
    capability?: EngineTrust,
    filter: 'browser' | 'external' | 'all' = 'all',
  ): EngineInfo[] {
    const engines: EngineInfo[] = [];
    if (filter !== 'browser') {
      engines.push(...this.externalEngines.filter(e => externalEngineSupports(e, variant)));
    }
    if (filter !== 'external') {
      for (const { info } of this.localEngineMap.values()) {
        if (!info.variants?.includes(variant)) continue;
        if (capability && !info.capabilities?.includes(capability)) continue;
        engines.push(info);
      }
    }
    return engines;
  }

  makeEngine(selector?: { id?: string; variant?: VariantKey }): CevalEngine {
    const e = (this.activeEngine = this.getEngine(selector));
    if (!e) throw Error(`Engine not found ${selector?.id ?? selector?.variant}`);

    return e.tech === 'EXTERNAL'
      ? new ExternalEngine(e, this.statusCallback)
      : this.localEngineMap.get(e.id)!.make(e);
  }

  private readonly statusCallback = (
    status: { download?: { bytes: number; total: number }; error?: string } = {},
  ): void => {
    if (this.ctrl.available()) this.ctrl.download = status.download;
    if (status.error) {
      log(status.error);
      this.ctrl.engineFailed(status.error);
    }
    this.ctrl.opts.redraw();
  };
}

function externalEngineSupports(e: EngineInfo, v: VariantKey) {
  const names = [v.toLowerCase()];
  if (v === 'standard' || v === 'fromPosition' || v === 'chess960') names.push('chess');
  if (v === 'threeCheck') names.push('3check');
  if (v === 'antichess') names.push('giveaway');
  return (e.variants ?? []).filter(v => names.includes(v.toLowerCase())).length;
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
const withDefaults = (engine: BrowserEngineInfo): BrowserEngineInfo => ({
  variants: ['standard', 'chess960', 'fromPosition'],
  minMem: 1024,
  maxHash,
  minThreads: 2,
  maxThreads: 32,
  ...engine,
});
