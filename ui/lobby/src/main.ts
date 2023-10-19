import { init, classModule, attributesModule, eventListenersModule } from 'snabbdom';
import { LobbyOpts } from './interfaces';

import makeCtrl from './ctrl';
import appView from './view/main';
import tableView from './view/table';
import { counterView } from './view/counter';

export const patch = init([classModule, attributesModule, eventListenersModule]);

export default function main(opts: LobbyOpts) {
  const ctrl = new makeCtrl(opts, redraw);

  opts.appElement.innerHTML = '';
  let appVNode = patch(opts.appElement, appView(ctrl));
  opts.tableElement.innerHTML = '';
  let tableVNode = patch(opts.tableElement, tableView(ctrl));
  let counterVNode = patch(document.querySelector('.lobby__counters')!, counterView(ctrl));

  function redraw() {
    appVNode = patch(appVNode, appView(ctrl));
    tableVNode = patch(tableVNode, tableView(ctrl));
    counterVNode = patch(counterVNode, counterView(ctrl));
  }

  let cols = 0;

  layout(); // escape row boundary constraints in the grid without using css subgrid
  window.addEventListener('resize', layout);

  function layout() {
    const lobby = document.querySelector('.lobby') as HTMLElement;
    const newCols = Number(window.getComputedStyle(lobby).getPropertyValue('--cols'));

    if (newCols === cols) return;
    cols = newCols;

    const forum = lobby.querySelector('.lobby__forum') as HTMLElement;
    const table = lobby.querySelector('.lobby__table') as HTMLElement;
    const timeline = lobby.querySelector('.lobby__timeline') as HTMLElement;
    const side = lobby.querySelector('.lobby__side') as HTMLElement;

    lobby.append(side, table, timeline, forum); // reset to start

    if (cols === 3) {
      table.append(side, timeline);
    } else if (cols === 4) {
      side.append(timeline);
      table.append(forum);
    }
    forum.classList.toggle('none', cols < 4);
  }

  return ctrl;
}
