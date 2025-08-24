import { expandMentions } from 'lib/richText';

export function initModule(): void {
  autolinkAtoms();
}

export function autolinkAtoms(el: HTMLElement = document.body): void {
  if (!el || el === once) return;
  once = el;
  for (const atom of el.querySelectorAll<HTMLElement>('.atom p, .mod-timeline__text')) {
    atom.innerHTML = expandMentions(atom.innerHTML.replace(pathMatchRe, '<a href="$1">$1</a>'));
  }
}

let once: HTMLElement;

const greedyAutoLinks = [
  'inbox',
  'forum',
  'study',
  'broadcast',
  'team',
  'tournament',
  '@',
  'insights',
  '(?:[A-Za-z0-9]{8})(?:[a-zA-Z0-9]{4})?', // game ids
];

const pathMatchRe = new RegExp(
  `(?:(?:^|\\s)https://)?(?:${location.hostname.replace('.', '\\.')})?` +
    `(/(?:${greedyAutoLinks.join('|')})(?:/|\\?|#|$)[\\w/:(&;)=#@-]*)`,
  'gi',
);
