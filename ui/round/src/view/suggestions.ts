import { impasseInfo } from 'common/impasse';
import { h } from 'snabbdom';
import RoundController from '../ctrl';
import { isPlayerTurn } from 'game';
import { prepaused } from 'game/status';

export function impasse(ctrl: RoundController) {
  if (!ctrl.impasseHelp) return null;

  const lastStep = ctrl.data.steps[ctrl.data.steps.length - 1],
    rules = ctrl.data.game.variant.key,
    initialSfen = ctrl.data.game.initialSfen,
    i = impasseInfo(rules, lastStep.sfen, initialSfen);

  if (!i) return null;

  return h('div.suggestion', [
    h('h5', [ctrl.noarg('impasse'), h('a.q-explanation', { attrs: { href: '/page/impasse', target: '_blank' } }, '?')]),
    h('div.impasse', [
      h(
        'div.color-icon.sente',
        h('ul.impasse-list', [
          h('li', [ctrl.noarg('enteringKing') + ': ', i.sente.king ? h('span.good', '✓') : '✗']),
          h('li', [ctrl.noarg('invadingPieces') + ': ', i.sente.nbOfPieces + '/10']),
          h('li', [ctrl.noarg('totalImpasseValue') + ': ', i.sente.pieceValue + '/28']),
        ])
      ),
      h(
        'div.color-icon.gote',
        h('ul.impasse-list', [
          h('li', [ctrl.noarg('enteringKing') + ': ', i.gote.king ? h('span.good', '✓') : '✗']),
          h('li', [ctrl.noarg('invadingPieces') + ': ', i.gote.nbOfPieces + '/10']),
          h('li', [ctrl.noarg('totalImpasseValue') + ': ', i.gote.pieceValue + '/27']),
        ])
      ),
    ]),
  ]);
}

export function sealedUsi(ctrl: RoundController) {
  if (!prepaused(ctrl.data)) return null;

  const myTurn = isPlayerTurn(ctrl.data);
  return h(
    'div.suggestion',
    {
      class: {
        glowing: myTurn,
      },
    },
    h('strong.sealed-move', ctrl.trans.noarg(myTurn ? 'makeASealedMove' : 'waitingForASealedMove'))
  );
}
