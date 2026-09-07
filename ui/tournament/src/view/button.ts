import { h, type VNode } from 'snabbdom';

import { spinnerVdom, bind, onInsert, snabIcon } from 'lib/view';

import type TournamentController from '../ctrl';

function orJoinSpinner(ctrl: TournamentController, f: () => VNode): VNode {
  return ctrl.joinSpinner ? spinnerVdom() : f();
}

export function withdraw(ctrl: TournamentController): VNode {
  return orJoinSpinner(ctrl, () => {
    const pause = ctrl.data.isStarted;
    return h('button.fbt.text', { hook: bind('click', ctrl.withdraw, ctrl.redraw) }, [
      snabIcon(pause ? 'pause' : 'flagOutline'),
      i18n.site[pause ? 'pause' : 'withdraw'],
    ]);
  });
}

export function join(ctrl: TournamentController): VNode {
  return orJoinSpinner(ctrl, () => {
    const delay = ctrl.data.me?.pauseDelay;
    const joinable = ctrl.data.verdicts.accepted && !delay;
    const button = h(
      'button' + (joinable ? '.button.button-green' : '.fbt.text'),
      {
        attrs: { disabled: !joinable },
        hook: bind('click', _ => ctrl.join(), ctrl.redraw),
      },
      [snabIcon('playTriangle'), i18n.site.join],
    );
    return delay
      ? h('div.delay-wrap', { attrs: { title: 'Waiting to be able to re-join the tournament' } }, [
          h(
            'div.delay',
            {
              hook: onInsert(el => {
                el.style.animation = `tour-delay ${delay}s linear`;
                setTimeout(() => {
                  if (delay === ctrl.data.me!.pauseDelay) {
                    ctrl.data.me!.pauseDelay = 0;
                    ctrl.redraw();
                  }
                }, delay * 1000);
              }),
            },
            button,
          ),
        ])
      : button;
  });
}

export function joinWithdraw(ctrl: TournamentController): VNode | undefined {
  if (!ctrl.opts.userId)
    return h('a.button.button-green', { attrs: { href: '/login?referrer=' + window.location.pathname } }, [
      snabIcon('playTriangle'),
      i18n.site.signIn,
    ]);
  if (!ctrl.data.isFinished) return ctrl.isIn() ? withdraw(ctrl) : join(ctrl);
  return undefined;
}
