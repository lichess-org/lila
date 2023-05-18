import { Redraw } from './util';
import { DasherCtrl, makeCtrl } from './dasher';
import { loading, loaded } from './view';
import * as xhr from 'common/xhr';
import { init, VNode, classModule, attributesModule } from 'snabbdom';

const patch = init([classModule, attributesModule]);

export default async function LichessDasher(element: Element, toggle: Element) {
  let vnode: VNode,
    ctrl: DasherCtrl | undefined = undefined;

  const redraw: Redraw = () => {
    vnode = patch(vnode || element, ctrl ? loaded(ctrl) : loading());
  };

  redraw();

  const data = await xhr.json('/dasher');
  ctrl = makeCtrl(data, redraw);
  redraw();

  new MutationObserver(_ => lichess.pubsub.emit('dasher.toggle', toggle.classList.contains('shown'))).observe(toggle, {
    attributes: true,
  });

  return ctrl;
}

(window as any).LichessDasher = LichessDasher; // esbuild
