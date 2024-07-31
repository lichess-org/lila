import { h, VNode } from 'snabbdom';
import AnalyseCtrl from '../ctrl';
import { moveArrowAttributes, winnerOf } from './explorerUtil';
import { TablebaseMoveStats } from './interfaces';
import { FEN } from 'chessground/types';

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
            h('td', [showDtz(ctrl, fen, move), showDtm(ctrl, fen, move), showDtw(fen, move)]),
          ]),
        ),
      ),
    ]),
  ];
}

function showDtm(ctrl: AnalyseCtrl, fen: FEN, move: TablebaseMoveStats) {
  if (move.dtm)
    return h(
      'result.' + winnerOf(fen, move),
      {
        attrs: { title: ctrl.trans.pluralSame('mateInXHalfMoves', Math.abs(move.dtm)) + ' (Depth To Mate)' },
      },
      'DTM ' + Math.abs(move.dtm),
    );
  return undefined;
}

function showDtw(fen: FEN, move: TablebaseMoveStats) {
  if (move.dtw)
    return h(
      'result.' + winnerOf(fen, move),
      { attrs: { title: 'Depth To Win' } },
      'DTW ' + Math.abs(move.dtw),
    );
  return undefined;
}

function showDtz(ctrl: AnalyseCtrl, fen: FEN, move: TablebaseMoveStats): VNode | null {
  const trans = ctrl.trans.noarg;
  if (move.checkmate) return h('result.' + winnerOf(fen, move), trans('checkmate'));
  else if (move.variant_win) return h('result.' + winnerOf(fen, move), trans('variantLoss'));
  else if (move.variant_loss) return h('result.' + winnerOf(fen, move), trans('variantWin'));
  else if (move.stalemate) return h('result.draws', trans('stalemate'));
  else if (move.insufficient_material) return h('result.draws', trans('insufficientMaterial'));
  else if (move.dtz === null) return null;
  else if (move.dtz === 0) return h('result.draws', trans('draw'));
  else if (move.zeroing)
    return move.san.includes('x')
      ? h('result.' + winnerOf(fen, move), trans('capture'))
      : h('result.' + winnerOf(fen, move), trans('pawnMove'));
  return h(
    'result.' + winnerOf(fen, move),
    {
      attrs: {
        title: trans('dtzWithRounding') + ' (Distance To Zeroing)',
      },
    },
    'DTZ ' + Math.abs(move.dtz),
  );
}
