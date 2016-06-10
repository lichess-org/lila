var m = require('mithril');
var makeModeration = require('./moderation').ctrl;

module.exports = function(opts) {

  var lines = opts.lines;
  var socketSend = lichess.socket.send;

  var vm = {
    enabled: m.prop(!lichess.storage.get('nochat')),
    isTroll: opts.kobold,
    isMod: opts.mod,
    isTimeout: m.prop(opts.timeout),
    placeholderKey: 'talkInChat',
    moderating: m.prop(null),
    loading: m.prop(false),
  };

  var onTimeout = function(username) {
    lines.forEach(function(l) {
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
    if (lines.length > 64) lines.shift();
    lines.push(line);
    m.redraw();
  };

  var moderation = vm.isMod ? makeModeration({
    reasons: opts.timeoutReasons,
    send: socketSend
  }) : null;

  lichess.pubsub.on('socket.in.message', onMessage);
  lichess.pubsub.on('socket.in.chat_timeout', onTimeout);
  lichess.pubsub.on('socket.in.chat_reinstate', onReinstate);

  return {
    lines: lines,
    vm: vm,
    post: function(text) {
      text = $.trim(text);
      if (!text) return false;
      if (text.length > 140) {
        alert('Max length: 140 chars. ' + text.length + ' chars used.');
        return false;
      }
      socketSend('talk', text);
      return false;
    },
    moderation: moderation,
    trans: lichess.trans(opts.i18n),
    setEnabled: function(v) {
      vm.enabled(v);
      if (!v) lichess.storage.set('nochat', 1);
      else lichess.storage.remove('nochat');
    }
  };
};
