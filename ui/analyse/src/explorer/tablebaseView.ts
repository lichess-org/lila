import { h, type VNode } from 'snabbdom';
import type AnalyseCtrl from '../ctrl';
import { moveArrowAttributes, winnerOf } from './explorerUtil';
import type { TablebaseMoveStats } from './interfaces';

export function showTablebase(
  ctrl: AnalyseCtrl,
  fen: FEN,
  title: string,
  tooltip: string | undefined,
  moves: TablebaseMoveStats[],
): VNode[] {
  if (!moves.length) return [];
  return [
    h('div.title', tooltip ? { attrs: { title: tooltip } } : {}, title),
    h('table.tablebase', [
      h(
        'tbody',
        moveArrowAttributes(ctrl, { fen, onClick: (_, uci) => uci && ctrl.explorerMove(uci) }),
        moves.map(move =>
          h('tr', { key: move.uci, attrs: { 'data-uci': move.uci } }, [
            h('td', move.san),
            h('td', [showDtz(fen, move), showDtc(fen, move), showDtm(fen, move), showDtw(fen, move)]),
          ]),
        ),
      ),
    ]),
  ];
}

function showDtm(fen: FEN, move: TablebaseMoveStats) {
  return move.dtm
    ? h(
        'result.' + winnerOf(fen, move),
        {
          attrs: { title: i18n.site.mateInXHalfMoves(Math.abs(move.dtm)) + ' (Depth To Mate)' },
        },
        'DTM ' + Math.abs(move.dtm),
      )
    : undefined;
}

function showDtw(fen: FEN, move: TablebaseMoveStats) {
  return move.dtw
    ? h(
        'result.' + winnerOf(fen, move),
        { attrs: { title: 'Half-moves to win (Depth To Win)' } },
        'DTW ' + Math.abs(move.dtw),
      )
    : undefined;
}

function showDtc(fen: FEN, move: TablebaseMoveStats) {
  return move.dtc
    ? h(
        'result.' + winnerOf(fen, move),
        { attrs: { title: 'Moves to capture, promotion, or checkmate (Depth To Conversion)' } },
        'DTC ' + Math.abs(move.dtc),
      )
    : undefined;
}

function showDtz(fen: FEN, move: TablebaseMoveStats): VNode | undefined {
  if (move.checkmate) return h('result.' + winnerOf(fen, move), i18n.site.checkmate);
  if (move.variant_win) return h('result.' + winnerOf(fen, move), i18n.site.variantLoss);
  if (move.variant_loss) return h('result.' + winnerOf(fen, move), i18n.site.variantWin);
  if (move.stalemate) return h('result.draws', i18n.site.stalemate);
  if (move.insufficient_material) return h('result.draws', i18n.site.insufficientMaterial);
  if (move.dtz === 0 || move.dtc === 0) return h('result.draws', i18n.site.draw);
  if ((move.dtz || move.dtc) && move.zeroing)
    return move.san.includes('x')
      ? h('result.' + winnerOf(fen, move), i18n.site.capture)
      : h('result.' + winnerOf(fen, move), i18n.site.pawnMove);
  return move.dtz
    ? h(
        'result.' + winnerOf(fen, move),
        {
          attrs: {
            title: i18n.site.dtzWithRounding + ' (Distance To Zeroing)',
          },
        },
        'DTZ ' + Math.abs(move.dtz),
      )
    : undefined;
}
