// var makeModeration = require('./moderation').ctrl;
// var makeNote = require('./note').ctrl;
// var makePreset = require('./preset').ctrl;

import { ChatOpts, Line, Preset } from './interfaces'

export default function makeCtrl(opts: ChatOpts, redraw: () => void) {

  var data = opts.data;

  var ps = window.lichess.pubsub;

  var vm = {
    enabled: !window.lichess.storage.get('nochat'),
    writeable: opts.writeable,
    isTroll: opts.kobold,
    isMod: opts.permissions.timeout,
    isTimeout: opts.timeout,
    parseMoves: opts.parseMoves,
    placeholderKey: 'talkInChat',
    // moderating: null,
    tab: 'discussion',
    loading: false
  };

  var post = function(text: string) {
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
    if (username.toLowerCase() === data.userId) vm.isTimeout = true;
    redraw();
  };

  var onReinstate = function(userId: string) {
    if (userId === data.userId) {
      vm.isTimeout = false;
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

  var emitEnabled = function() {
    ps.emit('chat.enabled')(vm.enabled);
  };
  emitEnabled();

  return {
    data: data,
    vm: vm,
    // moderation: moderation,
    // note: note,
    preset: preset,
    post: post,
    trans: trans,
    public: opts.public,
    setEnabled: function(v: boolean) {
      vm.enabled = v;
      emitEnabled();
      if (!v) window.lichess.storage.set('nochat', 1);
      else window.lichess.storage.remove('nochat');
    }
  };
};

