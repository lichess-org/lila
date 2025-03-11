// for many users, using the islamic calendar is not practical on the internet
// due to international context, so we make sure it's displayed using the gregorian calendar
export const displayLocale: string = document.documentElement.lang.startsWith('ar-')
  ? 'ar-ly'
  : document.documentElement.lang;

const commonDateFormatter = new Intl.DateTimeFormat(displayLocale, {
  year: 'numeric',
  month: 'short',
  day: 'numeric',
  hour: 'numeric',
  minute: 'numeric',
});

export const commonDateFormat: (d?: Date | number) => string = commonDateFormatter.format;

export const timeago: (d: DateLike) => string = (date: DateLike) =>
  formatAgo((Date.now() - toDate(date).getTime()) / 1000);

// format Date / string / timestamp to Date instance.
export const toDate = (input: DateLike): Date =>
  input instanceof Date ? input : new Date(isNaN(input as any) ? input : parseInt(input as any));

export const use24h = (): boolean => !commonDateFormatter.resolvedOptions().hour12;

// format the diff second to *** time ago
export const formatAgo = (seconds: number): string => {
  const absSeconds = Math.abs(seconds);
  const strIndex = seconds < 0 ? 1 : 0;
  const unit = agoUnits.find(unit => absSeconds >= unit[2] * unit[3] && unit[strIndex])!;
  const fmt = i18n.timeago[unit[strIndex]!];
  return typeof fmt === 'string' ? fmt : fmt(Math.floor(absSeconds / unit[2]));
};

type DateLike = Date | number | string;

// past, future, divisor, at least
const agoUnits: [keyof I18n['timeago'] | undefined, keyof I18n['timeago'], number, number][] = [
  ['nbYearsAgo', 'inNbYears', 60 * 60 * 24 * 365, 1],
  ['nbMonthsAgo', 'inNbMonths', (60 * 60 * 24 * 365) / 12, 1],
  ['nbWeeksAgo', 'inNbWeeks', 60 * 60 * 24 * 7, 1],
  ['nbDaysAgo', 'inNbDays', 60 * 60 * 24, 2],
  ['nbHoursAgo', 'inNbHours', 60 * 60, 1],
  ['nbMinutesAgo', 'inNbMinutes', 60, 1],
  [undefined, 'inNbSeconds', 1, 9],
  ['rightNow', 'justNow', 1, 0],
];

let numberFormatter: false | Intl.NumberFormat | null = false;

export const numberFormat = (n: number): string => {
  if (numberFormatter === false)
    numberFormatter = window.Intl && Intl.NumberFormat ? new Intl.NumberFormat() : null;
  if (numberFormatter === null) return '' + n;
  return numberFormatter.format(n);
};

export const numberSpread = (el: HTMLElement, nbSteps: number, duration: number, previous: number) => {
  let displayed: string;
  const display = (prev: number, cur: number, it: number) => {
    const val = numberFormat(Math.round((prev * (nbSteps - 1 - it) + cur * (it + 1)) / nbSteps));
    if (val !== displayed) {
      el.textContent = val;
      displayed = val;
    }
  };
  let timeouts: Timeout[] = [];
  return (nb: number, overrideNbSteps?: number): void => {
    if (!el || (!nb && nb !== 0)) return;
    if (overrideNbSteps) nbSteps = Math.abs(overrideNbSteps);
    timeouts.forEach(clearTimeout);
    timeouts = [];
    const prev = previous === 0 ? 0 : previous || nb;
    previous = nb;
    const interv = Math.abs(duration / nbSteps);
    for (let i = 0; i < nbSteps; i++)
      timeouts.push(setTimeout(display.bind(null, prev, nb, i), Math.round(i * interv)));
  };
};
