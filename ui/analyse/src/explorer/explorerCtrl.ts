import { prop } from 'common';
import { opposite } from 'draughtsground/util';
import { controller as configCtrl } from './explorerConfig';
import xhr = require('./explorerXhr');
import { winnerOf, colorOf } from './explorerUtil';
import { synthetic } from '../util';
import * as gameUtil from 'game';
import AnalyseCtrl from '../ctrl';
import { Hovering, ExplorerCtrl, ExplorerData, OpeningData, TablebaseData, SimpleTablebaseHit } from './interfaces';

function pieceCount(fen: Fen) {
    //max: W:W31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50:B1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20:H0:F1
    //min: W:WK5:BK46:H0:F1
    return fen.split(',').length + 1;
}

export function tablebaseGuaranteed(variant: VariantKey, fen: Fen) {
  switch (variant) {
    case 'standard':
    case 'fromPosition':
      return pieceCount(fen) <= 0;
    default:
      return false;
  }
}

function tablebaseRelevant(variant: VariantKey, fen: Fen) {
  const count = pieceCount(fen);
  switch (variant) {
    case 'standard':
    case 'fromPosition':
      return count <= 0;
    default:
      return false;
  }
}

export default function(root: AnalyseCtrl, opts, allow: boolean): ExplorerCtrl {
  const allowed = prop(allow),
  enabled = prop(false), //root.embed ? prop(false) : storedProp('explorer.enabled', false),
  loading = prop(true),
  failing = prop(false),
  hovering = prop<Hovering | null>(null),
  movesAway = prop(0),
  gameMenu = prop<string | null>(null);

  if ((location.hash === '#explorer' || location.hash === '#opening') && !root.embed) enabled(true);

  let cache = {};
  function onConfigClose() {
    root.redraw();
    cache = {};
    setNode();
  }
  const data = root.data,
  withGames = synthetic(data) || gameUtil.replayable(data) || !!data.opponent.ai,
  effectiveVariant = data.game.variant.key === 'fromPosition' ? 'standard' : data.game.variant.key,
  config = configCtrl(data.game, onConfigClose, root.trans, root.redraw);

  const fetch = window.lidraughts.fp.debounce(function() {
    const fen = root.node.fen;
    const request: JQueryPromise<ExplorerData> = (withGames && tablebaseRelevant(effectiveVariant, fen)) ?
      xhr.tablebase(opts.tablebaseEndpoint, effectiveVariant, fen) :
      xhr.opening(opts.endpoint, effectiveVariant, fen, config.data, withGames);

    request.then((res: ExplorerData) => {
      cache[fen] = res;
      movesAway(res.moves.length ? 0 : movesAway() + 1);
      loading(false);
      failing(false);
      root.redraw();
    }, () => {
      loading(false);
      failing(true);
      root.redraw();
    });
  }, 250, true);

  const empty = {
    opening: true,
    moves: {}
  };

  function setNode() {
    if (!enabled()) return;
    gameMenu(null);
    const node = root.node;
    if (node.ply > 50 && !tablebaseRelevant(effectiveVariant, node.fen)) {
      cache[node.fen] = empty;
    }
    const cached = cache[root.node.fen];
    if (cached) {
      movesAway(cached.moves.length ? 0 : movesAway() + 1);
      loading(false);
      failing(false);
    } else {
      loading(true);
      fetch();
    }
  };

  return {
    allowed,
    enabled,
    setNode,
    loading,
    failing,
    hovering,
    movesAway,
    config,
    withGames,
    gameMenu,
    current: () => cache[root.node.fen],
    toggle() {
      movesAway(0);
      enabled(!enabled());
      setNode();
      root.autoScroll();
    },
    disable() {
      if (enabled()) {
        enabled(false);
        gameMenu(null);
        root.autoScroll();
      }
    },
    setHovering(fen, uci) {
      hovering(uci ? {
        fen,
        uci,
      } : null);
      root.setAutoShapes();
    },
    fetchMasterOpening: (function() {
      const masterCache = {};
      return (fen: Fen): JQueryPromise<OpeningData> => {
        if (masterCache[fen]) return $.Deferred().resolve(masterCache[fen]).promise() as JQueryPromise<OpeningData>;
        return xhr.opening(opts.endpoint, 'standard', fen, {
          db: {
            selected: prop('masters')
          }
        }, false).then((res: OpeningData) => {
          masterCache[fen] = res;
          return res;
        });
      }
    })(),
    fetchTablebaseHit(fen: Fen): JQueryPromise<SimpleTablebaseHit> {
      return xhr.tablebase(opts.tablebaseEndpoint, effectiveVariant, fen).then((res: TablebaseData) => {
        const move = res.moves[0];
        if (move && move.dtz == null) throw 'unknown tablebase position';
        return {
          fen: fen,
          best: move && move.uci,
          winner: res.checkmate ? opposite(colorOf(fen)) : (
            res.stalemate ? undefined : winnerOf(fen, move!)
          )
        } as SimpleTablebaseHit
      });
    }
  };
};
