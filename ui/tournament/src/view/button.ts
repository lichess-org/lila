import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';
import { isIn } from '../tournament';
import { spinner, bind, dataIcon } from './util';
import TournamentController from '../ctrl';

function orJoinSpinner(ctrl: TournamentController, f: () => VNode): VNode {
  return ctrl.joinSpinner ? spinner() : f();
}

export function withdraw(ctrl: TournamentController): VNode {
  return orJoinSpinner(ctrl, () => {
    const pause = ctrl.data.isStarted;
    return h('button.fbt.text', {
      attrs: dataIcon(pause ? 'Z' : 'b'),
      hook: bind('click', ctrl.withdraw, ctrl.redraw)
    }, ctrl.trans.noarg(pause ? 'pause' : 'withdraw'));
  });
}

export function join(ctrl: TournamentController): VNode {
  return orJoinSpinner(ctrl, () => {
    const joinable = ctrl.data.verdicts.accepted;
    return h('button.fbt.text.' + (joinable ? 'highlight' : 'disabled'), {
      attrs: dataIcon('G'),
      hook: bind('click', _ => {
        if (ctrl.data.private) {
          const p = prompt(ctrl.trans.noarg('password'));
          if (p !== null) ctrl.join(p);
        } else ctrl.join();
      }, ctrl.redraw)
    }, ctrl.trans('join'));
  });
}

export function joinWithdraw(ctrl: TournamentController): VNode | undefined {
  if (!ctrl.opts.userId) return h('a.fbt.text.highlight', {
    attrs: {
      href: '/login?referrer=' + window.location.pathname,
      'data-icon': 'G'
    }
  }, ctrl.trans('signIn'));
  if (!ctrl.data.isFinished) return isIn(ctrl) ? withdraw(ctrl) : join(ctrl);
}
