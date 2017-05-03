/// <reference types="types/lichess" />

import { Redraw } from './util'

import { DasherCtrl, DasherOpts, makeCtrl } from './dasher';
import view from './view';

import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'
import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';
const patch = init([klass, attributes]);

export default function LichessDasher(element: Element, opts: DasherOpts) {

  let vnode: VNode, ctrl: DasherCtrl

  const redraw: Redraw = () => {
    vnode = patch(vnode, view(ctrl));
  }

  ctrl = makeCtrl(opts, redraw);

  vnode = patch(element, view(ctrl));
};
