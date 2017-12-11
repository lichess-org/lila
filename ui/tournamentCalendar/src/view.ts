import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { eachDay, addYears, addDays, format, getHours, getMinutes, areRangesOverlapping } from 'date-fns'
import { Tournament, Lanes, Ctrl } from './interfaces'

function displayClockLimit(limit) {
  switch (limit) {
    case 15:
      return '¼';
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

function tournamentClass(tour: Tournament, day: Date) {
  const classes = {
    rated: tour.rated,
    casual: !tour.rated,
    'max-rating': tour.hasMaxRating,
    yesterday: tour.bounds.start < day
  };
  if (tour.schedule) classes[tour.schedule.freq] = true;
  return classes;
}

function iconOf(tour, perfIcon) {
  return (tour.schedule && tour.schedule.freq === 'shield') ? '5' : perfIcon;
}

function renderTournament(ctrl: Ctrl, tour: Tournament, day: Date) {
  const paddingLeft = 0;
  let left = (getHours(tour.bounds.start) + getMinutes(tour.bounds.start) / 60) / 24 * 100;
  if (tour.bounds.start < day) left -= 100;
  const width = tour.minutes / 60 / 24 * 100;

  return h('a.tournament', {
    class: tournamentClass(tour, day),
    attrs: {
      href: '/tournament/' + tour.id,
      style: 'width: ' + width + '%; left: ' + left + '%',
      title: `${tour.fullName} - ${format(tour.bounds.start, 'DD/MM/YYYY HH:mm')}`
    }
  }, [
    h('span.icon', tour.perf ? {
      attrs: {
        'data-icon': iconOf(tour, tour.perf.icon)
      }
    } : {}),
    h('span.body', [ tour.fullName ])
  ]);
}

function renderLane(ctrl: Ctrl, tours: Tournament[], day: Date) {
  return h('lane', tours.map(t => renderTournament(ctrl, t, day)));
}

function fitLane(lane: Tournament[], tour2: Tournament) {
  return !lane.some(tour1 => {
    return areRangesOverlapping(tour1.bounds.start, tour1.bounds.end, tour2.bounds.start, tour2.bounds.end);
  });
}

function makeLanes(tours: Tournament[]): Lanes {
  const lanes: Lanes = [];
  tours.forEach(t => {
    let lane = lanes.find(l => fitLane(l, t));
    if (lane) lane.push(t);
    else lanes.push([t]);
  });
  return lanes;
}

function renderDay(ctrl: Ctrl) {
  return function(day: Date): VNode | undefined {
    const dayEnd = addDays(day, 1);
    const tours = ctrl.data.tournaments.filter(t =>
      t.bounds.start < dayEnd && t.bounds.end > day
    );
    return h('day', [
      h('date', [format(day, 'DD/MM')]),
      h('lanes', makeLanes(tours).map(l => renderLane(ctrl, l, day)))
    ]);
  }
}

function renderTimeline() {
  const hours: number[] = [];
  for (let i = 0; i < 24; i++) hours.push(i);
  return h('div.timeline', hours.map(hour =>
    h('div.timeheader', {
      attrs: { style: 'left: ' + (hour / 24 * 100) + '%' }
    }, timeString(hour))
  ));
}

// converts Date to "%H:%M" with leading zeros
function timeString(hour) {
  return ('0' + hour).slice(-2);
}

export default function(ctrl) {
  return h('div#tournament_calendar', [
    renderTimeline(),
    h('days', eachDay(new Date(ctrl.data.since), new Date(ctrl.data.to)).map(renderDay(ctrl)))
  ]);
}
