import { init } from 'snabbdom';
import { VNode } from 'snabbdom';

import makeCtrl from './ctrl';
import view from './view';
import { NotifyOpts, Ctrl } from './interfaces';

import { classModule } from 'snabbdom';
import { attributesModule } from 'snabbdom';

const patch = init([classModule, attributesModule]);

export default function LichessNotify(element: Element, opts: NotifyOpts) {
  let vnode: VNode, ctrl: Ctrl;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  ctrl = makeCtrl(opts, redraw);

  vnode = patch(element, view(ctrl));

  if (opts.data) ctrl.update(opts.data, opts.incoming);
  else ctrl.loadPage(1);

  return {
    update: ctrl.update,
    setVisible: ctrl.setVisible,
    setMsgRead: ctrl.setMsgRead,
    redraw,
  };
}
