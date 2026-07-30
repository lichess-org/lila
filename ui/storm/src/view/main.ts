import { Chessground as makeChessground } from '@lichess-org/chessground';

import { licon } from 'lib/licon';
import { pubsub } from 'lib/pubsub';
import { makeCgOpts, povMessage } from 'lib/puz/run';
import { getNow } from 'lib/puz/util';
import { makeConfig as makeCgConfig } from 'lib/puz/view/chessground';
import renderClock from 'lib/puz/view/clock';
import renderHistory from 'lib/puz/view/history';
import { playModifiers, renderCombo } from 'lib/puz/view/util';
import { onInsert, icon, div, main, button, dataIcon, a, strong, span, p, type VNode } from 'lib/view';

import config from '@/config';
import type StormCtrl from '@/ctrl';

import renderSummary from './end';

export default function (ctrl: StormCtrl): VNode {
  if (ctrl.vm.dupTab) return renderReload(i18n.storm.thisRunWasOpenedInAnotherTab);
  if (ctrl.vm.lateStart) return renderReload(i18n.storm.thisRunHasExpired);
  if (!ctrl.run.endAt) {
    return div('.storm.storm-app.storm--play', { class: playModifiers(ctrl.run) }, renderPlay(ctrl));
  }
  return main('.storm.storm--end', [renderSummary(ctrl), renderHistory(ctrl)]);
}

const chessground = (ctrl: StormCtrl): VNode =>
  div('.cg-wrap', {
    hook: onInsert(el => {
      ctrl.ground(
        makeChessground(
          el,
          makeCgConfig(makeCgOpts(ctrl.run, !ctrl.run.endAt, ctrl.flipped), ctrl.pref, ctrl.userMove),
        ),
      );
      pubsub.on('board.change', (is3d: boolean) =>
        ctrl.withGround(g => {
          g.state.addPieceZIndex = is3d;
          g.redrawAll();
        }),
      );
    }),
  });

const renderBonus = (bonus: number) => `${bonus}s`;

const renderPlay = (ctrl: StormCtrl): VNode[] => {
  const run = ctrl.run;
  const now = getNow();
  const start = now - ctrl.duration;
  const { malus, bonus } = run.modifier;
  return [
    div('.puz-board.main-board', [chessground(ctrl), ctrl.promotion.view()]),
    div('.puz-side', [
      run.clock.startAt ? renderSolved(ctrl) : startNode,
      div('.puz-clock', [
        renderClock(run, ctrl.endNow, true),
        !!malus && malus.at > start ? div('.puz-clock__malus', '-' + malus.seconds) : null,
        !!bonus && bonus.at > start ? div('.puz-clock__bonus', '+' + bonus.seconds) : null,
        run.clock.started() ? [div('.puz-clock__pov', povMessage(run))] : null,
      ]),
      div('.puz-side__table', [renderControls(ctrl), renderCombo(config, renderBonus)(run)]),
    ]),
  ];
};

const renderSolved = ({ countWins }: StormCtrl): VNode =>
  div('.puz-side__top.puz-side__solved', [div('.puz-side__solved__text', `${countWins()}`)]);

const renderControls = (ctrl: StormCtrl): VNode =>
  div('.puz-side__control', [
    button('.puz-side__control__flip.button', {
      class: { active: ctrl.flipped, 'button-empty': !ctrl.flipped },
      ...dataIcon(licon.ChasingArrows),
      title: i18n.site.flipBoard + ' (Keyboard: f)',
      hook: onInsert(el => el.addEventListener('click', ctrl.flip)),
    }),
    a('/storm')('.puz-side__control__reload.button.button-empty', {
      ...dataIcon(licon.Trash),
      title: i18n.storm.newRun,
    }),
    button('.puz-side__control__end.button.button-empty', {
      ...dataIcon(licon.FlagOutline),
      title: i18n.storm.endRun,
      hook: onInsert(el => el.addEventListener('click', ctrl.endNow)),
    }),
  ]);

const startNode = div('.puz-side__top.puz-side__start', [
  div('.puz-side__start__text', [strong('Puzzle Storm'), span(i18n.storm.moveToStart)]),
]);

const renderReload = (text: string) =>
  div('.storm.storm--reload.box.box-pad', [
    icon(licon.Storm)(),
    p(text),
    a('/storm')('.storm--dup__reload.button', i18n.storm.clickToReload),
  ]);
