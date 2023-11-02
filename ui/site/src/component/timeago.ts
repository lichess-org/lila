import { siteTrans } from './trans';

type DateLike = Date | number | string;

interface ElementWithDate extends Element {
  lichessDate: Date;
}

// past, future, divisor, at least
const units: [string, string, number, number][] = [
  ['nbYearsAgo', 'inNbYears', 60 * 60 * 24 * 365, 1],
  ['nbMonthsAgo', 'inNbMonths', (60 * 60 * 24 * 365) / 12, 1],
  ['nbWeeksAgo', 'inNbWeeks', 60 * 60 * 24 * 7, 1],
  ['nbDaysAgo', 'inNbDays', 60 * 60 * 24, 2],
  ['nbHoursAgo', 'inNbHours', 60 * 60, 1],
  ['nbMinutesAgo', 'inNbMinutes', 60, 1],
  ['nbSecondsAgo', 'inNbSeconds', 1, 9],
  ['rightNow', 'justNow', 1, 0],
];

// format Date / string / timestamp to Date instance.
const toDate = (input: DateLike): Date =>
  input instanceof Date ? input : new Date(isNaN(input as any) ? input : parseInt(input as any));

// format the diff second to *** time ago
const formatDiff = (seconds: number): string => {
  const absSeconds = Math.abs(seconds);
  const unit = units.find(unit => absSeconds >= unit[2] * unit[3])!;
  return siteTrans.pluralSame(unit[seconds < 0 ? 1 : 0], Math.floor(absSeconds / unit[2]));
};

let formatterInst: (date: Date) => string;

const newFormatter = () => {
  const options: Intl.DateTimeFormatOptions = {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: 'numeric',
  };
  // for many users, using the islamic calendar is not practical on the internet
  // due to international context, so we make sure it's displayed using the gregorian calender
  const lang = document.documentElement.lang.startsWith('ar-') ? 'ar-ly' : document.documentElement.lang;
  return new Intl.DateTimeFormat(lang, options).format;
};

export const formatter = () =>
  (formatterInst = formatterInst || (window.Intl ? newFormatter() : d => d.toLocaleString()));

export const format = (date: DateLike) => formatDiff((Date.now() - toDate(date).getTime()) / 1000);

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
