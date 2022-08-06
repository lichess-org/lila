import { Ctrl, Notification, NotifyOpts, NotifyData, SingleNotifyData, Redraw } from './interfaces';

import * as xhr from 'common/xhr';
import notify from 'common/notification';
//import { asText } from './view';    TODO, put i18n back into non-streamer incomings
import { defined } from 'common';

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
    opts.setCount(d.unread);
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
    if (incoming) alertUser(data);
    redraw();
  }

  function updateSingle(d: SingleNotifyData) {
    if (opts.isVisible()) {
      loadPage(1);
      return;
    }
    data = undefined;
    alertUser(d);
    redraw();
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

  function alertUser(d: NotifyData | SingleNotifyData) {
    const note = getUnreadNotification(d);
    if (!defined(note)) return;

    opts.pulse();
    if (!lichess.quietMode || note.content.user?.id == 'lichess') lichess.sound.playOnce('newPM');

    if ('alert' in d && d.alert) {
      const pushSubscribed = parseInt(lichess.storage.get('push-subscribed') || '0', 10) + 86400000 >= Date.now(); // 24h
      // firefox and chrome merged push and notify into a single permission many moons ago
      if (!pushSubscribed) notify(d.note.content.text);
    }
  }

  function getUnreadNotification(d: NotifyData | SingleNotifyData): Notification | undefined {
    if ('pager' in d) {
      const ndata = d as NotifyData;
      if (ndata.pager.currentPage !== 1) return undefined;
      return ndata.pager.currentPageResults.find(n => !n.read);
    } else {
      return (d as SingleNotifyData).note;
    }
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
