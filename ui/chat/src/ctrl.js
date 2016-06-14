var m = require('mithril');
var makeModeration = require('./moderation').ctrl;
var makeNote = require('./note').ctrl;

module.exports = function(opts) {

  var data = {
    id: opts.id,
    name: opts.name,
    lines: opts.lines
  };

  var vm = {
    enabled: m.prop(!lichess.storage.get('nochat')),
    writeable: m.prop(opts.writeable),
    isTroll: opts.kobold,
    isMod: opts.mod,
    isTimeout: m.prop(opts.timeout),
    placeholderKey: 'talkInChat',
    moderating: m.prop(null),
    tab: m.prop('discussion'),
    loading: m.prop(false)
  };

  var trans = lichess.trans(opts.i18n);

  var onTimeout = function(username) {
    data.lines.forEach(function(l) {
      if (l.u === username) l.d = true;
    });
    if (username.toLowerCase() === opts.userId) vm.isTimeout(true);
    m.redraw();
  };

  var onReinstate = function(userId) {
    if (userId === opts.userId) {
      vm.isTimeout(false);
      m.redraw();
    }
  };

  var onMessage = function(line) {
    if (data.lines.length > 64) data.lines.shift();
    data.lines.push(line);
    m.redraw();
  };

  var moderation = vm.isMod ? makeModeration({
    reasons: opts.timeoutReasons,
    send: lichess.pubsub.emit('socket.send')
  }) : null;

  var note = opts.noteId ? makeNote({
    id: opts.noteId,
    trans: trans
  }) : null;

  var setWriteable = function(v) {
    vm.writeable(v);
    m.redraw();
  };

  lichess.pubsub.on('socket.in.message', onMessage);
  lichess.pubsub.on('socket.in.chat_timeout', onTimeout);
  lichess.pubsub.on('socket.in.chat_reinstate', onReinstate);
  lichess.pubsub.on('chat.writeable', setWriteable);

  return {
    data: data,
    vm: vm,
    moderation: moderation,
    note: note,
    post: function(text) {
      text = $.trim(text);
      if (!text) return false;
      if (text.length > 140) {
        alert('Max length: 140 chars. ' + text.length + ' chars used.');
        return false;
      }
      lichess.pubsub.emit('socket.send')('talk', text);
      return false;
    },
    trans: trans,
    setEnabled: function(v) {
      vm.enabled(v);
      if (!v) lichess.storage.set('nochat', 1);
      else lichess.storage.remove('nochat');
    }
  };
};
