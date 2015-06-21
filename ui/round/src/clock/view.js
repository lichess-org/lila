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
  var seconds = prefixInteger(date.getSeconds(), 2);
  var tenths = Math.floor(date.getMilliseconds() / 100);
  var sep = (running && tenths < 5) ? sepLow : sepHigh;
  if (ctrl.data.showTenths && time < 10000) {
    return bold(minutes) + sep + bold(seconds) + '<tenths><seph>.</seph>' + bold(tenths) + '</tenths>';
  } else if (time >= 3600000) {
    var hours = prefixInteger(date.getUTCHours(), 2);
    return bold(hours) + sepHigh + bold(minutes) + sep + bold(seconds);
  } else {
    return bold(minutes) + sep + bold(seconds);
  }
}

function showBar(ctrl, time) {
  function chooseColor(time, emergTime) {
      if (time > emergTime * 5) return 'green';
      else if (time > emergTime * 2.5) return 'yellow';
      else if (time > emergTime) return 'orange';
      else return 'red';
  }
  return ctrl.data.showBar ? m('div.bar.' + chooseColor(time, ctrl.data.emerg),
    m('span', {
      style: {
        width: Math.max(0, Math.min(100, (time / ctrl.data.barTime) * 100)) + '%'
      }
    })
  ) : null;
}

module.exports = {
  formatClockTime: formatClockTime,
  showBar: showBar
};
