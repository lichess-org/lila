import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { eachDay, addYears, addDays, format, getHours, getMinutes } from 'date-fns'
import { Tournament, Ctrl } from './interfaces'

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

function tournamentClass(tour: Tournament) {
  const classes = {
    rated: tour.rated,
    casual: !tour.rated,
    'max-rating': tour.hasMaxRating
  };
  if (tour.schedule) classes[tour.schedule.freq] = true;
  return classes;
}

function iconOf(tour, perfIcon) {
  return (tour.schedule && tour.schedule.freq === 'shield') ? '5' : perfIcon;
}

function renderTournament(ctrl: Ctrl, tour: Tournament) {
  // moves content into viewport, for long tourneys and marathons
  // const paddingLeft = tour.minutes < 90 ? 0 : Math.max(0,
  //   Math.min(width - 250, // max padding, reserved text space
  //     leftPos(now) - left - 380)); // distance from Now
  //     // cut right overflow to fit viewport and not widen it, for marathons
  //     width = Math.min(width, leftPos(stopTime) - left);
  const paddingLeft = 0;
  const date = tour.startsAt;
  const left = (getHours(date) + getMinutes(date) / 60) / 24 * 100;
  const width = tour.minutes / 60 / 24 * 100;

  return h('a.tournament', {
    class: tournamentClass(tour),
    attrs: {
      href: '/tournament/' + tour.id,
      style: 'width: ' + width + '%; left: ' + left + '%'
    },
  }, [
    h('span.icon', tour.perf ? {
      attrs: {
        'data-icon': iconOf(tour, tour.perf.icon),
        title: tour.perf.name
      }
    } : {}),
    h('span.body', [ tour.fullName ])
  ]);
}

function endsAt(tour: Tournament): Date {
  return new Date(tour.startsAt + tour.minutes * 60 * 1000);
}

function renderDay(ctrl: Ctrl) {
  return function(day: Date): VNode | undefined {
    const dayEnd = addDays(day, 1);
    const tours = ctrl.data.tournaments.filter(t =>
      t.startsAt < dayEnd.getTime() && endsAt(t) > day
    );
    return h('div.day', [
      h('day', [format(day, 'DD/MM')]),
      h('tours', tours.map(t => renderTournament(ctrl, t)))
    ]);
  }
}

export default function(ctrl) {
  return h('div#tournament_calendar',
    eachDay(new Date(ctrl.data.since), new Date(ctrl.data.to)).map(renderDay(ctrl))
  );
}
