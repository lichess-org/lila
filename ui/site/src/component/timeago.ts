import { siteTrans } from './trans';

type DateLike = Date | number | string;

interface ElementWithDate extends Element {
  lichessDate: Date;
}

const NEXT = [
  9, // start showing seconds after 9 seconds
  60, // minutes
  60 * 60, // hours
  60 * 60 * 48, // start showing days after 48 hours
  60 * 60 * 24 * 7, // weeks
  (60 * 60 * 24 * 365) / 12, // months
  60 * 60 * 24 * 365, // years
];

const DIVS = [
  1, // just now
  1, // seconds
  60, // minutes
  60 * 60, // hours
  60 * 60 * 24, // days
  60 * 60 * 24 * 7, // weeks
  (60 * 60 * 24 * 365) / 12, // months
  60 * 60 * 24 * 365, // years
];

const I18N_KEYS = [
  ['rightNow', 'justNow'],
  ['nbSecondsAgo', 'inNbSeconds'],
  ['nbMinutesAgo', 'inNbMinutes'],
  ['nbHoursAgo', 'inNbHours'],
  ['nbDaysAgo', 'inNbDays'],
  ['nbWeeksAgo', 'inNbWeeks'],
  ['nbMonthsAgo', 'inNbMonths'],
  ['nbYearsAgo', 'inNbYears'],
];

// format Date / string / timestamp to Date instance.
const toDate = (input: DateLike): Date =>
  input instanceof Date ? input : new Date(isNaN(input as any) ? input : parseInt(input as any));

// format the diff second to *** time ago
const formatDiff = (diff: number): string => {
  const absDiff = Math.abs(diff);
  let idx = 0;
  while (idx < NEXT.length && absDiff >= NEXT[idx]) idx++;
  return siteTrans.plural(I18N_KEYS[idx][diff < 0 ? 1 : 0], Math.floor(absDiff / DIVS[idx]));
};

let formatterInst: (date: Date) => string;

export const formatter = () =>
  (formatterInst =
    formatterInst ||
    (window.Intl && Intl.DateTimeFormat
      ? new Intl.DateTimeFormat(document.documentElement.lang, {
          year: 'numeric',
          month: 'short',
          day: 'numeric',
          hour: 'numeric',
          minute: 'numeric',
        }).format
      : d => d.toLocaleString()));

export const format = (date: DateLike) => formatDiff((Date.now() - toDate(date).getTime()) / 1000);

export const findAndRender = (parent?: HTMLElement) =>
  requestAnimationFrame(() => {
    const now = Date.now();
    [].slice.call((parent || document).getElementsByClassName('timeago'), 0, 99).forEach((node: ElementWithDate) => {
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
      if (!abs) {
        const diff = (now - node.lichessDate.getTime()) / 1000;
        node.textContent = formatDiff(diff);
        if (Math.abs(diff) > 9999) cl.remove('timeago'); // ~3h
      }
    });
  });

export const updateRegularly = (interval: number) => {
  findAndRender();
  setTimeout(() => updateRegularly(interval * 1.1), interval);
};
