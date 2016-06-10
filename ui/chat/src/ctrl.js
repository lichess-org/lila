var m = require('mithril');
var makeSocket = require('./socket');
var makeModeration = require('./moderation').ctrl;

module.exports = function(opts) {

  var lines = opts.lines;

  var vm = {
    isTroll: opts.kobold,
    isMod: opts.mod,
    placeholderKey: 'talkInChat',
    moderating: m.prop(null),
    loading: m.prop(false)
  };

  var socket = makeSocket(lichess.socket.send);
  var moderation = vm.isMod ? makeModeration({
    reasons: opts.timeoutReasons,
    send: socket.send
  }) : null;

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
      socket.send('talk', text);
      return false;
    },
    newLine: function(line) {
      if (lines.length > 64) lines.shift();
      lines.push(line);
      m.redraw();
    },
    moderation: moderation,
    trans: lichess.trans(opts.i18n),
  };
};
