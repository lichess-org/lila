import { RunCtrl } from './run/runCtrl';
import { PromotionRole } from './util';
import { h } from 'snabbdom';
import { bind } from 'common/snabbdom';
import { opposite } from 'chessground/util';

const pieces: PromotionRole[] = ['queen', 'knight', 'rook', 'bishop'];

// TODO: needs implementation
// const key2pos = chessground.util.key2pos;
const key2pos = (key: Key) => [key && 0, 0];

export function promotionView(ctrl: RunCtrl) {
  const { promotionCtrl } = ctrl.levelCtrl;
  const { promoting } = promotionCtrl;
  const { chessground: ground } = ctrl;
  if (!promoting || !ground) return;

  const color = opposite(ground.state.turnColor);
  const orientation = ground.state.orientation;
  const vertical = color === orientation ? 'top' : 'bottom';

  let left = (8 - key2pos(promoting.dest)[0]) * 12.5;
  if (orientation === 'white') left = 87.5 - left;

  const explain = !!ctrl.levelCtrl.blueprint.explainPromotion;

  return h('div#promotion-choice.' + vertical, [
    ...pieces.map((serverRole, i) => {
      // TODO:
      i;
      left;
      return h(
        'square',
        {
          // TODO:
          // style: vertical + ': ' + i * 12.5 + '%;left: ' + left + '%',
          hook: bind('click', (e: Event) => {
            e.stopPropagation();
            promotionCtrl.finish(serverRole);
          }),
        },
        h('piece.' + serverRole + '.' + color),
      );
    }),
    explain ? renderExplanation(ctrl) : null,
  ]);
}

function renderExplanation(ctrl: RunCtrl) {
  return h('div.explanation', [
    h('h2', ctrl.trans.noarg('pawnPromotion')),
    h('p', ctrl.trans.noarg('yourPawnReachedTheEndOfTheBoard')),
    h('p', ctrl.trans.noarg('itNowPromotesToAStrongerPiece')),
    h('p', ctrl.trans.noarg('selectThePieceYouWant')),
  ]);
}
