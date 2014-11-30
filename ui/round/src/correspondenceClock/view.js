var classSet = require('chessground').util.classSet;

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
  var str = '';
  if (time >= 86400 * 1000) {
    var days = date.getUTCDate() - 1;
    str += (days === 1 ? trans('oneDay') : trans('nbDays', days)) + ' ';
    if (time === 86400 || days > 1) return str;
    str += ' &amp; ';
  }
  str += bold(prefixInteger(date.getUTCHours(), 2)) + ':' + bold(minutes);
  if (time < 3600 * 1000) str += ':' + bold(seconds);
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
