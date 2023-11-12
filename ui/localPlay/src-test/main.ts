import { attributesModule, classModule, init } from 'snabbdom';
import { ZerofishCtrl as Ctrl } from './zerofish/ctrl';
import view from './zerofish/view';
//import { StockfishWebCtrl as Ctrl } from './stockfishWebTest/bvbStockfishWebCtrl';
//import view from './stockfishWebTest/bvbStockfishWebView';
import { BvbOpts } from './interfaces';

const patch = init([classModule, attributesModule]);

export default async function (opts: BvbOpts) {
  const ctrl = new Ctrl(opts, redraw);
  const element = document.querySelector('main') as HTMLElement;
  element.innerHTML = '';
  let vnode = patch(element, view(ctrl));
  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }
  redraw();
}
