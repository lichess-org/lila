var m = require('mithril');
var makeSocket = require('./socket');

module.exports = function(opts) {

  var lines = opts.lines;

  var vm = {
    isTroll: opts.kobold,
    isMod: opts.mod,
    placeholderKey: 'talkInChat'
  };

  var socket = makeSocket(opts.socketSend);

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
      lichess.socket.send('talk', text);
      return false;
    },
    newLine: function(line) {
      if (lines.length > 64) lines.shift();
      lines.push(line);
      m.redraw();
    },
    trans: lichess.trans(opts.i18n),
  };
};
