import { Attrs, h, type VNode } from 'snabbdom';
import * as licon from 'common/licon';
import { spinnerVdom as spinner } from 'common/spinner';
import { bind, dataIcon, MaybeVNode, onInsert } from 'common/snabbdom';
import type TournamentController from '../ctrl';
import { bindMobileMousedown } from 'common/device';
import { onClickAway, Toggle } from 'common';

function orJoinSpinner(ctrl: TournamentController, f: () => VNode): VNode {
  return ctrl.joinSpinner ? spinner() : f();
}

export function withdraw(ctrl: TournamentController): VNode {
  return orJoinSpinner(ctrl, () => {
    const pause = ctrl.data.isStarted;
    return h(
      'button.fbt.text',
      {
        attrs: dataIcon(pause ? licon.Pause : licon.FlagOutline),
        hook: bind('click', ctrl.withdraw, ctrl.redraw),
      },
      i18n.site[pause ? 'pause' : 'withdraw'],
    );
  });
}

export function join(ctrl: TournamentController): VNode {
  return orJoinSpinner(ctrl, () => {
    const delay = ctrl.data.me && ctrl.data.me.pauseDelay;
    const joinable = ctrl.data.verdicts.accepted && !delay;
    const button = h(
      'button.fbt.text' + (joinable ? '.highlight' : ''),
      {
        attrs: { disabled: !joinable, 'data-icon': licon.PlayTriangle },
        hook: bind('click', _ => ctrl.join(), ctrl.redraw),
      },
      i18n.site.join,
    );
    return delay
      ? h('div.delay-wrap', { attrs: { title: 'Waiting to be able to re-join the tournament' } }, [
          h(
            'div.delay',
            {
              hook: {
                insert(vnode) {
                  const el = vnode.elm as HTMLElement;
                  el.style.animation = `tour-delay ${delay}s linear`;
                  setTimeout(() => {
                    if (delay === ctrl.data.me!.pauseDelay) {
                      ctrl.data.me!.pauseDelay = 0;
                      ctrl.redraw();
                    }
                  }, delay * 1000);
                },
              },
            },
            button,
          ),
        ])
      : button;
  });
}

export function joinWithdraw(ctrl: TournamentController): VNode | undefined {
  if (!ctrl.opts.userId)
    return h(
      'a.fbt.text.highlight',
      { attrs: { href: '/login?referrer=' + window.location.pathname, 'data-icon': licon.PlayTriangle } },
      i18n.site.signIn,
    );
  if (!ctrl.data.isFinished) return ctrl.isIn() ? withdraw(ctrl) : join(ctrl);
  return undefined;
}

export function calendarMenuToggleButton(toggle: Toggle): VNode {
  return h('button.fbt', {
    class: { active: toggle() },
    attrs: { 'data-icon': licon.CalendarPlus },
    hook: onInsert(bindMobileMousedown(toggle.toggle)),
  });
}

export function calendarMenu(ctrl: TournamentController): MaybeVNode {
  const toggle = ctrl.calendarMenu;
  const d = ctrl.data;
  const title = encodeURIComponent(d.fullName);
  const details = encodeURIComponent(`https://lichess.org/tournament/${d.id}`);
  const startDate = new Date(d.startsAt);
  const finishDate = new Date(d.finishesAt);

  function formatDateToYYYYMMDDTHHMMSSZ(date: Date): string {
    const year = date.getUTCFullYear();
    const month = String(date.getUTCMonth() + 1).padStart(2, '0'); // Months are 0-indexed
    const day = String(date.getUTCDate()).padStart(2, '0');
    const hours = String(date.getUTCHours()).padStart(2, '0');
    const minutes = String(date.getUTCMinutes()).padStart(2, '0');
    const seconds = String(date.getUTCSeconds()).padStart(2, '0');
    return `${year}${month}${day}T${hours}${minutes}${seconds}Z`;
  }

  const googleCalendarLink = () => {
    return `https://calendar.google.com/calendar/render?action=TEMPLATE&text=${title}&dates=${formatDateToYYYYMMDDTHHMMSSZ(startDate)}/${formatDateToYYYYMMDDTHHMMSSZ(finishDate)}&details=${details}`;
  };

  const outlookLink = () => {
    return `https://outlook.live.com/calendar/deeplink/compose?path=/calendar/action/compose&rru=addevent&startdt=${startDate.toISOString()}&enddt=${finishDate.toISOString()}&subject=${title}&body=${details}`;
  };

  const yahooLink = () => {
    return `https://calendar.yahoo.com/?v=60&TITLE=${title}&ST=${formatDateToYYYYMMDDTHHMMSSZ(startDate)}&ET=${formatDateToYYYYMMDDTHHMMSSZ(finishDate)}&DESC=${details}`;
  };

  const icsLink = () => {
    const icsContent = `BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//lichess.org//lichess.org//EN
BEGIN:VEVENT
SUMMARY:${d.fullName}
DESCRIPTION:https://lichess.org/tournament/${d.id}
DTSTART:${formatDateToYYYYMMDDTHHMMSSZ(startDate)}
DTEND:${formatDateToYYYYMMDDTHHMMSSZ(finishDate)}
END:VEVENT
END:VCALENDAR`;
    const blob = new Blob([icsContent], { type: 'text/calendar' });
    const dataURI = URL.createObjectURL(blob);
    return dataURI;
  };

  const links = [
    { text: i18n.site.addToGoogleCalendar, link: googleCalendarLink() },
    { text: i18n.site.addToOutlook, link: outlookLink() },
    { text: i18n.site.addToYahooCalendar, link: yahooLink() },
    { text: i18n.site.downloadICalendar, link: icsLink(), download: 'event.ics' },
  ];

  return toggle()
    ? h(
        'div.calendar-menu',
        { hook: onInsert(onClickAway(() => toggle(false))) },
        links.map(link => {
          const attrs: Attrs = { target: '_blank' };
          if (link.link) {
            attrs.href = link.link;
          }
          if (link.download) {
            attrs.download = link.download;
          }
          return h('section', [h('a', { attrs: attrs }, link.text)]);
        }),
      )
    : undefined;
}
