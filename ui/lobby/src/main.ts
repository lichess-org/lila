import { init, classModule, attributesModule, eventListenersModule, propsModule } from 'snabbdom';
import type { LobbyOpts } from './interfaces';
import makeCtrl from './ctrl';
import appView from './view/main';
import tableView from './view/table';
import { makeCarousel } from './view/carousel';

export const patch = init([classModule, attributesModule, propsModule, eventListenersModule]);

export default function main(opts: LobbyOpts) {
  const ctrl = new makeCtrl(opts, redraw);

  opts.appElement.innerHTML = '';
  let appVNode = patch(opts.appElement, appView(ctrl));
  opts.tableElement.innerHTML = '';
  let tableVNode = patch(opts.tableElement, tableView(ctrl));

  function redraw() {
    appVNode = patch(appVNode, appView(ctrl));
    tableVNode = patch(tableVNode, tableView(ctrl));
  }

  makeCarousel({ selector: '.lobby__blog', itemWidth: 192, pauseFor: 10 });
  return ctrl;
}
