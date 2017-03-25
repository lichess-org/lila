var m = require('mithril');
var opposite = require('chessground/util').opposite;

module.exports = function(ctrl) {
  var states = ctrl.data.game.clockStates;
  var bottomColor = ctrl.bottomColor();
  if (!states) return;
  return [
    renderClock(ctrl, states, bottomColor === 'black', 'top'),
    renderClock(ctrl, states, bottomColor === 'white', 'bottom')
  ];
}

function renderClock(ctrl, states, isWhite, position) {
  var ply = ctrl.vm.node.ply;
  var i = Math.max(0, ply - ctrl.tree.root.ply - 2);
  i = Math.floor(i / 2) * 2;
  if (!isWhite) i++;
  var tenths = states[i];
  return m('div', {
    class: 'clock ' + position + (ply % 2 === (isWhite ? 0 : 1) ? ' active' : '')
  }, clockContent(tenths));
}

function clockContent(tenths) {
  var date = new Date(tenths * 100);
  var millis = date.getUTCMilliseconds();
  var sep = ':';
  var baseStr = pad2(date.getUTCMinutes()) + sep + pad2(date.getUTCSeconds());
  if (tenths >= 36000) {
    var hours = pad2(Math.floor(tenths / 36000));
    return hours + sep + baseStr;
  }
  var tenthsStr = Math.floor(millis / 100).toString();
  return baseStr + '.' + tenthsStr;
}

function pad2(num) {
  return (num < 10 ? '0' : '') + num;
}
