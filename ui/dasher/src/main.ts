/// <reference types="types/lichess" />

import { Ctrl, DasherOpts, Redraw } from './interfaces'

import makeCtrl from './ctrl';
import view from './view';

import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'
import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';
const patch = init([klass, attributes]);

export default function LichessDasher(element: Element, opts: DasherOpts) {

  let vnode: VNode, ctrl: Ctrl

  const redraw: Redraw = () => {
    vnode = patch(vnode, view(ctrl));
  }

  ctrl = makeCtrl(opts, redraw);

  vnode = patch(element, view(ctrl));
};
