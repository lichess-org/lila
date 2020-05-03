import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';
import { spinner, bind, dataIcon } from './util';
import SwissController from '../ctrl';

function orJoinSpinner(ctrl: SwissController, f: () => VNode): VNode {
  return ctrl.joinSpinner ? spinner() : f();
}

export function withdraw(ctrl: SwissController): VNode {
  return orJoinSpinner(ctrl, () => {
    const pause = ctrl.data.isStarted;
    return h('button.fbt.text', {
      attrs: dataIcon(pause ? 'Z' : 'b'),
      hook: bind('click', ctrl.withdraw, ctrl.redraw)
    }, ctrl.trans.noarg(pause ? 'pause' : 'withdraw'));
  });
}

export function join(ctrl: SwissController): VNode {
  return orJoinSpinner(ctrl, () =>
    h('button.fbt.text.highlight', {
      attrs: dataIcon('G'),
      hook: bind('click', ctrl.join, ctrl.redraw)
    }, ctrl.trans.noarg('join')));
}

export function joinWithdraw(ctrl: SwissController): VNode | undefined {
  if (!ctrl.opts.userId) return h('a.fbt.text.highlight', {
    attrs: {
      href: '/login?referrer=' + window.location.pathname,
      'data-icon': 'G'
    }
  }, ctrl.trans('signIn'));
  if (!ctrl.data.isFinished) return ctrl.data.me ? withdraw(ctrl) : join(ctrl);
}
