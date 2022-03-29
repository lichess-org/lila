import { Redraw } from './util';
import { DasherCtrl, DasherOpts, makeCtrl } from './dasher';
import { loading, loaded } from './view';
import * as xhr from 'common/xhr';
import { init, VNode, classModule, attributesModule } from 'snabbdom';

const patch = init([classModule, attributesModule]);

export default async function LichessDasher(element: Element, opts: DasherOpts) {
  let vnode: VNode,
    ctrl: DasherCtrl | undefined = undefined;

  const redraw: Redraw = () => {
    vnode = patch(vnode || element, ctrl ? loaded(ctrl) : loading());
  };

  redraw();

  const data = await xhr.json('/dasher');
  ctrl = makeCtrl(opts, data, redraw);
  redraw();
  return ctrl;
}
