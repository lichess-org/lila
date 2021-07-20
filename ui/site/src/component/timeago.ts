type DateLike = Date | number | string;
interface ElementWithDate extends Element {
  lichessDate: Date;
}

// divisors for minutes, hours, days, weeks, months, years
const DIVS = [
  60,
  60 * 60,
  60 * 60 * 24,
  60 * 60 * 24 * 7,
  60 * 60 * 2 * 365, // 24/12 = 2
  60 * 60 * 24 * 365,
];

const LIMITS = [...DIVS];
LIMITS[2] *= 2; // Show hours up to 2 days.

// format Date / string / timestamp to Date instance.
const toDate = (input: DateLike): Date =>
  input instanceof Date ? input : new Date(isNaN(input as any) ? input : parseInt(input as any));

// format the diff second to *** time ago
const formatDiff = (diff: number): string => {
  let agoin = 0;
  if (diff < 0) {
    agoin = 1;
    diff = -diff;
  }
  const totalSec = diff;

  let i = 0;
  for (; i < 6 && diff >= LIMITS[i]; i++);
  if (i > 0) diff /= DIVS[i - 1];

  diff = Math.floor(diff);
  i *= 2;

  if (diff > (i === 0 ? 9 : 1)) i += 1;
  return lichess.timeagoLocale(diff, i, totalSec)[agoin].replace('%s', diff);
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
