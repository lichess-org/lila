import { commonDateFormat, toDate, formatAgo } from 'common/i18n';

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
      });
  });

export const updateTimeAgo = (interval: number): void => {
  renderTimeAgo();
  setTimeout(() => updateTimeAgo(interval * 1.1), interval);
};

// format the diff second to *** time remaining
const formatRemaining = (seconds: number): string =>
  seconds < 1
    ? site.trans.noarg('completed')
    : seconds < 3600
      ? site.trans.pluralSame('nbMinutesRemaining', Math.floor(seconds / 60))
      : site.trans.pluralSame('nbHoursRemaining', Math.floor(seconds / 3600));
