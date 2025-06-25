import config from '../config';
import renderClock from 'lib/puz/view/clock';
import renderEnd from './end';
import type StormCtrl from '../ctrl';
import type { VNode } from 'snabbdom';
import { makeCgOpts, povMessage } from 'lib/puz/run';
import { makeConfig as makeCgConfig } from 'lib/puz/view/chessground';
import { getNow } from 'lib/puz/util';
import { playModifiers, renderCombo } from 'lib/puz/view/util';
import * as licon from 'lib/licon';
import { onInsert, hl } from 'lib/snabbdom';
import { Chessground as makeChessground } from '@lichess-org/chessground';
import { pubsub } from 'lib/pubsub';

export default function (ctrl: StormCtrl): VNode {
  if (ctrl.vm.dupTab) return renderReload(i18n.storm.thisRunWasOpenedInAnotherTab);
  if (ctrl.vm.lateStart) return renderReload(i18n.storm.thisRunHasExpired);
  if (!ctrl.run.endAt)
    return hl('div.storm.storm-app.storm--play', { class: playModifiers(ctrl.run) }, renderPlay(ctrl));
  return hl('main.storm.storm--end', renderEnd(ctrl));
}

const chessground = (ctrl: StormCtrl): VNode =>
  hl('div.cg-wrap', {
    hook: {
      insert: vnode => {
        ctrl.ground(
          makeChessground(
            vnode.elm as HTMLElement,
            makeCgConfig(makeCgOpts(ctrl.run, !ctrl.run.endAt, ctrl.flipped), ctrl.pref, ctrl.userMove),
          ),
        );
        pubsub.on('board.change', (is3d: boolean) =>
          ctrl.withGround(g => {
            g.state.addPieceZIndex = is3d;
            g.redrawAll();
          }),
        );
      },
    },
  });

const renderBonus = (bonus: number) => `${bonus}s`;

const renderPlay = (ctrl: StormCtrl): VNode[] => {
  const run = ctrl.run;
  const malus = run.modifier.malus;
  const bonus = run.modifier.bonus;
  const now = getNow();
  return [
    hl('div.puz-board.main-board', [chessground(ctrl), ctrl.promotion.view()]),
    hl('div.puz-side', [
      run.clock.startAt ? renderSolved(ctrl) : renderStart(),
      hl('div.puz-clock', [
        renderClock(run, ctrl.endNow, true),
        !!malus && malus.at > now - 900 && hl('div.puz-clock__malus', '-' + malus.seconds),
        !!bonus && bonus.at > now - 900 && hl('div.puz-clock__bonus', '+' + bonus.seconds),
        run.clock.started() || [hl('span.puz-clock__pov', povMessage(run))],
      ]),
      hl('div.puz-side__table', [renderControls(ctrl), renderCombo(config, renderBonus)(run)]),
    ]),
  ];
};

const renderSolved = (ctrl: StormCtrl): VNode =>
  hl('div.puz-side__top.puz-side__solved', [hl('div.puz-side__solved__text', `${ctrl.countWins()}`)]);

const renderControls = (ctrl: StormCtrl): VNode =>
  hl('div.puz-side__control', [
    hl('a.puz-side__control__flip.button', {
      class: { active: ctrl.flipped, 'button-empty': !ctrl.flipped },
      attrs: { 'data-icon': licon.ChasingArrows, title: i18n.site.flipBoard + ' (Keyboard: f)' },
      hook: onInsert(el => el.addEventListener('click', ctrl.flip)),
    }),
    hl('a.puz-side__control__reload.button.button-empty', {
      attrs: { href: '/storm', 'data-icon': licon.Trash, title: i18n.storm.newRun },
    }),
    hl('a.puz-side__control__end.button.button-empty', {
      attrs: { 'data-icon': licon.FlagOutline, title: i18n.storm.endRun },
      hook: onInsert(el => el.addEventListener('click', ctrl.endNow)),
    }),
  ]);

const renderStart = () =>
  hl('div.puz-side__top.puz-side__start', [
    hl('div.puz-side__start__text', [hl('strong', 'Puzzle Storm'), hl('span', i18n.storm.moveToStart)]),
  ]);

const renderReload = (text: string) =>
  hl('div.storm.storm--reload.box.box-pad', [
    hl('i', { attrs: { 'data-icon': licon.Storm } }),
    hl('p', text),
    hl('a.storm--dup__reload.button', { attrs: { href: '/storm' } }, i18n.storm.clickToReload),
  ]);
