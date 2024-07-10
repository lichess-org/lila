import { init, classModule, attributesModule, eventListenersModule } from 'snabbdom';
import { requestIdleCallback } from 'common';
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

  requestIdleCallback(() => {
    layoutHacks();
    window.addEventListener('resize', layoutHacks);
  });

  return ctrl;
}

let cols = 0;

/* Move the timeline to/from the bottom depending on screen width.
 * This must not cause any FOUC or layout shifting on page load. */

let animationFrameId: number;

const layoutHacks = () => {
  cancelAnimationFrame(animationFrameId); // avoid more than one call per frame
  animationFrameId = requestAnimationFrame(() => {
    $('main.lobby').each(function (this: HTMLElement) {
      const newCols = Number(window.getComputedStyle(this).getPropertyValue('---cols'));
      if (newCols != cols) {
        cols = newCols;
        if (cols > 2) $('.lobby .lobby__timeline').appendTo('.lobby__side');
        else $('.lobby__side .lobby__timeline').appendTo('.lobby');
      }
    });
  });
};
