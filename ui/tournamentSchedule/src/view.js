var m = require('mithril');

var scale = 8;
var now;
var startTime;
var stopTime;

function displayClockLimit(limit) {
  switch (limit) {
    case 30:
      return '½';
    case 45:
      return '¾';
    case 90:
      return '1.5';
    default:
      return limit / 60;
  }
}

function displayClock(clock) {
  return displayClockLimit(clock.limit) + "+" + clock.increment;
}

function leftPos(time) {
  return scale * (time - startTime) / 1000 / 60;
}

function laneGrouper(t) {
  if (t.variant.key !== 'standard') {
    return 99;
  } else if (t.conditions) {
    return 50;
  } else if (t.schedule && t.schedule.freq === 'unique') {
    return t.perf.position - 0.7;
  } else if (t.schedule && t.schedule.speed === 'superblitz') {
    return t.perf.position - 0.5;
  } else {
    return t.perf.position;
  }
}

function group(arr, grouper) {
  var groups = {};
  arr.forEach(function(e) {
    var g = grouper(e);
    if (!groups[g]) groups[g] = [];
    groups[g].push(e);
  });
  return Object.keys(groups).sort().map(function(k) {
    return groups[k];
  });
}

function fitLane(lane, tour2) {
  return !lane.some(function(tour1) {
    return !(tour1.finishesAt <= tour2.startsAt || tour2.finishesAt <= tour1.startsAt);
  });
}

// splits lanes that have collisions, but keeps
// groups separate by not compacting existing lanes
function splitOverlaping(lanes) {
  var ret = [];
  lanes.forEach(function(lane) {
    var newLanes = [
      []
    ];
    lane.forEach(function(tour) {
      var collision = true;
      for (var i = 0; i < newLanes.length; i++) {
        if (fitLane(newLanes[i], tour)) {
          newLanes[i].push(tour);
          collision = false;
          break;
        }
      }
      if (collision) newLanes.push([tour]);
    });
    ret = ret.concat(newLanes);
  });
  return ret;
}

// Tries to compress lanes by moving tours
// upwards until it hits another tournament.
// Should not bubble past existing ones.
function bubbleUp(lanes, tours) {
  var returnLanes = lanes.concat([]);
  tours.forEach(function(tour) {
    var i = returnLanes.length - 1;
    while (i >= 0) {
      if (!fitLane(returnLanes[i], tour))
        break;
      i--;
    }
    if (i + 1 >= returnLanes.length) {
      returnLanes.push([tour]);
    } else {
      returnLanes[i + 1].push(tour);
    }
  });
  return returnLanes;
}

function tournamentClass(tour) {
  var classes = [
    tour.rated ? 'rated' : 'casual',
    tour.status === 30 ? 'finished' : 'joinable',
    tour.createdBy === 'lichess' ? 'system' : 'user-created'
  ];
  if (tour.schedule) classes.push(tour.schedule.freq);
  if (tour.position) classes.push('thematic');
  if (tour.minutes <= 30) classes.push('short');
  if (tour.conditions && tour.conditions.maxRating) classes.push('max-rating');

  return classes.join(' ');
}

function renderTournament(ctrl, tour) {
  var width = tour.minutes * scale;
  var left = leftPos(tour.startsAt);
  // moves content into viewport, for long tourneys and marathons
  var paddingLeft = tour.minutes < 90 ? 0 : Math.max(0,
    Math.min(width - 250, // max padding, reserved text space
      leftPos(now) - left - 380)); // distance from Now
  // cut right overflow to fit viewport and not widen it, for marathons
  width = Math.min(width, leftPos(stopTime) - left);

  var hasMaxRating = tour.conditions && tour.conditions.maxRating;

  return m('a.tournament', {
    key: tour.id,
    href: '/tournament/' + tour.id,
    style: {
      width: width + 'px',
      left: left + 'px',
      paddingLeft: paddingLeft + 'px'
    },
    class: tournamentClass(tour)
  }, [
    m('span.icon', tour.perf ? {
      'data-icon': tour.perf.icon,
      title: tour.perf.name
    } : null),
    m('span.body', [
      m('span.name', tour.fullName),
      m('span.infos', [
        m('span.text', [
          displayClock(tour.clock) + ' ',
          tour.variant.key === 'standard' ? null : tour.variant.name + ' ',
          tour.position ? 'Thematic ' : null,
          tour.rated ? ctrl.trans('rated') : ctrl.trans('casual')
        ]),
        tour.nbPlayers ? m('span.nb-players', {
          'data-icon': 'r'
        }, tour.nbPlayers) : null
      ])
    ])
  ]);
}

function renderTimeline() {
  var minutesBetween = 10;
  var time = new Date(startTime);
  time.setSeconds(0);
  time.setMinutes(Math.floor(time.getMinutes() / minutesBetween) * minutesBetween);

  var timeHeaders = [];
  var count = (stopTime - startTime) / (minutesBetween * 60 * 1000);
  for (var i = 0; i < count; i++) {
    var str = timeString(time);
    timeHeaders.push(m('div', {
      key: str,
      class: 'timeheader' + (time.getMinutes() === 0 ? ' hour' : ''),
      style: {
        left: leftPos(time.getTime()) + 'px'
      }
    }, str));
    time.setUTCMinutes(time.getUTCMinutes() + minutesBetween);
  }

  return m('div.timeline',
    timeHeaders,
    m('div.timeheader.now', {
      style: {
        left: leftPos(now) + 'px'
      }
    })
  );
}

// converts Date to "%H:%M" with leading zeros
function timeString(time) {
  return ('0' + time.getHours()).slice(-2) + ":" + ('0' + time.getMinutes()).slice(-2);
}

function not(f) {
  return function(x) {
    return !f(x);
  };
}

function isSystemTournament(t) {
  return !!t.schedule;
}

module.exports = function(ctrl) {
  now = (new Date()).getTime();
  startTime = now - 3 * 60 * 60 * 1000;
  stopTime = startTime + 10 * 60 * 60 * 1000;

  if (!ctrl.data.systemTours) {
    var tours = ctrl.data.finished.concat(ctrl.data.started).concat(ctrl.data.created);
    ctrl.data.systemTours = tours.filter(isSystemTournament);
    ctrl.data.userTours = tours.filter(not(isSystemTournament));
  }

  // group system tournaments into dedicated lanes for PerfType
  var tourLanes = splitOverlaping(
    bubbleUp(group(ctrl.data.systemTours, laneGrouper), ctrl.data.userTours));

  return m('div.schedule.dragscroll', {
    config: function(el, isUpdate) {
      if (isUpdate) return;
      var bitLater = now + (15 * 60 * 1000)
      el.scrollLeft = leftPos(bitLater - el.clientWidth / 2 / scale * 60 * 1000);
    }
  }, [
    renderTimeline(),
    tourLanes.filter(function(lane) {
      return lane.length > 0;
    }).map(function(lane) {
      return m('div.tournamentline',
        lane.map(function(tour) {
          return renderTournament(ctrl, tour);
        }));
    })
  ]);
};
