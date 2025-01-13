import type { Ctrl, NotifyOpts, NotifyData, Redraw } from './interfaces';
import { json as xhrJson, url as xhrUrl, text as xhrText } from 'common/xhr';
import { storage } from 'common/storage';

export default function makeCtrl(opts: NotifyOpts, redraw: Redraw): Ctrl {
  let data: NotifyData | undefined,
      initiating = true,
      scrolling = false;

  const readAllStorage = storage.make('notify-read-all');
  readAllStorage.listen(() => setAllRead(false));

  const update = (d: NotifyData) => {
    data = d;
    if (opts.updateUnread(data.unread) && !scrolling) attention();
    initiating = false;
    scrolling = false;
    opts.isVisible() && data.pager.currentPage === 1 ? setAllRead() : redraw();
  };

  const bumpUnread = () => {
    data = undefined; // fetch when the dropdown is opened
    opts.updateUnread('increment');
    opts.isVisible() ? loadPage(1) : attention();
  };

  const attention = () => {
    const id = data?.pager.currentPageResults.find(n => !n.read)?.content.user?.id;
    const playBell = storage.boolean('playBellSound').getOrDefault(true);
    if ((!site.quietMode || id === 'lichess') && playBell) site.sound.playOnce('newPM');
    opts.pulse();
  };

  const loadPage = async (page: number) => {
    try {
      const d = await xhrJson(xhrUrl('/notify', { page: page || 1 }));
      update(d);
    } catch {
      site.announce({ msg: 'Failed to load notifications' });
    }
  };

  const nextPage = () => {
    if (!data?.pager.nextPage) return;
    scrolling = true;
    loadPage(data.pager.nextPage);
    redraw();
  };

  const previousPage = () => {
    if (!data?.pager.previousPage) return;
    scrolling = true;
    loadPage(data.pager.previousPage);
    redraw();
  };

  const onShow = () => {
    if (!data || data.pager.currentPage === 1) loadPage(1);
  };

  const setAllRead = (notifyOthers = true) => {
    if (notifyOthers) {
      readAllStorage.fire();
      opts.setNotified();
    }
    if (data) data.unread = 0;
    opts.updateUnread(0);
    redraw();
  };

  const setMsgRead = (user: string) => {
    data?.pager.currentPageResults.forEach(n => {
      if (n.type === 'privateMessage' && n.content.user?.id === user && !n.read) {
        n.read = true;
        data!.unread = Math.max(0, data!.unread - 1);
        opts.updateUnread(data!.unread);
      }
    });
  };

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

  const clear = async () => {
    try {
      await xhrText('/notify/clear', { method: 'post' });
      update(emptyNotifyData);
    } catch {
      site.announce({ msg: 'Failed to clear notifications' });
    }
  };

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
