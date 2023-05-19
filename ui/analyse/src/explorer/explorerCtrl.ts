import { Prop, prop, defined } from 'common';
import { storedBooleanProp } from 'common/storage';
import { fenColor } from 'common/mini-game';
import debounce from 'common/debounce';
import { sync, Sync } from 'common/sync';
import { opposite } from 'chessground/util';
import * as xhr from './explorerXhr';
import { winnerOf } from './explorerUtil';
import * as gameUtil from 'game';
import AnalyseCtrl from '../ctrl';
import {
  isOpening,
  Hovering,
  ExplorerData,
  ExplorerDb,
  OpeningData,
  SimpleTablebaseHit,
  ExplorerOpts,
} from './interfaces';
import { ExplorerConfigCtrl } from './explorerConfig';
import { clearLastShow } from './explorerView';

export const MAX_DEPTH = 50;

function pieceCount(fen: Fen) {
  const parts = fen.split(/\s/);
  return parts[0].split(/[nbrqkp]/i).length - 1;
}

function tablebasePieces(variant: VariantKey) {
  switch (variant) {
    case 'standard':
    case 'fromPosition':
    case 'chess960':
      return 7;
    case 'atomic':
    case 'antichess':
      return 6;
    default:
      return 0;
  }
}

export const tablebaseGuaranteed = (variant: VariantKey, fen: Fen) => pieceCount(fen) <= tablebasePieces(variant);

export default class ExplorerCtrl {
  allowed: Prop<boolean>;
  enabled: Prop<boolean>;
  withGames: boolean;
  effectiveVariant: VariantKey;
  config: ExplorerConfigCtrl;

  loading = prop(true);
  failing = prop<Error | null>(null);
  hovering = prop<Hovering | null>(null);
  movesAway = prop(0);
  gameMenu = prop<string | null>(null);
  lastStream: Sync<true> | undefined;
  abortController: AbortController | undefined;
  cache: Dictionary<ExplorerData> = {};
  cacheUseful = prop(true);

  constructor(readonly root: AnalyseCtrl, readonly opts: ExplorerOpts, previous?: ExplorerCtrl) {
    this.allowed = prop(previous ? previous.allowed() : !root.embed);
    this.enabled = root.embed ? prop(false) : storedBooleanProp('explorer.enabled', false);
    this.withGames = root.synthetic || gameUtil.replayable(root.data) || !!root.data.opponent.ai;
    this.effectiveVariant = root.data.game.variant.key === 'fromPosition' ? 'standard' : root.data.game.variant.key;
    this.config = new ExplorerConfigCtrl(root, this.effectiveVariant, this.reload, previous?.config);
    window.addEventListener('hashchange', this.checkHash, false);
    this.checkHash();
  }

  checkHash = (e?: HashChangeEvent) => {
    const parts = location.hash.split('/');
    if ((parts[0] == '#explorer' || parts[0] == '#opening') && !this.root.embed) {
      this.enabled(true);
      if (parts[1] == 'lichess' || parts[1] === 'masters') this.config.data.db(parts[1]);
      else if (parts[1]?.match(/[A-Za-z0-9_-]{2,30}/)) {
        this.config.selectPlayer(parts[1]);
        this.config.data.color(parts[2] == 'black' ? 'black' : 'white');
      }
      if (e) this.reload();
    }
  };

  reload = () => {
    this.cache = {};
    this.setNode();
    this.root.redraw();
  };

  destroy = clearLastShow;

  private baseXhrOpening = () => ({
    endpoint: this.opts.endpoint,
    config: this.config.data,
  });

  fetch = debounce(
    () => {
      const fen = this.root.node.fen;
      const processData = (res: ExplorerData) => {
        this.cache[fen] = res;
        this.cacheUseful(isOpening(res) && res.white + res.black + res.draws > 10000);
        this.movesAway(res.moves.length ? 0 : this.movesAway() + 1);
        this.loading(false);
        this.failing(null);
        this.root.redraw();
      };
      const onError = (err: Error) => {
        if (err.name !== 'AbortError') {
          this.loading(false);
          this.failing(err);
          this.root.redraw();
        }
      };
      if (this.withGames && this.tablebaseRelevant(this.effectiveVariant, fen))
        xhr.tablebase(this.opts.tablebaseEndpoint, this.effectiveVariant, fen).then(processData, onError);
      else {
        this.abortController?.abort();
        this.abortController = new AbortController();
        this.lastStream = sync(
          xhr
            .opening(
              {
                ...this.baseXhrOpening(),
                db: this.db() as ExplorerDb,
                variant: this.effectiveVariant,
                rootFen: this.root.nodeList[0].fen,
                play: this.root.nodeList.slice(1).map(s => s.uci!),
                fen,
                withGames: this.withGames,
                cacheUseful: this.cacheUseful(),
              },
              processData,
              this.abortController.signal
            )
            .catch(onError)
            .then(_ => true)
        );
        this.lastStream.promise.then(() => this.root.redraw());
      }
    },
    250,
    true
  );

  empty: OpeningData = {
    white: 0,
    black: 0,
    draws: 0,
    isOpening: true,
    moves: [],
    fen: '',
    opening: this.root.data.game.opening,
  };

  tablebaseRelevant = (variant: VariantKey, fen: Fen) =>
    pieceCount(fen) - 1 <= tablebasePieces(variant) && this.root.ceval.possible;

  setNode = () => {
    if (!this.enabled()) return;
    this.gameMenu(null);
    const node = this.root.node;
    if (node.ply >= MAX_DEPTH && !this.tablebaseRelevant(this.effectiveVariant, node.fen)) {
      this.cache[node.fen] = this.empty;
    }
    const cached = this.cache[node.fen];
    if (cached) {
      this.movesAway(cached.moves.length ? 0 : this.movesAway() + 1);
      this.loading(false);
      this.failing(null);
    } else {
      this.loading(true);
      this.fetch();
    }
  };

  db = () => this.config.data.db();
  current = () => this.cache[this.root.node.fen];
  toggle = () => {
    this.movesAway(0);
    this.enabled(!this.enabled());
    this.setNode();
    this.root.autoScroll();
  };
  disable = () => {
    if (this.enabled()) {
      this.enabled(false);
      this.gameMenu(null);
      this.root.autoScroll();
    }
  };
  setHovering = (fen: Fen, uci: Uci | null) => {
    this.hovering(uci ? { fen, uci } : null);
    this.root.setAutoShapes();
  };
  onFlip = () => {
    if (this.db() == 'player') {
      this.cache = {};
      this.setNode();
    }
  };
  isIndexing = () => !!this.lastStream && !defined(this.lastStream.sync);
  fetchMasterOpening = (() => {
    const masterCache: Dictionary<OpeningData> = {};
    return (fen: Fen): Promise<OpeningData> => {
      const val = masterCache[fen];
      if (val) return Promise.resolve(val);
      return new Promise(resolve =>
        xhr.opening(
          {
            ...this.baseXhrOpening(),
            db: 'masters',
            rootFen: fen,
            play: [],
            fen,
            cacheUseful: true,
          },
          (res: OpeningData) => {
            masterCache[fen] = res;
            resolve(res);
          }
        )
      );
    };
  })();
  fetchTablebaseHit = async (fen: Fen): Promise<SimpleTablebaseHit> => {
    const res = await xhr.tablebase(this.opts.tablebaseEndpoint, this.effectiveVariant, fen);
    const move = res.moves[0];
    if (move && move.dtz == null) throw 'unknown tablebase position';
    return {
      fen,
      best: move && move.uci,
      winner: res.checkmate ? opposite(fenColor(fen)) : res.stalemate ? undefined : winnerOf(fen, move!),
    } as SimpleTablebaseHit;
  };
}
