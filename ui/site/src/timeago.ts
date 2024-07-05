import { memoize } from 'common';
import { siteTrans } from './trans';

type DateLike = Date | number | string;

interface ElementWithDate extends Element {
  lichessDate: Date;
}

// past, future, divisor, at least
const agoUnits: [string | undefined, string, number, number][] = [
  ['nbYearsAgo', 'inNbYears', 60 * 60 * 24 * 365, 1],
  ['nbMonthsAgo', 'inNbMonths', (60 * 60 * 24 * 365) / 12, 1],
  ['nbWeeksAgo', 'inNbWeeks', 60 * 60 * 24 * 7, 1],
  ['nbDaysAgo', 'inNbDays', 60 * 60 * 24, 2],
  ['nbHoursAgo', 'inNbHours', 60 * 60, 1],
  ['nbMinutesAgo', 'inNbMinutes', 60, 1],
  [undefined, 'inNbSeconds', 1, 9],
  ['rightNow', 'justNow', 1, 0],
];

// format Date / string / timestamp to Date instance.
const toDate = (input: DateLike): Date =>
  input instanceof Date ? input : new Date(isNaN(input as any) ? input : parseInt(input as any));

// format the diff second to *** time ago
const formatAgo = (seconds: number): string => {
  const absSeconds = Math.abs(seconds);
  const strIndex = seconds < 0 ? 1 : 0;
  const unit = agoUnits.find(unit => absSeconds >= unit[2] * unit[3] && unit[strIndex])!;
  return siteTrans.pluralSame(unit[strIndex]!, Math.floor(absSeconds / unit[2]));
};

// format the diff second to *** time remaining
const formatRemaining = (seconds: number): string =>
  seconds < 1
    ? siteTrans.noarg('completed')
    : seconds < 3600
    ? siteTrans.pluralSame('nbMinutesRemaining', Math.floor(seconds / 60))
    : siteTrans.pluralSame('nbHoursRemaining', Math.floor(seconds / 3600));

// for many users, using the islamic calendar is not practical on the internet
// due to international context, so we make sure it's displayed using the gregorian calendar
export const displayLocale = document.documentElement.lang.startsWith('ar-')
  ? 'ar-ly'
  : document.documentElement.lang;

export const formatter = memoize(() =>
  window.Intl
    ? new Intl.DateTimeFormat(displayLocale, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: 'numeric',
        minute: 'numeric',
      }).format
    : (d: Date) => d.toLocaleString(),
);

export const format = (date: DateLike) => formatAgo((Date.now() - toDate(date).getTime()) / 1000);

export const findAndRender = (parent?: HTMLElement) =>
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
          const str = formatter()(node.lichessDate);
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
      });
  });

export const updateRegularly = (interval: number) => {
  findAndRender();
  setTimeout(() => updateRegularly(interval * 1.1), interval);
};
