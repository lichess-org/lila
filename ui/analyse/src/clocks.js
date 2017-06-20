var m = require('mithril');
var opposite = require('chessground/util').opposite;

module.exports = function(ctrl) {
  var node = ctrl.vm.node, clock = node.clock;
  if (!clock && clock !== 0) return;
  var parentClock = ctrl.tree.getParentClock(node, ctrl.vm.path);
  var whiteCentis, blackCentis;
  var isWhiteTurn = node.ply % 2 === 0;
  if (isWhiteTurn) {
    whiteCentis = parentClock;
    blackCentis = clock;
  }
  else {
    whiteCentis = clock;
    blackCentis = parentClock;
  }
  var whitePov = ctrl.bottomColor() === 'white';
  var whiteEl = renderClock(whiteCentis, isWhiteTurn);
  var blackEl = renderClock(blackCentis, !isWhiteTurn);

  return m('div.aclocks', whitePov ? [blackEl, whiteEl] : [whiteEl, blackEl]);
}

function renderClock(centis, active) {
  return m('div', {
    class: 'aclock ' + (active ? ' active' : '')
  }, clockContent(centis));
}

function clockContent(centis) {
  if (!centis && centis !== 0) return '-';
  var date = new Date(centis * 10);
  var millis = date.getUTCMilliseconds();
  var sep = ':';
  var baseStr = pad2(date.getUTCMinutes()) + sep + pad2(date.getUTCSeconds());
  if (centis >= 360000) {
    var hours = Math.floor(centis / 360000);
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
