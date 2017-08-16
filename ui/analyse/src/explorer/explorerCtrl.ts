import { throttle, prop, storedProp } from 'common';
import { controller as configCtrl } from './explorerConfig';
import xhr = require('./openingXhr');
import { synthetic } from '../util';
import { game as gameUtil } from 'game';
import AnalyseCtrl from '../ctrl';
import { Hovering, ExplorerCtrl } from './interfaces';

function tablebaseRelevant(variant: string, fen: Fen) {
  const parts = fen.split(/\s/);
  const pieceCount = parts[0].split(/[nbrqkp]/i).length - 1;

  if (variant === 'standard' || variant === 'chess960' || variant === 'atomic')
    return pieceCount <= 7;
  else if (variant === 'antichess') return pieceCount <= 6;
  else return false;
}

export default function(root: AnalyseCtrl, opts, allow: boolean): ExplorerCtrl {
  const allowed = prop(allow);
  const enabled = root.embed ? prop(false) : storedProp('explorer.enabled', false);
  if ((location.hash === '#explorer' || location.hash === '#opening') && !root.embed) enabled(true);
  const loading = prop(true);
  const failing = prop(false);
  const hovering = prop<Hovering | null>(null);
  const movesAway = prop(0);

  let cache = {};
  function onConfigClose() {
    root.redraw();
    cache = {};
    setNode();
  }
  const withGames = synthetic(root.data) || gameUtil.replayable(root.data) || root.data.opponent.ai;
  const effectiveVariant = root.data.game.variant.key === 'fromPosition' ? 'standard' : root.data.game.variant.key;

  const config = configCtrl(root.data.game, onConfigClose, root.trans, root.redraw);

  const fetch = throttle(250, function() {
    const fen = root.node.fen;
    const request = (withGames && tablebaseRelevant(effectiveVariant, fen)) ?
      xhr.tablebase(opts.tablebaseEndpoint, effectiveVariant, fen) :
      xhr.opening(opts.endpoint, effectiveVariant, fen, config.data, withGames);

    request.then(function(res) {
      res.nbMoves = res.moves.length;
      res.fen = fen;
      cache[fen] = res;
      movesAway(res.nbMoves ? 0 : movesAway() + 1);
      loading(false);
      failing(false);
      root.redraw();
    }, () => {
      loading(false);
      failing(true);
      root.redraw();
    })
  }, false);

  const empty = {
    opening: true,
    moves: {}
  };

  function setNode() {
    if (!enabled()) return;
    const node = root.node;
    if (node.ply > 50 && !tablebaseRelevant(effectiveVariant, node.fen)) {
      cache[node.fen] = empty;
    }
    const cached = cache[root.node.fen];
    if (cached) {
      movesAway(cached.nbMoves ? 0 : movesAway() + 1);
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
      return function(fen) {
        if (masterCache[fen]) return $.Deferred().resolve(masterCache[fen]);
        return xhr.opening(opts.endpoint, 'standard', fen, {
          db: {
            selected: prop('masters')
          }
        }, false).then(function(res) {
          masterCache[fen] = res;
          return res;
        });
      }
    })()
  };
};
