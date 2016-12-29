var classSet = require('common').classSet;
var m = require('mithril');

function prefixInteger(num, length) {
  return (num / Math.pow(10, length)).toFixed(length).substr(2);
}

function bold(x) {
  return '<b>' + x + '</b>';
}

function formatClockTime(trans, time) {
  var date = new Date(time);
  var minutes = prefixInteger(date.getUTCMinutes(), 2);
  var seconds = prefixInteger(date.getSeconds(), 2);
  var hours, str = '';
  if (time >= 86400 * 1000) {
    // days : hours
    var days = date.getUTCDate() - 1;
    hours = date.getUTCHours();
    str += (days === 1 ? trans('oneDay') : trans('nbDays', days)) + ' ';
    if (hours !== 0) str += trans('nbHours', hours);
  } else if (time >= 3600 * 1000) {
    // hours : minutes
    hours = date.getUTCHours();
    str += bold(prefixInteger(hours, 2)) + ':' + bold(minutes);
  } else {
    // minutes : seconds
    str += bold(minutes) + ':' + bold(seconds);
  }
  return str;
}

module.exports = function(ctrl, trans, color, position, runningColor) {
  var time = ctrl.data[color];
  return m('div', {
    class: 'correspondence clock clock_' + color + ' clock_' + position + ' ' + classSet({
      'outoftime': !time,
      'running': runningColor === color,
      'emerg': time < ctrl.data.emerg
    })
  }, [
    ctrl.data.showBar ? m('div.bar',
      m('span', {
        style: {
          width: Math.max(0, Math.min(100, (time / ctrl.data.barTime) * 100)) + '%'
        }
      })
    ) : null,
    m('div.time', m.trust(formatClockTime(trans, time * 1000)))
  ]);
}
