import { Redraw } from './util';
import { DasherCtrl, DasherOpts, makeCtrl } from './dasher';
import { loading, loaded } from './view';
import * as xhr from 'common/xhr';
import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';

const patch = init([klass, attributes]);

export default function LichessDasher(element: Element, opts: DasherOpts) {
  let vnode: VNode, ctrl: DasherCtrl;

  const redraw: Redraw = () => {
    vnode = patch(vnode || element, ctrl ? loaded(ctrl) : loading());
  };

  redraw();

  return xhr.json('/dasher').then(data => {
    ctrl = makeCtrl(opts, data, redraw);
    redraw();
    return ctrl;
  });
}
