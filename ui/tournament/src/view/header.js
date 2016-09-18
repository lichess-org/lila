var m = require('mithril');

function startClock(time) {
  return function(el, isUpdate) {
    if (!isUpdate) $(el).clock({
      time: time
    });
  };
}

var oneDayInSeconds = 60 * 60 * 24;

function isMarathon(d) {
  return d.schedule && d.schedule.freq === 'marathon';
}

function clock(d) {
  if (d.isFinished) return;
  if (d.secondsToStart) {
    if (d.secondsToStart > oneDayInSeconds) return m('div.clock', [
      m('time.moment-from-now.shy', {
        datetime: d.startsAt
      }, d.startsAt)
    ]);
    return m('div.clock.created', {
      config: startClock(d.secondsToStart)
    }, [
      m('span.shy', 'Starting in '),
      m('span.time.text')
    ]);
  }
  if (d.secondsToFinish) return m('div.clock', {
      config: startClock(d.secondsToFinish)
    },
    m('div.time'));
}

function image(d) {
  if (d.isFinished) return;
  if (isMarathon(d)) return;
  var s = d.spotlight;
  if (s && s.iconImg) return m('img.img', {
    src: lichess.assetUrl('/assets/images/' + s.iconImg)
  });
  return m('i.img', {
    'data-icon': (s && s.iconFont) || 'g'
  });
}

function title(ctrl) {
  var d = ctrl.data;
  if (isMarathon(d)) return m('h1', [
    m('span.fire_trophy.marathonWinner', m('span[data-icon=\\]')),
    d.fullName
  ]);
  return m('h1', [
    d.greatPlayer ? [
      m('a', {
        href: d.greatPlayer.url,
        target: '_blank'
      }, d.greatPlayer.name),
      ' tournament'
    ] : d.fullName,
    d.private ? [
      ' ',
      m('span[data-icon=a]')
    ] : null
  ]);
}

module.exports = function(ctrl) {
  return [
    m('div.header', [
      image(ctrl.data),
      title(ctrl),
      clock(ctrl.data)
    ])
  ];
}
