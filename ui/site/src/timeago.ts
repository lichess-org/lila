/** based on https://github.com/hustcc/timeago.js Copyright (c) 2016 hustcc License: MIT **/

// divisors for minutes, hours, days, weeks, months, years
const DIVS: number[] = [
  60,
  60 * 60,
  60 * 60 * 24,
  60 * 60 * 24 * 7,
  60 * 60 * 2 * 365, // 24/12 = 2
  60 * 60 * 24 * 365,
] as const;

const LIMITS: number[] = [...DIVS];
LIMITS[2] *= 2; // Show hours up to 2 days.

// format Date / string / timestamp to Date instance.
function toDate(input: Date | string | number) {
  return input instanceof Date
    ? input
    : new Date(Number.isNaN(input as any) ? input : Number.parseInt(input as any));
}

// format the diff second to *** time ago
function formatDiff(diff: number): string {
  let agoin = 0;
  if (diff < 0) {
    agoin = 1;
    diff = -diff;
  }
  const total_sec = diff;

  let i = 0;
  for (; i < 6 && diff >= LIMITS[i]; i++);
  if (i > 0) diff /= DIVS[i - 1];

  diff = Math.floor(diff);
  i *= 2;

  if (diff > (i === 0 ? 9 : 1)) i += 1;
  return (window.lishogi as any).timeagoLocale(diff, i, total_sec)[agoin].replace('%s', diff);
}

let formatterInst: (date?: Date | number) => string;

function formatter(): (date?: Date | number) => string {
  return (formatterInst =
    formatterInst ||
    (window.Intl && Intl.DateTimeFormat
      ? new Intl.DateTimeFormat(document.documentElement.lang, {
          year: 'numeric',
          month: 'short',
          day: 'numeric',
          hour: 'numeric',
          minute: 'numeric',
        }).format
      : (d: Date) => d.toLocaleString()));
}

export function render(nodes: HTMLElement[]): void {
  let cl,
    abs,
    set,
    str,
    diff,
    now = Date.now();
  nodes.forEach((node: HTMLElement & { date?: Date }) => {
    (cl = node.classList), (abs = cl.contains('abs')), (set = cl.contains('set'));
    node.date = node.date || toDate(node.getAttribute('datetime')!);
    if (!set) {
      str = formatter()(node.date);
      if (abs) node.textContent = str;
      else node.setAttribute('title', str);
      cl.add('set');
      if (abs || cl.contains('once')) cl.remove('timeago');
    }
    if (!abs) {
      diff = (now - node.date.getTime()) / 1000;
      node.textContent = formatDiff(diff);
      if (Math.abs(diff) > 9999) cl.remove('timeago'); // ~3h
    }
  });
}

// relative
export function format(date: Date): string {
  return formatDiff((Date.now() - toDate(date).getTime()) / 1000);
}
export function absolute(date: Date): string {
  return formatter()(toDate(date));
}
