import { h } from 'snabbdom'

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

export default function(ctrl, trans, color, position, runningColor) {
  const millis = ctrl.millisOf(color);
  const update = (el: HTMLElement) => {
    el.innerHTML = formatClockTime(trans, millis);
  };
  return h('div.correspondence.clock.clock_' + color + '.clock_' + position, {
    class: {
      outoftime: millis <= 0,
      running: runningColor === color
    }
  }, [
    ctrl.data.showBar ? h('div.bar',
      h('span', {
        attrs: { style: { width: ctrl.timePercent(color) + '%'} }
      })
    ) : null,
    h('div.time', {
      hook: {
        insert: vnode => update(vnode.elm as HTMLElement),
        postpatch: (_, vnode) => update(vnode.elm as HTMLElement)
      }
    })
  ]);
}
