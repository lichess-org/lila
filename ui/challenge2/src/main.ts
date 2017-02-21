/// <reference path="../../types/index.d.ts" />

import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'

import makeCtrl from './ctrl';
import view from './view';
import { load } from './xhr'
import { ChallengeOpts, Ctrl } from './interfaces'

import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';

const patch = init([klass, attributes]);

export default function LichessChat(element: Element, opts: ChallengeOpts) {

  let vnode: VNode, ctrl: Ctrl

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  ctrl = makeCtrl(opts, redraw);

  vnode = patch(element, view(ctrl));

  if (opts.data) ctrl.update(opts.data);
  else load().then(ctrl.update);

  return {
    update: ctrl.update
  };
};
