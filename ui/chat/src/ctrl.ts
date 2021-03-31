import { Ctrl, ChatOpts, Line, Tab, ViewModel, Redraw, Permissions, ModerationCtrl } from './interfaces';
import { presetCtrl } from './preset';
import { noteCtrl } from './note';
import { moderationCtrl } from './moderation';
import { prop } from 'common';

export default function (opts: ChatOpts, redraw: Redraw): Ctrl {
  const data = opts.data;
  data.domVersion = 1; // increment to force redraw
  const maxLines = 200;
  const maxLinesDrop = 50; // how many lines to drop at once

  const palantir = {
    instance: undefined,
    loaded: false,
    enabled: prop(!!data.palantir),
  };

  const allTabs: Tab[] = ['discussion'];
  if (opts.noteId) allTabs.push('note');
  if (opts.plugin) allTabs.push(opts.plugin.tab.key);

  const tabStorage = lichess.storage.make('chat.tab'),
    storedTab = tabStorage.get();

  let moderation: ModerationCtrl | undefined;

  const vm: ViewModel = {
    tab: allTabs.find(tab => tab === storedTab) || allTabs[0],
    enabled: opts.alwaysEnabled || !lichess.storage.get('nochat'),
    placeholderKey: 'talkInChat',
    loading: false,
    timeout: opts.timeout,
    writeable: opts.writeable,
  };

  /* If discussion is disabled, and we have another chat tab,
   * then select that tab over discussion */
  if (allTabs.length > 1 && vm.tab === 'discussion' && lichess.storage.get('nochat')) vm.tab = allTabs[1];

  const post = (text: string): boolean => {
    text = text.trim();
    if (!text) return false;
    if (text == 'You too!' && !data.lines.some(l => l.u != data.userId)) return false;
    if (text.length > 140) {
      alert('Max length: 140 chars. ' + text.length + ' chars used.');
      return false;
    }
    lichess.pubsub.emit('socket.send', 'talk', text);
    return true;
  };

  const onTimeout = (userId: string) => {
    let change = false;
    data.lines.forEach(l => {
      if (l.u && l.u.toLowerCase() == userId) {
        l.d = true;
        change = true;
      }
    });
    if (userId == data.userId) vm.timeout = change = true;
    if (change) {
      data.domVersion++;
      redraw();
    }
  };

  const onReinstate = (userId: string) => {
    if (userId == data.userId) {
      vm.timeout = false;
      redraw();
    }
  };

  const onMessage = (line: Line) => {
    data.lines.push(line);
    const nb = data.lines.length;
    if (nb > maxLines) {
      data.lines.splice(0, nb - maxLines + maxLinesDrop);
      data.domVersion++;
    }
    redraw();
  };

  const onWriteable = (v: boolean) => {
    vm.writeable = v;
    redraw();
  };

  const onPermissions = (obj: Permissions) => {
    let p: keyof Permissions;
    for (p in obj) opts.permissions[p] = obj[p];
    instanciateModeration();
    redraw();
  };

  const trans = lichess.trans(opts.i18n);

  function instanciateModeration() {
    if (opts.permissions.timeout || opts.permissions.local) {
      moderation = moderationCtrl({
        reasons: opts.timeoutReasons || [{ key: 'other', name: 'Inappropriate behavior' }],
        permissions: opts.permissions,
        redraw,
      });
      opts.loadCss('chat.mod');
    }
  }
  instanciateModeration();

  const note = opts.noteId
    ? noteCtrl({
        id: opts.noteId,
        text: opts.noteText,
        trans,
        redraw,
      })
    : undefined;

  const preset = presetCtrl({
    initialGroup: opts.preset,
    post,
    redraw,
  });

  const subs: [string, PubsubCallback][] = [
    ['socket.in.message', onMessage],
    ['socket.in.chat_timeout', onTimeout],
    ['socket.in.chat_reinstate', onReinstate],
    ['chat.writeable', onWriteable],
    ['chat.permissions', onPermissions],
    ['palantir.toggle', palantir.enabled],
  ];
  subs.forEach(([eventName, callback]) => lichess.pubsub.on(eventName, callback));

  const destroy = () => {
    subs.forEach(([eventName, callback]) => lichess.pubsub.off(eventName, callback));
  };

  const emitEnabled = () => lichess.pubsub.emit('chat.enabled', vm.enabled);
  emitEnabled();

  return {
    data,
    opts,
    vm,
    allTabs,
    setTab(t: Tab) {
      vm.tab = t;
      tabStorage.set(t);
      // It's a lame way to do it. Give me a break.
      if (t === 'discussion')
        lichess.requestIdleCallback(
          () =>
            $('.mchat__say').each(function (this: HTMLElement) {
              this.focus();
            }),
          500
        );
      redraw();
    },
    moderation: () => moderation,
    note,
    preset,
    post,
    trans,
    plugin: opts.plugin,
    setEnabled(v: boolean) {
      vm.enabled = v;
      emitEnabled();
      if (!v) lichess.storage.set('nochat', '1');
      else lichess.storage.remove('nochat');
      redraw();
    },
    redraw,
    palantir,
    destroy,
  };
}
