import { opposite } from '@lichess-org/chessground/util';

import { type Prop, prop, defined, myUserId } from 'lib';
import { debounce, defer, sync, type Sync } from 'lib/async';
import { replayable } from 'lib/game';
import { pieceCount, fenColor } from 'lib/game/chess';
import { storedBooleanProp } from 'lib/storage';

import type AnalyseCtrl from '../ctrl';
import { ExplorerConfigCtrl } from './explorerConfig';
import { MAX_ANALYSE_DEPTH, winnerOf } from './explorerUtil';
import { clearLastShow } from './explorerView';
import * as xhr from './explorerXhr';
import type { Hovering, ExplorerData, OpeningData, SimpleTablebaseHit, ExplorerOpts } from './interfaces';

function tablebasePieces(variant: VariantKey) {
  switch (variant) {
    case 'standard':
    case 'fromPosition':
    case 'chess960':
      return 8;
    case 'atomic':
    case 'antichess':
      return 6;
    default:
      return 0;
  }
}

export const tablebaseGuaranteed = (variant: VariantKey, fen: FEN) =>
  pieceCount(fen) <= tablebasePieces(variant);

export default class ExplorerCtrl {
  allowed: Prop<boolean>;
  enabled: Prop<boolean>;
  withGames: boolean;
  private readonly effectiveVariant: VariantKey;
  config: ExplorerConfigCtrl;

  loading = prop(true);
  failing = prop<Error | null>(null);
  hovering = prop<Hovering | null>(null);
  movesAway = prop(0);
  gameMenu = prop<string | null>(null);
  private lastStream: Sync<true> | undefined;
  private abortController: AbortController | undefined;
  private cache: Dictionary<ExplorerData> = {};

  constructor(
    readonly root: AnalyseCtrl,
    readonly opts: ExplorerOpts,
    previous?: ExplorerCtrl,
  ) {
    this.allowed = prop(previous ? previous.allowed() : !root.isEmbed);
    this.enabled = storedBooleanProp('analyse.explorer.enabled', false);
    this.withGames = root.synthetic || replayable(root.data) || !!root.data.opponent.ai;
    this.effectiveVariant =
      root.data.game.variant.key === 'fromPosition' ? 'standard' : root.data.game.variant.key;
    this.config = new ExplorerConfigCtrl(root, this.effectiveVariant, this.reload, previous?.config);
    window.addEventListener('hashchange', this.checkHash, false);
    this.checkHash();
  }

  private readonly checkHash = (e?: HashChangeEvent) => {
    const parts = location.hash.split('/');
    if (parts[0] === '#explorer' || parts[0] === '#opening') {
      this.enabled(true);
      if (parts[1] === 'lichess' || parts[1] === 'masters') this.config.data.db(parts[1]);
      else if (parts[1]?.match(/[A-Za-z0-9_-]{2,30}/)) {
        this.config.selectPlayer(parts[1]);
        this.config.data.color(parts[2] === 'black' ? 'black' : 'white');
      }
      if (e) this.reload();
    }
  };

  isAuth = () => defined(myUserId());

  reload = () => {
    this.cache = {};
    this.setNode();
    this.root.redraw();
  };

  destroy = clearLastShow;

  private readonly baseXhrOpening = () => ({
    endpoint: this.opts.endpoint,
    config: this.config.data,
  });

  private readonly fetch = debounce(
    () => {
      const fen = this.root.node.fen;
      const processData = (res: ExplorerData) => {
        this.cache[fen] = res;
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
      this.abortController?.abort();
      this.abortController = new AbortController();
      if (this.withGames && this.tablebaseRelevant(this.effectiveVariant, fen))
        xhr
          .tablebase(this.opts.tablebaseEndpoint, this.effectiveVariant, fen, this.abortController.signal)
          .then(processData, onError);
      else {
        this.lastStream = sync(
          xhr
            .opening(
              {
                ...this.baseXhrOpening(),
                db: this.db(),
                variant: this.effectiveVariant,
                rootFen: this.root.nodeList[0].fen,
                play: this.root.nodeList.slice(1).map(s => s.uci!),
                fen,
                withGames: this.withGames,
              },
              processData,
              this.abortController.signal,
            )
            .catch(onError)
            .then(_ => true),
        );
        this.lastStream.promise.then(() => this.root.redraw());
      }
    },
    250,
    true,
  );

  private readonly empty: OpeningData = {
    white: 0,
    black: 0,
    draws: 0,
    isOpening: true,
    moves: [],
    fen: '',
    opening: this.root.data.game.opening,
  };

  private readonly tablebaseRelevant = (variant: VariantKey, fen: FEN) =>
    pieceCount(fen) - 1 <= tablebasePieces(variant) && this.root.isCevalAllowed();

  setNode = () => {
    if (!this.enabled()) return;
    this.gameMenu(null);
    const node = this.root.node;
    if (
      (!this.isAuth() || node.ply >= MAX_ANALYSE_DEPTH) &&
      !this.tablebaseRelevant(this.effectiveVariant, node.fen)
    )
      this.cache[node.fen] = this.empty;
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
  };
  disable = () => {
    if (this.enabled()) {
      this.enabled(false);
      this.gameMenu(null);
    }
  };
  setHovering = (fen: FEN, uci: Uci | null) => {
    this.root.fork.hover(uci);
    this.hovering(uci ? { fen, uci } : null);
    this.root.setAutoShapes();
  };
  onFlip = () => {
    if (this.db() === 'player') {
      this.cache = {};
      this.setNode();
    }
  };
  isIndexing = () => !!this.lastStream && !defined(this.lastStream.sync);
  fetchMasterOpening = async (fen: FEN): Promise<OpeningData> => {
    const deferred = defer<OpeningData>();
    await xhr.opening(
      {
        ...this.baseXhrOpening(),
        db: 'masters',
        rootFen: fen,
        play: [],
        fen,
      },
      deferred.resolve,
    );
    return await deferred.promise;
  };
  fetchTablebaseHit = async (fen: FEN): Promise<SimpleTablebaseHit> => {
    const res = await xhr.tablebase(this.opts.tablebaseEndpoint, this.effectiveVariant, fen);
    const move = res.moves[0];
    if (move && move.dtz === null) throw 'unknown tablebase position';
    return {
      fen,
      best: move && move.uci,
      winner: res.checkmate ? opposite(fenColor(fen)) : res.stalemate ? undefined : winnerOf(fen, move),
    };
  };
}
