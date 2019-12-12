import { Ctrl, NotifyOpts, NotifyData, Redraw } from './interfaces';
import notify from 'common/notification';
import { asText } from './view';

const li = window.lichess;

export default function ctrl(opts: NotifyOpts, redraw: Redraw): Ctrl {

  let data: NotifyData | undefined
  let initiating = true;
  let scrolling = false;

  const readAllStorage = li.storage.make('notify-read-all');

  readAllStorage.listen(() => {
    if (data) {
      data.unread = 0;
      opts.setCount(0);
      redraw();
    }
  });

  function update(d: NotifyData, incoming: boolean) {
    data = d;
    if (data.pager.currentPage === 1 && data.unread && opts.isVisible()) {
      opts.setNotified();
      data.unread = 0;
      readAllStorage.set('' + Math.random()); // tell other tabs
    }
    initiating = false;
    scrolling = false;
    opts.setCount(data.unread);
    if (incoming) notifyNew();
    redraw();
  }

  function notifyNew() {
    if (!data || data.pager.currentPage !== 1) return;
    const notif = data.pager.currentPageResults.find(n => !n.read);
    if (!notif) return;
    opts.pulse();
    if (!li.quietMode) li.sound.newPM();
    const text = asText(notif);
    const pushSubsribed = parseInt(li.storage.get('push-subscribed') || '0', 10) + 86400000 >= Date.now(); // 24h
    if (!pushSubsribed && text) notify(text);
  }

  function loadPage(page: number) {
    return $.get('/notify', {page: page || 1}).then(d => update(d, false));
  }

  function nextPage() {
    if (!data || !data.pager.nextPage) return;
    scrolling = true;
    loadPage(data.pager.nextPage);
    redraw();
  }

  function previousPage() {
    if (!data || !data.pager.previousPage) return;
    scrolling = true;
    loadPage(data.pager.previousPage);
    redraw();
  }

  function setVisible() {
    if (!data || data.pager.currentPage === 1) loadPage(1);
  }

  return {
    data: () => data,
    initiating: () => initiating,
    scrolling: () => scrolling,
    update,
    nextPage,
    previousPage,
    loadPage,
    setVisible
  };
}
