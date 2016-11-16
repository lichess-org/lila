var vn = require('mithril/render/vnode');
var game = require('game').game;

function prefixInteger(num, length) {
  return (num / Math.pow(10, length)).toFixed(length).substr(2);
}

function textNode(text) {
  return vn('#', undefined, undefined, text);
}

var sepHigh = vn('sep', undefined, undefined, undefined, ':');
var sepLow = vn('sep', undefined, {
  class: 'low'
}, undefined, ':');
var sepDot = vn('sep', undefined, undefined, undefined, '.');

function renderClockTime(ctrl, time, running) {
  var date = new Date(time);
  var minutes = prefixInteger(date.getUTCMinutes(), 2);
  var secs = date.getSeconds();
  var seconds = prefixInteger(secs, 2);
  var tenths = Math.floor(date.getMilliseconds() / 100);
  var sep = (running && tenths < 5) ? sepLow : sepHigh;
  if ((ctrl.data.showTenths == 2 && time < 3600000) || (ctrl.data.showTenths == 1 && time < 10000)) {
    var showHundredths = !running && secs < 1;
    return [textNode(minutes), sep, textNode(seconds), vn('tenths', undefined, undefined, [
      sepDot, textNode(tenths), showHundredths ?
      vn('huns', undefined, undefined, undefined, Math.floor(date.getMilliseconds() / 10) - tenths * 10) : null
    ])];
  } else if (time >= 3600000) {
    var hours = prefixInteger(date.getUTCHours(), 2);
    return [textNode(hours), sepHigh, textNode(minutes), sep, textNode(seconds)];
  } else
    return [textNode(minutes), sep, textNode(seconds)];
}

function showBar(ctrl, time, berserk) {
  return ctrl.data.showBar ? vn(
    'div',
    undefined, {
      class: 'bar' + (berserk ? ' berserk' : '')
    }, [vn('span',
      undefined, {
        style: {
          width: Math.max(0, Math.min(100, (time / ctrl.data.barTime) * 100)) + '%'
        }
      })]) : null;
}

module.exports = {
  renderClockTime: renderClockTime,
  showBar: showBar
};
