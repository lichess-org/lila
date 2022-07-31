import { Ctrl, NotifyOpts, NotifyData, SingleNotifyData, Redraw } from './interfaces';

import * as xhr from 'common/xhr';
import notify from 'common/notification';
import { asText } from './view';

export default function makeCtrl(opts: NotifyOpts, redraw: Redraw): Ctrl {
  let data: NotifyData | undefined,
    initiating = true,
    scrolling = false;

  const readAllStorage = lichess.storage.make('notify-read-all');

  readAllStorage.listen(_ => {
    if (data) {
      data.unread = 0;
      opts.setCount(0);
      redraw();
    }
  });

  function update(d: NotifyData | SingleNotifyData, incoming: boolean) {
    if (!('pager' in d)) {
      updateSingle(d as SingleNotifyData);
      return;
    }
    data = d;
    if (data.pager.currentPage === 1 && data.unread && opts.isVisible()) {
      opts.setNotified();
      data.unread = 0;
      readAllStorage.fire();
    }
    initiating = false;
    scrolling = false;
    opts.setCount(data.unread);
    if (incoming) {
      if (data.pager.currentPage !== 1) return;
      const notif = data.pager.currentPageResults.find(n => !n.read);
      if (!notif) return;
      pulsePlayAndPush(asText(notif, lichess.trans(data.i18n)), notif.content.user?.id == 'lichess');
    }
    redraw();
  }

  function updateSingle(d: SingleNotifyData) {
    if (opts.isVisible()) {
      loadPage(1);
      if (!lichess.quietMode) lichess.sound.playOnce('newPM');
      return;
    }
    opts.setCount(d.unread);
    data = undefined;
    pulsePlayAndPush(d.note.content.text, d.note.content.user?.id == 'lichess');
    redraw();
  }

  function pulsePlayAndPush(text: string | undefined, isLichess: boolean) {
    opts.pulse();
    if (!lichess.quietMode || isLichess) lichess.sound.playOnce('newPM');
    const pushSubscribed = parseInt(lichess.storage.get('push-subscribed') || '0', 10) + 86400000 >= Date.now(); // 24h
    if (!pushSubscribed && text) notify(text);
  }

  const loadPage = (page: number) =>
    xhr.json(xhr.url('/notify', { page: page || 1 })).then(
      d => update(d, false),
      _ => lichess.announce({ msg: 'Failed to load notifications' })
    );

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

  function setMsgRead(user: string) {
    if (data)
      data.pager.currentPageResults.forEach(n => {
        if (n.type == 'privateMessage' && n.content.user?.id == user && !n.read) {
          n.read = true;
          data!.unread = Math.max(0, data!.unread - 1);
          opts.setCount(data!.unread);
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
    xhr
      .text('/notify/clear', {
        method: 'post',
      })
      .then(
        _ => update(emptyNotifyData, false),
        _ => lichess.announce({ msg: 'Failed to clear notifications' })
      );
  }

  return {
    data: () => data,
    initiating: () => initiating,
    scrolling: () => scrolling,
    update,
    nextPage,
    previousPage,
    loadPage,
    setVisible,
    setMsgRead,
    clear,
  };
}
