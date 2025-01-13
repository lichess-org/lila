import { type VNode, attributesModule, classModule, init } from 'snabbdom';
import { type DasherCtrl, type DasherOpts, makeCtrl } from '../dasher';
import { loaded, loading } from '../view';

const patch = init([classModule, attributesModule]);

function main(opts: DasherOpts): Promise<DasherCtrl> {
  const element = document.getElementById('dasher_app')!;

  let vnode: VNode;
  let ctrl: DasherCtrl;

  const redraw: Redraw = () => {
    vnode = patch(vnode || element, ctrl ? loaded(ctrl) : loading());
  };

  redraw();

  return window.lishogi.xhr.json('GET', '/dasher').then(data => {
    ctrl = makeCtrl(opts, data, redraw);
    redraw();
    return ctrl;
  });
}

window.lishogi.registerModule(__bundlename__, main);
