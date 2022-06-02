import { init, classModule, attributesModule, eventListenersModule } from 'snabbdom';
import { Chessground } from 'chessground';
import { LobbyOpts } from './interfaces';

import makeCtrl from './ctrl';
import appView from './view/main';
import tableView from './view/table';

export const patch = init([classModule, attributesModule, eventListenersModule]);

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

  return ctrl;
}

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = Chessground;
