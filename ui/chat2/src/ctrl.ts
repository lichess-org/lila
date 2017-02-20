// var makeModeration = require('./moderation').ctrl;
// var makeNote = require('./note').ctrl;
// var makePreset = require('./preset').ctrl;

import { Ctrl, ChatOpts, Line, Preset, Tab, ViewModel } from './interfaces'

export default function makeCtrl(opts: ChatOpts, redraw: () => void): Ctrl {

  let data = opts.data;

  const ps = window.lichess.pubsub;

  var vm: ViewModel = {
    tab: 'discussion',
    enabled: !window.lichess.storage.get('nochat'),
    placeholderKey: 'talkInChat',
    // moderating: null,
    loading: false,
    timeout: opts.timeout,
    writeable: opts.writeable,
  };

  var post = function(text: string): boolean {
    text = text.trim();
    if (!text) return false;
    if (text.length > 140) {
      alert('Max length: 140 chars. ' + text.length + ' chars used.');
      return false;
    }
    ps.emit('socket.send')('talk', text);
    return false;
  };

  var onTimeout = function(username: string) {
    data.lines.forEach(function(l) {
      if (l.u === username) l.d = true;
    });
    if (username.toLowerCase() === data.userId) vm.timeout = true;
    redraw();
  };

  var onReinstate = function(userId: string) {
    if (userId === data.userId) {
      vm.timeout = false;
      redraw();
    }
  };

  var onMessage = function(line: Line) {
    if (data.lines.length > 64) data.lines.shift();
    data.lines.push(line);
    redraw();
  };

  var trans = window.lichess.trans(opts.i18n);

  // var moderation = vm.isMod ? makeModeration({
  //   reasons: opts.timeoutReasons,
  //   permissions: opts.permissions,
  //   send: window.lichess.pubsub.emit('socket.send')
  // }) : null;

  // var note = data.userId && opts.noteId ? makeNote({
  //   id: opts.noteId,
  //   trans: trans
  // }) : null;

  // var preset = makePreset({
  //   initialGroup: opts.preset,
  //   post: post
  // });
  let preset: Preset = {
    setGroup: (group) => null
  }

  ps.on('socket.in.message', onMessage);
  ps.on('socket.in.chat_timeout', onTimeout);
  ps.on('socket.in.chat_reinstate', onReinstate);
  ps.on('chat.writeable', function(v: boolean) {
    vm.writeable = v;
    redraw();
  });

  const emitEnabled = () => ps.emit('chat.enabled')(vm.enabled);
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
    setEnabled: function(v: boolean) {
      vm.enabled = v;
      emitEnabled();
      if (!v) window.lichess.storage.set('nochat', 1);
      else window.lichess.storage.remove('nochat');
    }
  };
};

