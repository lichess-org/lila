import { init, classModule, attributesModule } from 'snabbdom';
import makeCtrl from './ctrl';
import view from './view';
import { NotifyOpts, NotifyData, BumpUnread } from './interfaces';

const patch = init([classModule, attributesModule]);

export function initModule(opts: NotifyOpts) {
  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }
  function update(data: NotifyData | BumpUnread) {
    'pager' in data ? ctrl.update(data) : ctrl.bumpUnread();
  }

  const ctrl = makeCtrl(opts, redraw);
  let vnode = patch(opts.el, view(ctrl));

  if (opts.data) update(opts.data);
  else ctrl.loadPage(1);

  return {
    update,
    onShow: ctrl.onShow,
    setMsgRead: ctrl.setMsgRead,
    setAllRead: ctrl.setAllRead,
    redraw,
  };
}
