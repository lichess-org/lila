import { Ctrl, NotifyOpts, NotifyData, Redraw } from './interfaces';

import * as xhr from 'common/xhr';

export default function makeCtrl(opts: NotifyOpts, redraw: Redraw): Ctrl {
  let data: NotifyData | undefined,
    initiating = true,
    scrolling = false;

  const readAllStorage = site.storage.make('notify-read-all');

  readAllStorage.listen(_ => setAllRead(false));

  function update(d: NotifyData) {
    data = d;
    if (opts.updateUnread(data.unread) && !scrolling) attention();
    initiating = false;
    scrolling = false;
    if (opts.isVisible() && data.pager.currentPage === 1) setAllRead();
    else redraw();
  }

  function bumpUnread() {
    data = undefined; // fetch when the dropdown is opened
    opts.updateUnread('increment');
    if (opts.isVisible()) loadPage(1);
    else attention();
  }

  function attention() {
    const id = data?.pager.currentPageResults.find(n => !n.read)?.content.user?.id;
    const playBell = site.storage.boolean('playBellSound').getOrDefault(true);
    if ((!site.quietMode || id == 'lichess') && playBell) site.sound.playOnce('newPM');
    opts.pulse();
  }

  const loadPage = (page: number) =>
    xhr.json(xhr.url('/notify', { page: page || 1 })).then(
      d => update(d),
      _ => site.announce({ msg: 'Failed to load notifications' }),
    );

  function nextPage() {
    if (!data?.pager.nextPage) return;
    scrolling = true;
    loadPage(data.pager.nextPage);
    redraw();
  }

  function previousPage() {
    if (!data?.pager.previousPage) return;
    scrolling = true;
    loadPage(data.pager.previousPage);
    redraw();
  }

  function onShow() {
    if (!data || data.pager.currentPage === 1) loadPage(1);
  }

  function setAllRead(notifyOthers = true) {
    if (notifyOthers) {
      readAllStorage.fire();
      opts.setNotified();
    }
    if (data) data.unread = 0;
    opts.updateUnread(0);
    redraw();
  }

  function setMsgRead(user: string) {
    data?.pager.currentPageResults.forEach(n => {
      if (n.type == 'privateMessage' && n.content.user?.id == user && !n.read) {
        n.read = true;
        data!.unread = Math.max(0, data!.unread - 1);
        opts.updateUnread(data!.unread);
      }
    });
  }

  const emptyNotifyData = {
    pager: {
      currentPage: 1,
      maxPerPage: 1,
      currentPageResults: [],
      nbResults: 0,
      nbPages: 1,
    },
    unread: 0,
    i18n: {},
  };

  function clear() {
    xhr.text('/notify/clear', { method: 'post' }).then(
      _ => update(emptyNotifyData),
      _ => site.announce({ msg: 'Failed to clear notifications' }),
    );
  }

  return {
    data: () => data,
    initiating: () => initiating,
    scrolling: () => scrolling,
    update,
    bumpUnread,
    nextPage,
    previousPage,
    loadPage,
    onShow,
    setMsgRead,
    setAllRead,
    clear,
  };
}
