import { Redraw } from './util';
import { DasherCtrl, DasherOpts, makeCtrl } from './dasher';
import { loading, loaded } from './view';
import * as xhr from 'common/xhr';
import { init } from 'snabbdom';
import { VNode } from 'snabbdom';
import { classModule } from 'snabbdom';
import { attributesModule } from 'snabbdom';

const patch = init([classModule, attributesModule]);

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
