var m = require('mithril');
var game = require('game').game;

function prefixInteger(num, length) {
  return (num / Math.pow(10, length)).toFixed(length).substr(2);
}

function bold(x) {
  return '<b>' + x + '</b>';
}

var sepHigh = '<seph>:</seph>';
var sepLow = '<sepl>:</sepl>';

function formatClockTime(ctrl, time, running) {
  var date = new Date(time);
  var minutes = prefixInteger(date.getUTCMinutes(), 2);
  var secs = date.getSeconds();
  var seconds = prefixInteger(secs, 2);
  var tenths = Math.floor(date.getMilliseconds() / 100);
  var hundredths = function() {
    return Math.floor(date.getMilliseconds() / 10) - tenths * 10;
  };
  var sep = (running && tenths < 5) ? sepLow : sepHigh;
  if ((ctrl.data.showTenths == 2 && time < 3600000) || (ctrl.data.showTenths == 1 && time < 10000)) {
    var showHundredths = !running && secs < 1;
    return bold(minutes) + sep + bold(seconds) +
      '<tenths><seph>.</seph>' + bold(tenths) + (showHundredths ? '<huns>' + hundredths() + '</huns>' : '') + '</tenths>';
  } else if (time >= 3600000) {
    var hours = prefixInteger(date.getUTCHours(), 2);
    return bold(hours) + sepHigh + bold(minutes) + sep + bold(seconds);
  } else {
    return bold(minutes) + sep + bold(seconds);
  }
}

function showBar(ctrl, time, berserk) {
  return ctrl.data.showBar ? m('div', {
    class: 'bar' + (berserk ? ' berserk' : '')
  }, m('span', {
    style: {
      width: Math.max(0, Math.min(100, (time / ctrl.data.barTime) * 100)) + '%'
    }
  })) : null;
}

module.exports = {
  formatClockTime: formatClockTime,
  showBar: showBar
};
