var m = require('mithril');

var scale = 8;
var startTime;
var stopTime;

function leftPos(time) {
  return scale * (time-startTime) / 1000 / 60;
}

function scrollToNow(el) {
  el.scrollLeft = leftPos((new Date()).getTime() - el.clientWidth/2/scale * 60 * 1000);
}

function speedGrouper(t) {
  if (t.schedule && t.schedule.speed === 'superblitz') {
    return t.perf.index - 0.5;
  } else {
    return t.perf.index;
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
    return !(tour1.finishesAt < tour2.startsAt ||
             tour2.finishesAt < tour1.startsAt);
  });
}

// splits lanes that have collisions, but to keep
// groups separate it doesn't compact existing lanes
function fixCollisions(lanes) {
  var ret = [];
  lanes.forEach(function(lane) {
    var newLanes = [[]];
    lane.forEach(function(tour) {
      var collision = true;
      for(var i = 0; i < newLanes.length; i++) {
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

function renderTournament(ctrl, tour) {
  return m('div.tournament', {
    style: "width: " + tour.minutes * scale + 'px;' +
      'left: ' + leftPos(tour.startsAt) + 'px;',
      class: (tour.schedule ? tour.schedule.freq : 'unscheduled') +
        (tour.rated ? ' rated ' : ' unrated ') +
        (tour.minutes <= 30 ? ' short ' : '') +
        (tour.position ? ' thematic ' : '')
  }, [
  m('div.icon', tour.perf ? {'data-icon': tour.perf.icon, title: tour.perf.name} : null),
  m('span', [
    m('div.name', m('a', { href: "/tournament/" + tour.id}, tour.fullName)),
    m('div.clock', tour.clock.limit/60 + "+" + tour.clock.increment),
    tour.rated ? null : m('div.description', ctrl.trans('casual')),
    tour.position ? m('div.description', 'Thematic') : null,
    m('div.nb-players.text', {'data-icon': 'r'}, tour.nbPlayers),
  ])
  ]
  );
}

function timeString(time) {
  return ('0' + time.getHours()).slice(-2) + ":" + ('0' + time.getMinutes()).slice(-2);
}

function renderTimeline() {
  var time = new Date(startTime);
  time.setSeconds(0);
  time.setMinutes(Math.floor(time.getMinutes()/10) * 10);
  var times = [];
  while (time.getTime() < stopTime) {
    time.setMinutes(time.getMinutes() + 10);
    times.push(m('div.timeheader', {
      style: 'left: ' + leftPos(time.getTime()) + 'px;'
    }, timeString(time)));
  }
  return m('div.timeline',
      times,
      m('div.timeheader.now', {
        style: 'left: ' + leftPos(new Date().getTime()) + 'px;'
      }, '')
  );
}

module.exports = function(ctrl) {
  startTime = (new Date()).getTime() - 3 * 60 * 60 * 1000;
  stopTime = startTime + 6 * 60 * 60 * 1000;

  var tours = ctrl.data.finished.concat(ctrl.data.started).concat(ctrl.data.created);

  return m('div.schedule', {
    config: function(el, isUpdate) {
      if (!isUpdate) scrollToNow(el);
    }
  }, [
    renderTimeline(),
    fixCollisions(group(tours, speedGrouper)).map(function(lane) {
      return m('div.tournamentline',
          lane.map(function(tour) { return renderTournament(ctrl, tour); }));
    })
  ]);
};
