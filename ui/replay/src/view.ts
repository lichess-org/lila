import ReplayCtrl from './ctrl';
import { Chessground } from 'chessground';
import { Config as CgConfig } from 'chessground/config';
import { h, VNode } from 'snabbdom';
import { bindMobileMousedown, bindNonPassive, onInsert } from 'common/snabbdom';
import stepwiseScroll from 'common/wheel';

export default function view(ctrl: ReplayCtrl) {
  return ctrl.menu() ? renderMenu(ctrl) : h('div.replay', [renderBoard(ctrl), renderControls(ctrl)]);
}

const renderMenu = (ctrl: ReplayCtrl) =>
  h('div.replay.replay--menu', [
    h(
      'div.replay__menu',
      h('div.replay__menu__inner', [
        h(
          'button.replay__menu__entry.fbt.text',
          {
            attrs: {
              'data-icon': '',
            },
            on: { click: ctrl.flip },
          },
          'Flip the board'
        ),
        h(
          'a.replay__menu__entry.fbt.text',
          {
            attrs: {
              href: ctrl.analysisUrl(),
              target: '_blank',
              'data-icon': '',
            },
          },
          'Analysis board'
        ),
        h(
          'a.replay__menu__entry.fbt.text',
          {
            attrs: {
              href: ctrl.practiceUrl(),
              target: '_blank',
              'data-icon': '',
            },
          },
          'Practice with computer'
        ),
      ])
    ),
    renderControls(ctrl),
  ]);

const renderControls = (ctrl: ReplayCtrl) =>
  h('div.replay__controls', [
    dirButton('', ctrl.index < 1, ctrl.backward),
    h(
      'button.fbt.replay__controls__menu',
      {
        class: { active: ctrl.menu() },
        on: { click: ctrl.toggleMenu },
      },
      '⋮'
    ),
    dirButton('', ctrl.index > ctrl.nodes.length - 2, ctrl.forward),
  ]);

const dirButton = (icon: string, disabled: boolean, action: () => void) =>
  h('button.fbt', {
    attrs: {
      'data-icon': icon,
    },
    class: { disabled },
    hook: onInsert(el => bindMobileMousedown(el, e => repeater(action, e))),
  });

function repeater(action: () => void, e: Event) {
  const repeat = () => {
    action();
    delay = Math.max(100, delay - delay / 15);
    timeout = setTimeout(repeat, delay);
  };
  let delay = 350;
  let timeout = setTimeout(repeat, 500);
  action();
  const eventName = e.type == 'touchstart' ? 'touchend' : 'mouseup';
  document.addEventListener(eventName, () => clearTimeout(timeout), { once: true });
}

const renderBoard = (ctrl: ReplayCtrl): VNode =>
  h(
    'div.replay__board',
    {
      hook: wheelScroll(ctrl),
    },
    h('div.cg-wrap', {
      hook: {
        insert: vnode => ctrl.ground(Chessground(vnode.elm as HTMLElement, makeConfig(ctrl))),
      },
    })
  );

const wheelScroll = (ctrl: ReplayCtrl) =>
  'ontouchstart' in window || lichess.storage.get('scrollMoves') == '0'
    ? undefined
    : bindNonPassive(
        'wheel',
        stepwiseScroll((e: WheelEvent, scroll: boolean) => {
          e.preventDefault();
          if (e.deltaY > 0 && scroll) ctrl.forward();
          else if (e.deltaY < 0 && scroll) ctrl.backward();
        })
      );

export const makeConfig = (ctrl: ReplayCtrl): CgConfig => ({
  viewOnly: true,
  coordinates: true,
  // addDimensionsCssVars: true,
  drawable: {
    enabled: false,
    visible: false,
  },
  ...ctrl.cgOpts(),
});
