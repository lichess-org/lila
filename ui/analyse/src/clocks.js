var m = require('mithril');
var opposite = require('chessground/util').opposite;

module.exports = function(ctrl) {
  var states = ctrl.data.game.clockStates;
  if (!states || !ctrl.vm.onMainline) return;
  var bottomColor = ctrl.bottomColor();
  var firstIsWhite = ctrl.tree.root.ply % 2 === 0;
  var top = renderClock(ctrl, states, bottomColor === 'black', firstIsWhite, 'top');
  var bot = renderClock(ctrl, states, bottomColor === 'white', firstIsWhite, 'bottom');
  if (top && bot) return m('div.aclocks', [top, bot]);
}

function renderClock(ctrl, states, isWhite, firstIsWhite, position) {
  var ply = ctrl.vm.node.ply, i;
  var i = ply - ctrl.tree.root.ply;
  if (isWhite === firstIsWhite) i = Math.floor((i - 1) / 2) * 2;
  else i = Math.floor(i / 2) * 2 - 1;
  if (i < 0) i = isWhite === firstIsWhite ? 0 : 1;
  var tenths = states[i];
  if (typeof tenths !== 'undefined') return m('div', {
    class: 'aclock ' + position + (ply % 2 === (isWhite ? 0 : 1) ? ' active' : '')
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
  return [
    baseStr,
    m('tenths', '.' + tenthsStr)
  ];
}

function pad2(num) {
  return (num < 10 ? '0' : '') + num;
}
