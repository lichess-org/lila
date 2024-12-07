import { init, classModule, attributesModule, eventListenersModule } from 'snabbdom';
import { requestIdleCallback } from 'common';
import type { LobbyOpts } from './interfaces';
import makeCtrl from './ctrl';
import appView from './view/main';
import tableView from './view/table';
import { rotateBlogs } from './view/blog';

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
    layoutChanged();
    window.addEventListener('resize', layoutChanged);
  });

  return ctrl;
}

let animationFrameId: number;

const layoutChanged = () => {
  cancelAnimationFrame(animationFrameId);
  animationFrameId = requestAnimationFrame(rotateBlogs);
};
