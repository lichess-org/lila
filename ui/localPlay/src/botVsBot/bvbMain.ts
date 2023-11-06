import { attributesModule, classModule, init } from 'snabbdom';
//import { BvbCtrl } from './bvbCtrl';
//import bvbView from './bvbView';
import { BvbStockfishWebCtrl } from './stockfishWebTest/bvbStockfishWebCtrl';
import bvbStockfishWebView from './stockfishWebTest/bvbStockfishWebView';
import { BvbOpts } from './bvbInterfaces';

const patch = init([classModule, attributesModule]);

export default async function (opts: BvbOpts) {
  //const ctrl = new BvbCtrl(opts, redraw);
  //const blueprint = bvbView(ctrl);
  function redraw() {
    //vnode = patch(vnode, bvbView(ctrl));
    vnode = patch(vnode, bvbStockfishWebView(ctrl));
  }
  const ctrl = new BvbStockfishWebCtrl(opts, redraw);
  const element = document.querySelector('main') as HTMLElement;
  element.innerHTML = '';
  let vnode = patch(element, bvbStockfishWebView(ctrl));

  redraw();
}
