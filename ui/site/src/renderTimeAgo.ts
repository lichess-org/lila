import { commonDateFormat, toDate, formatAgo, displayLocale, timeago } from 'lib/i18n';

interface ElementWithDate extends Element {
  lichessDate: Date;
}

export const renderTimeAgo = (parent?: HTMLElement): number =>
  requestAnimationFrame(() => {
    const now = Date.now();
    [].slice
      .call((parent || document).getElementsByClassName('timeago'), 0, 99)
      .forEach((node: ElementWithDate) => {
        const cl = node.classList,
          abs = cl.contains('abs'),
          set = cl.contains('set');
        node.lichessDate = node.lichessDate || toDate(node.getAttribute('datetime')!);
        if (!set) {
          const str = commonDateFormat(node.lichessDate);
          if (abs) node.textContent = str;
          else node.setAttribute('title', str);
          cl.add('set');
          if (abs || cl.contains('once')) cl.remove('timeago');
        }
        if (cl.contains('remaining')) {
          const diff = (node.lichessDate.getTime() - now) / 1000;
          node.textContent = formatRemaining(diff);
        } else if (!abs) {
          const diff = (now - node.lichessDate.getTime()) / 1000;
          node.textContent = formatAgo(diff);
          if (Math.abs(diff) > 9999) cl.remove('timeago'); // ~3h
        }
        if (site.blindMode) {
          node.removeAttribute('title');
          node.removeAttribute('datetime');
        }
      });
  });

export const updateTimeAgo = (interval: number): void => {
  renderTimeAgo();
  setTimeout(() => updateTimeAgo(interval * 1.1), interval);
};

export const renderLocalizedTimestamps = (): void => {
  requestAnimationFrame(() => {
    [].slice.call(document.querySelectorAll('time[format]'), 0, 99).forEach((node: HTMLElement) => {
      const format = node.getAttribute('format');
      if (format) {
        const date = toDate(node.getAttribute('datetime')!);
        node.textContent = datetimeFormat(date, format);
      }
    });
  });
};

const discordFormats: { [key: string]: Intl.DateTimeFormatOptions } = {
  d: { year: 'numeric', month: '2-digit', day: '2-digit' }, // 12/31/2025
  D: { year: 'numeric', month: 'long', day: 'numeric' }, // December 31st, 2025
  t: { hour: 'numeric', minute: 'numeric' }, // 6:26 PM
  T: { hour: 'numeric', minute: 'numeric', second: 'numeric' }, // 6:26:00 PM
  f: {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: 'numeric',
    minute: 'numeric',
  }, // December 31st, 2025 at 6:26 PM
  F: {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: 'numeric',
    minute: 'numeric',
  }, // Wednesday, December 31st, 2025 at 6:26 PM
  s: {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: 'numeric',
    minute: 'numeric',
  }, // 12/31/2025, 6:26 PM
  S: {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: 'numeric',
    minute: 'numeric',
    second: 'numeric',
  }, // 12/31/2025, 6:26:00 PM
};

const datetimeFormat = (date: Date, formatStr: string): string => {
  if (formatStr === 'R') {
    return timeago(date);
  }

  const fmt = discordFormats[formatStr];

  if (fmt) {
    const formatter = new Intl.DateTimeFormat(displayLocale, fmt);
    return formatter.format(date);
  }

  return date.toString();
};

// format the diff second to *** time remaining
const formatRemaining = (seconds: number): string =>
  seconds < 1
    ? i18n.timeago.completed
    : seconds < 3600
      ? i18n.timeago.nbMinutesRemaining(Math.floor(seconds / 60))
      : i18n.timeago.nbHoursRemaining(Math.floor(seconds / 3600));
