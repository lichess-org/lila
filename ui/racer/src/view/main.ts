import { Chessground } from 'chessground';
import { makeConfig as makeCgConfig } from 'puz/view/chessground';
import renderClock from 'puz/view/clock';
import RacerCtrl from '../ctrl';
import { playModifiers, renderCombo, renderSolved } from 'puz/view/util';
import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import { makeCgOpts } from 'puz/run';
import { renderRace } from './race';
import { Race } from '../interfaces';

export default function (ctrl: RacerCtrl): VNode {
  return h(
    'div.racer.racer-app.racer--play',
    {
      class: playModifiers(ctrl.run),
    },
    renderPlay(ctrl)
  );
}

const chessground = (ctrl: RacerCtrl): VNode =>
  h('div.cg-wrap', {
    hook: {
      insert: vnode =>
        ctrl.ground(
          Chessground(vnode.elm as HTMLElement, makeCgConfig(makeCgOpts(ctrl.run), ctrl.pref, ctrl.userMove))
        ),
      destroy: _ => ctrl.withGround(g => g.destroy()),
    },
  });

const renderPlay = (ctrl: RacerCtrl): VNode[] => [
  renderRace(ctrl),
  h('div.puz-board.main-board', [chessground(ctrl), ctrl.promotion.view()]),
  h('div.puz-side', [
    ctrl.run.clock.startAt ? renderSolved(ctrl.run) : renderStart(),
    ctrl.race.isPlayer ? renderClock(ctrl.run, ctrl.endNow) : renderJoin(ctrl.race),
    h('div.puz-side__table', [renderCombo(ctrl.run)]),
  ]),
];

const renderJoin = (race: Race) =>
  h(
    'form.puz-side__join',
    {
      attrs: {
        action: `/racer/${race.id}`,
        method: 'post',
      },
    },
    h(
      'button.button.button-fat',
      {
        attrs: {
          type: 'submit',
        },
      },
      'Join the race!'
    )
  );

const renderStart = () =>
  h(
    'div.puz-side__top.puz-side__start',
    h('div.puz-side__start__text', [h('strong', 'Puzzle Racer'), h('span', 'Waiting to start')])
  );
