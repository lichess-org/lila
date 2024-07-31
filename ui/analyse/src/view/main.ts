import { view as cevalView } from 'ceval';
import * as licon from 'common/licon';
import { onInsert, looseH as h } from 'common/snabbdom';
import { playable } from 'game';
import * as router from 'game/router';
import { VNode } from 'snabbdom';
import { render as trainingView } from './roundTraining';
import crazyView from '../crazy/crazyView';
import AnalyseCtrl from '../ctrl';
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

export default function (deps?: typeof studyDeps) {
  return function (ctrl: AnalyseCtrl): VNode {
    if (ctrl.nvui) return ctrl.nvui.render();
    else if (deps && ctrl.study?.relay) return relayView(ctrl, ctrl.study, ctrl.study.relay, deps);
    else return analyseView(ctrl, deps);
  };
}

function analyseView(ctrl: AnalyseCtrl, deps?: typeof studyDeps): VNode {
  const ctx = viewContext(ctrl, deps);
  const { study, gamebookPlayView, gaugeOn } = ctx;

  return renderMain(ctx, [
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
      : h(
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
                  h(
                    'div.back-to-game',
                    h(
                      'a.button.button-empty.text',
                      {
                        attrs: {
                          href: router.game(ctrl.data, ctrl.data.player.color),
                          'data-icon': licon.Back,
                        },
                      },
                      ctrl.trans.noarg('backToGame'),
                    ),
                  ),
              ],
        ),
    h('div.chat__members.none', { hook: onInsert(site.watchers) }),
  ]);
}
