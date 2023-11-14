import { BrowserEngineInfo, ExternalEngineInfo, EngineInfo, CevalEngine } from '../types';
import CevalCtrl from '../ctrl';
import { SimpleEngine } from './simpleEngine';
import { StockfishWebEngine } from './stockfishWebEngine';
import { ThreadedEngine } from './threadedEngine';
import { ExternalEngine } from './externalEngine';
import { storedStringProp, StoredProp } from 'common/storage';
import { isAndroid, isIOS, isIPad, hasFeature } from 'common/device';
import { xhrHeader } from 'common/xhr';
import { pow2floor } from '../util';
import { lichessRules } from 'chessops/compat';

export class Engines {
  private localEngines: BrowserEngineInfo[];
  private localEngineMap: Map<string, WithMake>;
  private externalEngines: ExternalEngineInfo[];
  private selectProp: StoredProp<string>;
  private _active: EngineInfo | undefined = undefined;

  constructor(private ctrl: CevalCtrl) {
    this.localEngineMap = this.makeEngineMap();
    this.localEngines = [...this.localEngineMap.values()].map(e => e.info);
    this.externalEngines = this.ctrl.opts.externalEngines?.map(e => ({ tech: 'EXTERNAL', ...e })) ?? [];
    this.selectProp = storedStringProp('ceval.engine', this.localEngines[0].id);
  }

  makeEngineMap() {
    const progress = (download?: { bytes: number; total: number }) => {
      if (this.ctrl.enabled()) this.ctrl.download = download;
      this.ctrl.opts.redraw();
    };

    return new Map<string, WithMake>(
      [
        {
          info: {
            id: '__sf16nnue7',
            name: 'Stockfish 16 NNUE · 7MB',
            short: 'SF 16 · 7MB',
            tech: 'NNUE',
            requires: 'simd',
            minMem: 1536,
            assets: {
              version: 'sfw002',
              root: 'npm/lila-stockfish-web',
              js: 'linrock-nnue-7.js',
            },
          },
          make: (e: BrowserEngineInfo) => new StockfishWebEngine(e, progress),
        },
        {
          info: {
            id: '__sf16nnue40',
            name: 'Stockfish 16 NNUE · 40MB',
            short: 'SF 16 · 40MB',
            tech: 'NNUE',
            requires: 'simd',
            minMem: 2048,
            assets: {
              version: 'sfw002',
              root: 'npm/lila-stockfish-web',
              js: 'sf-nnue-40.js',
            },
          },
          make: (e: BrowserEngineInfo) => new StockfishWebEngine(e, progress),
        },
        {
          info: {
            id: '__sf14nnue',
            name: 'Stockfish 14 NNUE',
            short: 'SF 14',
            tech: 'NNUE',
            requires: 'simd',
            minMem: 2048,
            assets: {
              version: 'b6939d',
              root: 'npm/stockfish-nnue.wasm',
              js: 'stockfish.js',
              wasm: 'stockfish.wasm',
            },
          },
          make: (e: BrowserEngineInfo) => new ThreadedEngine(e, progress),
        },
        {
          info: {
            id: '__fsfhce',
            name: 'Fairy Stockfish 16',
            short: 'FSF 16',
            tech: 'HCE',
            requires: 'simd',
            variants: [
              'crazyhouse',
              'atomic',
              'horde',
              'kingOfTheHill',
              'racingKings',
              'antichess',
              'threeCheck',
            ],
            assets: {
              version: 'sfw002',
              root: 'npm/lila-stockfish-web',
              js: 'fsf-hce.js',
            },
          },
          make: (e: BrowserEngineInfo) =>
            new StockfishWebEngine(e, undefined, v => (v === 'threeCheck' ? '3check' : v.toLowerCase())),
        },
        {
          info: {
            id: '__sf11mv',
            name: 'Stockfish 11 Multi-Variant',
            short: 'SF 11 MV',
            tech: 'HCE',
            requires: 'sharedMem',
            variants: [
              'crazyhouse',
              'atomic',
              'horde',
              'kingOfTheHill',
              'racingKings',
              'antichess',
              'threeCheck',
            ],
            assets: {
              version: 'a022fa',
              root: 'npm/stockfish-mv.wasm',
              js: 'stockfish.js',
              wasm: 'stockfish.wasm',
            },
          },
          make: (e: BrowserEngineInfo) =>
            new ThreadedEngine(e, progress, (v: VariantKey) =>
              v === 'antichess' ? 'giveaway' : lichessRules(v),
            ),
        },
        {
          info: {
            id: '__sf11hce',
            name: 'Stockfish 11 HCE',
            short: 'SF 11',
            tech: 'HCE',
            requires: 'sharedMem',
            assets: {
              version: 'a022fa',
              root: 'npm/stockfish.wasm',
              js: 'stockfish.js',
              wasm: 'stockfish.wasm',
            },
          },
          make: (e: BrowserEngineInfo) => new ThreadedEngine(e, progress),
        },
        {
          info: {
            id: '__sfwasm',
            name: 'Stockfish WASM',
            short: 'Stockfish',
            tech: 'HCE',
            maxThreads: 1,
            requires: 'wasm',
            obsoletedBy: 'sharedMem',
            assets: {
              version: 'a022fa',
              root: 'npm/stockfish.js',
              js: 'stockfish.wasm.js',
            },
          },
          make: (e: BrowserEngineInfo) => new SimpleEngine(e, progress),
        },
        {
          info: {
            id: '__sfjs',
            name: 'Stockfish JS',
            short: 'Stockfish',
            tech: 'HCE',
            maxThreads: 1,
            obsoletedBy: 'wasm',
            assets: {
              version: 'a022fa',
              root: 'npm/stockfish.js',
              js: 'stockfish.js',
            },
          },
          make: (e: BrowserEngineInfo) => new SimpleEngine(e, progress),
        },
      ]
        .filter(e => hasFeature(e.info.requires) && !(e.info.obsoletedBy && hasFeature(e.info.obsoletedBy)))
        .map(e => [e.info.id, { info: withDefaults(e.info as BrowserEngineInfo), make: e.make }]),
    );
  }

  get active() {
    return this._active ?? this.activate();
  }

  activate() {
    this._active = this.getEngine({ id: this.selectProp(), variant: this.ctrl.opts.variant.key });
    return this._active;
  }

  select(id: string) {
    this.selectProp(id);
    this.activate();
  }

  get external() {
    return this.active && 'endpoint' in this.active ? this.active : undefined;
  }

  async deleteExternal(id: string) {
    if (this.externalEngines.every(e => e.id !== id)) return false;
    const r = await fetch(`/api/external-engine/${id}`, { method: 'DELETE', headers: xhrHeader });
    if (!r.ok) return false;
    this.externalEngines = this.externalEngines.filter(e => e.id !== id);
    this.activate();
    return true;
  }

  updateCevalCtrl(ctrl: CevalCtrl) {
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
      : new ExternalEngine(e as ExternalEngineInfo, this.ctrl.opts.redraw);
  }
}

function maxHashMB() {
  if (navigator.deviceMemory) return pow2floor(navigator.deviceMemory * 128); // chrome/edge/opera
  else if (isAndroid()) return 64; // budget androids are easy to crash @ 128
  else if (isIPad()) return 64; // iPadOS safari pretends to be desktop but acts more like iphone
  else if (isIOS()) return 32;
  return 256; // this is safe, mostly desktop firefox / mac safari users here
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
  maxThreads: navigator.hardwareConcurrency ?? 1,
  minMem: 1024,
  maxHash,
  ...engine,
});

type WithMake = {
  info: BrowserEngineInfo;
  make: (e: BrowserEngineInfo) => CevalEngine;
};
