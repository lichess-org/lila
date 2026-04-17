import { init, classModule, attributesModule, eventListenersModule, propsModule } from 'snabbdom';

import makeCtrl from './ctrl';
import type { LobbyOpts } from './interfaces';
import { makeCarousel } from './view/carousel';
import appView from './view/main';
import tableView from './view/table';

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
