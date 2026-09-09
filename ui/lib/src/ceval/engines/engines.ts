import { type Rules } from 'chessops/types';

import type { BrowserEngineInfo, ExternalEngineInfo, EngineInfo, CevalEngine } from '@/ceval';
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
    type Variant = { key: Rules; nnue: string };
    const variants: Variant[] = [
      { key: 'antichess', nnue: 'antichess-dd3cbe53cd4e' },
      { key: 'atomic', nnue: 'atomic-2cf13ff256cc' },
      { key: 'crazyhouse', nnue: 'crazyhouse-8ebf84784ad2' },
      { key: 'horde', nnue: 'horde-28173ddccabe' },
      { key: 'kingofthehill', nnue: 'kingofthehill-978b86d0e6a4' },
      { key: '3check', nnue: '3check-cb5f517c228b' },
      { key: 'racingkings', nnue: 'racingkings-636b95f085e3' },
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
          id: '__sf_19',
          name: 'Stockfish 19 · 94MB',
          short: 'SF 19 94MB',
          url: 'https://github.com/lichess-org/stockfish-web#sf_19-stockfish-19',
          tech: 'NNUE',
          requires: ['sharedMem', 'simd', 'dynamicImportFromWorker'],
          minMem: 2560,
          supportsCloudEval: true,
          supportsPuzzleReport: true,
          assets: {
            root: 'npm/stockfish-web',
            js: 'sf_19.js',
          },
        },
        make: (e: BrowserEngineInfo) => new StockfishWebEngine(e, this.statusCallback),
      }),
      ...relaxedSimdPair({
        info: {
          id: '__sf_19_smallnet',
          name: 'Stockfish 19 · 1MB',
          short: 'SF 19 1MB',
          url: 'https://github.com/lichess-org/stockfish-web#sf_19_smallnet-stockfish-19-with-sscg13size-optimize-nnue',
          tech: 'NNUE',
          requires: ['sharedMem', 'simd', 'dynamicImportFromWorker'],
          minMem: 1536,
          supportsCloudEval: true,
          supportsPuzzleReport: true,
          preferred: true,
          assets: {
            root: 'npm/stockfish-web',
            js: 'sf_19_smallnet.js',
          },
        },
        make: (e: BrowserEngineInfo) => new StockfishWebEngine(e, this.statusCallback),
      }),
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
            supportsCloudEval: true,
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
      {
        info: {
          id: '__fsfhce',
          name: 'Fairy Stockfish 14+ HCE',
          short: 'FSF 14+',
          url: 'https://github.com/lichess-org/stockfish-web#fsf_14-fairy-stockfish-14',
          tech: 'HCE',
          requires: ['sharedMem', 'simd', 'dynamicImportFromWorker'],
          variants: ['chess', ...variants.map(v => v.key)],
          supportsNonStandardMaterial: true,
          assets: {
            root: 'npm/stockfish-web',
            js: 'fsf_14.js',
          },
        },
        make: (e: BrowserEngineInfo) => new StockfishWebEngine(e, this.statusCallback),
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
        make: (e: BrowserEngineInfo) => new ThreadedEngine(e, undefined),
      },
      {
        info: {
          id: '__sf11mv',
          name: 'Stockfish 11 Multi-Variant',
          short: 'SF 11 MV',
          tech: 'HCE',
          requires: ['sharedMem'],
          minThreads: 1,
          variants: ['chess', ...variants.map(v => v.key)],
          supportsNonStandardMaterial: true,
          assets: {
            version: 'a022fa',
            root: 'npm/stockfish-mv.wasm',
            js: 'stockfish.js',
            wasm: 'stockfish.wasm',
          },
        },
        make: (e: BrowserEngineInfo) =>
          new ThreadedEngine(e, undefined, (v: Rules) => (v === 'antichess' ? 'giveaway' : v)),
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
        preferred: true,
        maxMovetime: 30 * 1000, // broker timeouts prevent long search
        ...e,
      })) ?? [];
  }

  getEngine(selector?: { id?: string; rules: Rules; nonStandardMaterial: boolean }): EngineInfo | undefined {
    const id = selector?.id ?? this.activeEngine?.id;
    const engines = this.supporting({
      rules: selector?.rules || 'chess',
      nonStandardMaterial: !!selector?.nonStandardMaterial,
    });
    return engines.find(info => info.id === id) ?? engines.find(info => info.preferred) ?? engines[0];
  }

  active(): EngineInfo | undefined {
    this.activeEngine ??= this.getEngine({
      rules: this.ctrl.rules,
      nonStandardMaterial: this.ctrl.nonStandardMaterial,
    });
    return this.activeEngine;
  }

  setActive(id: string): EngineInfo | undefined {
    if (!this.activeEngine || id !== this.activeEngine.id) {
      this.activeEngine = this.getEngine({
        id,
        rules: this.ctrl.rules,
        nonStandardMaterial: this.ctrl.nonStandardMaterial,
      });
    }
    return this.activeEngine;
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

  supporting(selector: {
    rules: Rules;
    nonStandardMaterial: boolean;
    filter?: 'browser' | 'external';
  }): EngineInfo[] {
    const engines: EngineInfo[] = [
      ...(selector.filter !== 'browser' ? this.externalEngines : []),
      ...(selector.filter !== 'external' ? [...this.localEngineMap.values()].map(e => e.info) : []),
    ];
    return engines.filter(
      info =>
        (!selector.nonStandardMaterial || info.supportsNonStandardMaterial) &&
        (info.variants ?? ['chess']).includes(selector.rules),
    );
  }

  makeEngine(selector?: { id?: string; rules: Rules; nonStandardMaterial: boolean }): CevalEngine {
    const e = (this.activeEngine = this.getEngine(selector));
    if (!e) throw Error(`Engine not found ${selector?.id ?? selector?.rules}`);

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
  variants: ['chess'],
  minMem: 1024,
  maxHash,
  minThreads: 2,
  maxThreads: 32,
  ...engine,
});
