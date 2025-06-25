import { view as cevalView } from 'lib/ceval/ceval';
import * as licon from 'lib/licon';
import { type VNode, onInsert, hl } from 'lib/snabbdom';
import { playable } from 'lib/game/game';
import * as router from 'lib/game/router';
import { render as trainingView } from './roundTraining';
import crazyView from '../crazy/crazyView';
import type AnalyseCtrl from '../ctrl';
import forecastView from '../forecast/forecastView';
import { view as keyboardView } from '../keyboard';
import { render as renderKeyboardMove } from 'keyboardMove';
import type * as studyDeps from '../study/studyDeps';
import { relayView } from '../study/relay/relayView';
import {
  viewContext,
  renderBoard,
  renderMain,
  renderControls,
  renderTools,
  renderUnderboard,
} from './components';
import { wikiToggleBox } from '../wiki';
import { watchers } from 'lib/view/watchers';
import { renderChat } from 'lib/chat/renderChat';

export default function (deps?: typeof studyDeps) {
  return function (ctrl: AnalyseCtrl): VNode {
    if (ctrl.nvui) return ctrl.nvui.render(deps);
    else if (deps && ctrl.study?.relay) return relayView(ctrl, ctrl.study, ctrl.study.relay, deps);
    else return analyseView(ctrl, deps);
  };
}

function analyseView(ctrl: AnalyseCtrl, deps?: typeof studyDeps): VNode {
  const ctx = viewContext(ctrl, deps);
  const { study, gamebookPlayView, gaugeOn } = ctx;
  return renderMain(
    ctx,
    ctrl.keyboardHelp && keyboardView(ctrl),
    study && deps?.studyView.overboard(study),
    renderBoard(ctx),
    gaugeOn && cevalView.renderGauge(ctrl),
    crazyView(ctrl, ctrl.topColor(), 'top'),
    gamebookPlayView || renderTools(ctx),
    crazyView(ctrl, ctrl.bottomColor(), 'bottom'),
    !gamebookPlayView && renderControls(ctrl),
    renderUnderboard(ctx),
    ctrl.keyboardMove && renderKeyboardMove(ctrl.keyboardMove),
    trainingView(ctrl),
    ctrl.studyPractice
      ? deps?.studyPracticeView.side(study!)
      : hl(
          'aside.analyse__side',
          {
            hook: onInsert(elm => {
              if (ctrl.opts.$side && ctrl.opts.$side.length) {
                $(elm).replaceWith(ctrl.opts.$side);
                wikiToggleBox();
              }
            }),
          },
          ctrl.studyPractice
            ? [deps?.studyPracticeView.side(study!)]
            : study
              ? [deps?.studyView.side(study, true)]
              : [
                  ctrl.forecast && forecastView(ctrl, ctrl.forecast),
                  !ctrl.synthetic &&
                    playable(ctrl.data) &&
                    hl(
                      'div.back-to-game',
                      hl(
                        'a.button.button-empty.text',
                        {
                          attrs: {
                            href: router.game(ctrl.data, ctrl.data.player.color),
                            'data-icon': licon.Back,
                          },
                        },
                        i18n.site.backToGame,
                      ),
                    ),
                ],
        ),
    ctrl.chatCtrl && renderChat(ctrl.chatCtrl),
    hl('div.chat__members.none', { hook: onInsert(watchers) }),
  );
}
