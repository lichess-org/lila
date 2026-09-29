import { licon, type LiconValue } from 'lib/licon';
import { type VNode, bind, dataIcon, hl } from 'lib/view';

import type RecallCtrl from './recallCtrl';
import type { State } from './recallCtrl';

const statusText: Record<State, string> = {
  empty: i18n.study.recallNoMovesYet,
  end: i18n.study.recallEndOfTheLine,
  play: i18n.study.recallPlayTheMoveYouRemember,
  wait: i18n.study.recallWaitingForTheReply,
  right: i18n.study.recallRightMove,
  wrong: i18n.study.recallWrongMove,
};

export function render(ctrl: RecallCtrl): VNode {
  const state = ctrl.state();
  const button = (key: string, icon: LiconValue, text: string, click: () => void): VNode =>
    hl(
      'button.button.button-empty.text',
      { key, attrs: dataIcon(icon), hook: bind('click', click, ctrl.redraw) },
      text,
    );
  return hl('div.recall', [
    hl('div.recall__status.' + state, statusText[state]),
    state === 'empty'
      ? hl('div.recall__help', [
          hl('p', i18n.study.recallExplanation),
          hl(
            'p',
            i18n.study.recallHowToStoreMoves(`"${i18n.study.normalAnalysis}"`, `"${i18n.study.recall}"`),
          ),
        ])
      : [
          ctrl.hideMoves() && hl('div.recall__help', i18n.study.recallMovesAreHidden),
          hl('div.recall__actions', [
            state === 'end' && button('restart', licon.Reload, i18n.study.playAgain, ctrl.restart),
            button(
              'show',
              licon.Eye,
              ctrl.hideMoves() ? i18n.study.showMoves : i18n.study.hideMoves,
              ctrl.showMoves.toggle,
            ),
          ]),
        ],
  ]);
}
