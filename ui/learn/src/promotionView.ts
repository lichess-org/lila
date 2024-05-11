import { RunCtrl } from './run/runCtrl';
import { PromotionRole } from './util';
import { h } from 'snabbdom';
import { bind } from 'common/snabbdom';
import { opposite, key2pos } from 'chessground/util';

const pieces: PromotionRole[] = ['queen', 'knight', 'rook', 'bishop'];

export function promotionView(ctrl: RunCtrl) {
  const { promotionCtrl } = ctrl.levelCtrl;
  const { promoting } = promotionCtrl;
  const { chessground: ground } = ctrl;
  if (!promoting || !ground) return;

  const color = opposite(ground.state.turnColor);
  const orientation = ground.state.orientation;
  const vertical = color === orientation ? 'top' : 'bottom';

  let left = key2pos(promoting.dest)[0] * 12.5;
  if (orientation === 'black') left = 87.5 - left;

  const explain = !!ctrl.levelCtrl.blueprint.explainPromotion;
  const bounds = ground.state.dom.bounds();

  return h(
    'div#promotion-choice.' + vertical,
    {
      // a hack for now... not sure how else to fix at the moment
      style: { width: `${bounds.width}px`, height: `${bounds.height}px` },
    },
    [
      ...pieces.map((role, i) =>
        h(
          'square',
          {
            style: { [vertical]: `${i * 12.5}%`, left: `${left}%` },
            hook: bind('click', (e: Event) => {
              e.stopPropagation();
              promotionCtrl.finish(role);
            }),
          },
          h('piece.' + role + '.' + color),
        ),
      ),
      explain ? renderExplanation(ctrl) : null,
    ],
  );
}

function renderExplanation(ctrl: RunCtrl) {
  return h('div.explanation', [
    h('h2', ctrl.trans.noarg('pawnPromotion')),
    h('p', ctrl.trans.noarg('yourPawnReachedTheEndOfTheBoard')),
    h('p', ctrl.trans.noarg('itNowPromotesToAStrongerPiece')),
    h('p', ctrl.trans.noarg('selectThePieceYouWant')),
  ]);
}
