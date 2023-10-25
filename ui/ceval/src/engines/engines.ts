import { BrowserEngineInfo, ExternalEngineInfo, EngineInfo, CevalEngine, Redraw, Progress } from '../types';
import CevalCtrl from '../ctrl';
import { SimpleEngine } from './simpleEngine';
import { StockfishWebEngine } from './stockfishWebEngine';
import { ThreadedEngine } from './threadedEngine';
import { ExternalEngine } from './externalEngine';
import { storedStringProp } from 'common/storage';
import { isAndroid, isIOS, isIPad, hasFeature } from 'common/device';
import { pow2floor } from '../util';

const version = 'a022fa';
const maxHash = maxHashMB();

export class Engines {
  localEngines: BrowserEngineInfo[] = [...localEngineMap.values()].map(e => e.info);
  externalEngines: ExternalEngineInfo[] =
    this.ctrl.opts.externalEngines?.map(e => ({ requires: 'external', ...e })) ?? [];

  lastSelected = storedStringProp('ceval.engine', this.localEngines[0].id);
  active: EngineInfo;

  constructor(readonly ctrl: CevalCtrl) {
    if (this.lastSelected() === 'lichess') this.localEngines[0].id; // backcompat
    this.select(this.lastSelected());
  }

  get external() {
    return this.active instanceof ExternalEngine ? this.active : undefined;
  }

  supporting(variant: VariantKey): EngineInfo[] {
    return [
      ...this.localEngines.filter(e => e.variants?.includes(variant)),
      ...this.externalEngines.filter(e => e.variants?.includes(variant)),
    ];
  }

  select(id: string) {
    this.lastSelected(id);
    this.active = this.engineFor({ id })!;
  }

  engineFor(selector?: { id?: string; variant?: VariantKey }): EngineInfo | undefined {
    const id = selector?.id ?? this.lastSelected();
    const variant = selector?.variant ?? 'standard';
    return (
      this.ctrl.opts.externalEngines?.find(e => e.id === id && e.variants?.includes(variant)) ??
      this.localEngines.find(e => e.id === id && e.variants?.includes(variant)) ??
      this.localEngines.find(e => e.variants?.includes(variant))
    );
  }

  make(selector?: { id?: string; variant?: VariantKey }): CevalEngine {
    const e = this.engineFor(selector);
    if (!e) throw Error(`Engine not found ${selector?.id ?? selector?.variant ?? this.lastSelected()}}`);
    const progress = (download?: { bytes: number; total: number }) => {
      if (this.ctrl.enabled()) this.ctrl.download = download;
      this.ctrl.opts.redraw();
    };
    return e.requires === 'external'
      ? new ExternalEngine(e as ExternalEngineInfo, this.ctrl.opts.redraw)
      : localEngineMap.get(e.id)!.make(e as BrowserEngineInfo, progress, this.ctrl.opts.redraw);
  }
}

function maxHashMB() {
  if (navigator.deviceMemory) return pow2floor(navigator.deviceMemory * 128); // chrome/edge/opera
  else if (isAndroid()) return 64; // budget androids are easy to crash @ 128
  else if (isIPad()) return 64; // iPadOS safari pretends to be desktop but acts more like iphone
  else if (isIOS()) return 32;
  return 256; // this is safe, mostly desktop firefox / mac safari users here
}

const withDefaults = (engine: BrowserEngineInfo) => ({
  variants: ['standard', 'chess960', 'fromPosition'],
  maxThreads: navigator.hardwareConcurrency ?? 1,
  maxHash,
  ...engine,
  assets: { version, ...engine.assets },
});

type WithMake = {
  info: BrowserEngineInfo;
  make: (e: BrowserEngineInfo, download?: Progress, redraw?: Redraw) => CevalEngine;
};

const localEngineMap = new Map<string, WithMake>(
  [
    {
      info: {
        id: 'sf16nnue7',
        name: 'Stockfish 16 NNUE (7 MB)',
        short: 'SF 16 · 7MB',
        class: 'NNUE',
        requires: 'simd',
        assets: {
          root: 'npm/stockfish-web',
          js: 'stockfishWeb.js',
          wasm: 'stockfishWeb.7.wasm',
        },
      },
      make: (e: BrowserEngineInfo, d: Progress) => new StockfishWebEngine(e, d),
    },
    {
      info: {
        id: 'sf16nnue40',
        name: 'Stockfish 16 NNUE (40 MB)',
        short: 'SF 16 · 40MB',
        class: 'NNUE',
        requires: 'simd',
        assets: {
          root: 'npm/stockfish-web',
          js: 'stockfishWeb.js',
          wasm: 'stockfishWeb.wasm',
        },
      },
      make: (e: BrowserEngineInfo, d: Progress) => new StockfishWebEngine(e, d),
    },
    {
      info: {
        id: 'sf16hce',
        name: 'Stockfish 16 HCE',
        short: 'SF 16',
        class: 'HCE',
        requires: 'simd',
        assets: {
          root: 'npm/stockfish-web',
          js: 'stockfishWeb.js',
          wasm: 'stockfishWeb.wasm',
        },
      },
      make: (e: BrowserEngineInfo) => new StockfishWebEngine(e),
    },
    {
      info: {
        id: 'sf14nnue',
        name: 'Stockfish 14 NNUE',
        short: 'SF 14',
        version: 'b6939d',
        class: 'NNUE',
        requires: 'simd',
        assets: {
          root: 'npm/stockfish-nnue.wasm',
          js: 'stockfish.js',
          wasm: 'stockfish.wasm',
        },
      },
      make: (e: BrowserEngineInfo, d: Progress, r: Redraw) => new ThreadedEngine(e, r, d),
    },
    {
      info: {
        id: 'sf11hce',
        name: 'Stockfish 11 HCE',
        short: 'SF 11',
        class: 'HCE',
        requires: 'sharedMem',
        assets: {
          root: 'npm/stockfish.wasm',
          js: 'stockfish.js',
          wasm: 'stockfish.wasm',
        },
      },
      make: (e: BrowserEngineInfo, _: Progress, r: Redraw) => new ThreadedEngine(e, r),
    },
    {
      info: {
        id: 'sf11mv',
        name: 'Stockfish 11 MV',
        short: 'SF 11 · MV',
        class: 'HCE',
        requires: 'sharedMem',
        variants: [
          'standard',
          'chess960',
          'fromPosition',
          'crazyhouse',
          'atomic',
          'horde',
          'kingOfTheHill',
          'racingKings',
          'antichess',
          'threeCheck',
        ],
        assets: {
          root: 'npm/stockfish-mv.wasm',
          js: 'stockfish.js',
          wasm: 'stockfish.wasm',
        },
      },
      make: (e: BrowserEngineInfo, _: Progress, r: Redraw) => new ThreadedEngine(e, r),
    },
    {
      info: {
        id: 'sfwasm',
        name: 'Stockfish WASM',
        short: 'Stockfish',
        class: 'WASM',
        maxThreads: 1,
        requires: 'wasm',
        assets: {
          root: 'npm/stockfish.js',
          js: 'stockfish.wasm.js',
        },
      },
      make: (e: BrowserEngineInfo, _: Progress, r: Redraw) => new SimpleEngine(e, r),
    },
    {
      info: {
        id: 'sfjs',
        name: 'Stockfish JS',
        short: 'Stockfish',
        class: 'JS',
        maxThreads: 1,
        assets: {
          root: 'npm/stockfish.js',
          js: 'stockfish.js',
        },
      },
      make: (e: BrowserEngineInfo, _: Progress, r: Redraw) => new SimpleEngine(e, r),
    },
  ]
    .filter(e => hasFeature(e.info.requires))
    .map(e => ({ info: withDefaults(e.info as BrowserEngineInfo), make: e.make }))
    .map(e => [e.info.id, e] as [string, WithMake]),
);
