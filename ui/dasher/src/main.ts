import { VNode, attributesModule, classModule, init } from 'snabbdom';
import { DasherCtrl, DasherOpts, makeCtrl } from './dasher';
import { Redraw } from './util';
import { loaded, loading } from './view';
import { get } from './xhr';

const patch = init([classModule, attributesModule]);

export default function LishogiDasher(element: Element, opts: DasherOpts) {
  let vnode: VNode, ctrl: DasherCtrl;

  const redraw: Redraw = () => {
    vnode = patch(vnode || element, ctrl ? loaded(ctrl) : loading());
  };

  redraw();

  return get('/dasher').then(data => {
    ctrl = makeCtrl(opts, data, redraw);
    redraw();
    return ctrl;
  });
}
