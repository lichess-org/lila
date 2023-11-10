import { attributesModule, classModule, init } from 'snabbdom';
import { BvbCtrl } from './bvbCtrl';
import bvbView from './bvbView';
//import { BvbStockfishWebCtrl as BvbCtrl } from './stockfishWebTest/bvbStockfishWebCtrl';
//import bvbView from './stockfishWebTest/bvbStockfishWebView';
import { BvbOpts } from './bvbInterfaces';

const patch = init([classModule, attributesModule]);

export default async function (opts: BvbOpts) {
  const ctrl = new BvbCtrl(opts, redraw);
  const element = document.querySelector('main') as HTMLElement;
  element.innerHTML = '';
  let vnode = patch(element, bvbView(ctrl));
  function redraw() {
    vnode = patch(vnode, bvbView(ctrl));
  }
  redraw();
}
