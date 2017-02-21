/// <reference path="../dts/index.d.ts" />

import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'

import makeCtrl from './ctrl';
import view from './view';
import { ChatOpts, Ctrl } from './interfaces'

import klass from 'snabbdom/modules/class';
import props from 'snabbdom/modules/props';
import attributes from 'snabbdom/modules/attributes';
import listeners from 'snabbdom/modules/eventlisteners';

const patch = init([klass, props, attributes, listeners]);

export default function LichessChat(element: Element, opts: ChatOpts) {

  let vnode: VNode, ctrl: Ctrl

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  ctrl = makeCtrl(opts, redraw);

  vnode = patch(element, view(ctrl));

  window.Mousetrap.bind('/', () => {
    (element.querySelector('input.lichess_say') as HTMLElement).focus();
    return false;
  });

  return {
    preset: ctrl.preset
  };
};
