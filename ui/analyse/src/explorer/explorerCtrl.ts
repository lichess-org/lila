import { prop } from 'common/common';
import { storedProp } from 'common/storage';
import * as gameUtil from 'game';
import { opposite } from 'shogiground/util';
import AnalyseCtrl from '../ctrl';
import { controller as configCtrl } from './explorerConfig';
import { colorOf, winnerOf } from './explorerUtil';
import * as xhr from './explorerXhr';
import { ExplorerCtrl, ExplorerData, Hovering, OpeningData, SimpleTablebaseHit, TablebaseData } from './interfaces';

function pieceCount(sfen: Sfen) {
  const parts = sfen.split(/\s/);
  return parts[0].split(/[nbrqkp]/i).length - 1;
}

function tablebasePieces(variant: VariantKey) {
  switch (variant) {
    case 'standard':
      return 7;
    default:
      return 0;
  }
}

export function tablebaseGuaranteed(variant: VariantKey, sfen: Sfen) {
  return pieceCount(sfen) <= tablebasePieces(variant);
}

function tablebaseRelevant(variant: VariantKey, sfen: Sfen) {
  return pieceCount(sfen) - 1 <= tablebasePieces(variant);
}

export default function (root: AnalyseCtrl, opts, allow: boolean): ExplorerCtrl {
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
  const data = root.data,
    withGames = root.synthetic || gameUtil.replayable(data) || !!data.opponent.ai,
    effectiveVariant = data.game.variant.key,
    config = configCtrl(data.game, onConfigClose, root.trans, root.redraw);

  const fetch = window.lishogi.debounce(
    function () {
      const sfen = root.node.sfen;
      const request: JQueryPromise<ExplorerData> =
        withGames && tablebaseRelevant(effectiveVariant, sfen)
          ? xhr.tablebase(opts.tablebaseEndpoint, effectiveVariant, sfen)
          : xhr.opening(
              opts.endpoint,
              effectiveVariant,
              sfen,
              root.nodeList[0].sfen,
              root.nodeList.slice(1).map(s => s.usi!),
              config.data,
              withGames
            );

      request.then(
        (res: ExplorerData) => {
          cache[sfen] = res;
          movesAway(res.moves.length ? 0 : movesAway() + 1);
          loading(false);
          failing(false);
          root.redraw();
        },
        () => {
          loading(false);
          failing(true);
          root.redraw();
        }
      );
    },
    250,
    true
  );

  const empty = {
    isOpening: true,
    moves: {},
  };

  function setNode() {
    if (!enabled()) return;
    gameMenu(null);
    const node = root.node;
    if (node.ply > 50 && !tablebaseRelevant(effectiveVariant, node.sfen)) {
      cache[node.sfen] = empty;
    }
    const cached = cache[node.sfen];
    if (cached) {
      movesAway(cached.moves.length ? 0 : movesAway() + 1);
      loading(false);
      failing(false);
    } else {
      loading(true);
      fetch();
    }
  }

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
    current: () => cache[root.node.sfen],
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
    setHovering(sfen, usi) {
      hovering(
        usi
          ? {
              sfen,
              usi,
            }
          : null
      );
      root.setAutoShapes();
    },
    fetchMasterOpening: (function () {
      const masterCache = {};
      return (sfen: Sfen): JQueryPromise<OpeningData> => {
        if (masterCache[sfen]) return $.Deferred().resolve(masterCache[sfen]).promise() as JQueryPromise<OpeningData>;
        return xhr
          .opening(
            opts.endpoint,
            'standard',
            sfen,
            sfen,
            [],
            {
              db: {
                selected: prop('masters'),
              },
            },
            false
          )
          .then((res: OpeningData) => {
            masterCache[sfen] = res;
            return res;
          });
      };
    })(),
    fetchTablebaseHit(sfen: Sfen): JQueryPromise<SimpleTablebaseHit> {
      return xhr.tablebase(opts.tablebaseEndpoint, effectiveVariant, sfen).then((res: TablebaseData) => {
        const move = res.moves[0];
        if (move && move.dtz == null) throw 'unknown tablebase position';
        return {
          sfen: sfen,
          best: move && move.usi,
          winner: res.checkmate ? opposite(colorOf(sfen)) : res.stalemate ? undefined : winnerOf(sfen, move!),
        } as SimpleTablebaseHit;
      });
    },
  };
}
