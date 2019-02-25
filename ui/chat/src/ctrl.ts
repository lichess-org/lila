import { Ctrl, ChatOpts, Line, Tab, ViewModel, Redraw, Permissions, ModerationCtrl } from './interfaces'
import { presetCtrl } from './preset'
import { noteCtrl } from './note'
import { moderationCtrl } from './moderation'

const li = window.lichess;

export default function(opts: ChatOpts, redraw: Redraw): Ctrl {

  const data = opts.data;

  const pubsub = li.pubsub;

  const allTabs: Tab[] = ['discussion'];
  if (opts.noteId) allTabs.push('note');
  if (opts.plugin) allTabs.push(opts.plugin.tab.key);

  const tabStorage = li.storage.make('chat.tab'),
  storedTab = tabStorage.get();

  let moderation: ModerationCtrl | undefined;

  const vm: ViewModel = {
    tab: allTabs.indexOf(storedTab) > -1 ? storedTab : allTabs[0],
    enabled: opts.alwaysEnabled || !li.storage.get('nochat'),
    placeholderKey: 'talkInChat',
    loading: false,
    timeout: opts.timeout,
    writeable: opts.writeable
  };

  const post = function(text: string): boolean {
    text = text.trim();
    if (!text) return false;
    if (text.length > 140) {
      alert('Max length: 140 chars. ' + text.length + ' chars used.');
      return false;
    }
    pubsub.emit('socket.send')('talk', text);
    return false;
  };

  const onTimeout = function(username: string) {
    data.lines.forEach(function(l) {
      if (l.u === username) l.d = true;
    });
    if (username.toLowerCase() === data.userId) vm.timeout = true;
    redraw();
  };

  const onReinstate = function(userId: string) {
    if (userId === data.userId) {
      vm.timeout = false;
      redraw();
    }
  };

  const onMessage = function(line: Line) {
    if (data.lines.length > 64) data.lines.shift();
    data.lines.push(line);
    redraw();
  };

  const trans = li.trans(opts.i18n);

  function canMod() {
    return opts.permissions.timeout || opts.permissions.local;
  }

  function instanciateModeration() {
    moderation = canMod() ? moderationCtrl({
      reasons: opts.timeoutReasons || ([{key: 'other', name: 'Inappropriate behavior'}]),
      permissions: opts.permissions,
      send: li.pubsub.emit('socket.send'),
      redraw
    }) : undefined;
    // if (canMod()) opts.loadCss('chat.mod');
  }
  instanciateModeration();

  const note = opts.noteId ? noteCtrl({
    id: opts.noteId,
    trans,
    redraw
  }) : undefined;

  const preset = presetCtrl({
    initialGroup: opts.preset,
    post,
    redraw
  });

  pubsub.on('socket.in.message', onMessage);
  pubsub.on('socket.in.chat_timeout', onTimeout);
  pubsub.on('socket.in.chat_reinstate', onReinstate);
  pubsub.on('chat.writeable', function(v: boolean) {
    vm.writeable = v;
    redraw();
  });
  pubsub.on('chat.permissions', function(obj: Permissions) {
    let p: keyof Permissions;
    for (p in obj) opts.permissions[p] = obj[p];
    instanciateModeration();
    redraw();
  });

  const emitEnabled = () => pubsub.emit('chat.enabled')(vm.enabled);
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
      if (t === 'discussion') li.requestIdleCallback(() => $('.mchat__say').focus());
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
      if (!v) li.storage.set('nochat', '1');
      else li.storage.remove('nochat');
      redraw();
    },
    redraw
  };
};
