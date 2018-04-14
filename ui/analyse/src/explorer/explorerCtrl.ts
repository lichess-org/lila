import { prop, storedProp } from 'common';
import { controller as configCtrl } from './explorerConfig';
import xhr = require('./explorerXhr');
import { synthetic } from '../util';
import { game as gameUtil } from 'game';
import AnalyseCtrl from '../ctrl';
import { Hovering, ExplorerCtrl, ExplorerData, OpeningData } from './interfaces';

function tablebaseRelevant(variant: string, fen: Fen) {
  const parts = fen.split(/\s/);
  const pieceCount = parts[0].split(/[nbrqkp]/i).length - 1;

  if (variant === 'standard' || variant === 'chess960') return pieceCount <= 8;
  else if (variant === 'atomic' || variant === 'antichess') return pieceCount <= 7;
  else return false;
}

export default function(root: AnalyseCtrl, opts, allow: boolean): ExplorerCtrl {
  const allowed = prop(allow),
  enabled = root.embed ? prop(false) : storedProp('explorer.enabled', false),
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
  const withGames = synthetic(root.data) || gameUtil.replayable(root.data) || !!root.data.opponent.ai;
  const effectiveVariant = root.data.game.variant.key === 'fromPosition' ? 'standard' : root.data.game.variant.key;

  const config = configCtrl(root.data.game, onConfigClose, root.trans, root.redraw);

  const fetch = window.lichess.fp.debounce(function() {
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
    })()
  };
};
