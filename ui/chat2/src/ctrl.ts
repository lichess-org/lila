import { Ctrl, ChatOpts, Line, Tab, ViewModel } from './interfaces'
import { presetCtrl } from './preset'

export default function(opts: ChatOpts, redraw: () => void): Ctrl {

  const data = opts.data;

  const pubsub = window.lichess.pubsub;

  const vm: ViewModel = {
    tab: 'discussion',
    enabled: !window.lichess.storage.get('nochat'),
    placeholderKey: 'talkInChat',
    // moderating: null,
    loading: false,
    timeout: opts.timeout,
    writeable: opts.writeable,
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

  const trans = window.lichess.trans(opts.i18n);

  // const moderation = vm.isMod ? makeModeration({
  //   reasons: opts.timeoutReasons,
  //   permissions: opts.permissions,
  //   send: window.lichess.pubsub.emit('socket.send')
  // }) : null;

  // const note = data.userId && opts.noteId ? makeNote({
  //   id: opts.noteId,
  //   trans: trans
  // }) : null;

  const preset = presetCtrl({
    initialGroup: opts.preset,
    post: post,
    redraw: redraw
  });

  pubsub.on('socket.in.message', onMessage);
  pubsub.on('socket.in.chat_timeout', onTimeout);
  pubsub.on('socket.in.chat_reinstate', onReinstate);
  pubsub.on('chat.writeable', function(v: boolean) {
    vm.writeable = v;
    redraw();
  });

  const emitEnabled = () => pubsub.emit('chat.enabled')(vm.enabled);
  emitEnabled();

  return {
    data: data,
    opts: opts,
    vm: vm,
    setTab(t: Tab) {
      vm.tab = t
      redraw()
    },
    // moderation: moderation,
    // note: note,
    preset: preset,
    post: post,
    trans: trans,
    setEnabled(v: boolean) {
      vm.enabled = v;
      emitEnabled();
      if (!v) window.lichess.storage.set('nochat', 1);
      else window.lichess.storage.remove('nochat');
    }
  };
};
